package net.oschina.j2cache.service.cluster.impl;

import com.rabbitmq.client.*;
import net.oschina.j2cache.exception.CacheException;
import net.oschina.j2cache.service.cache.impl.CacheProviderHolder;
import net.oschina.j2cache.model.Command;
import net.oschina.j2cache.service.cluster.ClusterPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * ClassName: RabbitMQClusterPolicy
 * Package: net.oschina.j2cache.service.cluster.impl
 * Description: 使用 RabbitMQ 实现集群内节点的数据通知（用于对数据一致性要求特别严格的场景）
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 1:02
 */
public class RabbitMQClusterPolicy implements ClusterPolicy, Consumer {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQClusterPolicy.class);

    private int LOCAL_COMMAND_ID = Command.genRandomSrc(); //命令源标识，随机生成，每个节点都有唯一标识

    private static final String EXCHANGE_TYPE = "fanout";

    private CacheProviderHolder holder;

    private ConnectionFactory factory;
    private Connection conn_publisher;
    private Connection conn_consumer;
    private Channel channel_publisher;
    private Channel channel_consumer;
    private String exchange;

    /**
     * @param props RabbitMQ 配置信息
     */
    public RabbitMQClusterPolicy(Properties props){
        this.exchange = props.getProperty("exchange", "j2cache");
        factory = new ConnectionFactory();
        factory.setHost(props.getProperty("host" , "127.0.0.1"));
        factory.setPort(Integer.valueOf(props.getProperty("port", "5672")));
        factory.setUsername(props.getProperty("username" , null));
        factory.setPassword(props.getProperty("password" , null));
        factory.setVirtualHost(props.getProperty("virtualHost"));
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
    public void evict(String region, String... keys) {
        holder.getLevel1Cache(region).evict(keys);
    }

    /**
     * 清除本地整个缓存区域
     * @param region 区域名称
     */
    public void clear(String region) {
        holder.getLevel1Cache(region).clear();
    }

    @Override
    public void connect(Properties props,  CacheProviderHolder holder) {
        this.holder = holder;
        try {
            long ct = System.currentTimeMillis();
            conn_publisher = factory.newConnection();
            channel_publisher = conn_publisher.createChannel();
            channel_publisher.exchangeDeclare(exchange, EXCHANGE_TYPE);
            publish(Command.join());

            conn_consumer = factory.newConnection();
            channel_consumer = conn_consumer.createChannel();
            channel_consumer.exchangeDeclare(exchange, EXCHANGE_TYPE);
            String queueName = channel_consumer.queueDeclare().getQueue();
            channel_consumer.queueBind(queueName, exchange, "");

            channel_consumer.basicConsume(queueName, true, this);

            log.info("Connected to RabbitMQ:{}, time {}ms", conn_consumer, System.currentTimeMillis()-ct);
        } catch (Exception e) {
            throw new CacheException(String.format("Failed to connect to RabbitMQ (%s:%d)", factory.getHost(), factory.getPort()), e);
        }
    }

    /**
     * 发布消息
     * @param cmd 消息数据
     */
    @Override
    public void publish(Command cmd) {
        //失败重连
        if(!channel_publisher.isOpen() || !conn_publisher.isOpen()) {
            synchronized (RabbitMQClusterPolicy.class) {
                if(!channel_publisher.isOpen() || !conn_publisher.isOpen()) {
                    try {
                        conn_publisher = factory.newConnection();
                        channel_publisher = conn_publisher.createChannel();
                    } catch(Exception e) {
                        throw new CacheException("Failed to connect to RabbitMQ!", e);
                    }
                }
            }
        }
        try {
            cmd.setSrc(LOCAL_COMMAND_ID);
            channel_publisher.basicPublish(exchange, "", null, cmd.json().getBytes());
        } catch (IOException e ) {
            throw new CacheException("Failed to publish cmd to RabbitMQ!", e);
        }
    }

    @Override
    public void disconnect() {
        try {
            publish(Command.quit());
        } finally {
            try {
                channel_publisher.close();
                conn_publisher.close();
            } catch(Exception e){}

            try {
                channel_consumer.close();
                conn_consumer.close();
            } catch(Exception e){}
        }
    }

    @Override
    public void handleDelivery(String s, Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) {
        handleCommand(Command.parse(new String(bytes)));
    }

    @Override
    public void handleConsumeOk(String s) {

    }

    @Override
    public void handleCancelOk(String s) {

    }

    @Override
    public void handleCancel(String s) {

    }

    @Override
    public void handleShutdownSignal(String s, ShutdownSignalException e) {

    }

    @Override
    public void handleRecoverOk(String s) {

    }

}

