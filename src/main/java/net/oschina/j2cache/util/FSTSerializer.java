package net.oschina.j2cache.util;

import org.nustaq.serialization.FSTConfiguration;

/**
 * ClassName: FSTSerializer
 * Package: net.oschina.j2cache.util
 * Description: 使用 FST 实现序列化
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 0:32
 */
public class FSTSerializer implements Serializer {

    private FSTConfiguration fstConfiguration ;

    public FSTSerializer() {
        fstConfiguration = FSTConfiguration.getDefaultConfiguration();
        fstConfiguration.setClassLoader(Thread.currentThread().getContextClassLoader());
    }

    @Override
    public String name() {
        return "fst";
    }

    @Override
    public byte[] serialize(Object obj) {
        return fstConfiguration.asByteArray(obj);
    }

    @Override
    public Object deserialize(byte[] bytes) {
        return fstConfiguration.asObject(bytes);
    }

}
