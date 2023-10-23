package net.oschina.j2cache;

import net.oschina.j2cache.config.J2CacheConfig;
import net.oschina.j2cache.exception.CacheException;
import net.oschina.j2cache.service.cache.CacheChannel;

import java.io.IOException;

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

    private final static String CONFIG_FILE = "/j2cache.properties";

    private final static J2CacheBuilder builder;

    static {
        try {
            J2CacheConfig config = J2CacheConfig.initFromConfig(CONFIG_FILE);
            builder = J2CacheBuilder.init(config);
        } catch (IOException e) {
            throw new CacheException("Failed to load j2cache configuration " + CONFIG_FILE, e);
        }
    }

    /**
     * 返回缓存操作接口
     * @return CacheChannel
     */
    public static CacheChannel getChannel(){
        return builder.getChannel();
    }

    /**
     * 关闭 J2Cache
     */
    public static void close() {
        builder.close();
    }
}
