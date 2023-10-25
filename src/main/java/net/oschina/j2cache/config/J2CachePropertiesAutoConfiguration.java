package net.oschina.j2cache.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ClassName: J2CachePropertiesAutoConfiguration
 * Package: net.oschina.j2cache.config
 * Description:
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/25 16:30
 */

@Configuration
@EnableConfigurationProperties(J2CacheProperties.class)
public class J2CachePropertiesAutoConfiguration {

    private J2CacheProperties properties;

    public J2CachePropertiesAutoConfiguration(J2CacheProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean(J2Cache.class)
    public J2Cache j2Cache() {
        return J2Cache.getInstance(properties);
    }

}