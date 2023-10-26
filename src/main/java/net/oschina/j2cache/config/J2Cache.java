package net.oschina.j2cache.config;

import net.oschina.j2cache.service.cache.CacheChannel;

/**
 * ClassName: J2Cache
 * Package: net.oschina.j2cache
 * Description: J2Cache 的缓存入口
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 0:51
 */

public class J2Cache {

    private static volatile J2Cache instance = null;

    private static volatile J2CacheBuilder builder = null;

    private J2Cache(J2CacheProperties config) {
        builder = J2CacheBuilder.init(config);
    }

    public static J2Cache getInstance(J2CacheProperties config) {
        if (instance == null) {
            synchronized (J2Cache.class) {
                if (instance == null) {
                    instance = new J2Cache(config);
                }
            }
        }
        return instance;
    }

    public CacheChannel getChannel() {
        return getBuilder().getChannel();
    }

    public void close() {
        getBuilder().close();
    }

    private static J2CacheBuilder getBuilder() {
        if (builder == null) {
            throw new IllegalStateException("J2CacheBuilder is not initialized");
        }
        return builder;
    }
}