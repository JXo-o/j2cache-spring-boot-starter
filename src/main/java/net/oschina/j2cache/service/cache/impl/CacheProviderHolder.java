package net.oschina.j2cache.service.cache.impl;

import net.oschina.j2cache.config.J2CacheProperties;
import net.oschina.j2cache.exception.CacheException;
import net.oschina.j2cache.model.CacheObject;
import net.oschina.j2cache.service.cache.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * ClassName: CacheProviderHolder
 * Package: net.oschina.j2cache.service.cache.impl
 * Description: 两级的缓存管理器
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 0:54
 */
public class CacheProviderHolder {

    private final static Logger log = LoggerFactory.getLogger(CacheProviderHolder.class);

    private CacheProvider l1_provider;
    private CacheProvider l2_provider;

    private CacheExpiredListener listener;

    private CacheProviderHolder() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private J2CacheProperties config;
        private CacheExpiredListener listener;

        public Builder withConfig(J2CacheProperties config) {
            this.config = config;
            return this;
        }

        public Builder withListener(CacheExpiredListener listener) {
            this.listener = listener;
            return this;
        }

        public CacheProviderHolder build() {
            CacheProviderHolder holder = new CacheProviderHolder();
            holder.listener = this.listener;

            // 初始化 l1_provider
            holder.l1_provider = CacheProviderFactory.createCacheProvider(config.getL1CacheName());
            if (!holder.l1_provider.isLevel(CacheObject.LEVEL_1))
                throw new CacheException(holder.l1_provider.getClass().getName() + " is not level_1 cache provider");
            holder.l1_provider.start(config.getL1CacheProperties());
            log.info("Using L1 CacheProvider : {}", holder.l1_provider.getClass().getName());

            // 初始化 l2_provider
            holder.l2_provider = CacheProviderFactory.createCacheProvider(config.getL2CacheName());
            if (!holder.l2_provider.isLevel(CacheObject.LEVEL_2))
                throw new CacheException(holder.l2_provider.getClass().getName() + " is not level_2 cache provider");
            holder.l2_provider.start(config.getL2CacheProperties());
            log.info("Using L2 CacheProvider : {}", holder.l2_provider.getClass().getName());

            return holder;
        }
    }

//    /**
//     * Initialize Cache Provider
//     *
//     * @param config   j2cache config instance
//     * @param listener cache listener
//     * @return holder : return CacheProviderHolder instance
//     */
//    public static CacheProviderHolder init(J2CacheProperties config, CacheExpiredListener listener) {
//
//        CacheProviderHolder holder = new CacheProviderHolder();
//
//        holder.listener = listener;
//        holder.l1_provider = CacheProviderFactory.createCacheProvider(config.getL1CacheName());
//        if (!holder.l1_provider.isLevel(CacheObject.LEVEL_1))
//            throw new CacheException(holder.l1_provider.getClass().getName() + " is not level_1 cache provider");
//        holder.l1_provider.start(config.getL1CacheProperties());
//        log.info("Using L1 CacheProvider : {}", holder.l1_provider.getClass().getName());
//
//        holder.l2_provider = CacheProviderFactory.createCacheProvider(config.getL2CacheName());
//        if (!holder.l2_provider.isLevel(CacheObject.LEVEL_2))
//            throw new CacheException(holder.l2_provider.getClass().getName() + " is not level_2 cache provider");
//        holder.l2_provider.start(config.getL2CacheProperties());
//        log.info("Using L2 CacheProvider : {}", holder.l2_provider.getClass().getName());
//
//        return holder;
//    }

    /**
     * 关闭缓存
     */
    public void shutdown() {
        l1_provider.stop();
        l2_provider.stop();
    }

//    private static CacheProvider loadProviderInstance(String cacheIdent) {
//        switch (cacheIdent.toLowerCase()) {
//            case "ehcache":
//                return new EhCacheProvider();
//            case "ehcache3":
//                return new EhCacheProvider3();
//            case "caffeine":
//                return new CaffeineProvider();
//            case "redis":
//                return new RedisCacheProvider();
//            case "readonly-redis":
//                return new ReadonlyRedisCacheProvider();
//            case "memcached":
//                return new XmemcachedCacheProvider();
//            case "lettuce":
//                return new LettuceCacheProvider();
//            case "none":
//                return new NullCacheProvider();
//        }
//        try {
//            return (CacheProvider) Class.forName(cacheIdent).newInstance();
//        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
//            throw new CacheException("Failed to initialize cache providers", e);
//        }
//    }

    public CacheProvider getL1Provider() {
        return l1_provider;
    }

    public CacheProvider getL2Provider() {
        return l2_provider;
    }

    /**
     * 一级缓存实例
     *
     * @param region cache region
     * @return level 1 cache instance
     */
    public Level1Cache getLevel1Cache(String region) {
        return (Level1Cache) l1_provider.buildCache(region, listener);
    }

    /**
     * 一级缓存实例
     *
     * @param region            cache region
     * @param timeToLiveSeconds cache ttl
     * @return level 1 cache instance
     */
    public Level1Cache getLevel1Cache(String region, long timeToLiveSeconds) {
        return (Level1Cache) l1_provider.buildCache(region, timeToLiveSeconds, listener);
    }

    /**
     * 二级缓存实例
     *
     * @param region cache region
     * @return level 2 cache instance
     */
    public Level2Cache getLevel2Cache(String region) {
        return (Level2Cache) l2_provider.buildCache(region, listener);
    }

    /**
     * return all regions
     *
     * @return all regions
     */
    public Collection<CacheChannel.Region> regions() {
        return l1_provider.regions();
    }

}

