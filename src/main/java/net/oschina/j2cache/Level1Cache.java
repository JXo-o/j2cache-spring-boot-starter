package net.oschina.j2cache;

/**
 * ClassName: Level1Cache
 * Package: net.oschina.j2cache
 * Description: 一级缓存接口
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 0:09
 */
public interface Level1Cache extends Cache {

    /**
     * 返回该缓存区域的 TTL 设置（单位：秒）
     * @return true if cache support ttl setting
     */
    long ttl();

    /**
     * 返回该缓存区域中，内存存储对象的最大数量
     * @return cache size in memory
     */
    long size();

}
