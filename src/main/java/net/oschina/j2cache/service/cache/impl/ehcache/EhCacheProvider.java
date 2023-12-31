package net.oschina.j2cache.service.cache.impl.ehcache;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import net.oschina.j2cache.service.cache.CacheChannel;
import net.oschina.j2cache.model.CacheObject;
import net.sf.ehcache.config.CacheConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.oschina.j2cache.service.cache.CacheExpiredListener;
import net.oschina.j2cache.service.cache.CacheProvider;
import net.sf.ehcache.CacheManager;

/**
 * ClassName: EhCacheProvider
 * Package: net.oschina.j2cache.service.cache.impl.ehcache
 * Description: EhCache 2.x 缓存管理器的封装，用来管理多个缓存区域
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 1:21
 */
public class EhCacheProvider implements CacheProvider {

    private final static Logger log = LoggerFactory.getLogger(EhCacheProvider.class);

    public final static String KEY_EHCACHE_NAME = "name";
    public final static String KEY_EHCACHE_CONFIG_XML = "configXml";

    private CacheManager manager;
    private ConcurrentHashMap<String, EhCache> caches;

    @Override
    public String name() {
        return "ehcache";
    }

    @Override
    public int level() {
        return CacheObject.LEVEL_1;
    }

    @Override
    public Collection<CacheChannel.Region> regions() {
        Collection<CacheChannel.Region> regions = new ArrayList<>();
        caches.forEach((k,c) -> regions.add(new CacheChannel.Region(k, c.size(), c.ttl())));
        return regions;
    }

    /**
     * Builds a Cache.
     *
     * @param regionName the regionName of the cache. Must match a cache configured in ehcache.xml
     * @param listener cache listener
     * @return a newly built cache will be built and initialised
     */
    @Override
    public EhCache buildCache(String regionName, CacheExpiredListener listener) {
        return caches.computeIfAbsent(regionName, v -> {
            net.sf.ehcache.Cache cache = manager.getCache(regionName);
            if (cache == null) {
                manager.addCache(regionName);
                cache = manager.getCache(regionName);
                log.warn("Could not find configuration [{}]; using defaults (TTL:{} seconds).", regionName, cache.getCacheConfiguration().getTimeToLiveSeconds());
            }
            return new EhCache(cache, listener);
        });
    }

    @Override
    public EhCache buildCache(String region, long timeToLiveInSeconds, CacheExpiredListener listener) {
        EhCache ehcache = caches.computeIfAbsent(region, v -> {
            //配置缓存
            CacheConfiguration cfg = manager.getConfiguration().getDefaultCacheConfiguration().clone();
            cfg.setName(region);
            if(timeToLiveInSeconds > 0) {
                cfg.setTimeToLiveSeconds(timeToLiveInSeconds);
                cfg.setTimeToIdleSeconds(timeToLiveInSeconds);
            }

            net.sf.ehcache.Cache cache = new net.sf.ehcache.Cache(cfg);
            manager.addCache(cache);

            log.info("Started Ehcache region [{}] with TTL: {}", region, timeToLiveInSeconds);

            return new EhCache(cache, listener);
        });

        if (ehcache.ttl() != timeToLiveInSeconds)
            throw new IllegalArgumentException(String.format("Region [%s] TTL %d not match with %d", region, ehcache.ttl(), timeToLiveInSeconds));

        return ehcache;
    }

    @Override
    public void removeCache(String region) {
        caches.remove(region);
        manager.removeCache(region);
    }

    /**
     * init ehcache config
     *
     * @param props current configuration settings.
     */
    public void start(Properties props) {
        if (manager != null) {
            log.warn("Attempt to restart an already started EhCacheProvider.");
            return;
        }

        // 如果指定了名称,那么尝试获取已有实例
        String ehcacheName = (String)props.get(KEY_EHCACHE_NAME);
        if (ehcacheName != null && ehcacheName.trim().length() > 0)
            manager = CacheManager.getCacheManager(ehcacheName);
        if (manager == null) {
            // 指定了配置文件路径? 加载之
            if (props.containsKey(KEY_EHCACHE_CONFIG_XML)) {
                String propertiesFile = props.getProperty(KEY_EHCACHE_CONFIG_XML);
                URL url = getClass().getResource(propertiesFile);
                url = (url == null) ? getClass().getClassLoader().getResource(propertiesFile) : url;
                manager = CacheManager.newInstance(url);
            } else {
                // 加载默认实例
                manager = CacheManager.getInstance();
            }
        }
        caches = new ConcurrentHashMap<>();
    }

    /**
     * Callback to perform any necessary cleanup of the underlying cache implementation.
     */
    public void stop() {
        if (manager != null) {
            manager.shutdown();
            caches.clear();
            manager = null;
        }
    }

}
