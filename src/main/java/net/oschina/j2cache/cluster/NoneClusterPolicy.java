package net.oschina.j2cache.cluster;

import net.oschina.j2cache.CacheProviderHolder;
import net.oschina.j2cache.Command;

import java.util.Properties;

/**
 * ClassName: NoneClusterPolicy
 * Package: net.oschina.j2cache.cluster
 * Description: 实现空的集群通知策略
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 1:01
 */
public class NoneClusterPolicy implements ClusterPolicy {

    private int LOCAL_COMMAND_ID = Command.genRandomSrc(); //命令源标识，随机生成，每个节点都有唯一标识

    @Override
    public boolean isLocalCommand(Command cmd) {
        return cmd.getSrc() == LOCAL_COMMAND_ID;
    }

    @Override
    public void connect(Properties props, CacheProviderHolder holder) {
    }

    @Override
    public void disconnect() {
    }

    @Override
    public void publish(Command cmd) {
    }

    @Override
    public void evict(String region, String... keys) {
    }

    @Override
    public void clear(String region) {
    }
}
