package net.oschina.j2cache.lettuce;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.BaseRedisCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import net.oschina.j2cache.CacheException;
import net.oschina.j2cache.Level2Cache;
import org.apache.commons.pool2.impl.GenericObjectPool;

/**
 * ClassName: LettuceCache
 * Package: net.oschina.j2cache.lettuce
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

    protected StatefulConnection connect() {
        try {
            return pool.borrowObject();
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    protected BaseRedisCommands sync(StatefulConnection conn) {
        if(conn instanceof StatefulRedisClusterConnection)
            return ((StatefulRedisClusterConnection)conn).sync();
        else if(conn instanceof StatefulRedisConnection)
            return ((StatefulRedisConnection)conn).sync();
        return null;
    }

}
