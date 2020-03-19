package com.example.redisdemo.core.utils;

import lombok.Getter;

/**
 * @Author: wang
 * @Date: 2020/3/18 15:48
 */
public interface CriterionFilter {

    @Getter
    enum Operator {
        EQ("="), NE("!="), LIKE("like"), GT(">"), LT("<"),
        GTE(">="), LTE("<="), AND("and"), OR("or"), IN("in"),
        NOTIN("not in"), BETWEEN("between");

        private String value;

        Operator(String value) {
            this.value = value;
        }
    }
}
