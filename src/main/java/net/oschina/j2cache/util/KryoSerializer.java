package net.oschina.j2cache.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * ClassName: KryoSerializer
 * Package: net.oschina.j2cache.util
 * Description: 使用 Kryo 实现序列化
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 0:33
 */
public class KryoSerializer implements Serializer {

    @Override
    public String name() {
        return "kryo";
    }

    @Override
    public byte[] serialize(Object obj) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Output output = new Output(baos);){
            new Kryo().writeClassAndObject(output, obj);
            output.flush();
            return baos.toByteArray();
        }
    }

    @Override
    public Object deserialize(byte[] bits) {
        if(bits == null || bits.length == 0)
            return null;
        try (Input ois = new Input(new ByteArrayInputStream(bits))){
            return new Kryo().readClassAndObject(ois);
        }
    }

}