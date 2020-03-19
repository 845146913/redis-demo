package com.example.redisdemo.core;

import com.example.redisdemo.core.support.DefaultJpaRepositoryFactoryBean;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * @Author: wang
 * @Date: 2020/3/12 17:08
 */
@Configuration
@EnableJpaRepositories(basePackages = {"tech.rongxin.oryx"},repositoryFactoryBeanClass = DefaultJpaRepositoryFactoryBean.class)
@ComponentScan(basePackages = {"tech.rongxin.oryx"})
public class OryxRepositoryConfig {
}
