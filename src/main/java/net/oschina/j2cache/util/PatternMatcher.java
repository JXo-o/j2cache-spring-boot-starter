package net.oschina.j2cache.util;

/**
 * ClassName: PatternMatcher
 * Package: net.oschina.j2cache.util
 * Description:
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 1:05
 */
public interface PatternMatcher {

    /**
     * Returns <code>true</code> if the given <code>source</code> matches the specified <code>pattern</code>,
     * <code>false</code> otherwise.
     *
     * @param pattern the pattern to match against
     * @param source  the source to match
     * @return <code>true</code> if the given <code>source</code> matches the specified <code>pattern</code>,
     *         <code>false</code> otherwise.
     */
    boolean matches(String pattern, String source);
}
