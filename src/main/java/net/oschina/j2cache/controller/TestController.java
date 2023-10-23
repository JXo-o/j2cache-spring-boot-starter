package net.oschina.j2cache.controller;

import net.oschina.j2cache.CacheChannel;
import net.oschina.j2cache.CacheObject;
import net.oschina.j2cache.J2Cache;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassName: TestController
 * Package: net.oschina.j2cache.controller
 * Description: 以接口形式测试 J2Cache
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 13:26
 */
@RestController
@RequestMapping("/J2Cache")
public class TestController {
    private static long TTL = 0;
    private CacheChannel cacheChannel = J2Cache.getChannel();
    @PostMapping("/set")
    public String testSet(@RequestParam String region,
                          @RequestParam String key,
                          @RequestParam String value) {

        Map<String, Object> objs = new HashMap<>();
        String[] keys = key.split(",");
        String[] values = value.split(",");

        if (keys.length != values.length)
            return "Error: keys.length != values.length";

        for (int i = 0; i < keys.length; i++) {
            if ("null".equalsIgnoreCase(values[i]))
                values[i] = null;
            objs.put(keys[i], values[i]);
        }

        cacheChannel.set(region, objs, TTL, true);
        StringBuilder res = new StringBuilder();
        objs.forEach((k, v) -> res.append(String.format("[%s,%s]<=%s(TTL:%d)%n", region, k, v, TTL)));
        return res.toString();
    }
    @PostMapping("/get")
    public String testGet(@RequestParam String region,
                          @RequestParam String key) {

        List<String> keys = Arrays.asList(key.split(","));
        Map<String, CacheObject> values = cacheChannel.get(region, keys);
        StringBuilder res = new StringBuilder();
        if (values != null && values.size() > 0) {
            values.forEach((k, v) -> res.append(String.format("[%s,%s,L%d]=>%s(TTL:%d)%n", v.getRegion(), v.getKey(), v.getLevel(), v.getValue(), TTL)));
        } else {
            res.append("none!");
        }

        return res.toString();
    }
}