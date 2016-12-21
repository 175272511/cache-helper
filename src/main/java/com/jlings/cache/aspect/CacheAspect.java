package com.jlings.cache.aspect;

import com.alibaba.fastjson.JSON;
import com.jlings.cache.annotation.JlingsCache;
import com.jlings.cache.annotation.JlingsCacheClear;
import com.jlings.cache.annotation.JlingsCacheParam;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Created by liuhui on 2016/12/13.
 */
@Aspect
@Component
public class CacheAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheAspect.class);

    @Autowired
    RedisTemplate redisTemplate;

    private static final String CACHE_NAME = "JLINGS_CACHE:";

    //拦截注解
    @Pointcut(value="@annotation(jlingsCache)", argNames="jlingsCache")
    private void cacheMethod(JlingsCache jlingsCache){}

    //拦截注解
    @Pointcut(value="@annotation(jlingsCacheClear)", argNames="jlingsCacheClear")
    private void cacheClearMethod(JlingsCacheClear jlingsCacheClear){}

    @Around(value = "cacheMethod(jlingsCache)")
    public Object cacheAround(ProceedingJoinPoint joinPoint, JlingsCache jlingsCache) throws Throwable {

        String cacheKey = getCacheKey(joinPoint, jlingsCache);

        BoundValueOperations<String, Object> boundValueOps = redisTemplate.boundValueOps(cacheKey);
        int second = jlingsCache.expireTime();
        Object value;
        //缓存不存在
        if (boundValueOps.setIfAbsent("")){
            value = joinPoint.proceed();
            boundValueOps.set(value);
            if (second > 0){
                boundValueOps.expire(second, TimeUnit.SECONDS);
            }
            LOGGER.debug("redis cache ===> key: {}, value: {}",cacheKey, value);
            return value;

        }else{
            try {
                value = redisTemplate.opsForValue().get(cacheKey);
                LOGGER.debug("return cache ===> key: {}, value: {}",cacheKey, value);
            }catch (Exception e){
                LOGGER.error("return cache error", e);
                //重置缓存, 解决对象更改导致的异常问题
                redisTemplate.delete(cacheKey);
                value = joinPoint.proceed();
            }

            return StringUtils.isEmpty(value) ? joinPoint.proceed() : value;
        }


    }

    @AfterReturning(value = "cacheClearMethod(jlingsCacheClear)")
    public void cacheClearAfterReturning(JoinPoint joinPoint, JlingsCacheClear jlingsCacheClear) throws Throwable{
        String cacheKey = getCacheKey(joinPoint, jlingsCacheClear);
        redisTemplate.delete(cacheKey);
        LOGGER.debug("delete cache ===> key:{}, value:{}",cacheKey);
    }

    private String getCacheKey(JoinPoint joinPoint, Annotation annotation){
        Object target = joinPoint.getTarget();
        String targetName = target.getClass().getName();
        Signature sig = joinPoint.getSignature();
        String methodName = sig.getName();
        Object[] arguments = joinPoint.getArgs();

        Method method = getCurrentMethod(joinPoint);

        String paramKey = getParamKey(method, arguments);

        String cacheKey = null;
        if (annotation instanceof JlingsCache){
            cacheKey = ((JlingsCache)annotation).key();
        }else if (annotation instanceof JlingsCacheClear){
            cacheKey = ((JlingsCacheClear)annotation).key();
        }
        if (StringUtils.isEmpty(cacheKey)){
            cacheKey = CACHE_NAME + getDefaultKey(targetName, methodName, arguments) + paramKey;
        }else{
            cacheKey = CACHE_NAME + cacheKey + paramKey;
        }

        return  cacheKey;
    }

    /**
     * 根据参数注解拼接参数key
     * @param method
     * @param arguments
     * @return
     */
    private String getParamKey(Method method, Object[] arguments) {
        StringBuffer sb = new StringBuffer();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < parameterAnnotations.length; i++){
            int length = parameterAnnotations[i].length;
            if (length > 0) {// 存在annotation
                for (int j = 0; j < length; j++) {
                    if (parameterAnnotations[i][j] instanceof JlingsCacheParam) {
                        sb.append(".").append(JSON.toJSONString(arguments[i]));
                    }
                }
            }
        }

        return sb.toString();
    }

    /**
     * 获取当前接口的实现类方法
     * @param joinPoint
     * @return
     * @throws NoSuchMethodException
     */
    private Method getCurrentMethod(JoinPoint joinPoint){
        Object target = joinPoint.getTarget();
        Signature sig = joinPoint.getSignature();
        if (!(sig instanceof MethodSignature)) {
            throw new IllegalArgumentException("该注解只能用于方法");
        }

        MethodSignature methodSignature = (MethodSignature) sig;
        Method currentMethod = null;
        try {
            currentMethod = target.getClass().getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return currentMethod;
    }


    /**
     * defaultKey 包名+类名+方法名
     */
    private String getDefaultKey(String targetName, String methodName, Object[] arguments) {
        StringBuffer sb = new StringBuffer();
        sb.append(targetName).append(".").append(methodName);
        if ((arguments != null) && (arguments.length != 0)) {
            for (int i = 0; i < arguments.length; i++) {
                sb.append(".").append(JSON.toJSONString(arguments[i]));
            }
        }
        return sb.toString();
    }


}
