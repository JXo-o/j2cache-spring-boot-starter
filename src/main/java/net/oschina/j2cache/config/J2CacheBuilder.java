package net.oschina.j2cache.config;

import net.oschina.j2cache.service.cache.CacheChannel;
import net.oschina.j2cache.service.cache.impl.CacheProviderHolder;
import net.oschina.j2cache.service.cluster.ClusterPolicy;
import net.oschina.j2cache.service.cluster.ClusterPolicyFactory;
import net.oschina.j2cache.service.cache.Level2Cache;
import net.oschina.j2cache.util.serializer.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ClassName: J2CacheBuilder
 * Package: net.oschina.j2cache
 * Description: 使用自定义配置构建 J2Cache
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 0:51
 */
public class J2CacheBuilder {

    private final static Logger log = LoggerFactory.getLogger(J2CacheBuilder.class);

    private CacheChannel channel;
    private CacheProviderHolder holder;
    private ClusterPolicy policy; //不同的广播策略
    private AtomicBoolean opened = new AtomicBoolean(false);
    private J2CacheProperties config;

    private J2CacheBuilder(J2CacheProperties config) {
        this.config = config;
    }

    /**
     * 初始化 J2Cache，这是一个很重的操作，请勿重复执行
     *
     * @param config j2cache config instance
     * @return J2CacheBuilder instance
     */
    public final static J2CacheBuilder init(J2CacheProperties config) {
        return new J2CacheBuilder(config);
    }

    /**
     * 返回缓存操作接口
     *
     * @return CacheChannel
     */
    public CacheChannel getChannel() {
        if (this.channel == null || !this.opened.get()) {
            synchronized (J2CacheBuilder.class) {
                if (this.channel == null || !this.opened.get()) {
                    this.initFromConfig(config);
                    /* 初始化缓存接口 */
                    this.channel = new CacheChannel(config, holder) {
                        @Override
                        public void sendClearCmd(String region) {
                            policy.sendClearCmd(region);
                        }

                        @Override
                        public void sendEvictCmd(String region, String... keys) {
                            policy.sendEvictCmd(region, keys);
                        }

                        @Override
                        public void close() {
                            super.close();
                            policy.disconnect();
                            holder.shutdown();
                            opened.set(false);
                        }
                    };
                    this.opened.set(true);
                }
            }
        }
        return this.channel;
    }

    /**
     * 关闭 J2Cache
     */
    public void close() {
        this.channel.close();
        this.channel = null;
    }

    /**
     * 加载配置
     *
     * @return
     * @throws IOException
     */
    private void initFromConfig(J2CacheProperties config) {
        SerializationUtils.init(config.getSerialization(), config.getSubProperties(config.getSerialization()));
        //初始化两级的缓存管理
        this.holder = CacheProviderHolder.init(config, (region, key) -> {
            //当一级缓存中的对象失效时，自动清除二级缓存中的数据
            Level2Cache level2 = this.holder.getLevel2Cache(region);
            level2.evict(key);
            if (!level2.supportTTL()) {
                //再一次清除一级缓存是为了避免缓存失效时再次从 L2 获取到值
                this.holder.getLevel1Cache(region).evict(key);
            }
            log.debug("Level 1 cache object expired, evict level 2 cache object [{},{}]", region, key);
            if (policy != null)
                policy.sendEvictCmd(region, key);
        });

        policy = ClusterPolicyFactory.init(holder, config.getBroadcast(), config.getBroadcastProperties());
        log.info("Using cluster policy : {}", policy.getClass().getName());
    }

    public ClusterPolicy getPolicy() {
        return policy;
    }

}
