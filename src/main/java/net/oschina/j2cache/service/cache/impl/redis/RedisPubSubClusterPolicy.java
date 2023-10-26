package net.oschina.j2cache.service.cache.impl.redis;

import net.oschina.j2cache.service.cache.impl.CacheProviderHolder;
import net.oschina.j2cache.model.Command;
import net.oschina.j2cache.service.cluster.ClusterPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.util.Pool;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * ClassName: RedisPubSubClusterPolicy
 * Package: net.oschina.j2cache.service.cache.impl.redis
 * Description:
 * 使用 Redis 的订阅和发布进行集群中的节点通知
 * 该策略器使用 j2cache.properties 中的 redis 配置自行保持两个到 redis 的连接用于发布和订阅消息（并在失败时自动重连）
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 1:41
 */
public class RedisPubSubClusterPolicy extends JedisPubSub implements ClusterPolicy {

    private final static Logger log = LoggerFactory.getLogger(RedisPubSubClusterPolicy.class);

    private int LOCAL_COMMAND_ID = Command.genRandomSrc(); //命令源标识，随机生成，每个节点都有唯一标识
    private static int CONNECT_TIMEOUT = 5000;    //Redis连接超时时间
    private static int SO_TIMEOUT = 5000;
    private static int MAX_ATTEMPTS = 3;

    private Pool<Jedis> client;
    private JedisCluster cluster;
    private String channel;
    private CacheProviderHolder holder;
    private boolean clusterMode = false;

    public RedisPubSubClusterPolicy(String channel, Properties props){
        this.channel = channel;
        int timeout = Integer.parseInt((String)props.getOrDefault("timeout", "2000"));
        String password = props.getProperty("password");
        if(password != null && password.trim().length() == 0)
            password = null;

        int database = Integer.parseInt(props.getProperty("database", "0"));
        boolean ssl = Boolean.valueOf(props.getProperty("ssl", "false"));

        JedisPoolConfig config = RedisUtils.newPoolConfig(props, null);

        String node = props.getProperty("channel.host");
        if(node == null || node.trim().length() == 0)
            node = props.getProperty("hosts");

        String mode = props.getProperty("mode");
        if ("sentinel".equalsIgnoreCase(mode)) {
            Set<String> hosts = new HashSet();
            hosts.addAll(Arrays.asList(node.split(",")));
            String masterName = props.getProperty("cluster_name", "j2cache");
            this.client = new JedisSentinelPool(masterName, hosts, config, timeout, password, database);
        }
//        else if ("cluster".equalsIgnoreCase(mode)) {
//            String[] nodeArray = node.split(",");
//            Set<HostAndPort> nodeSet = new HashSet<HostAndPort>(nodeArray.length);
//            for (String nodeItem : nodeArray) {
//                String[] arr = nodeItem.split(":");
//                nodeSet.add(new HostAndPort(arr[0], Integer.valueOf(arr[1])));
//            }
//            JedisPoolConfig poolConfig = RedisUtils.newPoolConfig(props, null);
//            this.cluster = new JedisCluster(nodeSet, CONNECT_TIMEOUT, SO_TIMEOUT, MAX_ATTEMPTS, password, poolConfig);
//            this.clusterMode = true;
//        }
        else {
            node = node.split(",")[0]; //取第一台主机
            String[] infos = node.split(":");
            String host = infos[0];
            int port = (infos.length > 1)?Integer.parseInt(infos[1]):6379;
            this.client = new JedisPool(config, host, port, timeout, password, database, ssl);
        }
    }

    @Override
    public boolean isLocalCommand(Command cmd) {
        return cmd.getSrc() == LOCAL_COMMAND_ID;
    }

    /**
     * 删除本地某个缓存条目
     * @param region 区域名称
     * @param keys   缓存键值
     */
    @Override
    public void evict(String region, String... keys) {
        holder.getLevel1Cache(region).evict(keys);
    }

    /**
     * 清除本地整个缓存区域
     * @param region 区域名称
     */
    @Override
    public void clear(String region) {
        holder.getLevel1Cache(region).clear();
    }

    /**
     * 加入 Redis 的发布订阅频道
     */
    @Override
    public void connect(Properties props, CacheProviderHolder holder) {
        long ct = System.currentTimeMillis();
        this.holder = holder;

        this.publish(Command.join());

        Thread subscribeThread = new Thread(()-> {
            if (clusterMode) {
                // 如果出现集群节点宕机，需要重连
                while (cluster != null) {
                    try {
                        this.cluster.subscribe(this, channel);
                        break;
                    } catch (Exception e) {
                        log.error("failed connect redis cluster, reconnect it.", e);
                        e.printStackTrace();
                        if (cluster != null) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ie) {
                                break;
                            }
                        }
                    }
                }
            } else {
                //当 Redis 重启会导致订阅线程断开连接，需要进行重连
                while(!client.isClosed()) {
                    try (Jedis jedis = client.getResource()){
                        jedis.subscribe(this, channel);
                        log.info("Disconnect to redis channel: {}", channel);
                        break;
                    } catch (JedisConnectionException e) {
                        log.error("Failed connect to redis, reconnect it.", e);
                        if(!client.isClosed())
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ie){
                                break;
                            }
                    }
                }
            }
        }, "RedisSubscribeThread");

        subscribeThread.setDaemon(true);
        subscribeThread.start();

        log.info("Connected to redis channel:{}, time {} ms.", channel, (System.currentTimeMillis()-ct));
    }

    /**
     * 退出 Redis 发布订阅频道
     */
    @Override
    public void disconnect() {
        try {
            this.publish(Command.quit());
            if(this.isSubscribed())
                this.unsubscribe();
        } finally {
            close();
            //this.client.close();
            //subscribeThread will auto terminate
        }
    }

    @Override
    public void publish(Command cmd) {
        cmd.setSrc(LOCAL_COMMAND_ID);
        if (this.clusterMode) {
            this.cluster.publish(channel, cmd.json());
        } else {
            try (Jedis jedis = client.getResource()) {
                jedis.publish(channel, cmd.json());
            }
        }
    }

    /**
     * 当接收到订阅频道获得的消息时触发此方法
     * @param channel 频道名称
     * @param message 消息体
     */
    @Override
    public void onMessage(String channel, String message) {
        Command cmd = Command.parse(message);
        handleCommand(cmd);
    }

    private void close() {
        try {
            if (this.client != null) {
                this.client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.cluster != null) {
            this.cluster.close();
        }
        this.cluster = null;
    }
}

