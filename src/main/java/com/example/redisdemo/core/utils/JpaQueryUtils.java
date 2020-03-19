package com.example.redisdemo.core.utils;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;


import javax.persistence.Query;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @Author: wang
 * @Date: 2020/3/18 10:12
 */
public class JpaQueryUtils {

    private static final String DYNAMIC_CONDITION_STRING = "%s.%s %s :%s";
    private static final String BETWEEN_CONDITION_STRING = "%s.%s BETWEEN :%s AND :%s";
    private static final String SPECIAL_CONDITION_STRING = "%s.%s %s (";
    private static final String VARIABLE_CONDITION_STRING = " :%s";
    private static final String pointSeparator = ".";

    private JpaQueryUtils() {
    }

    /**
     * 动态参数
     *
     * @param jpql
     * @param object 支持单属性实体和List<SearchFilter>
     * @param alias
     * @return
     */
    public static StringBuilder getDynamicSqlCondition(String jpql, Object object, String alias) {
        StringBuilder dynamicSql = new StringBuilder();

        // 截取主表别名，暂不支持截取 各种join条件的表的别名
        // String alias = getJpqlTableAlias(jpql, domainSimpleName);

        String whereOrAnd = " WHERE ";
        String match = ".*((W|w)(H|h)(E|e)(R|r)(E|e))+.+";
        if (jpql.matches(match)) {
            whereOrAnd = " AND ";
        }
        if (object instanceof List) {
            wrapForSearchFilters((List<CriterionFilter>) object, dynamicSql, alias, whereOrAnd, match);
        } else {
            wrapForObject(object, dynamicSql, alias, whereOrAnd, match);
        }
        return dynamicSql;
    }

    private static void wrapForObject(Object object, StringBuilder dynamicSql, String alias, String whereOrAnd,
            String match) {
        List<Field> fields = getDeclaredFields(object);
        for (Field field : fields) {
            try {
                String name = field.getName();
                Object value = ReflectionUtils.getField(field, object);
                if (value != null) {
                    if (dynamicSql.toString().matches(match)) {
                        whereOrAnd = " AND ";
                    }
                    dynamicSql.append(whereOrAnd)
                            .append(alias).append(name)
                            .append("=:").append(name);
                }
            } catch (Exception e) {
            }
        }
    }

    private static void wrapForSearchFilters(List<CriterionFilter> object, StringBuilder dynamicSql, String alias, String whereOrAnd, String match) {
        List<CriterionFilter> filters = object;
        for (CriterionFilter filter : filters) {
            if (dynamicSql.toString().matches(match)) {
                whereOrAnd = " AND ";
            }
            if (filter instanceof SearchFilter) {
                // 处理简单的条件，拼接参数
                CriterionFilter.Operator op = ((SearchFilter) filter).getOp();
                if (Objects.nonNull(op) && !op.equals(CriterionFilter.Operator.AND) && !op.equals(CriterionFilter.Operator.OR)) {
                    addCondition(dynamicSql, alias, whereOrAnd, (SearchFilter) filter);
                } else {
                    throw new IllegalArgumentException(" SearchFilter does not support the Operator.AND, OR condition! please use LogicSearchFilter !");
                }
            } else if (filter instanceof LogicSearchFilter) {
                // 判断Logic AND 或者 OR条件，递归进行拼装参数
                recursionAddLogicCondition(dynamicSql, alias, (LogicSearchFilter) filter);
            }
        }
    }

    private static void recursionAddLogicCondition(StringBuilder dynamicSql, String alias, LogicSearchFilter filter) {
        LogicSearchFilter logicFilter = filter;
        CriterionFilter.Operator op = logicFilter.getOp();
        if (Objects.nonNull(op) && !op.equals(CriterionFilter.Operator.AND) && !op.equals(CriterionFilter.Operator.OR)) {
            throw new IllegalArgumentException(" LogicSearchFilter just support the Operator.AND, OR condition! others codition please use SearchFilter !");
        }
        CriterionFilter[] criterion = logicFilter.getCriterion();
        if (criterion != null) {
            int len = criterion.length;
            if (len == 1) {
                // 处理只有一条数据的时候拼装
                dynamicSql.append(" ");
                CriterionFilter criter = criterion[0];
                if (criter instanceof SearchFilter) {
                    addCondition(dynamicSql, alias, op.getValue() + " ", (SearchFilter) criter);
                }
            } else {
                dynamicSql.append(" AND (");
                StringBuilder sb = new StringBuilder();
                CriterionFilter filterFirst = criterion[0];
                if (filterFirst instanceof SearchFilter) {
                    addCondition(dynamicSql, alias, " ", (SearchFilter) filterFirst);
                    for (int i = 1; i < len; i++) {
                        CriterionFilter criter = criterion[i];
                        if (criter instanceof SearchFilter) {
                            addCondition(sb, alias, " " + op.getValue() + " ", (SearchFilter) criter);
                        } else if (criter instanceof LogicSearchFilter) {
                            recursionAddLogicCondition(sb, alias, (LogicSearchFilter) criter);
                        }
                    }
                    dynamicSql.append(sb).append(") ");
                } else if (filterFirst instanceof LogicSearchFilter) {
                    recursionAddLogicCondition(dynamicSql, alias, (LogicSearchFilter) filterFirst);
                }
//                for (int i = 0; i < len; i++) {
//                    CriterionFilter criter = criterion[i];
//                    if (criter instanceof SearchFilter) {
//                        addCondition(sb, alias, " "+ op.getValue() + " ", (SearchFilter) criter);
//                    }
//                }
//                dynamicSql.append(sb).append(") ");
            }
        }
    }

    /**
     * 处理简单条件除AND、OR之外的参数拼装
     *
     * @param dynamicSql
     * @param alias
     * @param whereOrAnd
     * @param filter
     */
    private static void addCondition(StringBuilder dynamicSql, String alias, String whereOrAnd, SearchFilter filter) {
        String aliasName = alias.substring(0, alias.lastIndexOf(pointSeparator));
        String fieldName = filter.getFieldName();
        String field = fieldName;
        if (fieldName.contains(pointSeparator)) {
            String[] split = fieldName.split(pointSeparator);
            aliasName = split[0];
            field = split[1];
        }
        SearchFilter.Operator op = filter.getOp();
        switch (op) {
            case IN:
            case NOTIN: {
                opInOrNotInCondition(dynamicSql, whereOrAnd, filter, aliasName, fieldName, field, op);
                break;
            }
            case BETWEEN: {
                Object value = filter.getValue();
                if (value instanceof List) {
                    List values = (List) value;
                    if (!CollectionUtils.isEmpty(values) && values.size() > 1) {
                        String format = String.format(BETWEEN_CONDITION_STRING, aliasName, field, fieldName + 0, fieldName + 1);
                        dynamicSql.append(whereOrAnd).append(format);
                    }
                }
                break;
            }
            default:
                String format = String.format(DYNAMIC_CONDITION_STRING, aliasName, field, op.getValue(), fieldName);
                dynamicSql.append(whereOrAnd).append(format);
                break;
        }

    }

    private static void opInOrNotInCondition(StringBuilder dynamicSql, String whereOrAnd, SearchFilter filter, String aliasName, String fieldName, String field, CriterionFilter.Operator op) {
        Object value = filter.getValue();
        String format = String.format(SPECIAL_CONDITION_STRING, aliasName, field, op);
        dynamicSql.append(whereOrAnd).append(format);
        if (value instanceof List) {
            List values = (List) value;
            if (!CollectionUtils.isEmpty(values)) {
                String fieldStr = "";
                for (int i = 0; i < values.size(); i++) {
                    fieldStr += String.format(VARIABLE_CONDITION_STRING, fieldName + i) + ",";
                }
                if (fieldStr.length() > 0) {
                    fieldStr = fieldStr.substring(0, fieldStr.length() - 1);
                }
                dynamicSql.append(fieldStr).append(") ");
            }
        } else {
            dynamicSql.append(String.format(VARIABLE_CONDITION_STRING, fieldName)).append(") ");
        }
    }

    public static void setJpqlParameters(Object object, Query query) {

        if (object == null) {
            return;
        }
        if (object instanceof List) {
            List<CriterionFilter> filters = (List<CriterionFilter>) object;
            for (CriterionFilter criter : filters) {
                if (criter instanceof SearchFilter) {
                    setSearchFilterParameter(query, (SearchFilter) criter);
                } else if (criter instanceof LogicSearchFilter) {
                    recursionSetLogicSearchFilterParameter(query, (LogicSearchFilter) criter);
                }
            }
        } else {
            List<Field> fields = JpaQueryUtils.getDeclaredFields(object);
            for (Field field : fields) {
                try {
                    String name = field.getName();
                    Object value = ReflectionUtil.getValueByFieldName(object, name);
                    if (value != null) {
                        query.setParameter(name, value);
                    }
                } catch (IllegalAccessException e) {
                }
            }
        }
    }

    /**
     * 循环设置logicSearchFilter条件的参数值
     *
     * @param query
     * @param criter
     */
    private static void recursionSetLogicSearchFilterParameter(Query query, LogicSearchFilter criter) {
        LogicSearchFilter logicFilter = criter;
        CriterionFilter.Operator op = logicFilter.getOp();
        if (Objects.nonNull(op)) {
            switch (op) {
                case OR:
                case AND:
                    CriterionFilter[] criterion = logicFilter.getCriterion();
                    if (criterion != null) {
                        for (int i = 0; i < criterion.length; i++) {
                            CriterionFilter cri = criterion[i];
                            if (cri instanceof SearchFilter) {
                                setSearchFilterParameter(query, (SearchFilter) cri);
                            } else if (cri instanceof LogicSearchFilter) {
                                recursionSetLogicSearchFilterParameter(query, (LogicSearchFilter) cri);
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 设置简单条件的参数值
     *
     * @param query
     * @param criter
     */
    private static void setSearchFilterParameter(Query query, SearchFilter criter) {
        SearchFilter filter = criter;
        SearchFilter.Operator op = filter.getOp();
        String fieldName = filter.getFieldName();
        switch (op) {
            case AND:
            case OR: {
                throw new IllegalArgumentException(" SearchFilter does not support the AND,OR Operator, please use LogidSearchFilter! ");
            }
            case NOTIN:
            case IN: {
                Object value = filter.getValue();
                if (value instanceof List) {
                    List values = (List) value;
                    for (int i = 0; i < values.size(); i++) {
                        query.setParameter(fieldName + i, values.get(i));
                    }
                } else {
                    query.setParameter(fieldName, value);
                }
                break;
            }
            case BETWEEN: {
                Object value = filter.getValue();
                if (value instanceof List) {
                    List values = (List) value;
                    if (!CollectionUtils.isEmpty(values) && values.size() > 1) {
                        query.setParameter(fieldName + 0, values.get(0));
                        query.setParameter(fieldName + 1, values.get(1));
                    }
                }
                break;
            }
            default:
                query.setParameter(fieldName, filter.getValue());
                break;
        }
    }

    public static String getJpqlTableAlias(String jpql, String simpleName) {
        String alias = "";
        int simpleNameIdx = jpql.indexOf(simpleName);
        if (simpleNameIdx <= 0) {
            throw new IllegalArgumentException("jpql entityName must not be null!");
        }
        if (simpleNameIdx == simpleName.length()) {
            throw new IllegalArgumentException("jpql entityName must has alias name, but not found!");
        }
        String aliasStr = jpql.substring(simpleNameIdx + simpleName.length());
        String[] split = aliasStr.split(" ");
        if (split != null && split.length > 1) {
            for (String arg : split) {
                if (!"".equals(arg)) {
                    alias = arg + ".";
                    break;
                }
            }
        }
        return alias;
    }

    public static List<Field> getDeclaredFields(Object object) {
        List<Field> fields = new ArrayList<Field>();
        for (Class<?> superClass = object.getClass(); superClass != Object.class; superClass = superClass.getSuperclass()) {
            fields.addAll(Arrays.asList(superClass.getDeclaredFields()));
        }
        return fields;
    }

    public static String getCountSqlString(String jpql) {
        int fromIdx = jpql.indexOf("from");
        fromIdx = fromIdx > 0 ? fromIdx : jpql.indexOf("FROM");
        if (fromIdx <= 0) {
            throw new IllegalArgumentException("jpql is non-standard sql");
        }
        return "select count(1) " + jpql.substring(fromIdx);
    }

    public static String getSortCondition(Pageable pageable, String alias) {
        Sort sort = pageable.isPaged() ? pageable.getSort() : Sort.unsorted();
        return getSortCondition(sort, alias);
    }

    public static String getSortCondition(Sort sort, String alias) {
        if (Objects.nonNull(sort) && sort.isSorted()) {
            String sortStr = sort.toString();
            StringBuilder sb = new StringBuilder(sortStr.length() + 10);
            String[] split = sortStr.split(",");
            if (split == null) {
                return "";
            }
            StringBuilder orderBy = new StringBuilder(sortStr.length());
            for (String order : split) {
                String[] split1 = order.split(":");
                String key = split1[0];
                String direction = split1[1];
                if (key.contains(".")) {
                    orderBy.append(key).append(direction).append(",");
                } else {
                    orderBy.append(alias).append(key).append(direction).append(",");
                }
            }
            int i = orderBy.lastIndexOf(",");
            String orderStr = orderBy.substring(0, i);
            sb.append(" ORDER BY ").append(orderStr);
            return sb.toString();
        }
        return "";
    }
}
