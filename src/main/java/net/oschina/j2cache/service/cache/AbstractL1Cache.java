package net.oschina.j2cache.service.cache;

import java.util.Collection;
import java.util.Map;

/**
 * ClassName: AbstractCache
 * Package: net.oschina.j2cache.service.cache
 * Description:
 *
 * @author JX
 * @version 1.0
 * @date 2023/12/21 9:26
 */
public abstract class AbstractL1Cache implements Level1Cache {

    @Override
    public Object get(String key) {
        // 具体获取实现由子类提供
        return getFromCache(key);
    }

    @Override
    public Map<String, Object> get(Collection<String> keys) {
        // 具体批量获取实现由子类提供
        return getFromCache(keys);
    }

    @Override
    public void put(String key, Object value) {
        // 具体插入实现由子类提供
        putInCache(key, value);
    }

    @Override
    public void put(Map<String, Object> elements) {
        // 具体批量插入实现由子类提供
        putInCache(elements);
    }

    @Override
    public Collection<String> keys() {
        return getKeysFromCache();
    }

    @Override
    public boolean exists(String key) {
        // 默认实现，子类可以覆盖
        return get(key) != null;
    }

    @Override
    public void evict(String... keys) {
        // 具体删除实现由子类提供
        evictFromCache(keys);
    }

    @Override
    public void clear() {
        // 具体清除实现由子类提供
        clearCache();
    }

    /**
     * 由子类提供具体获取实现
     */
    protected abstract Object getFromCache(String key);

    /**
     * 由子类提供具体批量获取实现
     */
    protected abstract Map<String, Object> getFromCache(Collection<String> keys);

    /**
     * 由子类提供具体插入实现
     */
    protected abstract void putInCache(String key, Object value);

    /**
     * 由子类提供具体批量插入实现
     */
    protected abstract void putInCache(Map<String, Object> elements);

    /**
     * 由子类提供具体删除实现
     */
    protected abstract void evictFromCache(String... keys);

    /**
     * 由子类提供具体清除实现
     */
    protected abstract void clearCache();

    protected abstract Collection<String> getKeysFromCache();

}