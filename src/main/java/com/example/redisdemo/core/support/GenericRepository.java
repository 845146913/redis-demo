package com.example.redisdemo.core.support;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.List;

import com.example.redisdemo.core.utils.CriterionFilter;

/**
 * @Author: wang
 * @Date: 2020/3/12 16:21
 */
@NoRepositoryBean
public interface GenericRepository<T, ID extends Serializable> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {


    /**
     * 动态条件查询转换dto
     *
     * @param jpql
     * @param dynamicFilter
     * @param <DTO>
     * @return
     */
    <DTO> List<DTO> queryListByCondition(String jpql, List<CriterionFilter> dynamicFilter);

    /**
     * 条件查询带排序
     *
     * @param jpql
     * @param dynamicFilter
     * @param sort
     * @param <DTO>
     * @return
     */
    <DTO> List<DTO> queryListByCondition(String jpql, List<CriterionFilter> dynamicFilter, Sort sort);

    /**
     * 条件查询带分页条件，不返回分页信息
     *
     * @param jpql
     * @param dynamicFilter
     * @param pageable
     * @param <DTO>
     * @return
     */
    <DTO> List<DTO> queryListByCondition(String jpql, List<CriterionFilter> dynamicFilter, Pageable pageable);

    /**
     * 分页查询
     *
     * @param jpql
     * @param dynamicFilter
     * @param pageable
     * @param <DTO>
     * @return
     */
    <DTO> Page<DTO> queryPageByCondition(String jpql, List<CriterionFilter> dynamicFilter, Pageable pageable);

    /**
     * 查询所有的数据并过滤
     *
     * @param dtoCls
     * @param spec
     * @param pageable
     * @param <S>
     * @return
     */
    <S> List<S> queryList(Class<S> dtoCls, Specification<T> spec, Pageable pageable);

    /**
     * 通过hql方式查询数据
     *
     * @param jpql
     * @param entity
     * @param pageable
     * @param <DTO>
     * @return
     */
    <DTO> List<DTO> queryListByDomain(String jpql, T entity, Pageable pageable);

    /**
     * 获取分页
     *
     * @param jpql
     * @param entity
     * @param pageable
     * @param <DTO>
     * @return
     */
    <DTO> Page<DTO> queryPageByDomain(String jpql, T entity, Pageable pageable);

}
