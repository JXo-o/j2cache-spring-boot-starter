package net.oschina.j2cache.service.cache.impl.redis;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.oschina.j2cache.exception.CacheException;
import net.oschina.j2cache.service.cache.Level2Cache;

import redis.clients.jedis.commands.JedisBinaryCommands;
import redis.clients.jedis.commands.KeyBinaryCommands;
import redis.clients.jedis.commands.StringBinaryCommands;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

/**
 * ClassName: RedisGenericCache
 * Package: net.oschina.j2cache.service.cache.impl.redis
 * Description: Redis 缓存操作封装，基于 region+_key 实现多个 Region 的缓存
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 1:39
 */
public class RedisGenericCache implements Level2Cache {

    private final static Logger log = LoggerFactory.getLogger(RedisGenericCache.class);

    private String namespace;
    private String region;
    private RedisClient client;
    private int scanCount;

    /**
     * 缓存构造
     * @param namespace 命名空间，用于在多个实例中避免 _key 的重叠
     * @param region 缓存区域的名称
     * @param client 缓存客户端接口
     */
    public RedisGenericCache(String namespace, String region, RedisClient client, int scanCount) {
        if (region == null || region.isEmpty())
            region = "_"; // 缺省region

        this.client = client;
        this.namespace = namespace;
        this.region = _regionName(region);
        this.scanCount = scanCount;
    }

    @Override
    public boolean supportTTL() {
        return true;
    }

    /**
     * 在region里增加一个可选的层级,作为命名空间,使结构更加清晰
     * 同时满足小型应用,多个J2Cache共享一个redis database的场景
     *
     * @param region
     * @return
     */
    private String _regionName(String region) {
        if (namespace != null && !namespace.trim().isEmpty())
            region = namespace + ":" + region;
        return region;
    }

    private byte[] _key(String key) {
        try {
            return (this.region + ":" + key).getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            return (this.region + ":" + key).getBytes();
        }
    }

    @Override
    public byte[] getBytes(String key) {
        try {
            return client.get().get(_key(key));
        } finally {
            client.release();
        }
    }

    @Override
    public List<byte[]> getBytes(Collection<String> keys) {
        try {
            JedisBinaryCommands cmd = client.get();
            if(cmd instanceof StringBinaryCommands) {
                byte[][] bytes = keys.stream().map(k -> _key(k)).toArray(byte[][]::new);
                return ((StringBinaryCommands)cmd).mget(bytes);
            }
            return keys.stream().map(k -> getBytes(k)).collect(Collectors.toList());
        } finally {
            client.release();
        }
    }

    @Override
    public void setBytes(String key, byte[] bytes) {
        try {
            client.get().set(_key(key), bytes);
        } finally {
            client.release();
        }
    }

    @Override
    public void setBytes(Map<String,byte[]> bytes) {
        try {
            JedisBinaryCommands cmd = client.get();
            if(cmd instanceof StringBinaryCommands) {
                byte[][] data = new byte[bytes.size() * 2][];
                int idx = 0;
                for(String key : bytes.keySet()){
                    data[idx++] = _key(key);
                    data[idx++] = bytes.get(key);
                }
                ((StringBinaryCommands)cmd).mset(data);
            }
            else
            bytes.forEach((k,v) -> setBytes(k, v));
        } finally {
            client.release();
        }
    }

    @Override
    public void setBytes(String key, byte[] bytes, long timeToLiveInSeconds) {
        if (timeToLiveInSeconds <= 0) {
            log.debug(String.format("Invalid timeToLiveInSeconds value : %d , skipped it.", timeToLiveInSeconds));
            setBytes(key, bytes);
        }
        else {
            try {
                client.get().setex(_key(key), (int) timeToLiveInSeconds, bytes);
            } finally {
                client.release();
            }
        }
    }

    @Override
    public void setBytes(Map<String,byte[]> bytes, long timeToLiveInSeconds) {
        try {
            /* 为了支持 TTL ，没法使用批量写入方法 */
            /*
            BinaryJedisCommands cmd = client.get();
            if(cmd instanceof MultiKeyBinaryCommands) {
                byte[][] data = new byte[bytes.size() * 2][];
                int idx = 0;
                for(String key : bytes.keySet()){
                    data[idx++] = _key(key);
                    data[idx++] = bytes.get(key);
                }
                ((MultiKeyBinaryCommands)cmd).mset(data);
            }
            else
            */

            if (timeToLiveInSeconds <= 0) {
                log.debug(String.format("Invalid timeToLiveInSeconds value : %d , skipped it.", timeToLiveInSeconds));
                setBytes(bytes);
            }
            else
                bytes.forEach((k,v) -> setBytes(k, v, timeToLiveInSeconds));
        } finally {
            client.release();
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return client.get().exists(_key(key));
        } finally {
            client.release();
        }
    }

    /**
     * 1、线上redis服务大概率会禁用或重命名keys命令；
     * 2、keys命令效率太低容易致使redis宕机；
     * 所以使用scan命令替换keys命令操作，增加可用性及提升执行性能
     */
    @Override
    public Collection<String> keys() {
        try {
            JedisBinaryCommands cmd = client.get();
            if (cmd instanceof StringBinaryCommands) {
                Collection<String> keys = keys(cmd);

                return keys.stream().map(k -> k.substring(this.region.length()+1)).collect(Collectors.toList());
            }
        } finally {
            client.release();
        }
        throw new CacheException("keys() not implemented in Redis Generic Mode");
    }

    private Collection<String> keys(JedisBinaryCommands cmd) {
        Collection<String> keys = new ArrayList<>();
        String cursor = "0";
        ScanParams scanParams = new ScanParams();
        scanParams.match(this.region + ":*");
        scanParams.count(scanCount); // 这个不是返回结果的数量，应该是每次scan的数量
        ScanResult<byte[]> scan = ((KeyBinaryCommands) cmd).scan(cursor.getBytes(StandardCharsets.UTF_8), scanParams);
        while (null != scan.getCursor()) {
            keys.addAll(convertByteListToStringList(scan.getResult())); // 这一次scan match到的结果
            if (!StringUtils.equals(cursor, scan.getCursor())) { // 不断拿着新的cursor scan，最终会拿到所有匹配的值
                scan = ((KeyBinaryCommands) cmd).scan(scan.getCursor().getBytes(StandardCharsets.UTF_8), scanParams);
                continue;
            } else {
                break;
            }
        }
        return keys;
    }

    public static List<String> convertByteListToStringList(List<byte[]> byteList) {
        List<String> stringList = new ArrayList<>();
        for (byte[] bytes : byteList) {
            String encodedString = new String(bytes, StandardCharsets.UTF_8);
            stringList.add(encodedString);
        }
        return stringList;
    }

    @Override
    public void evict(String...keys) {
        try {
            JedisBinaryCommands cmd = client.get();
            if (cmd instanceof KeyBinaryCommands) {
                byte[][] bytes = Arrays.stream(keys).map(k -> _key(k)).toArray(byte[][]::new);
                ((KeyBinaryCommands)cmd).del(bytes);
            }
            else {
                for (String key : keys)
                    cmd.del(_key(key));
            }
            for (String key : keys)
                cmd.del(_key(key));
        } finally {
            client.release();
        }
    }

    /**
     * 已使用scan命令替换keys命令操作
     */
    @Override
    public void clear() {
        try {
            JedisBinaryCommands cmd = client.get();
            if (cmd instanceof KeyBinaryCommands) {
                Collection<String> keysCollection = keys(cmd);
                String[] keys = keysCollection.stream().toArray(String[]::new);
                if (keys != null && keys.length > 0)
                    ((KeyBinaryCommands) cmd).del(convertStringArrayToByteArrayArray(keys));
            }
            else
                throw new CacheException("clear() not implemented in Redis Generic Mode");
        } finally {
            client.release();
        }
    }

    public static byte[][] convertStringArrayToByteArrayArray(String[] stringArray) {
        byte[][] byteArrayArray = new byte[stringArray.length][];
        try {
            for (int i = 0; i < stringArray.length; i++) {
                byteArrayArray[i] = stringArray[i].getBytes(StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            // Handle the exception
            e.printStackTrace();
        }
        return byteArrayArray;
    }

}