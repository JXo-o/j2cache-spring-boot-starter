package net.oschina.j2cache.service.cluster;

import net.oschina.j2cache.exception.CacheException;
import net.oschina.j2cache.service.cache.impl.CacheProviderHolder;
import net.oschina.j2cache.service.cache.impl.lettuce.LettuceCacheProvider;
import net.oschina.j2cache.service.cache.impl.redis.RedisPubSubClusterPolicy;
import net.oschina.j2cache.service.cluster.impl.JGroupsClusterPolicy;
import net.oschina.j2cache.service.cluster.impl.NoneClusterPolicy;
import net.oschina.j2cache.service.cluster.impl.RabbitMQClusterPolicy;
import net.oschina.j2cache.service.cluster.impl.RocketMQClusterPolicy;

import java.util.Properties;

/**
 * ClassName: ClusterPolicyFactory
 * Package: net.oschina.j2cache.service.cluster
 * Description: 集群策略工厂
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 1:00
 */
public class ClusterPolicyFactory {

    private ClusterPolicyFactory(){}

    /**
     * 初始化集群消息通知机制
     * @param holder  CacheProviderHolder instance
     * @param broadcast  j2cache.broadcast value
     * @param props  broadcast configuations
     * @return ClusterPolicy instance
     */
    public final static ClusterPolicy init(CacheProviderHolder holder, String broadcast, Properties props) {

        ClusterPolicy policy;
        if ("redis".equalsIgnoreCase(broadcast))
            policy = ClusterPolicyFactory.redis(props, holder);
        else if ("jgroups".equalsIgnoreCase(broadcast))
            policy = ClusterPolicyFactory.jgroups(props, holder);
        else if ("rabbitmq".equalsIgnoreCase(broadcast))
            policy = ClusterPolicyFactory.rabbitmq(props, holder);
        else if ("rocketmq".equalsIgnoreCase(broadcast))
            policy = ClusterPolicyFactory.rocketmq(props, holder);
        else if ("lettuce".equalsIgnoreCase(broadcast))
            policy = ClusterPolicyFactory.lettuce(props, holder);
        else if ("none".equalsIgnoreCase(broadcast))
            policy = new NoneClusterPolicy();
        else
            policy = ClusterPolicyFactory.custom(broadcast, props, holder);

        return policy;
    }

    /**
     * 使用 Redis 订阅和发布机制，该方法只能调用一次
     * @param props 框架配置
     * @return 返回 Redis 集群策略的实例
     */
    private final static ClusterPolicy redis(Properties props, CacheProviderHolder holder) {
        String name = props.getProperty("channel");
        RedisPubSubClusterPolicy policy = new RedisPubSubClusterPolicy(name, props);
        policy.connect(props, holder);
        return policy;
    }

    /**
     * 使用 JGroups 组播机制
     * @param props 框架配置
     * @return 返回 JGroups 集群策略的实例
     */
    private final static ClusterPolicy jgroups(Properties props, CacheProviderHolder holder) {
        String name = props.getProperty("channel.name");
        JGroupsClusterPolicy policy = new JGroupsClusterPolicy(name, props);
        policy.connect(props, holder);
        return policy;
    }

    /**
     * 使用 RabbitMQ 消息机制
     * @param props 框架配置
     * @return 返回 RabbitMQ 集群策略的实例
     */
    private final static ClusterPolicy rabbitmq(Properties props, CacheProviderHolder holder) {
        RabbitMQClusterPolicy policy = new RabbitMQClusterPolicy(props);
        policy.connect(props, holder);
        return policy;
    }

    private final static ClusterPolicy rocketmq(Properties props, CacheProviderHolder holder) {
        RocketMQClusterPolicy policy = new RocketMQClusterPolicy(props);
        policy.connect(props, holder);
        return policy;
    }

    private final static ClusterPolicy lettuce(Properties props, CacheProviderHolder holder) {
        LettuceCacheProvider policy = new LettuceCacheProvider();
        policy.connect(props, holder);
        return policy;
    }

    /**
     * 加载自定义的集群通知策略
     * @param classname
     * @param props
     * @return
     */
    private final static ClusterPolicy custom(String classname, Properties props, CacheProviderHolder holder) {
        try {
            ClusterPolicy policy = (ClusterPolicy)Class.forName(classname).newInstance();
            policy.connect(props, holder);
            return policy;
        } catch (Exception e) {
            throw new CacheException("Failed in load custom cluster policy. class = " + classname, e);
        }
    }

}

