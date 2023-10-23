package net.oschina.j2cache.exception;

/**
 * ClassName: CacheException
 * Package: net.oschina.j2cache.exception
 * Description: J2Cache exception
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 0:23
 */
public class CacheException extends RuntimeException {

    public CacheException(String s) {
        super(s);
    }

    public CacheException(String s, Throwable e) {
        super(s, e);
    }

    public CacheException(Throwable e) {
        super(e);
    }

}
