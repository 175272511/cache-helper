package com.jlings.cache.annotation;

import java.lang.annotation.*;

/**
 * Created by liuhui on 2016/12/14.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JlingsCacheClear {

    /**
     * 要删除的缓存标识
     * @return
     */
    String key();
}
