package net.oschina.j2cache;

/**
 * ClassName: CacheExpiredListener
 * Package: net.oschina.j2cache
 * Description: When cached data expired in ehcache, this listener will be invoked.
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 0:47
 */
public interface CacheExpiredListener {

    /**
     * 缓存因为超时失效后触发的通知
     * @param region 缓存 region
     * @param key 缓存 key
     */
    void notifyElementExpired(String region, String key) ;

}
