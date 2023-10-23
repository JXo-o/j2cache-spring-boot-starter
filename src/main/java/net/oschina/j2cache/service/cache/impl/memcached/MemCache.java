package net.oschina.j2cache.service.cache.impl.memcached;

import net.oschina.j2cache.service.cache.Level2Cache;
import net.rubyeye.xmemcached.MemcachedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ClassName: MemCache
 * Package: net.oschina.j2cache.service.cache.impl.memcached
 * Description: Memcached 缓存操作封装，基于 region+_key 实现多个 Region 的缓存
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 1:31
 */
public class MemCache implements Level2Cache {

    private static final Logger log = LoggerFactory.getLogger(MemCache.class);

    private String region;
    private MemcachedClient client;

    public MemCache(String region, MemcachedClient client) {
        this.region = region;
        this.client = client;
    }

    @Override
    public boolean supportTTL() {
        return true;
    }

    @Override
    public byte[] getBytes(String key) {
        try {
            return client.get(_key(key));
        } catch (Exception e) {
            log.error("Failed to get data from memcached.", e);
        }
        return null;
    }

    @Override
    public List<byte[]> getBytes(Collection<String> keys) {
        try {
            return keys.stream().map(k -> getBytes(_key(k))).collect(Collectors.toList());//TODO 改为批量读取方式
        } catch (Exception e) {
            log.error("Failed to get data from memcached.", e);
        }
        return null;
    }

    @Override
    public void setBytes(String key, byte[] bytes) {
        setBytes(key, bytes, -1);
    }

    @Override
    public void setBytes(Map<String, byte[]> bytes) {
        setBytes(bytes, -1);
    }

    /**
     * 设置缓存数据字节数组（带有效期）
     * @param key  cache key
     * @param bytes cache data
     * @param timeToLiveInSeconds cache ttl
     */
    public void setBytes(String key, byte[] bytes, long timeToLiveInSeconds){
        try {
            client.set(_key(key), (int)timeToLiveInSeconds, bytes);
        } catch (Exception e) {
            log.error("Failed to set data to memcached.", e);
        }
    }

    /**
     * 批量设置带 TTL 的缓存数据
     * @param bytes  cache data
     * @param timeToLiveInSeconds cache ttl
     */
    public void setBytes(Map<String,byte[]> bytes, long timeToLiveInSeconds) {
        try {
            for(String key : bytes.keySet()) {
                String mkey = _key(key);
                client.set(mkey, (int) timeToLiveInSeconds, bytes.get(key));
            }
        } catch (Exception e) {
            log.error("Failed to set data to memcached.", e);
        }
    }

    @Override
    public Collection<String> keys() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void evict(String... keys) {
        if(keys.length > 0) {
            try {
                for (String key : keys)
                    client.delete(_key(key));
            } catch (Exception e) {
                log.error("Failed to set data to memcached.", e);
            }
        }
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    private String _key(String key) {
        return this.region + ":" + key;
    }

}
