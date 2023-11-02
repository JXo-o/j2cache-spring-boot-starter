package net.oschina.j2cache.service.cache.impl.redis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.args.*;
import redis.clients.jedis.commands.JedisBinaryCommands;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.params.*;
import redis.clients.jedis.resps.GeoRadiusResponse;
import redis.clients.jedis.resps.LCSMatchResult;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.resps.Tuple;
import redis.clients.jedis.util.KeyValue;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * ClassName: RedisClient
 * Package: net.oschina.j2cache.service.cache.impl.redis
 * Description: 封装各种模式的 Redis 客户端成统一接口
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 1:36
 */
public class RedisClient implements Closeable, AutoCloseable {

    private final static Logger log = LoggerFactory.getLogger(RedisClient.class);

    private final static int CONNECT_TIMEOUT = 5000;    //Redis连接超时时间
    private final static int SO_TIMEOUT = 5000;
    private final static int MAX_ATTEMPTS = 3;

    private ThreadLocal<JedisBinaryCommands> clients;

    private JedisCluster cluster;
    private JedisPool single;
    private JedisSentinelPool sentinel;
    //private ShardedJedisPool sharded;

    /**
     * RedisClient 构造器
     */
    public static class Builder {
        private String mode;
        private String hosts;
        private String password;
        private String cluster;
        private int database;
        private JedisPoolConfig poolConfig;
        private boolean ssl;

        public Builder(){}

        public Builder mode(String mode){
            if(mode == null || mode.trim().length() == 0)
                this.mode = "single";
            else
                this.mode = mode;
            return this;
        }
        public Builder hosts(String hosts){
            if(hosts == null || hosts.trim().length() == 0)
                this.hosts = "127.0.0.1:6379";
            else
                this.hosts = hosts;
            return this;
        }
        public Builder password(String password){
            if(password != null && password.trim().length() > 0)
                this.password = password;
            return this;
        }
        public Builder cluster(String cluster) {
            if(cluster == null || cluster.trim().length() == 0)
                this.cluster = "j2cache";
            else
                this.cluster = cluster;
            return this;
        }
        public Builder database(int database){
            this.database = database;
            return this;
        }
        public Builder poolConfig(JedisPoolConfig poolConfig){
            this.poolConfig = poolConfig;
            return this;
        }
        public Builder ssl(boolean ssl) {
            this.ssl = ssl;
            return this;
        }
        public RedisClient newClient() {
            return new RedisClient(mode, hosts, password, cluster, database, poolConfig, ssl);
        }
    }


    /**
     * 各种模式 Redis 客户端的封装
     * @param mode Redis 服务器运行模式
     * @param hosts Redis 主机连接信息
     * @param password  Redis 密码（如果有的话）
     * @param cluster_name  集群名称
     * @param database 数据库
     * @param poolConfig    连接池配置
     * @param ssl    使用ssl
     */
    private RedisClient(String mode, String hosts, String password, String cluster_name, int database, JedisPoolConfig poolConfig, boolean ssl) {
        password = (password != null && password.trim().length() > 0)? password.trim(): null;
        this.clients = new ThreadLocal<>();
        switch(mode){
            case "sentinel":
                Set<String> nodes = new HashSet<>();
                for(String node : hosts.split(","))
                    nodes.add(node);
                this.sentinel = new JedisSentinelPool(cluster_name, nodes, poolConfig, CONNECT_TIMEOUT, password, database);
                break;
            case "cluster":
                Set<HostAndPort> hps = new HashSet<>();
                for(String node : hosts.split(",")){
                    String[] infos = node.split(":");
                    String host = infos[0];
                    int port = (infos.length > 1)?Integer.parseInt(infos[1]):6379;
                    hps.add(new HostAndPort(host, port));
                }
                GenericObjectPoolConfig<Connection> poolConfig1 = new GenericObjectPoolConfig<>();
                poolConfig1.setMaxTotal(poolConfig.getMaxTotal());
                poolConfig1.setMaxIdle(poolConfig.getMaxIdle());
                poolConfig1.setMinIdle(poolConfig.getMinIdle());
                poolConfig1.setMaxWaitMillis(poolConfig.getMaxWaitMillis());
                poolConfig1.setTestOnBorrow(poolConfig.getTestOnBorrow());
                poolConfig1.setTestOnReturn(poolConfig.getTestOnReturn());
                poolConfig1.setTestWhileIdle(poolConfig.getTestWhileIdle());
                poolConfig1.setNumTestsPerEvictionRun(poolConfig.getNumTestsPerEvictionRun());
                poolConfig1.setTimeBetweenEvictionRunsMillis(poolConfig.getTimeBetweenEvictionRunsMillis());
                poolConfig1.setMinEvictableIdleTimeMillis(poolConfig.getMinEvictableIdleTimeMillis());
                poolConfig1.setSoftMinEvictableIdleTimeMillis(poolConfig.getSoftMinEvictableIdleTimeMillis());
                poolConfig1.setEvictionPolicyClassName(poolConfig.getEvictionPolicyClassName());
                poolConfig1.setBlockWhenExhausted(poolConfig.getBlockWhenExhausted());
                poolConfig1.setJmxEnabled(poolConfig.getJmxEnabled());
                poolConfig1.setJmxNamePrefix(poolConfig.getJmxNamePrefix());
                poolConfig1.setJmxNameBase(poolConfig.getJmxNameBase());
                poolConfig1.setFairness(poolConfig.getFairness());
                poolConfig1.setLifo(poolConfig.getLifo());
                this.cluster = new JedisCluster(hps, CONNECT_TIMEOUT, SO_TIMEOUT, MAX_ATTEMPTS, password, poolConfig1);
                break;
//            case "sharded":
//                List<JedisShardInfo> shards = new ArrayList<>();
//                try {
//                    for(String node : hosts.split(","))
//                        shards.add(new JedisShardInfo(new URI(node)));
//                } catch (URISyntaxException e) {
//                    throw new JedisConnectionException(e);
//                }
//                this.sharded = new ShardedJedisPool(poolConfig, shards);
//                break;
            default:
                for(String node : hosts.split(",")) {
                    String[] infos = node.split(":");
                    String host = infos[0];
                    int port = (infos.length > 1)?Integer.parseInt(infos[1]):6379;
                    this.single = new JedisPool(poolConfig, host, port, CONNECT_TIMEOUT, password, database, ssl);
                    break;
                }
                if(!"single".equalsIgnoreCase(mode))
                    log.warn("Redis mode [{}] not defined. Using 'single'.", mode);
                break;
        }
    }

    /**
     * 获取客户端接口
     * @return 返回基本的 Jedis 二进制命令接口
     */
    public JedisBinaryCommands get() {
        JedisBinaryCommands client = clients.get();
        if(client == null) {
            if (single != null)
                client = single.getResource();
            else if (sentinel != null)
                client = sentinel.getResource();
//            else if (sharded != null)
//                client = sharded.getResource();
            else if (cluster != null)
                client = toJedisBinaryCommands(cluster);

            clients.set(client);
        }
        return client;
    }

    /**
     * 释放当前 Redis 连接
     */
    public void release() {
        Closeable client = (Closeable) clients.get();
        if(client != null) {
            //JedisCluster 会自动释放连接
            if(client instanceof Closeable && !(client instanceof JedisCluster)) {
                try {
                    client.close();
                } catch(IOException e) {
                    log.error("Failed to release jedis connection.", e);
                }
            }
            clients.remove();
        }
    }

    /**
     * 释放连接池
     * @throws IOException  io close exception
     */
    @Override
    public void close() throws IOException {
        if(single != null)
            single.close();
        if(sentinel != null)
            sentinel.close();
        if(cluster != null)
            cluster.close();
//        if(sharded != null)
//            sharded.close();
    }

    /**
     * 为了变态的 jedis 接口设计，搞了五百多行垃圾代码
     * @param cluster Jedis 集群实例
     * @return
     */
    private JedisBinaryCommands toJedisBinaryCommands(JedisCluster cluster) {
        return new JedisBinaryCommands(){
            @Override
            public byte[] xadd(byte[] bytes, XAddParams xAddParams, Map<byte[], byte[]> map) {
                return new byte[0];
            }

            @Override
            public long xlen(byte[] bytes) {
                return 0;
            }

            @Override
            public List<Object> xrange(byte[] bytes, byte[] bytes1, byte[] bytes2) {
                return null;
            }

            @Override
            public List<Object> xrange(byte[] bytes, byte[] bytes1, byte[] bytes2, int i) {
                return null;
            }

            @Override
            public List<Object> xrevrange(byte[] bytes, byte[] bytes1, byte[] bytes2) {
                return null;
            }

            @Override
            public List<Object> xrevrange(byte[] bytes, byte[] bytes1, byte[] bytes2, int i) {
                return null;
            }

            @Override
            public long xack(byte[] bytes, byte[] bytes1, byte[]... bytes2) {
                return 0;
            }

            @Override
            public String xgroupCreate(byte[] bytes, byte[] bytes1, byte[] bytes2, boolean b) {
                return null;
            }

            @Override
            public String xgroupSetID(byte[] bytes, byte[] bytes1, byte[] bytes2) {
                return null;
            }

            @Override
            public long xgroupDestroy(byte[] bytes, byte[] bytes1) {
                return 0;
            }

            @Override
            public boolean xgroupCreateConsumer(byte[] bytes, byte[] bytes1, byte[] bytes2) {
                return false;
            }

            @Override
            public long xgroupDelConsumer(byte[] bytes, byte[] bytes1, byte[] bytes2) {
                return 0;
            }

            @Override
            public long xdel(byte[] bytes, byte[]... bytes1) {
                return 0;
            }

            @Override
            public long xtrim(byte[] bytes, long l, boolean b) {
                return 0;
            }

            @Override
            public long xtrim(byte[] bytes, XTrimParams xTrimParams) {
                return 0;
            }

            @Override
            public Object xpending(byte[] bytes, byte[] bytes1) {
                return null;
            }

            @Override
            public List<Object> xpending(byte[] bytes, byte[] bytes1, XPendingParams xPendingParams) {
                return null;
            }

            @Override
            public List<byte[]> xclaim(byte[] bytes, byte[] bytes1, byte[] bytes2, long l, XClaimParams xClaimParams, byte[]... bytes3) {
                return null;
            }

            @Override
            public List<byte[]> xclaimJustId(byte[] bytes, byte[] bytes1, byte[] bytes2, long l, XClaimParams xClaimParams, byte[]... bytes3) {
                return null;
            }

            @Override
            public List<Object> xautoclaim(byte[] bytes, byte[] bytes1, byte[] bytes2, long l, byte[] bytes3, XAutoClaimParams xAutoClaimParams) {
                return null;
            }

            @Override
            public List<Object> xautoclaimJustId(byte[] bytes, byte[] bytes1, byte[] bytes2, long l, byte[] bytes3, XAutoClaimParams xAutoClaimParams) {
                return null;
            }

            @Override
            public Object xinfoStream(byte[] bytes) {
                return null;
            }

            @Override
            public Object xinfoStreamFull(byte[] bytes) {
                return null;
            }

            @Override
            public Object xinfoStreamFull(byte[] bytes, int i) {
                return null;
            }

            @Override
            public List<Object> xinfoGroups(byte[] bytes) {
                return null;
            }

            @Override
            public List<Object> xinfoConsumers(byte[] bytes, byte[] bytes1) {
                return null;
            }

            @Override
            public List<Object> xread(XReadParams xReadParams, Map.Entry<byte[], byte[]>... entries) {
                return null;
            }

            @Override
            public List<Object> xreadGroup(byte[] bytes, byte[] bytes1, XReadGroupParams xReadGroupParams, Map.Entry<byte[], byte[]>... entries) {
                return null;
            }

            @Override
            public Object eval(byte[] bytes) {
                return null;
            }

            @Override
            public Object eval(byte[] bytes, int i, byte[]... bytes1) {
                return null;
            }

            @Override
            public Object eval(byte[] bytes, List<byte[]> list, List<byte[]> list1) {
                return null;
            }

            @Override
            public Object evalReadonly(byte[] bytes, List<byte[]> list, List<byte[]> list1) {
                return null;
            }

            @Override
            public Object evalsha(byte[] bytes) {
                return null;
            }

            @Override
            public Object evalsha(byte[] bytes, int i, byte[]... bytes1) {
                return null;
            }

            @Override
            public Object evalsha(byte[] bytes, List<byte[]> list, List<byte[]> list1) {
                return null;
            }

            @Override
            public Object evalshaReadonly(byte[] bytes, List<byte[]> list, List<byte[]> list1) {
                return null;
            }

            @Override
            public Object fcall(byte[] bytes, List<byte[]> list, List<byte[]> list1) {
                return null;
            }

            @Override
            public Object fcallReadonly(byte[] bytes, List<byte[]> list, List<byte[]> list1) {
                return null;
            }

            @Override
            public String functionDelete(byte[] bytes) {
                return null;
            }

            @Override
            public byte[] functionDump() {
                return new byte[0];
            }

            @Override
            public String functionFlush() {
                return null;
            }

            @Override
            public String functionFlush(FlushMode flushMode) {
                return null;
            }

            @Override
            public String functionKill() {
                return null;
            }

            @Override
            public List<Object> functionListBinary() {
                return null;
            }

            @Override
            public List<Object> functionList(byte[] bytes) {
                return null;
            }

            @Override
            public List<Object> functionListWithCodeBinary() {
                return null;
            }

            @Override
            public List<Object> functionListWithCode(byte[] bytes) {
                return null;
            }

            @Override
            public String functionLoad(byte[] bytes) {
                return null;
            }

            @Override
            public String functionLoadReplace(byte[] bytes) {
                return null;
            }

            @Override
            public String functionRestore(byte[] bytes) {
                return null;
            }

            @Override
            public String functionRestore(byte[] bytes, FunctionRestorePolicy functionRestorePolicy) {
                return null;
            }

            @Override
            public Object functionStatsBinary() {
                return null;
            }

            @Override
            public String set(byte[] bytes, byte[] bytes1) {
                return cluster.set(bytes, bytes1);
            }

            @Override
            public String set(byte[] bytes, byte[] bytes1, SetParams var) {
                return null;
            }

            @Override
            public byte[] get(byte[] bytes) {
                return cluster.get(bytes);
            }

            @Override
            public byte[] setGet(byte[] bytes, byte[] bytes1) {
                return new byte[0];
            }

            @Override
            public byte[] setGet(byte[] bytes, byte[] bytes1, SetParams setParams) {
                return new byte[0];
            }

            @Override
            public byte[] getDel(byte[] bytes) {
                return new byte[0];
            }

            @Override
            public byte[] getEx(byte[] bytes, GetExParams getExParams) {
                return new byte[0];
            }

            @Override
            public boolean exists(byte[] bytes) {
                return cluster.exists(bytes);
            }

            @Override
            public long exists(byte[]... bytes) {
                return 0;
            }

            @Override
            public long persist(byte[] bytes) {
                return cluster.persist(bytes);
            }

            @Override
            public String type(byte[] bytes) {
                return cluster.type(bytes);
            }

            @Override
            public byte[] dump(byte[] bytes) {
                return new byte[0];
            }

            @Override
            public String restore(byte[] bytes, long l, byte[] bytes1) {
                return null;
            }

            @Override
            public String restore(byte[] bytes, long l, byte[] bytes1, RestoreParams restoreParams) {
                return null;
            }

            @Override
            public long expire(byte[] bytes, long i) {
                return cluster.expire(bytes, i);
            }

            @Override
            public long expire(byte[] bytes, long l, ExpiryOption expiryOption) {
                return 0;
            }

            @Override
            public long pexpire(byte[] bytes, long l) {
                return cluster.pexpire(bytes, l);
            }

            @Override
            public long pexpire(byte[] bytes, long l, ExpiryOption expiryOption) {
                return 0;
            }

            @Override
            public long expireTime(byte[] bytes) {
                return 0;
            }

            @Override
            public long pexpireTime(byte[] bytes) {
                return 0;
            }

            @Override
            public long expireAt(byte[] bytes, long l) {
                return cluster.expireAt(bytes, l);
            }

            @Override
            public long expireAt(byte[] bytes, long l, ExpiryOption expiryOption) {
                return 0;
            }

            @Override
            public long pexpireAt(byte[] bytes, long l) {
                return cluster.pexpireAt(bytes, l);
            }

            @Override
            public long pexpireAt(byte[] bytes, long l, ExpiryOption expiryOption) {
                return 0;
            }

            @Override
            public long ttl(byte[] bytes) {
                return cluster.ttl(bytes);
            }

            @Override
            public long pttl(byte[] bytes) {
                return 0;
            }

            @Override
            public long touch(byte[] bytes) {
                return 0;
            }

            @Override
            public long touch(byte[]... bytes) {
                return 0;
            }

            @Override
            public boolean setbit(byte[] bytes, long l, boolean b) {
                return cluster.setbit(bytes, l, b);
            }

            @Override
            public boolean getbit(byte[] bytes, long l) {
                return cluster.getbit(bytes, l);
            }

            @Override
            public long setrange(byte[] bytes, long l, byte[] bytes1) {
                return cluster.setrange(bytes, l, bytes1);
            }

            @Override
            public byte[] getrange(byte[] bytes, long l, long l1) {
                return cluster.getrange(bytes,l,l1);
            }

            @Override
            public byte[] getSet(byte[] bytes, byte[] bytes1) {
                return cluster.getSet(bytes, bytes1);
            }

            @Override
            public long setnx(byte[] bytes, byte[] bytes1) {
                return cluster.setnx(bytes, bytes1);
            }

            @Override
            public String setex(byte[] bytes, long i, byte[] bytes1) {
                return cluster.setex(bytes, i, bytes1);
            }

            @Override
            public String psetex(byte[] bytes, long l, byte[] bytes1) {
                return null;
            }

            @Override
            public List<byte[]> mget(byte[]... bytes) {
                return null;
            }

            @Override
            public String mset(byte[]... bytes) {
                return null;
            }

            @Override
            public long msetnx(byte[]... bytes) {
                return 0;
            }

            @Override
            public long decrBy(byte[] bytes, long l) {
                return cluster.decrBy(bytes, l);
            }

            @Override
            public long decr(byte[] bytes) {
                return cluster.decr(bytes);
            }

            @Override
            public long incrBy(byte[] bytes, long l) {
                return cluster.incrBy(bytes, l);
            }

            @Override
            public double incrByFloat(byte[] bytes, double v) {
                return cluster.incrByFloat(bytes, v);
            }

            @Override
            public long incr(byte[] bytes) {
                return cluster.incr(bytes);
            }

            @Override
            public long append(byte[] bytes, byte[] bytes1) {
                return cluster.append(bytes, bytes1);
            }

            @Override
            public byte[] substr(byte[] bytes, int i, int i1) {
                return cluster.substr(bytes, i, i1);
            }

            @Override
            public long hset(byte[] bytes, byte[] bytes1, byte[] bytes2) {
                return cluster.hset(bytes, bytes1, bytes2);
            }

            @Override
            public long hset(byte[] bytes, Map<byte[], byte[]> map) {
                return 0;
            }

            @Override
            public byte[] hget(byte[] bytes, byte[] bytes1) {
                return cluster.hget(bytes, bytes1);
            }

            @Override
            public long hsetnx(byte[] bytes, byte[] bytes1, byte[] bytes2) {
                return cluster.hsetnx(bytes, bytes1, bytes2);
            }

            @Override
            public String hmset(byte[] bytes, Map<byte[], byte[]> map) {
                return cluster.hmset(bytes, map);
            }

            @Override
            public List<byte[]> hmget(byte[] bytes, byte[]... bytes1) {
                return cluster.hmget(bytes, bytes1);
            }

            @Override
            public long hincrBy(byte[] bytes, byte[] bytes1, long l) {
                return cluster.hincrBy(bytes, bytes1, l);
            }

            @Override
            public double hincrByFloat(byte[] bytes, byte[] bytes1, double v) {
                return cluster.hincrByFloat(bytes, bytes1, v);
            }

            @Override
            public boolean hexists(byte[] bytes, byte[] bytes1) {
                return cluster.hexists(bytes, bytes1);
            }

            @Override
            public long hdel(byte[] bytes, byte[]... bytes1) {
                return cluster.hdel(bytes, bytes1);
            }

            @Override
            public long hlen(byte[] bytes) {
                return cluster.hlen(bytes);
            }

            @Override
            public Set<byte[]> hkeys(byte[] bytes) {
                return cluster.hkeys(bytes);
            }

            @Override
            public List<byte[]> hvals(byte[] bytes) {
                return cluster.hvals(bytes);
            }

            @Override
            public Map<byte[], byte[]> hgetAll(byte[] bytes) {
                return cluster.hgetAll(bytes);
            }

            @Override
            public byte[] hrandfield(byte[] bytes) {
                return new byte[0];
            }

            @Override
            public List<byte[]> hrandfield(byte[] bytes, long l) {
                return null;
            }

            @Override
            public List<Map.Entry<byte[], byte[]>> hrandfieldWithValues(byte[] bytes, long l) {
                return null;
            }

            @Override
            public long rpush(byte[] bytes, byte[]... bytes1) {
                return cluster.rpush(bytes, bytes1);
            }

            @Override
            public long lpush(byte[] bytes, byte[]... bytes1) {
                return cluster.lpush(bytes, bytes1);
            }

            @Override
            public long llen(byte[] bytes) {
                return cluster.llen(bytes);
            }

            @Override
            public List<byte[]> lrange(byte[] bytes, long l, long l1) {
                return cluster.lrange(bytes, l, l1);
            }

            @Override
            public String ltrim(byte[] bytes, long l, long l1) {
                return cluster.ltrim(bytes, l, l1);
            }

            @Override
            public byte[] lindex(byte[] bytes, long l) {
                return cluster.lindex(bytes, l);
            }

            @Override
            public String lset(byte[] bytes, long l, byte[] bytes1) {
                return cluster.lset(bytes, l, bytes1);
            }

            @Override
            public long lrem(byte[] bytes, long l, byte[] bytes1) {
                return cluster.lrem(bytes, l, bytes1);
            }

            @Override
            public byte[] lpop(byte[] bytes) {
                return cluster.lpop(bytes);
            }

            @Override
            public List<byte[]> lpop(byte[] bytes, int i) {
                return null;
            }

            @Override
            public Long lpos(byte[] bytes, byte[] bytes1) {
                return null;
            }

            @Override
            public Long lpos(byte[] bytes, byte[] bytes1, LPosParams lPosParams) {
                return null;
            }

            @Override
            public List<Long> lpos(byte[] bytes, byte[] bytes1, LPosParams lPosParams, long l) {
                return null;
            }

            @Override
            public byte[] rpop(byte[] bytes) {
                return cluster.rpop(bytes);
            }

            @Override
            public List<byte[]> rpop(byte[] bytes, int i) {
                return null;
            }

            @Override
            public long sadd(byte[] bytes, byte[]... bytes1) {
                return cluster.sadd(bytes, bytes1);
            }

            @Override
            public Set<byte[]> smembers(byte[] bytes) {
                return cluster.smembers(bytes);
            }

            @Override
            public long srem(byte[] bytes, byte[]... bytes1) {
                return cluster.srem(bytes, bytes1);
            }

            @Override
            public byte[] spop(byte[] bytes) {
                return cluster.spop(bytes);
            }

            @Override
            public Set<byte[]> spop(byte[] bytes, long l) {
                return cluster.spop(bytes, l);
            }

            @Override
            public long scard(byte[] bytes) {
                return cluster.scard(bytes);
            }

            @Override
            public boolean sismember(byte[] bytes, byte[] bytes1) {
                return cluster.sismember(bytes, bytes1);
            }

            @Override
            public List<Boolean> smismember(byte[] bytes, byte[]... bytes1) {
                return null;
            }

            @Override
            public byte[] srandmember(byte[] bytes) {
                return cluster.srandmember(bytes);
            }

            @Override
            public List<byte[]> srandmember(byte[] bytes, int i) {
                return cluster.srandmember(bytes, i);
            }

            @Override
            public long strlen(byte[] bytes) {
                return cluster.strlen(bytes);
            }

            @Override
            public LCSMatchResult lcs(byte[] bytes, byte[] bytes1, LCSParams lcsParams) {
                return null;
            }

            @Override
            public long zadd(byte[] bytes, double v, byte[] bytes1) {
                return cluster.zadd(bytes, v, bytes1);
            }

            @Override
            public long zadd(byte[] bytes, double v, byte[] bytes1, ZAddParams zAddParams) {
                return cluster.zadd(bytes, v, bytes1, zAddParams);
            }

            @Override
            public long zadd(byte[] bytes, Map<byte[], Double> map) {
                return cluster.zadd(bytes, map);
            }

            @Override
            public long zadd(byte[] bytes, Map<byte[], Double> map, ZAddParams zAddParams) {
                return cluster.zadd(bytes, map, zAddParams);
            }

            @Override
            public Double zaddIncr(byte[] bytes, double v, byte[] bytes1, ZAddParams zAddParams) {
                return null;
            }

            @Override
            public List<byte[]> zrange(byte[] bytes, long l, long l1) {
                return cluster.zrange(bytes, l, l1);
            }

            @Override
            public long zrem(byte[] bytes, byte[]... bytes1) {
                return cluster.zrem(bytes, bytes1);
            }

            @Override
            public double zincrby(byte[] bytes, double v, byte[] bytes1) {
                return cluster.zincrby(bytes, v, bytes1);
            }

            @Override
            public Double zincrby(byte[] bytes, double v, byte[] bytes1, ZIncrByParams zIncrByParams) {
                return cluster.zincrby(bytes, v, bytes1, zIncrByParams);
            }

            @Override
            public Long zrank(byte[] bytes, byte[] bytes1) {
                return cluster.zrank(bytes, bytes1);
            }

            @Override
            public Long zrevrank(byte[] bytes, byte[] bytes1) {
                return cluster.zrevrank(bytes, bytes1);
            }

            @Override
            public KeyValue<Long, Double> zrankWithScore(byte[] bytes, byte[] bytes1) {
                return null;
            }

            @Override
            public KeyValue<Long, Double> zrevrankWithScore(byte[] bytes, byte[] bytes1) {
                return null;
            }

            @Override
            public List<byte[]> zrevrange(byte[] bytes, long l, long l1) {
                return cluster.zrevrange(bytes, l, l1);
            }

            @Override
            public List<Tuple> zrangeWithScores(byte[] bytes, long l, long l1) {
                return cluster.zrangeWithScores(bytes, l, l1);
            }

            @Override
            public List<Tuple> zrevrangeWithScores(byte[] bytes, long l, long l1) {
                return cluster.zrevrangeWithScores(bytes, l, l1);
            }

            @Override
            public List<byte[]> zrange(byte[] bytes, ZRangeParams zRangeParams) {
                return null;
            }

            @Override
            public List<Tuple> zrangeWithScores(byte[] bytes, ZRangeParams zRangeParams) {
                return null;
            }

            @Override
            public long zrangestore(byte[] bytes, byte[] bytes1, ZRangeParams zRangeParams) {
                return 0;
            }

            @Override
            public byte[] zrandmember(byte[] bytes) {
                return new byte[0];
            }

            @Override
            public List<byte[]> zrandmember(byte[] bytes, long l) {
                return null;
            }

            @Override
            public List<Tuple> zrandmemberWithScores(byte[] bytes, long l) {
                return null;
            }

            @Override
            public long zcard(byte[] bytes) {
                return cluster.zcard(bytes);
            }

            @Override
            public Double zscore(byte[] bytes, byte[] bytes1) {
                return cluster.zscore(bytes, bytes1);
            }

            @Override
            public List<Double> zmscore(byte[] bytes, byte[]... bytes1) {
                return null;
            }

            @Override
            public Tuple zpopmax(byte[] bytes) {
                return null;
            }

            @Override
            public List<Tuple> zpopmax(byte[] bytes, int i) {
                return null;
            }

            @Override
            public Tuple zpopmin(byte[] bytes) {
                return null;
            }

            @Override
            public List<Tuple> zpopmin(byte[] bytes, int i) {
                return null;
            }

            @Override
            public List<byte[]> sort(byte[] bytes) {
                return cluster.sort(bytes);
            }

            @Override
            public List<byte[]> sort(byte[] bytes, SortingParams sortingParams) {
                return cluster.sort(bytes, sortingParams);
            }

            @Override
            public long zcount(byte[] bytes, double v, double v1) {
                return cluster.zcount(bytes, v, v1);
            }

            @Override
            public long zcount(byte[] bytes, byte[] bytes1, byte[] bytes2) {
                return cluster.zcount(bytes, bytes1, bytes2);
            }

            @Override
            public List<byte[]> zrangeByScore(byte[] bytes, double v, double v1) {
                return cluster.zrangeByScore(bytes, v, v1);
            }

            @Override
            public List<byte[]> zrangeByScore(byte[] bytes, byte[] bytes1, byte[] bytes2) {
                return cluster.zrangeByScore(bytes, bytes1, bytes2);
            }

            @Override
            public List<byte[]> zrevrangeByScore(byte[] bytes, double v, double v1) {
                return cluster.zrevrangeByScore(bytes, v, v1);
            }

            @Override
            public List<byte[]> zrangeByScore(byte[] bytes, double v, double v1, int i, int i1) {
                return cluster.zrangeByScore(bytes, v,v1,i,i1);
            }

            @Override
            public List<byte[]> zrevrangeByScore(byte[] bytes, byte[] bytes1, byte[] bytes2) {
                return cluster.zrevrangeByScore(bytes, bytes1, bytes2);
            }

            @Override
            public List<byte[]> zrangeByScore(byte[] bytes, byte[] bytes1, byte[] bytes2, int i, int i1) {
                return cluster.zrangeByScore(bytes, bytes1, bytes2, i,i1);
            }

            @Override
            public List<byte[]> zrevrangeByScore(byte[] bytes, double v, double v1, int i, int i1) {
                return cluster.zrevrangeByScore(bytes, v,v1,i,i1);
            }

            @Override
            public List<Tuple> zrangeByScoreWithScores(byte[] bytes, double v, double v1) {
                return cluster.zrangeByScoreWithScores(bytes,v,v1);
            }

            @Override
            public List<Tuple> zrevrangeByScoreWithScores(byte[] bytes, double v, double v1) {
                return cluster.zrevrangeByScoreWithScores(bytes, v, v1);
            }

            @Override
            public List<Tuple> zrangeByScoreWithScores(byte[] bytes, double v, double v1, int i, int i1) {
                return cluster.zrangeByScoreWithScores(bytes, v, v1, i, i1);
            }

            @Override
            public List<byte[]> zrevrangeByScore(byte[] bytes, byte[] bytes1, byte[] bytes2, int i, int i1) {
                return cluster.zrevrangeByScore(bytes, bytes1, bytes2, i, i1);
            }

            @Override
            public List<Tuple> zrangeByScoreWithScores(byte[] bytes, byte[] bytes1, byte[] bytes2) {
                return cluster.zrangeByScoreWithScores(bytes, bytes1, bytes2);
            }

            @Override
            public List<Tuple> zrevrangeByScoreWithScores(byte[] bytes, byte[] bytes1, byte[] bytes2) {
                return cluster.zrevrangeByScoreWithScores(bytes, bytes1, bytes2);
            }

            @Override
            public List<Tuple> zrangeByScoreWithScores(byte[] bytes, byte[] bytes1, byte[] bytes2, int i, int i1) {
                return cluster.zrangeByScoreWithScores(bytes, bytes1, bytes2, i, i1);
            }

            @Override
            public List<Tuple> zrevrangeByScoreWithScores(byte[] bytes, double v, double v1, int i, int i1) {
                return cluster.zrevrangeByScoreWithScores(bytes, v, v1, i, i1);
            }

            @Override
            public List<Tuple> zrevrangeByScoreWithScores(byte[] bytes, byte[] bytes1, byte[] bytes2, int i, int i1) {
                return cluster.zrevrangeByScoreWithScores(bytes, bytes1, bytes2, i, i1);
            }

            @Override
            public long zremrangeByRank(byte[] bytes, long l, long l1) {
                return cluster.zremrangeByRank(bytes, l ,l1);
            }

            @Override
            public long zremrangeByScore(byte[] bytes, double v, double v1) {
                return cluster.zremrangeByScore(bytes, v, v1);
            }

            @Override
            public long zremrangeByScore(byte[] bytes, byte[] bytes1, byte[] bytes2) {
                return cluster.zremrangeByScore(bytes, bytes1, bytes2);
            }

            @Override
            public long zlexcount(byte[] bytes, byte[] bytes1, byte[] bytes2) {
                return cluster.zlexcount(bytes, bytes1, bytes2);
            }

            @Override
            public List<byte[]> zrangeByLex(byte[] bytes, byte[] bytes1, byte[] bytes2) {
                return cluster.zrangeByLex(bytes, bytes1, bytes2);
            }

            @Override
            public List<byte[]> zrangeByLex(byte[] bytes, byte[] bytes1, byte[] bytes2, int i, int i1) {
                return cluster.zrangeByLex(bytes, bytes1, bytes2, i, i1);
            }

            @Override
            public List<byte[]> zrevrangeByLex(byte[] bytes, byte[] bytes1, byte[] bytes2) {
                return cluster.zrevrangeByLex(bytes, bytes1, bytes2);
            }

            @Override
            public List<byte[]> zrevrangeByLex(byte[] bytes, byte[] bytes1, byte[] bytes2, int i, int i1) {
                return cluster.zrevrangeByLex(bytes, bytes1, bytes2, i, i1);
            }

            @Override
            public long zremrangeByLex(byte[] bytes, byte[] bytes1, byte[] bytes2) {
                return cluster.zremrangeByLex(bytes, bytes1, bytes2);
            }

            @Override
            public long linsert(byte[] bytes, ListPosition list_position, byte[] bytes1, byte[] bytes2) {
                return cluster.linsert(bytes, list_position, bytes1, bytes2);
            }

            @Override
            public long lpushx(byte[] bytes, byte[]... bytes1) {
                return cluster.lpushx(bytes, bytes1);
            }

            @Override
            public long rpushx(byte[] bytes, byte[]... bytes1) {
                return cluster.rpushx(bytes, bytes1);
            }

            @Override
            public List<byte[]> blpop(int var1, byte[]... var2) {
                return cluster.blpop(var1, var2);
            }

            @Override
            public KeyValue<byte[], byte[]> blpop(double v, byte[]... bytes) {
                return null;
            }

            @Override
            public List<byte[]> brpop(int var1, byte[]... var2) {
                return cluster.brpop(var1, var2);
            }

            @Override
            public KeyValue<byte[], byte[]> brpop(double v, byte[]... bytes) {
                return null;
            }

            @Override
            public byte[] rpoplpush(byte[] bytes, byte[] bytes1) {
                return new byte[0];
            }

            @Override
            public byte[] brpoplpush(byte[] bytes, byte[] bytes1, int i) {
                return new byte[0];
            }

            @Override
            public byte[] lmove(byte[] bytes, byte[] bytes1, ListDirection listDirection, ListDirection listDirection1) {
                return new byte[0];
            }

            @Override
            public byte[] blmove(byte[] bytes, byte[] bytes1, ListDirection listDirection, ListDirection listDirection1, double v) {
                return new byte[0];
            }

            @Override
            public KeyValue<byte[], List<byte[]>> lmpop(ListDirection listDirection, byte[]... bytes) {
                return null;
            }

            @Override
            public KeyValue<byte[], List<byte[]>> lmpop(ListDirection listDirection, int i, byte[]... bytes) {
                return null;
            }

            @Override
            public KeyValue<byte[], List<byte[]>> blmpop(double v, ListDirection listDirection, byte[]... bytes) {
                return null;
            }

            @Override
            public KeyValue<byte[], List<byte[]>> blmpop(double v, ListDirection listDirection, int i, byte[]... bytes) {
                return null;
            }

            @Override
            public long del(byte[] bytes) {
                return cluster.del(bytes);
            }

            @Override
            public long del(byte[]... bytes) {
                return 0;
            }

            @Override
            public long unlink(byte[] bytes) {
                return 0;
            }

            @Override
            public long unlink(byte[]... bytes) {
                return 0;
            }

            @Override
            public boolean copy(byte[] bytes, byte[] bytes1, boolean b) {
                return false;
            }

            @Override
            public String rename(byte[] bytes, byte[] bytes1) {
                return null;
            }

            @Override
            public long renamenx(byte[] bytes, byte[] bytes1) {
                return 0;
            }

            @Override
            public long sort(byte[] bytes, SortingParams sortingParams, byte[] bytes1) {
                return 0;
            }

            @Override
            public long sort(byte[] bytes, byte[] bytes1) {
                return 0;
            }

            @Override
            public List<byte[]> sortReadonly(byte[] bytes, SortingParams sortingParams) {
                return null;
            }

            @Override
            public Long memoryUsage(byte[] bytes) {
                return null;
            }

            @Override
            public Long memoryUsage(byte[] bytes, int i) {
                return null;
            }

            @Override
            public Long objectRefcount(byte[] bytes) {
                return null;
            }

            @Override
            public byte[] objectEncoding(byte[] bytes) {
                return new byte[0];
            }

            @Override
            public Long objectIdletime(byte[] bytes) {
                return null;
            }

            @Override
            public Long objectFreq(byte[] bytes) {
                return null;
            }

            @Override
            public String migrate(String s, int i, byte[] bytes, int i1) {
                return null;
            }

            @Override
            public String migrate(String s, int i, int i1, MigrateParams migrateParams, byte[]... bytes) {
                return null;
            }

            @Override
            public Set<byte[]> keys(byte[] bytes) {
                return null;
            }

            @Override
            public ScanResult<byte[]> scan(byte[] bytes) {
                return null;
            }

            @Override
            public ScanResult<byte[]> scan(byte[] bytes, ScanParams scanParams) {
                return null;
            }

            @Override
            public ScanResult<byte[]> scan(byte[] bytes, ScanParams scanParams, byte[] bytes1) {
                return null;
            }

            @Override
            public byte[] randomBinaryKey() {
                return new byte[0];
            }

            @Override
            public long bitcount(byte[] bytes) {
                return cluster.bitcount(bytes);
            }

            @Override
            public long bitcount(byte[] bytes, long l, long l1) {
                return cluster.bitcount(bytes, l, l1);
            }

            @Override
            public long bitcount(byte[] bytes, long l, long l1, BitCountOption bitCountOption) {
                return 0;
            }

            @Override
            public long bitpos(byte[] bytes, boolean b) {
                return 0;
            }

            @Override
            public long bitpos(byte[] bytes, boolean b, BitPosParams bitPosParams) {
                return 0;
            }

            @Override
            public long pfadd(byte[] bytes, byte[]... bytes1) {
                return cluster.pfadd(bytes, bytes1);
            }

            @Override
            public String pfmerge(byte[] bytes, byte[]... bytes1) {
                return null;
            }

            @Override
            public long pfcount(byte[] bytes) {
                return cluster.pfcount(bytes);
            }

            @Override
            public long pfcount(byte[]... bytes) {
                return 0;
            }

            @Override
            public long geoadd(byte[] bytes, double v, double v1, byte[] bytes1) {
                return cluster.geoadd(bytes, v, v1, bytes1);
            }

            @Override
            public long geoadd(byte[] bytes, Map<byte[], GeoCoordinate> map) {
                return cluster.geoadd(bytes, map);
            }

            @Override
            public long geoadd(byte[] bytes, GeoAddParams geoAddParams, Map<byte[], GeoCoordinate> map) {
                return 0;
            }

            @Override
            public Double geodist(byte[] bytes, byte[] bytes1, byte[] bytes2) {
                return cluster.geodist(bytes, bytes1, bytes2);
            }

            @Override
            public Double geodist(byte[] bytes, byte[] bytes1, byte[] bytes2, GeoUnit geoUnit) {
                return cluster.geodist(bytes, bytes1, bytes2, geoUnit);
            }

            @Override
            public List<byte[]> geohash(byte[] bytes, byte[]... bytes1) {
                return cluster.geohash(bytes, bytes1);
            }

            @Override
            public List<GeoCoordinate> geopos(byte[] bytes, byte[]... bytes1) {
                return cluster.geopos(bytes, bytes1);
            }

            @Override
            public List<GeoRadiusResponse> georadius(byte[] bytes, double v, double v1, double v2, GeoUnit geoUnit) {
                return cluster.georadius(bytes, v,v1,v2, geoUnit);
            }

            @Override
            public List<GeoRadiusResponse> georadiusReadonly(byte[] bytes, double v, double v1, double v2, GeoUnit geoUnit) {
                return null;
            }

            @Override
            public List<GeoRadiusResponse> georadius(byte[] bytes, double v, double v1, double v2, GeoUnit geoUnit, GeoRadiusParam geoRadiusParam) {
                return cluster.georadius(bytes, v, v1, v2, geoUnit, geoRadiusParam);
            }

            @Override
            public List<GeoRadiusResponse> georadiusReadonly(byte[] bytes, double v, double v1, double v2, GeoUnit geoUnit, GeoRadiusParam geoRadiusParam) {
                return null;
            }

            @Override
            public List<GeoRadiusResponse> georadiusByMember(byte[] bytes, byte[] bytes1, double v, GeoUnit geoUnit) {
                return cluster.georadiusByMember(bytes, bytes1, v, geoUnit);
            }

            @Override
            public List<GeoRadiusResponse> georadiusByMemberReadonly(byte[] bytes, byte[] bytes1, double v, GeoUnit geoUnit) {
                return null;
            }

            @Override
            public List<GeoRadiusResponse> georadiusByMember(byte[] bytes, byte[] bytes1, double v, GeoUnit geoUnit, GeoRadiusParam geoRadiusParam) {
                return cluster.georadiusByMember(bytes, bytes1, v, geoUnit, geoRadiusParam);
            }

            @Override
            public List<GeoRadiusResponse> georadiusByMemberReadonly(byte[] bytes, byte[] bytes1, double v, GeoUnit geoUnit, GeoRadiusParam geoRadiusParam) {
                return null;
            }

            @Override
            public long georadiusStore(byte[] bytes, double v, double v1, double v2, GeoUnit geoUnit, GeoRadiusParam geoRadiusParam, GeoRadiusStoreParam geoRadiusStoreParam) {
                return 0;
            }

            @Override
            public long georadiusByMemberStore(byte[] bytes, byte[] bytes1, double v, GeoUnit geoUnit, GeoRadiusParam geoRadiusParam, GeoRadiusStoreParam geoRadiusStoreParam) {
                return 0;
            }

            @Override
            public List<GeoRadiusResponse> geosearch(byte[] bytes, byte[] bytes1, double v, GeoUnit geoUnit) {
                return null;
            }

            @Override
            public List<GeoRadiusResponse> geosearch(byte[] bytes, GeoCoordinate geoCoordinate, double v, GeoUnit geoUnit) {
                return null;
            }

            @Override
            public List<GeoRadiusResponse> geosearch(byte[] bytes, byte[] bytes1, double v, double v1, GeoUnit geoUnit) {
                return null;
            }

            @Override
            public List<GeoRadiusResponse> geosearch(byte[] bytes, GeoCoordinate geoCoordinate, double v, double v1, GeoUnit geoUnit) {
                return null;
            }

            @Override
            public List<GeoRadiusResponse> geosearch(byte[] bytes, GeoSearchParam geoSearchParam) {
                return null;
            }

            @Override
            public long geosearchStore(byte[] bytes, byte[] bytes1, byte[] bytes2, double v, GeoUnit geoUnit) {
                return 0;
            }

            @Override
            public long geosearchStore(byte[] bytes, byte[] bytes1, GeoCoordinate geoCoordinate, double v, GeoUnit geoUnit) {
                return 0;
            }

            @Override
            public long geosearchStore(byte[] bytes, byte[] bytes1, byte[] bytes2, double v, double v1, GeoUnit geoUnit) {
                return 0;
            }

            @Override
            public long geosearchStore(byte[] bytes, byte[] bytes1, GeoCoordinate geoCoordinate, double v, double v1, GeoUnit geoUnit) {
                return 0;
            }

            @Override
            public long geosearchStore(byte[] bytes, byte[] bytes1, GeoSearchParam geoSearchParam) {
                return 0;
            }

            @Override
            public long geosearchStoreStoreDist(byte[] bytes, byte[] bytes1, GeoSearchParam geoSearchParam) {
                return 0;
            }

            @Override
            public ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] bytes, byte[] bytes1) {
                return cluster.hscan(bytes, bytes1);
            }

            @Override
            public ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] bytes, byte[] bytes1, ScanParams scanParams) {
                return cluster.hscan(bytes, bytes1, scanParams);
            }

            @Override
            public long hstrlen(byte[] bytes, byte[] bytes1) {
                return 0;
            }

            @Override
            public ScanResult<byte[]> sscan(byte[] bytes, byte[] bytes1) {
                return cluster.sscan(bytes, bytes1);
            }

            @Override
            public ScanResult<byte[]> sscan(byte[] bytes, byte[] bytes1, ScanParams scanParams) {
                return cluster.sscan(bytes, bytes1, scanParams);
            }

            @Override
            public Set<byte[]> sdiff(byte[]... bytes) {
                return null;
            }

            @Override
            public long sdiffstore(byte[] bytes, byte[]... bytes1) {
                return 0;
            }

            @Override
            public Set<byte[]> sinter(byte[]... bytes) {
                return null;
            }

            @Override
            public long sinterstore(byte[] bytes, byte[]... bytes1) {
                return 0;
            }

            @Override
            public long sintercard(byte[]... bytes) {
                return 0;
            }

            @Override
            public long sintercard(int i, byte[]... bytes) {
                return 0;
            }

            @Override
            public Set<byte[]> sunion(byte[]... bytes) {
                return null;
            }

            @Override
            public long sunionstore(byte[] bytes, byte[]... bytes1) {
                return 0;
            }

            @Override
            public long smove(byte[] bytes, byte[] bytes1, byte[] bytes2) {
                return 0;
            }

            @Override
            public ScanResult<Tuple> zscan(byte[] bytes, byte[] bytes1) {
                return cluster.zscan(bytes, bytes1);
            }

            @Override
            public ScanResult<Tuple> zscan(byte[] bytes, byte[] bytes1, ScanParams scanParams) {
                return cluster.zscan(bytes, bytes1, scanParams);
            }

            @Override
            public KeyValue<byte[], Tuple> bzpopmax(double v, byte[]... bytes) {
                return null;
            }

            @Override
            public KeyValue<byte[], Tuple> bzpopmin(double v, byte[]... bytes) {
                return null;
            }

            @Override
            public List<byte[]> zdiff(byte[]... bytes) {
                return null;
            }

            @Override
            public List<Tuple> zdiffWithScores(byte[]... bytes) {
                return null;
            }

            @Override
            public long zdiffStore(byte[] bytes, byte[]... bytes1) {
                return 0;
            }

            @Override
            public long zdiffstore(byte[] bytes, byte[]... bytes1) {
                return 0;
            }

            @Override
            public List<byte[]> zinter(ZParams zParams, byte[]... bytes) {
                return null;
            }

            @Override
            public List<Tuple> zinterWithScores(ZParams zParams, byte[]... bytes) {
                return null;
            }

            @Override
            public long zinterstore(byte[] bytes, byte[]... bytes1) {
                return 0;
            }

            @Override
            public long zinterstore(byte[] bytes, ZParams zParams, byte[]... bytes1) {
                return 0;
            }

            @Override
            public long zintercard(byte[]... bytes) {
                return 0;
            }

            @Override
            public long zintercard(long l, byte[]... bytes) {
                return 0;
            }

            @Override
            public List<byte[]> zunion(ZParams zParams, byte[]... bytes) {
                return null;
            }

            @Override
            public List<Tuple> zunionWithScores(ZParams zParams, byte[]... bytes) {
                return null;
            }

            @Override
            public long zunionstore(byte[] bytes, byte[]... bytes1) {
                return 0;
            }

            @Override
            public long zunionstore(byte[] bytes, ZParams zParams, byte[]... bytes1) {
                return 0;
            }

            @Override
            public KeyValue<byte[], List<Tuple>> zmpop(SortedSetOption sortedSetOption, byte[]... bytes) {
                return null;
            }

            @Override
            public KeyValue<byte[], List<Tuple>> zmpop(SortedSetOption sortedSetOption, int i, byte[]... bytes) {
                return null;
            }

            @Override
            public KeyValue<byte[], List<Tuple>> bzmpop(double v, SortedSetOption sortedSetOption, byte[]... bytes) {
                return null;
            }

            @Override
            public KeyValue<byte[], List<Tuple>> bzmpop(double v, SortedSetOption sortedSetOption, int i, byte[]... bytes) {
                return null;
            }

            @Override
            public List<Long> bitfield(byte[] bytes, byte[]... bytes1) {
                return cluster.bitfield(bytes, bytes1);
            }

            @Override
            public List<Long> bitfieldReadonly(byte[] bytes, byte[]... bytes1) {
                return null;
            }

            @Override
            public long bitop(BitOP bitOP, byte[] bytes, byte[]... bytes1) {
                return 0;
            }
        };
    }

}
