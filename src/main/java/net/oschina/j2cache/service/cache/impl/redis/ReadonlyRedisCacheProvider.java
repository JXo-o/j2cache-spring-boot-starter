package net.oschina.j2cache.service.cache.impl.redis;

import net.oschina.j2cache.service.cache.Cache;
import net.oschina.j2cache.service.cache.CacheExpiredListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

/**
 * ClassName: net.oschina.j2cache.service.cache.impl.redis
 * Package: net.oschina.j2cache.redis
 * Description: 实现只读的 Redis 缓存管理，相当于只从 Redis 读数据，不往 Redis 写数据
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 1:37
 */
public class ReadonlyRedisCacheProvider extends RedisCacheProvider {

    private final static Logger log = LoggerFactory.getLogger(ReadonlyRedisCacheProvider.class);

    @Override
    public String name() {
        return "redis";
    }

    @Override
    public Cache buildCache(String region, CacheExpiredListener listener) {
        Cache cache = super.buildCache(region, listener);
        return new ReadonlyCache(cache).get();
    }

    @Override
    public Cache buildCache(String region, long timeToLiveInSeconds, CacheExpiredListener listener) {
        Cache cache = super.buildCache(region, timeToLiveInSeconds, listener);
        return new ReadonlyCache(cache).get();
    }

    /**
     * 接管 Cache 接口，并对缓存更新的方法进行屏蔽
     */
    private static final class ReadonlyCache implements InvocationHandler {

        private static final List<String> ignoreMethods = Arrays.asList("put","evict","clear");

        private Cache cache;

        public ReadonlyCache(Cache cache) {
            this.cache = cache;
        }

        public Cache get() {
            return (Cache)Proxy.newProxyInstance(cache.getClass().getClassLoader(), cache.getClass().getInterfaces(), this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if(ignoreMethods.contains(methodName)) {
                log.debug("Cache [{}] skipped.", methodName);
                return null;
            }
            try {
                return method.invoke(cache, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
    }

}
