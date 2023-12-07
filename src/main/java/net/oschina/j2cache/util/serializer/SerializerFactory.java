package net.oschina.j2cache.util.serializer;

import net.oschina.j2cache.exception.CacheException;

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

    public static Serializer createSerializer(String ser, Properties props) {
        Serializer serializer;

        if (ser == null || "".equals(ser.trim())) {
            serializer = new JavaSerializer();
        } else {
            switch (ser) {
                case "java":
                    serializer = new JavaSerializer();
                    break;
                case "fst":
                    serializer = new FSTSerializer();
                    break;
                case "kryo":
                    serializer = new KryoSerializer();
                    break;
                case "kryo-pool":
                    serializer = new KryoPoolSerializer();
                    break;
                case "fst-snappy":
                    serializer = new FstSnappySerializer();
                    break;
                case "json":
                    serializer = new FstJSONSerializer(props);
                    break;
                case "fastjson":
                    serializer = new FastjsonSerializer();
                    break;
                case "fse":
                    serializer = new FseSerializer();
                    break;
                default:
                    try {
                        serializer = (Serializer) Class.forName(ser).newInstance();
                    } catch (Exception e) {
                        throw new CacheException("Cannot initialize Serializer named [" + ser + ']', e);
                    }
            }
        }

        return serializer;
    }
}