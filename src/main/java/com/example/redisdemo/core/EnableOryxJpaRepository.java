package com.example.redisdemo.core;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @Author: wang
 * @Date: 2020/3/13 17:58
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(OryxRepositoryConfig.class)
public @interface EnableOryxJpaRepository {
}
