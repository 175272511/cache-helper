package com.jlings.cache.utils;

import com.google.common.util.concurrent.RateLimiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流器
 * Created by liuhui on 2017/9/8.
 */
public class RatelimiterUtil {

    private static final Map<String, RateLimiter> RATE_MAP = new ConcurrentHashMap<>();

    /**
     * 根据来源和qps取限流器
     * @param source
     * @param qps
     * @return
     */
    public static RateLimiter getRateLimiter(String source, int qps){

        String key = source + "." + qps;
        RateLimiter rateLimiter = RATE_MAP.get(key);
        if (rateLimiter == null){
            synchronized (RatelimiterUtil.class){
                rateLimiter = RATE_MAP.get(key);
                if (rateLimiter == null){
                    rateLimiter = RateLimiter.create(qps);
                    RATE_MAP.put(key, rateLimiter);
                }
            }
        }
        return rateLimiter;
    }

    /**
     * 限流判断
     * @param source
     * @param qps
     * @return
     */
    public static boolean tryAcquire(String source, int qps){
        RateLimiter rateLimiter = getRateLimiter(source, qps);
        return rateLimiter.tryAcquire();
    }
}
