package net.oschina.j2cache.service.cache.impl.memcached;

import net.oschina.j2cache.model.CacheObject;
import net.oschina.j2cache.service.cache.*;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.auth.AuthInfo;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.utils.AddrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClassName: XmemcachedCacheProvider
 * Package: net.oschina.j2cache.service.cache.impl.memcached
 * Description: Memcached 缓存管理
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 1:32
 */
public class XmemcachedCacheProvider implements CacheProvider {

    private static final Logger log = LoggerFactory.getLogger(XmemcachedCacheProvider.class);
    private MemcachedClient client ;

    private final ConcurrentHashMap<String, Level2Cache> regions = new ConcurrentHashMap();

    @Override
    public String name() {
        return "memcached";
    }

    @Override
    public void start(Properties props) {

        long ct = System.currentTimeMillis();

        String servers = props.getProperty("servers", "127.0.0.1:11211");
        String username = props.getProperty("username", "");
        String password = props.getProperty("password", "");
        MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil.getAddresses(servers));
        builder.setCommandFactory(new BinaryCommandFactory());
        boolean needAuth = username != null && password != null && username.trim().length() > 0 && password.trim().length() > 0;
        if(needAuth)
            builder.addAuthInfo(AddrUtil.getOneAddress(servers), AuthInfo.typical(username, password));

        builder.setConnectionPoolSize(Integer.valueOf(props.getProperty("connectionPoolSize", "10")));
        builder.setConnectTimeout(Long.valueOf(props.getProperty("connectTimeout", "1000")));
        builder.setHealSessionInterval(Long.valueOf(props.getProperty("healSessionInterval", "1000")));
        builder.setMaxQueuedNoReplyOperations(Integer.valueOf(props.getProperty("maxQueuedNoReplyOperations", "100")));
        builder.setOpTimeout(Long.valueOf(props.getProperty("opTimeout", "100")));
        builder.setSanitizeKeys("true".equalsIgnoreCase(props.getProperty("sanitizeKeys", "false")));

        try {
            client = builder.build();

            log.info("Memcached client starts with servers({}),auth({}),pool-size({}),time({}ms)",
                    servers,
                    needAuth,
                    builder.getConfiguration().getSelectorPoolSize(),
                    System.currentTimeMillis() - ct
            );
        } catch (IOException e) {
            log.error("Failed to connect to memcached", e);
        }
    }

    @Override
    public int level() {
        return CacheObject.LEVEL_2;
    }

    @Override
    public Cache buildCache(String region, CacheExpiredListener listener) {
        return regions.computeIfAbsent(region, v -> new MemCache(region, client));
    }

    @Override
    public Cache buildCache(String region, long timeToLiveInSeconds, CacheExpiredListener listener) {
        return buildCache(region, listener);
    }

    @Override
    public Collection<CacheChannel.Region> regions() {
        return Collections.emptyList();
    }

    @Override
    public void stop() {
        try {
            this.regions().clear();
            this.client.shutdown();
        } catch (IOException e) {
            log.error("Failed to disconnect to memcached", e);
        }
    }
}
