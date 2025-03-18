package org.zheng.db;

import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

//将java类映射到数据库表
public class Mapper<T> {
    final Logger logger = LoggerFactory.getLogger(getClass());

    //目标java类，构造方法
    final Class<T> entityClass;
    final Constructor<T> constructor;
    //目标表名
    final String tableName;

    // @Id 主键属性
    final AccessibleProperty id;

    // 所有属性（包括@Id） 关键字就是属性名
    final List<AccessibleProperty> allProperties;

    // 字段名到属性的映射
    final Map<String, AccessibleProperty> allPropertiesMap;

    //可插入 更新
    final List<AccessibleProperty> insertableProperties;
    final List<AccessibleProperty> updatableProperties;

    // 可更新字段的映射
    final Map<String, AccessibleProperty> updatablePropertiesMap;

    //查询结果映射到java的提取器，一个保存bean对象的列表
    final ResultSetExtractor<List<T>> resultSetExtractor;

    //自动生成的SQL语句
    final String selectSQL;
    final String insertSQL;
    final String insertIgnoreSQL;
    final String updateSQL;
    final String deleteSQL;

    public T newInstance() throws ReflectiveOperationException {
        return this.constructor.newInstance();
    }

    //解析实体类的字段和注解，生成SQL语句和结果集提取器
    public Mapper(Class<T> clazz) throws Exception {
        List<AccessibleProperty> all = getProperties(clazz);
        //过滤主键字段
        AccessibleProperty[] ids = all.stream().filter(AccessibleProperty::isId).toArray(AccessibleProperty[]::new);
        if (ids.length != 1) {
            throw new RuntimeException("只需要一个主键" + clazz.getName());
        }
        this.id = ids[0];
        this.allProperties = all;
        this.allPropertiesMap = buildPropertiesMap(this.allProperties);
        this.insertableProperties = all.stream().filter(AccessibleProperty::isInsertable).collect(Collectors.toList());
        this.updatableProperties = all.stream().filter(AccessibleProperty::isUpdatable).collect(Collectors.toList());
        this.updatablePropertiesMap = buildPropertiesMap(this.updatableProperties);
        //实体类赋值
        this.entityClass = clazz;
        this.constructor = clazz.getConstructor();
        this.tableName = getTableName(clazz);
        this.selectSQL = "SELECT * FROM " + this.tableName + " WHERE " + this.id.propertyName + " = ?";
        this.insertSQL = "INSERT INTO " + this.tableName + " ("
                + String.join(", ", this.insertableProperties.stream().map(p -> p.propertyName).toArray(String[]::new))
                + ") VALUES (" + numOfQuestions(this.insertableProperties.size()) + ")";
        this.insertIgnoreSQL = this.insertSQL.replace("INSERT INTO", "INSERT IGNORE INTO");
        this.updateSQL = "UPDATE " + this.tableName + " SET "
                + String.join(", ",
                this.updatableProperties.stream().map(p -> p.propertyName + " = ?").toArray(String[]::new))
                + " WHERE " + this.id.propertyName + " = ?";
        this.deleteSQL = "DELETE FROM " + this.tableName + " WHERE " + this.id.propertyName + " = ?";
        //实现ResultSetExtractor接口
        this.resultSetExtractor = new ResultSetExtractor<>() {
            @Override
            public List<T> extractData(ResultSet rs) throws SQLException, DataAccessException {
                final List<T> results = new ArrayList<>();
                final ResultSetMetaData m = rs.getMetaData();
                final int cols = m.getColumnCount();
                final String[] names = new String[cols];
                for (int i = 0; i < cols; i++) {
                    names[i] = m.getColumnLabel(i + 1);
                }
                try {
                    while (rs.next()) {
                        //创建对象
                        T bean = newInstance();
                        for (int i = 0; i < cols; i++) {
                            String name = names[i];
                            //赋值
                            AccessibleProperty p = allPropertiesMap.get(name);
                            if (p != null) {
                                p.set(bean, rs.getObject(i + 1));
                            }
                        }
                        results.add(bean);
                    }
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
                return results;
            }
        };
    }

    Object getIdValue(Object bean) throws ReflectiveOperationException {
        return this.id.get(bean);
    }

    Map<String, AccessibleProperty> buildPropertiesMap(List<AccessibleProperty> props) {
        Map<String, AccessibleProperty> map = new HashMap<>();
        for (AccessibleProperty prop : props) {
            map.put(prop.propertyName, prop);
        }
        return map;
    }

    //占位符“？”的生成
    private String numOfQuestions(int n) {
        String[] qs = new String[n];
        return String.join(", ", Arrays.stream(qs).map((s) -> {
            return "?";
        }).toArray(String[]::new));
    }

    private String getTableName(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        if (table != null && !table.name().isEmpty()) {
            return table.name();
        }
        String name = clazz.getSimpleName();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    //获取字段列表
    private List<AccessibleProperty> getProperties(Class<?> clazz) throws Exception {
        List<AccessibleProperty> properties = new ArrayList<>();
        for (Field f : clazz.getFields()) {
            if (Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            if (f.isAnnotationPresent(Transient.class)) {
                continue;
            }
            var p = new AccessibleProperty(f);
            logger.debug("found accessible property: {}", p);
            properties.add(p);
        }
        return properties;
    }

    //生成创建表的DDL语句 数据库模式定义语言 data definition language
    public String ddl() {
        StringBuilder sb = new StringBuilder(256);
        sb.append("CREATE TABLE ").append(this.tableName).append(" (\n");
        sb.append(String.join(",\n", this.allProperties.stream().sorted((o1, o2) -> {
            // sort by ID first:
            if (o1.isId()) {
                return -1;
            }
            if (o2.isId()) {
                return 1;
            }
            // sort by columnName:
            return o1.propertyName.compareTo(o2.propertyName);
        }).map((p) -> {
            return "  " + p.propertyName + " " + p.columnDefinition;
        }).toArray(String[]::new)));
        sb.append(",\n");
        // add unique key:
        sb.append(getUniqueKey());
        // add index:
        sb.append(getIndex());
        // add primary key:
        sb.append("  PRIMARY KEY(").append(this.id.propertyName).append(")\n");
        sb.append(") CHARACTER SET utf8 COLLATE utf8_general_ci AUTO_INCREMENT = 1000;\n");
        return sb.toString();
    }

    String getUniqueKey() {
        Table table = this.entityClass.getAnnotation(Table.class);
        if (table != null) {
            return Arrays.stream(table.uniqueConstraints()).map((c) -> {
                String name = c.name().isEmpty() ? "UNI_" + String.join("_", c.columnNames()) : c.name();
                return "  CONSTRAINT " + name + " UNIQUE (" + String.join(", ", c.columnNames()) + "),\n";
            }).reduce("", (acc, s) -> {
                return acc + s;
            });
        }
        return "";
    }

    String getIndex() {
        Table table = this.entityClass.getAnnotation(Table.class);
        if (table != null) {
            return Arrays.stream(table.indexes()).map((c) -> {
                if (c.unique()) {
                    String name = c.name().isEmpty() ? "UNI_" + c.columnList().replace(" ", "").replace(",", "_")
                            : c.name();
                    return "  CONSTRAINT " + name + " UNIQUE (" + c.columnList() + "),\n";
                } else {
                    String name = c.name().isEmpty() ? "IDX_" + c.columnList().replace(" ", "").replace(",", "_")
                            : c.name();
                    return "  INDEX " + name + " (" + c.columnList() + "),\n";
                }
            }).reduce("", (acc, s) -> {
                return acc + s;
            });
        }
        return "";
    }

    static List<String> columnDefinitionSortBy = Arrays.asList("BIT", "BOOL", "TINYINT", "SMALLINT", "MEDIUMINT", "INT",
            "INTEGER", "BIGINT", "FLOAT", "REAL", "DOUBLE", "DECIMAL", "YEAR", "DATE", "TIME", "DATETIME", "TIMESTAMP",
            "VARCHAR", "CHAR", "BLOB", "TEXT", "MEDIUMTEXT");

    static int columnDefinitionSortIndex(String definition) {
        int pos = definition.indexOf('(');
        if (pos > 0) {
            definition = definition.substring(0, pos);
        }
        int index = columnDefinitionSortBy.indexOf(definition.toUpperCase());
        return index == (-1) ? Integer.MAX_VALUE : index;
    }

}
