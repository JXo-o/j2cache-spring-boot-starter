package net.oschina.j2cache.cluster;

import net.oschina.j2cache.CacheProviderHolder;
import net.oschina.j2cache.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * ClassName: ClusterPolicy
 * Package: net.oschina.j2cache.cluster
 * Description: 缓存集群策略接口
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 0:59
 */
public interface ClusterPolicy {

    Logger log = LoggerFactory.getLogger(ClusterPolicy.class);

    /**
     * 连接到集群
     * @param props j2cache 配置信息
     * @param holder Cache Provider Instance
     */
    void connect(Properties props, CacheProviderHolder holder);

    /**
     * 发送消息
     * @param cmd command to send
     */
    void publish(Command cmd);

    /**
     * 发送清除缓存的命令
     * @param region 区域名称
     * @param keys   缓存键值
     */
    default void sendEvictCmd(String region, String...keys) {
        publish(new Command(Command.OPT_EVICT_KEY, region, keys));
    }

    /**
     * 发送清除整个缓存区域的命令
     * @param region 区域名称
     */
    default void sendClearCmd(String region) {
        publish(new Command(Command.OPT_CLEAR_KEY, region));
    }

    /**
     * 断开集群连接
     */
    void disconnect();

    /**
     * 删除本地某个缓存条目
     * @param region 区域名称
     * @param keys   缓存键值
     */
    void evict(String region, String... keys);

    /**
     * 清除本地整个缓存区域
     * @param region 区域名称
     */
    void clear(String region) ;

    /**
     * 判断是否本地实例的命令
     * @param cmd 命令信息
     * @return  true if the cmd sent by self
     */
    boolean isLocalCommand(Command cmd) ;

    /**
     * 处理缓存事件逻辑
     * @param cmd the received command
     */
    default void handleCommand(Command cmd) {
        try {
            if (cmd == null || isLocalCommand(cmd))
                return;

            switch (cmd.getOperator()) {
                case Command.OPT_JOIN:
                    log.info("Node-{} joined !", cmd.getSrc());
                    break;
                case Command.OPT_EVICT_KEY:
                    this.evict(cmd.getRegion(), cmd.getKeys());
                    log.debug("Received cache evict message, region=" + cmd.getRegion() + ",key=" + String.join(",", cmd.getKeys()));
                    break;
                case Command.OPT_CLEAR_KEY:
                    this.clear(cmd.getRegion());
                    log.debug("Received cache clear message, region=" + cmd.getRegion());
                    break;
                case Command.OPT_QUIT:
                    log.info("Node-{} quit !", cmd.getSrc());
                    break;
                default:
                    log.warn("Unknown message type = " + cmd.getOperator());
            }
        } catch (Exception e) {
            log.error("Failed to handle received msg", e);
        }
    }
}
