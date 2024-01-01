package net.oschina.j2cache.service.cache.impl.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import net.oschina.j2cache.service.cache.AbstractL1Cache;
import net.oschina.j2cache.service.cache.Level1Cache;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.EventType;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ClassName: CaffeineCache
 * Package: net.oschina.j2cache.service.cache.impl.caffeine
 * Description: Caffeine cache
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 0:44
 */
public class CaffeineCache extends AbstractL1Cache {

    private Cache<String, Object> cache;
    private long size ;
    private long expire ;

    public CaffeineCache(Cache<String, Object> cache, long size, long expire) {
        this.cache = cache;
        this.size = size;
        this.expire = expire;
    }

    @Override
    public long ttl() {
        return expire;
    }

    @Override
    public long size() { return size; }

    @Override
    protected Object getFromCache(String key) {
        return cache.getIfPresent(key);
    }

    @Override
    protected Map<String, Object> getFromCache(Collection<String> keys) {
        return cache.getAllPresent(keys);
    }

    @Override
    protected void putInCache(String key, Object value) {
        cache.put(key, value);
    }

    @Override
    protected void putInCache(Map<String, Object> elements) {
        cache.putAll(elements);
    }

    @Override
    protected void evictFromCache(String... keys) {
        cache.invalidateAll(Arrays.asList(keys));
    }

    @Override
    protected void clearCache() {
        cache.invalidateAll();
    }

    @Override
    protected Collection<String> getKeysFromCache() {
        return cache.asMap().keySet();
    }

}
