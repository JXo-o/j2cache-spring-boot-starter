package net.oschina.j2cache.service.cache.impl.lettuce;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.sync.BaseRedisCommands;

/**
 * ClassName: SyncStrategy
 * Package: net.oschina.j2cache.service.cache.impl.lettuce
 * Description:
 *
 * @author JX
 * @version 1.0
 * @date 2023/12/7 19:51
 */
public interface SyncStrategy {
    BaseRedisCommands sync(StatefulConnection conn);
}
