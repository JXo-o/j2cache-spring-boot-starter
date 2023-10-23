package net.oschina.j2cache.util.serializer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;

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
        return JSON.toJSONString(obj, SerializerFeature.WriteMapNullValue, SerializerFeature.WriteClassName).getBytes();
    }

    @Override
    public Object deserialize(byte[] bytes) {
        return JSON.parse(new String(bytes), Feature.SupportAutoType);
    }

}
