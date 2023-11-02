package net.oschina.j2cache.util.serializer;

import com.alibaba.fastjson2.*;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;


/**
 * ClassName: FastjsonSerializer
 * Package: net.oschina.j2cache.util.serializer
 * Description: 使用 fastjson 进行对象的 JSON 格式序列化
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 0:37
 */
public class FastjsonSerializer implements Serializer {

    @Override
    public String name() {
        return "fastjson";
    }

    @Override
    public byte[] serialize(Object obj) {
        return JSON.toJSONString(obj, JSONWriter.Feature.WriteMapNullValue, JSONWriter.Feature.WriteClassName).getBytes();
    }

    @Override
    public Object deserialize(byte[] bytes) {
        return JSON.parse(new String(bytes), JSONReader.Feature.SupportAutoType);
    }

}
