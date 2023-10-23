package net.oschina.j2cache.service.cache.impl.lettuce;

import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import io.lettuce.core.support.ConnectionPoolSupport;
import net.oschina.j2cache.model.CacheObject;
import net.oschina.j2cache.model.Command;
import net.oschina.j2cache.service.cache.*;
import net.oschina.j2cache.service.cache.impl.CacheProviderHolder;
import net.oschina.j2cache.service.cluster.ClusterPolicy;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClassName: LettuceCacheProvider
 * Package: net.oschina.j2cache.service.cache.impl.lettuce
 * Description: 使用 Lettuce 进行 Redis 的操作
 * Config:
 *  lettuce.namespace =
 *  lettuce.storage = generic
 *  lettuce.scheme = redis|rediss|redis-sentinel
 *  lettuce.hosts = 127.0.0.1:6379
 *  lettuce.password =
 *  lettuce.database = 0
 *  lettuce.sentinelMasterId =
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 1:27
 */
public class LettuceCacheProvider extends RedisPubSubAdapter<String, String> implements CacheProvider, ClusterPolicy {

    private int LOCAL_COMMAND_ID = Command.genRandomSrc(); //命令源标识，随机生成，每个节点都有唯一标识

    private static final LettuceByteCodec codec = new LettuceByteCodec();

    private static AbstractRedisClient redisClient;
    GenericObjectPool<StatefulConnection<String, byte[]>> pool;
    private StatefulRedisPubSubConnection<String, String> pubsub_subscriber;
    private String storage;

    private CacheProviderHolder holder;

    private String channel;
    private String namespace;
    private int scanCount;

    private final ConcurrentHashMap<String, Level2Cache> regions = new ConcurrentHashMap();

    @Override
    public String name() {
        return "lettuce";
    }

    @Override
    public int level() {
        return CacheObject.LEVEL_2;
    }

    @Override
    public boolean isLocalCommand(Command cmd) {
        return cmd.getSrc() == LOCAL_COMMAND_ID;
    }

    @Override
    public void start(Properties props) {
        this.namespace = props.getProperty("namespace");
        this.scanCount = Integer.valueOf(props.getProperty("scanCount", "1000"));
        this.storage = props.getProperty("storage", "hash");
        this.channel = props.getProperty("channel", "j2cache");

        String scheme = props.getProperty("scheme", "redis");
        String hosts = props.getProperty("hosts", "127.0.0.1:6379");
        String password = props.getProperty("password");
        int database = Integer.parseInt(props.getProperty("database", "0"));
        String sentinelMasterId = props.getProperty("sentinelMasterId");
        String sentinelPassword = props.getProperty("sentinelPassword");
        long clusterTopologyRefreshMs = Long.valueOf(props.getProperty("clusterTopologyRefresh", "3000"));

        if("redis-cluster".equalsIgnoreCase(scheme)) {
            scheme = "redis";
            List<RedisURI> redisURIs = new ArrayList<>();
            String[] hostArray = hosts.split(",");
            for(String host : hostArray) {
                String[] redisArray = host.split(":");
                RedisURI uri = RedisURI.create(redisArray[0], Integer.valueOf(redisArray[1]));
                uri.setDatabase(database);
                uri.setPassword(password);
                uri.setSentinelMasterId(sentinelMasterId);
                redisURIs.add(uri);
            }
            redisClient = RedisClusterClient.create(redisURIs);
            ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                    //开启自适应刷新
                    .enableAdaptiveRefreshTrigger(ClusterTopologyRefreshOptions.RefreshTrigger.MOVED_REDIRECT, ClusterTopologyRefreshOptions.RefreshTrigger.PERSISTENT_RECONNECTS)
                    .enableAllAdaptiveRefreshTriggers()
                    .adaptiveRefreshTriggersTimeout(Duration.ofMillis(clusterTopologyRefreshMs))
                    //开启定时刷新,时间间隔根据实际情况修改
                    .enablePeriodicRefresh(Duration.ofMillis(clusterTopologyRefreshMs))
                    .build();
            ((RedisClusterClient)redisClient).setOptions(ClusterClientOptions.builder().topologyRefreshOptions(topologyRefreshOptions).build());
        }
        else if("redis-sentinel".equalsIgnoreCase(scheme)) {
            scheme = "redis";
            String[] hostArray = hosts.split(",");
            RedisURI.Builder builder = null;
            boolean isFirst = true;
            for(String host : hostArray) {
                String[] redisArray = host.split(":");
                if(isFirst) {
                    builder = RedisURI.Builder.sentinel(
                            redisArray[0],
                            Integer.valueOf(redisArray[1]),
                            sentinelMasterId,
                            sentinelPassword);
                    isFirst = false;
                }
                else {
                    builder.withSentinel(redisArray[0], Integer.valueOf(redisArray[1]));
                }
            }
            builder.withDatabase(database).withPassword(password);

            RedisURI uri = builder.build();
            redisClient = RedisClient.create(uri);
        }
        else {
            String[] redisArray = hosts.split(":");
            RedisURI uri = RedisURI.create(redisArray[0], Integer.valueOf(redisArray[1]));
            uri.setDatabase(database);
            uri.setPassword(password);
            redisClient = RedisClient.create(uri);
        }

        try {
            int timeout = Integer.parseInt(props.getProperty("timeout", "10000"));
            redisClient.setDefaultTimeout(Duration.ofMillis(timeout));
        }catch(Exception e){
            log.warn("Failed to set default timeout, using default 10000 milliseconds.", e);
        }

        //connection pool configurations
        GenericObjectPoolConfig<StatefulConnection<String, byte[]>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(Integer.parseInt(props.getProperty("maxTotal", "100")));
        poolConfig.setMaxIdle(Integer.parseInt(props.getProperty("maxIdle", "10")));
        poolConfig.setMinIdle(Integer.parseInt(props.getProperty("minIdle", "10")));

        pool = ConnectionPoolSupport.createGenericObjectPool(() -> {
            if(redisClient instanceof RedisClient)
                return ((RedisClient)redisClient).connect(codec);
            else if(redisClient instanceof RedisClusterClient)
                return ((RedisClusterClient)redisClient).connect(codec);
            return null;
        }, poolConfig);
    }

    @Override
    public void stop() {
        pool.close();
        regions.clear();
        redisClient.shutdown();
    }

    @Override
    public Cache buildCache(String region, CacheExpiredListener listener) {
        return regions.computeIfAbsent(this.namespace + ":" + region, v -> "hash".equalsIgnoreCase(this.storage)?
                new LettuceHashCache(this.namespace, region, pool):
                new LettuceGenericCache(this.namespace, region, pool, scanCount));
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
     * 删除本地某个缓存条目
     * @param region 区域名称
     * @param keys   缓存键值
     */
    @Override
    public void evict(String region, String... keys) {
        holder.getLevel1Cache(region).evict(keys);
    }

    /**
     * 清除本地整个缓存区域
     * @param region 区域名称
     */
    @Override
    public void clear(String region) {
        holder.getLevel1Cache(region).clear();
    }

    /**
     * Get PubSub connection
     * @return connection instance
     */
    private StatefulRedisPubSubConnection pubsub() {
        if(redisClient instanceof RedisClient)
            return ((RedisClient)redisClient).connectPubSub();
        else if(redisClient instanceof RedisClusterClient)
            return ((RedisClusterClient)redisClient).connectPubSub();
        return null;
    }

    @Override
    public void connect(Properties props, CacheProviderHolder holder) {
        long ct = System.currentTimeMillis();
        this.holder = holder;
        this.channel = props.getProperty("channel", "j2cache");
        this.publish(Command.join());

        this.pubsub_subscriber = this.pubsub();
        this.pubsub_subscriber.addListener(this);
        RedisPubSubAsyncCommands<String, String> async = this.pubsub_subscriber.async();
        async.subscribe(this.channel);

        log.info("Connected to redis channel:{}, time {}ms.", this.channel, System.currentTimeMillis()-ct);
    }

    @Override
    public void message(String channel, String message) {
        Command cmd = Command.parse(message);
        handleCommand(cmd);
    }

    @Override
    public void publish(Command cmd) {
        cmd.setSrc(LOCAL_COMMAND_ID);
        try (StatefulRedisPubSubConnection<String, String> connection = this.pubsub()){
            RedisPubSubCommands<String, String> sync = connection.sync();
            sync.publish(this.channel, cmd.json());
        }
    }

    @Override
    public void disconnect() {
        try {
            this.publish(Command.quit());
            super.unsubscribed(this.channel, 1);
        } finally {
            this.pubsub_subscriber.close();
        }
    }
}
