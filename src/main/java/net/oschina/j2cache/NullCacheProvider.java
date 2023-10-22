package net.oschina.j2cache;

import java.util.Collection;
import java.util.Properties;

/**
 * ClassName: NullCacheProvider
 * Package: net.oschina.j2cache
 * Description:
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 0:57
 */
public class NullCacheProvider implements CacheProvider {

    private final static NullCache cache = new NullCache();

    @Override
    public String name() {
        return "none";
    }

    @Override
    public int level() {
        return CacheObject.LEVEL_1 | CacheObject.LEVEL_2;
    }

    @Override
    public Cache buildCache(String regionName, CacheExpiredListener listener) throws CacheException {
        return cache;
    }

    @Override
    public Cache buildCache(String region, long timeToLiveInSeconds, CacheExpiredListener listener) {
        return cache;
    }

    @Override
    public void start(Properties props) throws CacheException {
    }

    @Override
    public Collection<CacheChannel.Region> regions() {
        return null;
    }

    @Override
    public void stop() {
    }

}
