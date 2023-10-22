package net.oschina.j2cache.util;

/**
 * ClassName: DeserializeException
 * Package: net.oschina.j2cache.util
 * Description: 反序列化的对象兼容异常
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 0:20
 */
public class DeserializeException extends RuntimeException {

    public DeserializeException(String message) {
        super(message);
    }
}
