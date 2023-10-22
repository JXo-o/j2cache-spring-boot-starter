package net.oschina.j2cache.util;

import net.oschina.j2cache.CacheException;

import java.io.*;

/**
 * ClassName: JavaSerializer
 * Package: net.oschina.j2cache.util
 * Description: 标准的 Java 序列化
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 0:31
 */
public class JavaSerializer implements Serializer {

    @Override
    public String name() {
        return "java";
    }

    @Override
    public byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)){
            oos.writeObject(obj);
            return baos.toByteArray();
        }
    }

    @Override
    public Object deserialize(byte[] bits) throws IOException {
        if(bits == null || bits.length == 0)
            return null;
        ByteArrayInputStream bais = new ByteArrayInputStream(bits);
        try (ObjectInputStream ois = new ObjectInputStream(bais)){
            return ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new CacheException(e);
        }
    }

}
