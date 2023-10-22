package net.oschina.j2cache.util;

import java.io.IOException;

/**
 * ClassName: Serializer
 * Package: net.oschina.j2cache.util
 * Description: 对象序列化接口
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 0:25
 */
public interface Serializer {

    /**
     * 序列化器的名称，该方法仅用于打印日志的时候显示
     * @return 返回序列化器名称
     */
    String name();

    /**
     * 对象序列化到字节数组
     * @param obj  待序列化的对象
     * @return 返回序列化数据
     * @throws IOException io exception
     */
    byte[] serialize(Object obj) throws IOException ;

    /**
     * 反序列化到对象
     * @param bytes  反序列化的数据
     * @return 返回序列化对象
     * @throws IOException io exception
     */
    Object deserialize(byte[] bytes) throws IOException ;

}
