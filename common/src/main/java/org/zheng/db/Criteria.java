package org.zheng.db;

import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;

import java.util.ArrayList;
import java.util.List;

/**
 * 标准查询的信息
 * Hold criteria query information.
 *
 * @param <T> Entity type.
 */
final class Criteria<T> {

    DbTemplate db;
    //实体类映射
    Mapper<T> mapper;
    //实体类类型
    Class<T> clazz;
    //字段列表
    List<String> select = null;
    boolean distinct = false;
    String table = null;
    String where = null;
    List<Object> whereParams = null;
    List<String> orderBy = null;
    int offset = 0;
    int maxResults = 0;

    Criteria(DbTemplate db) {
        this.db = db;
    }

    //动态生成查询语句
    String sql() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("SELECT ");
        sb.append((select == null ? "*" : String.join(", ", select)));
        sb.append(" FROM ").append(mapper.tableName);
        if (where != null) {
            sb.append(" WHERE ").append(String.join(" ", where));
        }
        if (orderBy != null) {
            sb.append(" ORDER BY ").append(String.join(", ", orderBy));
        }
        if (offset >= 0 && maxResults > 0) {
            sb.append(" LIMIT ?, ?");
        }
        String s = sb.toString();
        return s;
    }

    //生成查询参数
    Object[] params() {
        List<Object> params = new ArrayList<>();
        if (where != null) {
            for (Object obj : whereParams) {
                if (obj == null) {
                    params.add(null);
                } else {
                    params.add(obj);
                }
            }
        }
        if (offset >= 0 && maxResults > 0) {
            params.add(offset);
            params.add(maxResults);
        }
        return params.toArray();
    }

    List<T> list() {
        String selectSql = sql();
        Object[] selectParams = params();
        //返回执行结果
        return db.jdbcTemplate.query(selectSql, mapper.resultSetExtractor, selectParams);
    }

    T first() {
        this.offset = 0;
        this.maxResults = 1;
        String selectSql = sql();
        Object[] selectParams = params();
        List<T> list = db.jdbcTemplate.query(selectSql, mapper.resultSetExtractor, selectParams);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    T unique() {
        this.offset = 0;
        this.maxResults = 2;
        String selectSql = sql();
        Object[] selectParams = params();
        List<T> list = db.jdbcTemplate.query(selectSql, mapper.resultSetExtractor, selectParams);
        if (list.isEmpty()) {
            throw new NoResultException("Expected unique row but nothing found.");
        }
        if (list.size() > 1) {
            throw new NonUniqueResultException("Expected unique row but more than 1 rows found.");
        }
        return list.get(0);
    }
}