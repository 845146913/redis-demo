package com.example.redisdemo.core.utils;


import lombok.Getter;
import lombok.ToString;

/**
 * 特殊条件用来处理 AND、OR这些
 * @Author: wang
 * @Date: 2020/3/18 15:35
 */
@Getter
@ToString
public class LogicSearchFilter implements CriterionFilter {

    private final CriterionFilter[] criterion;
    private final Operator op;

    public LogicSearchFilter(Operator op, CriterionFilter... criterion) {
        this.criterion = criterion;
        this.op = op;
    }
}
