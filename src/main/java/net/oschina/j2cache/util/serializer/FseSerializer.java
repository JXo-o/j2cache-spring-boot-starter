package net.oschina.j2cache.util.serializer;

import com.jfirer.fse.ByteArray;
import com.jfirer.fse.Fse;

/**
 * ClassName: FseSerializer
 * Package: net.oschina.j2cache.util.serializer
 * Description: 使用 fse 实现序列化
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 0:37
 */
public class FseSerializer implements Serializer {

    @Override
    public String name() {
        return "fse";
    }

    @Override
    public byte[] serialize(Object obj) {
        ByteArray buf = ByteArray.allocate(100);
        new Fse().serialize(obj, buf);
        byte[] resultBytes = buf.toArray();
        buf.clear();
        return resultBytes;
    }

    @Override
    public Object deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        ByteArray buf = ByteArray.allocate(100);
        buf.put(bytes);
        Object result = new Fse().deSerialize(buf);
        buf.clear();
        return result;
    }

}
