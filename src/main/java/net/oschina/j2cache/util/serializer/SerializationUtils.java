package net.oschina.j2cache.util.serializer;

import net.oschina.j2cache.exception.CacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * ClassName: SerializationUtils
 * Package: net.oschina.j2cache.util.serializer
 * Description: 对象序列化工具包
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 0:22
 */
public class SerializationUtils {

    private final static Logger log = LoggerFactory.getLogger(SerializationUtils.class);
    private static Serializer g_serializer;

    /**
     * 私有化构造器
     * */
    private SerializationUtils() {
    }

    /**
     * 初始化序列化器
     * @param ser  serialization method
     * @param props serializer properties
     */
    public static void init(String ser, Properties props) {
        g_serializer = SerializerFactory.createSerializer(ser, props);
        log.info("Using Serializer -> [{}:{}]", g_serializer.name(), g_serializer.getClass().getName());
    }

    /**
     * 针对不同类型做单独处理
     * @param obj 待序列化的对象
     * @return 返回序列化后的字节数组
     * @throws IOException io exception
     */
    public static byte[] serialize(Object obj) throws IOException {
        if (obj == null)
            return null;
        return g_serializer.serialize(obj);
    }

    public static byte[] serializeWithoutException(Object obj) {
        try {
            return serialize(obj);
        } catch (IOException e) {
            throw new CacheException(e);
        }
    }

    /**
     * 反序列化
     * @param bytes 待反序列化的字节数组
     * @return 序列化后的对象
     * @throws IOException io exception
     */
    public static Object deserialize(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0)
            return null;
        return g_serializer.deserialize(bytes);
    }
}