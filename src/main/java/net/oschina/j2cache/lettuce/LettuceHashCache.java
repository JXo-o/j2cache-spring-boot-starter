package net.oschina.j2cache.lettuce;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.sync.RedisHashCommands;
import io.lettuce.core.api.sync.RedisKeyCommands;
import org.apache.commons.pool2.impl.GenericObjectPool;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ClassName: LettuceHashCache
 * Package: net.oschina.j2cache.lettuce
 * Description: Redis 缓存操作封装，基于 Hashs 实现多个 Region 的缓存
 * Notice: 重要提示！！！  hash 存储模式无法单独对 key 设置 expire
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 1:30
 */
public class LettuceHashCache extends LettuceCache {

    public LettuceHashCache(String namespace, String region, GenericObjectPool<StatefulConnection<String, byte[]>> pool) {
        if (region == null || region.isEmpty())
            region = "_"; // 缺省region

        super.pool = pool;
        super.namespace = namespace;
        super.region = getRegionName(region);
    }

    /**
     * 在region里增加一个可选的层级,作为命名空间,使结构更加清晰
     * 同时满足小型应用,多个J2Cache共享一个redis database的场景
     *
     * @param region
     * @return
     */
    private String getRegionName(String region) {
        if (namespace != null && !namespace.trim().isEmpty())
            region = namespace + ":" + region;
        return region;
    }

    @Override
    public byte[] getBytes(String key) {
        try(StatefulConnection<String, byte[]> connection = super.connect()) {
            RedisHashCommands<String, byte[]> cmd = (RedisHashCommands)super.sync(connection);
            return cmd.hget(this.region, key);
        }
    }

    @Override
    public List<byte[]> getBytes(Collection<String> keys) {
        try(StatefulConnection<String, byte[]> connection = super.connect()) {
            RedisHashCommands<String, byte[]> cmd = (RedisHashCommands)super.sync(connection);
            return cmd.hmget(this.region, keys.stream().toArray(String[]::new)).stream().map(kv -> kv.hasValue()?kv.getValue():null).collect(Collectors.toList());
        }
    }

    @Override
    public void setBytes(String key, byte[] bytes) {
        try(StatefulConnection<String, byte[]> connection = super.connect()) {
            RedisHashCommands<String, byte[]> cmd = (RedisHashCommands)super.sync(connection);
            cmd.hset(this.region, key, bytes);
        }
    }

    @Override
    public void setBytes(Map<String, byte[]> bytes) {
        try(StatefulConnection<String, byte[]> connection = super.connect()) {
            RedisHashCommands<String, byte[]> cmd = (RedisHashCommands)super.sync(connection);
            cmd.hmset(this.region, bytes);
        }
    }

    @Override
    public Collection<String> keys() {
        try(StatefulConnection<String, byte[]> connection = super.connect()) {
            RedisHashCommands<String, byte[]> cmd = (RedisHashCommands)super.sync(connection);
            return cmd.hkeys(this.region);
        }
    }

    @Override
    public void evict(String... keys) {
        try(StatefulConnection<String, byte[]> connection = super.connect()) {
            RedisHashCommands<String, byte[]> cmd = (RedisHashCommands)super.sync(connection);
            cmd.hdel(this.region, keys);
        }
    }

    @Override
    public void clear() {
        try(StatefulConnection<String, byte[]> connection = super.connect()) {
            RedisKeyCommands<String, byte[]> cmd = (RedisKeyCommands)super.sync(connection);
            cmd.del(this.region);
        }
    }
}