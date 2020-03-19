package com.example.redisdemo.core.utils;

import lombok.Getter;
import lombok.ToString;

/**
 * 处理除AND、OR以外的Operator条件
 * @Author: wang
 * @Date: 2020/3/18 10:15
 */
@Getter
@ToString
public class SearchFilter implements CriterionFilter {

    private final String fieldName;
    private final Operator op;
    private final Object value;

    public SearchFilter(String fieldName, Operator op, Object value) {
        this.fieldName = fieldName;
        this.op = op;
        this.value = value;
    }
}
