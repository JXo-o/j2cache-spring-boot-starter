package net.oschina.j2cache.service.cache.impl.nil;

import net.oschina.j2cache.service.cache.CacheChannel;
import net.oschina.j2cache.exception.CacheException;
import net.oschina.j2cache.model.CacheObject;
import net.oschina.j2cache.service.cache.Cache;
import net.oschina.j2cache.service.cache.CacheExpiredListener;
import net.oschina.j2cache.service.cache.CacheProvider;

import java.util.Collection;
import java.util.Properties;

/**
 * ClassName: NullCacheProvider
 * Package: net.oschina.j2cache.service.cache.impl.nil
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
