package net.oschina.j2cache.service.cache.impl.ehcache;

import net.oschina.j2cache.service.cache.CacheExpiredListener;
import net.oschina.j2cache.service.cache.Level1Cache;
import org.ehcache.config.ResourceType;
import org.ehcache.event.*;
import org.ehcache.expiry.Duration;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ClassName: EhCache3
 * Package: net.oschina.j2cache.service.cache.impl.ehcache
 * Description:
 * <p>EHCache 3.x 的缓存封装</p>
 * <p>该封装类实现了缓存操作以及对缓存数据失效的侦听</p>
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 1:21
 */
public class EhCache3 implements Level1Cache, CacheEventListener {

    private String name;
    private org.ehcache.Cache<String, Object> cache;
    private CacheExpiredListener listener;

    public EhCache3(String name, org.ehcache.Cache<String, Object> cache, CacheExpiredListener listener) {
        this.name = name;
        this.cache = cache;
        this.cache.getRuntimeConfiguration().registerCacheEventListener(this,
                EventOrdering.ORDERED,
                EventFiring.ASYNCHRONOUS,
                EventType.EXPIRED);
        this.listener = listener;
    }

    @Override
    public long ttl() {
        Duration dur = this.cache.getRuntimeConfiguration().getExpiry().getExpiryForCreation(null,null);
        if (dur.isInfinite())
            return 0L;
        return dur.getTimeUnit().toSeconds(dur.getLength());
    }

    @Override
    public long size() {
        return this.cache.getRuntimeConfiguration().getResourcePools().getPoolForResource(ResourceType.Core.HEAP).getSize();
    }

    @Override
    public Object get(String key) {
        return this.cache.get(key);
    }

    @Override
    public void put(String key, Object value) {
        this.cache.put(key, value);
    }

    @Override
    public Map<String, Object> get(Collection<String> keys) {
        return cache.getAll(keys.stream().collect(Collectors.toSet()));
    }

    @Override
    public boolean exists(String key) {
        return cache.containsKey(key);
    }

    @Override
    public void put(Map<String, Object> elements) {
        cache.putAll(elements);
    }

    @Override
    public Collection<String> keys() {
        return Collections.emptyList();
    }

    @Override
    public void evict(String...keys) {
        this.cache.removeAll(Arrays.stream(keys).collect(Collectors.toSet()));
    }

    @Override
    public void clear() {
        this.cache.clear();
    }

    @Override
    public void onEvent(CacheEvent cacheEvent) {
        if(cacheEvent.getType() == EventType.EXPIRED){
            this.listener.notifyElementExpired(name, (String)cacheEvent.getKey());
        }
    }
}
