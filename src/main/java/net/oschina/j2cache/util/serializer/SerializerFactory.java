package net.oschina.j2cache.util.serializer;

import net.oschina.j2cache.exception.CacheException;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * ClassName: SerializerFactory
 * Package: net.oschina.j2cache.util.serializer
 * Description:
 *
 * @author JX
 * @version 1.0
 * @date 2023/12/7 11:12
 */
public class SerializerFactory {

    private static final Map<String, Serializer> serializerCache = new HashMap<>();

    public static Serializer createSerializer(String ser, Properties props) {
        if (ser == null || "".equals(ser.trim())) {
            return getSerializer("java", props);
        }

        return getSerializer(ser, props);
    }

    private static Serializer getSerializer(String ser, Properties props) {
        return serializerCache.computeIfAbsent(ser, key -> createNewSerializer(ser, props));
    }

    private static Serializer createNewSerializer(String ser, Properties props) {
        switch (ser) {
            case "java":
                return new JavaSerializer();
            case "fst":
                return new FSTSerializer();
            case "kryo":
                return new KryoSerializer();
            case "kryo-pool":
                return new KryoPoolSerializer();
            case "fst-snappy":
                return new FstSnappySerializer();
            case "json":
                return new FstJSONSerializer(props);
            case "fastjson":
                return new FastjsonSerializer();
            case "fse":
                return new FseSerializer();
            default:
                try {
                    return (Serializer) Class.forName(ser).newInstance();
                } catch (Exception e) {
                    throw new CacheException("Cannot initialize Serializer named [" + ser + ']', e);
                }
        }
    }
}