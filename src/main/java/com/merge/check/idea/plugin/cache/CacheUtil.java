package com.merge.check.idea.plugin.cache;

import java.time.Duration;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * @Author : song hao
 * @CreateTime : 2024/08/02 16:17
 * @Description :
 */

public class CacheUtil {
    public static volatile Cache<String, String> AVOID_REPEAT_CONFLICT_CACHE =
        CacheBuilder.newBuilder().expireAfterWrite(Duration.ofSeconds(10)).build();;

    public static volatile Cache<String, String> PROJECT_CONFLICT_CACHE =
        CacheBuilder.newBuilder().expireAfterAccess(Duration.ofMinutes(30)).maximumSize(100).build();

}
