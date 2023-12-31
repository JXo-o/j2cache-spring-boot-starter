package net.oschina.j2cache.service.cache.impl.redis;

import net.oschina.j2cache.model.CacheObject;
import net.oschina.j2cache.service.cache.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClassName: RedisCacheProvider
 * Package: net.oschina.j2cache.service.cache.impl.redis
 * Description: Redis 缓存管理，实现对多种 Redis 运行模式的支持和自动适配，实现连接池管理等
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 1:38
 */
public class RedisCacheProvider implements CacheProvider {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheProvider.class);

    private RedisClient redisClient;
    private String namespace;
    private String storage;
    private int scanCount;

    private final ConcurrentHashMap<String, Level2Cache> regions = new ConcurrentHashMap();

    @Override
    public String name() {
        return "redis";
    }

    @Override
    public int level() {
        return CacheObject.LEVEL_2;
    }

    /**
     * 初始化 Redis 连接
     * @param props current configuration settings.
     */
    @Override
    public void start(Properties props) {
        this.scanCount = Integer.valueOf(props.getProperty("scanCount", "1000"));
        this.namespace = props.getProperty("namespace");
        this.storage = props.getProperty("storage");

        JedisPoolConfig poolConfig = RedisUtils.newPoolConfig(props, null);

        String hosts = props.getProperty("hosts", "127.0.0.1:6379");
        String mode = props.getProperty("mode", "single");
        String clusterName = props.getProperty("cluster_name");
        String password = props.getProperty("password");
        int database = Integer.parseInt(props.getProperty("database", "0"));
        boolean ssl = Boolean.valueOf(props.getProperty("ssl", "false"));

        long ct = System.currentTimeMillis();

        this.redisClient = new RedisClient.Builder()
                .mode(mode)
                .hosts(hosts)
                .password(password)
                .cluster(clusterName)
                .database(database)
                .poolConfig(poolConfig)
                .ssl(ssl)
                .newClient();

        log.info("Redis client starts with mode({}),db({}),storage({}),namespace({}),time({}ms)",
                mode,
                database,
                storage,
                namespace,
                (System.currentTimeMillis()-ct)
        );
    }

    @Override
    public void stop() {
        regions.clear();
        try {
            redisClient.close();
        } catch (IOException e) {
            log.warn("Failed to close redis connection.", e);
        }
    }

    @Override
    public Cache buildCache(String region, CacheExpiredListener listener) {
        return regions.computeIfAbsent(this.namespace+":"+region, v -> "hash".equalsIgnoreCase(this.storage)?
                new RedisHashCache(this.namespace, region, redisClient):
                new RedisGenericCache(this.namespace, region, redisClient, scanCount));
    }

    @Override
    public Cache buildCache(String region, long timeToLiveInSeconds, CacheExpiredListener listener) {
        return buildCache(region, listener);
    }

    @Override
    public Collection<CacheChannel.Region> regions() {
        return Collections.emptyList();
    }

    /**
     * 获取 Redis 客户端实例
     * @return redis client interface instance
     */
    public RedisClient getRedisClient() {
        return redisClient;
    }
}

