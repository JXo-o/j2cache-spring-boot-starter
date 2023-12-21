package net.oschina.j2cache.service.cache.impl.lettuce;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.BaseRedisCommands;

/**
 * ClassName: SingleNodeSyncStrategy
 * Package: net.oschina.j2cache.service.cache.impl.lettuce
 * Description:
 *
 * @author JX
 * @version 1.0
 * @date 2023/12/7 19:51
 */
public class SingleNodeSyncStrategy implements SyncStrategy {
    @Override
    public BaseRedisCommands sync(StatefulConnection conn) {
        if (conn instanceof StatefulRedisConnection)
            return ((StatefulRedisConnection) conn).sync();
        return null;
    }
}