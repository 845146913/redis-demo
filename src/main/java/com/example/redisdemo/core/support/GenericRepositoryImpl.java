package com.example.redisdemo.core.support;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.example.redisdemo.core.utils.CriterionFilter;
import com.example.redisdemo.core.utils.JpaQueryUtils;

import java.io.Serializable;
import java.util.List;

import static org.springframework.data.jpa.repository.query.QueryUtils.toOrders;

/**
 * @Author: wang
 * @Date: 2020/3/12 16:24
 */
public class GenericRepositoryImpl<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> implements GenericRepository<T, ID> {
    protected Class<T> domainClass;
    protected EntityManager em;

    public GenericRepositoryImpl(Class<T> domainClass, EntityManager em) {
        super(domainClass, em);
        this.domainClass = domainClass;
        this.em = em;
    }

    @Override
    public <S> List<S> queryList(Class<S> dtoCls, Specification<T> spec, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<S> query = cb.createQuery(dtoCls);
        Root<T> root = query.from(domainClass);
        if (spec != null) {
            Predicate predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
        }
        Sort sort = pageable.isPaged() ? pageable.getSort() : Sort.unsorted();
        if (sort.isSorted()) {
            query.orderBy(toOrders(sort, root, cb));
        }
        query.select((Root<S>) root);
        TypedQuery<S> tq = em.createQuery(query);
        wrapPageable(pageable, tq);
        return tq.getResultList();
    }

    @Override
    public <DTO> List<DTO> queryListByDomain(String jpql, T entity, Pageable pageable) {
        validateJpql(jpql);
        String tableAlias = JpaQueryUtils.getJpqlTableAlias(jpql, domainClass.getSimpleName());
        if (entity != null) {
            StringBuilder dynamicSql = JpaQueryUtils.getDynamicSqlCondition(jpql, entity,
                    tableAlias);
            jpql += dynamicSql;
        }
        Query query = getJpQuery(jpql, entity, tableAlias, pageable);
        return query.getResultList();
    }


    @Override
    public <DTO> Page<DTO> queryPageByDomain(String jpql, T entity, Pageable pageable) {
        validateJpql(jpql);
        String tableAlias = JpaQueryUtils.getJpqlTableAlias(jpql, domainClass.getSimpleName());
        if (entity != null) {
            StringBuilder dynamicSql = JpaQueryUtils.getDynamicSqlCondition(jpql, entity, tableAlias);
            jpql += dynamicSql;
        }
        Query query = getJpQuery(jpql, entity, tableAlias, pageable);
        return new PageImpl<>(query.getResultList(), pageable, doCountQueryHql(jpql, entity));
    }

    @Override
    public <DTO> List<DTO> queryListByCondition(String jpql, List<CriterionFilter> dynamicFilter) {
        validateJpql(jpql);
        String tableAlias = JpaQueryUtils.getJpqlTableAlias(jpql, domainClass.getSimpleName());
        if (!CollectionUtils.isEmpty(dynamicFilter)) {
            jpql += JpaQueryUtils.getDynamicSqlCondition(jpql, dynamicFilter, tableAlias);
        }
        Query query = getJpQuery(jpql, dynamicFilter);
        return query.getResultList();
    }

    @Override
    public <DTO> List<DTO> queryListByCondition(String jpql, List<CriterionFilter> dynamicFilter, Sort sort) {
        validateJpql(jpql);
        String tableAlias = JpaQueryUtils.getJpqlTableAlias(jpql, domainClass.getSimpleName());
        if (!CollectionUtils.isEmpty(dynamicFilter)) {
            jpql += JpaQueryUtils.getDynamicSqlCondition(jpql, dynamicFilter, tableAlias);
        }
        if (sort != null) {
            jpql += JpaQueryUtils.getSortCondition(sort, tableAlias);
        }
        Query query = getJpQuery(jpql, dynamicFilter);
        return query.getResultList();
    }

    @Override
    public <DTO> List<DTO> queryListByCondition(String jpql, List<CriterionFilter> dynamicFilter, Pageable pageable) {
        validateJpql(jpql);
        String tableAlias = JpaQueryUtils.getJpqlTableAlias(jpql, domainClass.getSimpleName());
        if (!CollectionUtils.isEmpty(dynamicFilter)) {
            jpql += JpaQueryUtils.getDynamicSqlCondition(jpql, dynamicFilter, tableAlias);
        }
        Query query = getJpQuery(jpql, dynamicFilter, tableAlias, pageable);
        return query.getResultList();
    }

    @Override
    public <DTO> Page<DTO> queryPageByCondition(String jpql, List<CriterionFilter> dynamicFilter, Pageable pageable) {
        validateJpql(jpql);
        String tableAlias = JpaQueryUtils.getJpqlTableAlias(jpql, domainClass.getSimpleName());
        if (!CollectionUtils.isEmpty(dynamicFilter)) {
            jpql += JpaQueryUtils.getDynamicSqlCondition(jpql, dynamicFilter, tableAlias);
        }
        Query query = getJpQuery(jpql, dynamicFilter, tableAlias, pageable);
        return new PageImpl<>(query.getResultList(), pageable, doCountQueryHql(jpql, dynamicFilter));
    }

    private Query getJpQuery(String jpql, List<CriterionFilter> dynamicFilter) {
        Query query = em.createQuery(jpql);
        if (!CollectionUtils.isEmpty(dynamicFilter)) {
            JpaQueryUtils.setJpqlParameters(dynamicFilter, query);
        }
        return query;
    }

    private Query getJpQuery(String finalJpql, Object object, String alias, Pageable pageable) {

        finalJpql += JpaQueryUtils.getSortCondition(pageable, alias);
        Query query = em.createQuery(finalJpql);
        wrapPageable(pageable, query);
        // 传递参数值
        JpaQueryUtils.setJpqlParameters(object, query);
        return query;
    }

    private void validateJpql(String jpql) {
        jpql.replaceAll("\\(.*\\)\\s+as[\\s+(\\S)\\s+]", "");
        if (jpql == null || "".equals(jpql)) {
            throw new IllegalArgumentException("jpql must not be null!");
        }
    }


    private Long doCountQueryHql(String finalJpql, Object objOrFilter) {
        String countHql = JpaQueryUtils.getCountSqlString(finalJpql);
        Query query = em.createQuery(countHql);
        JpaQueryUtils.setJpqlParameters(objOrFilter, query);
        Long count = (Long) query.getSingleResult();
        return count;
    }


    private void wrapPageable(Pageable pageable, Query query) {
        if (pageable.isPaged()) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }
    }

    private static long executeCountQuery(TypedQuery<Long> query) {

        Assert.notNull(query, "TypedQuery must not be null!");

        List<Long> totals = query.getResultList();
        long total = 0L;

        for (Long element : totals) {
            total += element == null ? 0 : element;
        }

        return total;
    }
}
