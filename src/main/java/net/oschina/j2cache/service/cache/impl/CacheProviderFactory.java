package net.oschina.j2cache.service.cache.impl;

import net.oschina.j2cache.exception.CacheException;
import net.oschina.j2cache.service.cache.CacheProvider;
import net.oschina.j2cache.service.cache.impl.caffeine.CaffeineProvider;
import net.oschina.j2cache.service.cache.impl.ehcache.EhCacheProvider;
import net.oschina.j2cache.service.cache.impl.ehcache.EhCacheProvider3;
import net.oschina.j2cache.service.cache.impl.lettuce.LettuceCacheProvider;
import net.oschina.j2cache.service.cache.impl.memcached.XmemcachedCacheProvider;
import net.oschina.j2cache.service.cache.impl.nil.NullCacheProvider;
import net.oschina.j2cache.service.cache.impl.redis.ReadonlyRedisCacheProvider;
import net.oschina.j2cache.service.cache.impl.redis.RedisCacheProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * ClassName: CacheProviderFactory
 * Package: net.oschina.j2cache.service.cache.impl
 * Description:
 *
 * @author JX
 * @version 1.0
 * @date 2023/12/7 11:53
 */
public class CacheProviderFactory {

    /*
    * 新添加的享元模式
    * */
    private static final Map<String, CacheProvider> cacheProviders = new HashMap<>();

    public static synchronized CacheProvider createCacheProvider(String cacheIdent) {
        return cacheProviders.computeIfAbsent(cacheIdent.toLowerCase(), CacheProviderFactory::createProviderInstance);
    }

    private static CacheProvider createProviderInstance(String cacheIdent) {
        switch (cacheIdent.toLowerCase()) {
            case "ehcache":
                return new EhCacheProvider();
            case "ehcache3":
                return new EhCacheProvider3();
            case "caffeine":
                return new CaffeineProvider();
            case "redis":
                return new RedisCacheProvider();
            case "readonly-redis":
                return new ReadonlyRedisCacheProvider();
            case "memcached":
                return new XmemcachedCacheProvider();
            case "lettuce":
                return new LettuceCacheProvider();
            case "none":
                return new NullCacheProvider();
            default:
                return createCustomCacheProvider(cacheIdent);
        }
    }

    private static CacheProvider createCustomCacheProvider(String className) {
        try {
            return (CacheProvider) Class.forName(className).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new CacheException("初始化缓存提供程序失败", e);
        }
    }
}