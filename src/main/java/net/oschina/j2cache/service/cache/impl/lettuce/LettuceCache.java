package net.oschina.j2cache.service.cache.impl.lettuce;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.BaseRedisCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import net.oschina.j2cache.exception.CacheException;
import net.oschina.j2cache.service.cache.Level2Cache;
import org.apache.commons.pool2.impl.GenericObjectPool;

/**
 * ClassName: LettuceCache
 * Package: net.oschina.j2cache.service.cache.impl.lettuce
 * Description: Lettuce 的基类，封装了普通 Redis 连接和集群 Redis 连接的差异
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 1:26
 */
public abstract class LettuceCache implements Level2Cache {

    protected String namespace;
    protected String region;
    protected GenericObjectPool<StatefulConnection<String, byte[]>> pool;
    protected int scanCount;
    private SyncStrategy strategy;

    protected StatefulConnection connect() {
        try {
            setStrategy(pool.borrowObject());
            return pool.borrowObject();
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    /**
     * 策略模式进行处理
     * */

    protected void setStrategy(StatefulConnection conn) {
        if (conn instanceof StatefulRedisClusterConnection) {
            strategy = new ClusterSyncStrategy();
        } else if (conn instanceof StatefulRedisConnection) {
            strategy = new SingleNodeSyncStrategy();
        } else {
            strategy = null;
        }
    }

    protected BaseRedisCommands sync(StatefulConnection conn) {
        return strategy == null ? null : strategy.sync(conn);
    }

}
