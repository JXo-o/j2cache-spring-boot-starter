package net.oschina.j2cache.util;

import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.coders.FSTJsonFieldNames;
import org.nustaq.serialization.serializers.FSTDateSerializer;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Properties;

/**
 * ClassName: FstJSONSerializer
 * Package: net.oschina.j2cache.util
 * Description: 使用 FST 的 JSON 对象序列化
 *
 * Usage：
 * j2cache.serialization = json
 * json.map.list = java.util.Arrays$ArrayList
 * json.map.person = net.oschina.j2cache.demo.Person
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 0:35
 */
public class FstJSONSerializer implements Serializer {

    private static final FSTConfiguration conf = FSTConfiguration.createJsonConfiguration();
    private static final String PREFIX = "map.";

    public FstJSONSerializer(Properties props) {
        conf.setJsonFieldNames(new FSTJsonFieldNames("@type", "@object", "@stype", "@seq", "@enum", "@value", "@ref"));
        conf.registerCrossPlatformClassMapping("list", "java.util.Arrays$ArrayList");
        conf.registerSerializer(Timestamp.class, new FSTDateSerializer(), true);
        conf.registerSerializer(Date.class, new FSTDateSerializer(), true);
        if(props != null)
            props.forEach((k,v) -> {
                String key = (String)k;
                String value = (String)v;
                if(key.startsWith(PREFIX) && value != null && value.trim().length() > 0)
                    conf.registerCrossPlatformClassMapping(key.substring(PREFIX.length()), value.trim());
            });
    }

    @Override
    public String name() {
        return "json";
    }

    @Override
    public byte[] serialize(Object obj) {
        return conf.asByteArray(obj);
    }

    @Override
    public Object deserialize(byte[] bytes) {
        return conf.asObject(bytes);
    }

}
