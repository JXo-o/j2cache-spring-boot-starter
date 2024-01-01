package net.oschina.j2cache.service.cache.impl;

import net.oschina.j2cache.config.J2CacheProperties;
import net.oschina.j2cache.service.cache.CacheExpiredListener;

/**
 * ClassName: CacheProviderBuilder
 * Package: net.oschina.j2cache.service.cache.impl
 * Description:
 *
 * @author JX
 * @version 1.0
 * @date 2024/1/1 17:17
 */
public interface CacheProviderBuilder {
    CacheProviderBuilder withConfig(J2CacheProperties config);
    CacheProviderBuilder withListener(CacheExpiredListener listener);
    CacheProviderHolder build();
}