package net.oschina.j2cache.controller;

import net.oschina.j2cache.service.cache.CacheChannel;
import net.oschina.j2cache.model.CacheObject;
import net.oschina.j2cache.J2Cache;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;

import java.util.*;

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
    private final CacheChannel cacheChannel = J2Cache.getChannel();
    @PostMapping("/set")
    public String testSet(@NonNull @RequestParam String region,
                          @NonNull @RequestParam String key,
                          @NonNull @RequestParam String value) {

        String[] keys = key.split(",");
        String[] values = value.split(",");
        StringBuilder res = new StringBuilder();

        if (keys.length != values.length)
            return "Error: keys.length != values.length";
        for (int i = 0; i < keys.length; i++) {
            if ("null".equalsIgnoreCase(values[i]))
                values[i] = null;
        }

        if (keys.length == 1) {
            cacheChannel.set(region, keys[0], values[0], TTL, true);
            res.append(String.format("[%s,%s]<=%s(TTL:%d)%n", region, keys[0], values[0], TTL));
        } else {
            Map<String, Object> objs = new HashMap<>();
            for (int i = 0; i < keys.length; i++) {
                objs.put(keys[i], values[i]);
            }
            cacheChannel.set(region, objs, TTL, true);
            objs.forEach((k, v) -> res.append(String.format("[%s,%s]<=%s(TTL:%d)%n", region, k, v, TTL)));
        }

        return res.toString();
    }

    @PostMapping("/get")
    public String testGet(@NonNull @RequestParam String region,
                          @NonNull @RequestParam String key) {

        StringBuilder res = new StringBuilder();

        if (!key.contains(",")) {
            CacheObject value = cacheChannel.get(region, key);
            if (value.getValue() != null)
                res.append(String.format("[%s,%s,L%d]=>%s(TTL:%d)%n",
                        value.getRegion(), value.getKey(), value.getLevel(), value.getValue(), TTL));
        } else {
            List<String> keys = Arrays.asList(key.split(","));
            Map<String, CacheObject> values = cacheChannel.get(region, keys);
            if (values != null && !values.isEmpty()) {
                values.forEach((k, v) -> {
                    if (v.getValue() != null) {
                        res.append(String.format("[%s,%s,L%d]=>%s(TTL:%d)%n",
                                v.getRegion(), v.getKey(), v.getLevel(), v.getValue(), TTL));
                    }
                });
            } else {
                res.append("none!");
            }
        }

        return res.toString();
    }

    @PostMapping("/evict")
    public String testEvict(@NonNull @RequestParam String region,
                            @NonNull @RequestParam String key) {

        StringBuilder res = new StringBuilder();
        String[] keys = key.split(",");
        cacheChannel.evict(region, keys);

        for (String k : keys) {
            res.append(String.format("[%s,%s]=>null%n", region, k));
        }

        return res.toString();
    }

    @PostMapping("/clear")
    public String testClear(@NonNull @RequestParam String region) {

        StringBuilder res = new StringBuilder();

        cacheChannel.clear(region);
        res.append(String.format("Cache [%s] clear.%n", region));

        return res.toString();
    }

    @PostMapping("/regions")
    public String testRegions() {

        StringBuilder res = new StringBuilder();

        res.append("Regions:\n");
        cacheChannel.regions().forEach(r -> res.append(r).append("\n"));

        return res.toString();
    }

    @PostMapping("/keys")
    public String testKeys(@NonNull @RequestParam String region) {

        StringBuilder res = new StringBuilder();

        Collection<String> keys = cacheChannel.keys(region);
        if (keys != null) {
            res.append(String.format("[%s:keys] => (%s)(TTL:%d)%n", region, String.join(",", keys), TTL));
        } else {
            res.append("none!");
        }

        return res.toString();
    }

    @PostMapping("/ttl")
    public String testTtl(@NonNull @RequestParam String ttl) {

        StringBuilder res = new StringBuilder();

        try {
            Long.parseLong(ttl);
        } catch (NumberFormatException e) {
            return "Error: ttl is not a number";
        }
        TTL = Long.parseLong(ttl);
        res.append(String.format("TTL <= %d%n", TTL));

        return res.toString();
    }

    @PostMapping("/help")
    public String testHelp() {

        return """
                Usage:
                Notice: Separate multiple keys or values with ','
                http://localhost:9998/J2Cache/set - <region> <key> <value> - Set a value to cache
                http://localhost:9998/J2Cache/get <region> <key> - Get a value from cache
                http://localhost:9998/J2Cache/evict <region> <key> - Evict a value from cache
                http://localhost:9998/J2Cache/clear <region> - Clear cache
                http://localhost:9998/J2Cache/regions - List all regions
                http://localhost:9998/J2Cache/keys <region> - List all keys in region
                http://localhost:9998/J2Cache/ttl <ttl> - Set default TTL
                http://localhost:9998/J2Cache/help - Show this help message
                """;
    }

}