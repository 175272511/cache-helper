package com.jlings.cache.annotation;

import java.lang.annotation.*;

/**
 * Created by liuhui on 2016/12/13.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JlingsCache {

    /**
     * 缓存唯一标识,默认包名+类名+方法名,建议用户设置
     * @return
     */
    String key() default "";

    /**
     * 默认一小时
     * @return
     */
    int expireTime() default 60*60;

}
