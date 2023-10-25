package net.oschina.j2cache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Properties;

/**
 * ClassName: J2CacheProperties
 * Package: net.oschina.j2cache.config
 * Description:
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/25 14:08
 */

@ConfigurationProperties(prefix = "j2cache")
public class J2CacheProperties {

    private Properties serializationProperties = new Properties();
    private Properties broadcastProperties;
    private Properties l1CacheProperties;
    private Properties l2CacheProperties;

    private String broadcast = "redis";
    private String l1CacheName = "caffeine";
    private String l2CacheName = "redis";
    private String serialization = "fastjson";
    private boolean syncTtlToRedis = true;
    private boolean defaultCacheNullObject = false;

    /**
     * read sub properties by prefix
     *
     * @param i_prefix prefix of config
     * @return properties without prefix
     */
    public Properties getSubProperties(String i_prefix) {
        Properties props = new Properties();
        final String prefix = i_prefix + '.';
        serializationProperties.forEach((k, v) -> {
            String key = (String) k;
            if (key.startsWith(prefix)) {
                props.setProperty(key.substring(prefix.length()), trim((String) v));
            }
        });
        return props;
    }

    private static String trim(String str) {
        return (str != null) ? str.trim() : null;
    }

    public Properties getBroadcastProperties() {
        return broadcastProperties;
    }

    public void setBroadcastProperties(Properties broadcastProperties) {
        this.broadcastProperties = broadcastProperties;
    }

    public Properties getL1CacheProperties() {
        return l1CacheProperties;
    }

    public void setL1CacheProperties(Properties l1CacheProperties) {
        this.l1CacheProperties = l1CacheProperties;
    }

    public Properties getL2CacheProperties() {
        return l2CacheProperties;
    }

    public void setL2CacheProperties(Properties l2CacheProperties) {
        this.l2CacheProperties = l2CacheProperties;
    }

    public String getBroadcast() {
        return broadcast;
    }

    public void setBroadcast(String broadcast) {
        this.broadcast = broadcast;
    }

    public String getL1CacheName() {
        return l1CacheName;
    }

    public void setL1CacheName(String l1CacheName) {
        this.l1CacheName = l1CacheName;
    }

    public String getL2CacheName() {
        return l2CacheName;
    }

    public void setL2CacheName(String l2CacheName) {
        this.l2CacheName = l2CacheName;
    }

    public String getSerialization() {
        return serialization;
    }

    public void setSerialization(String serialization) {
        this.serialization = serialization;
    }

    public boolean isSyncTtlToRedis() {
        return syncTtlToRedis;
    }

    public void setSyncTtlToRedis(boolean syncTtlToRedis) {
        this.syncTtlToRedis = syncTtlToRedis;
    }

    public boolean isDefaultCacheNullObject() {
        return defaultCacheNullObject;
    }

    public void setDefaultCacheNullObject(boolean defaultCacheNullObject) {
        this.defaultCacheNullObject = defaultCacheNullObject;
    }

    public String toString() {
        return "properties{" +
                "broadcastProperties=" + broadcastProperties +
                ", l1CacheProperties=" + l1CacheProperties +
                ", l2CacheProperties=" + l2CacheProperties +
                ", broadcast='" + broadcast + '\'' +
                ", l1CacheName='" + l1CacheName + '\'' +
                ", l2CacheName='" + l2CacheName + '\'' +
                ", serialization='" + serialization + '\'' +
                ", syncTtlToRedis=" + syncTtlToRedis +
                ", defaultCacheNullObject=" + defaultCacheNullObject +
                '}';
    }

}