package net.oschina.j2cache.service.cache.impl.lettuce;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.sync.BaseRedisCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;

/**
 * ClassName: ClusterSyncStrategy
 * Package: net.oschina.j2cache.service.cache.impl.lettuce
 * Description:
 *
 * @author JX
 * @version 1.0
 * @date 2023/12/7 19:51
 */
public class ClusterSyncStrategy implements SyncStrategy {
    @Override
    public BaseRedisCommands sync(StatefulConnection conn) {
        if (conn instanceof StatefulRedisClusterConnection)
            return ((StatefulRedisClusterConnection) conn).sync();
        return null;
    }
}