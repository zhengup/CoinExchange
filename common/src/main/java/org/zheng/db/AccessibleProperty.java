package org.zheng.db;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

//访问属性类，使用JPA注解表示bean公共字段，将java的字段映射到sql的列属性
class AccessibleProperty {
    // 目标字段，反射
    private final Field field;

    // 字段的java类型
    final Class<?> propertyType;

    //java字段和数据库直接转换数据类型
    final Function<Object, Object> javaToSqlMapper;

    final Function<Object, Object> sqlToJavaMapper;

    // 字段名称
    final String propertyName;

    // 数据库列的定义
    final String columnDefinition;

    //获取目标字段的值
    public Object get(Object bean) throws ReflectiveOperationException {
        Object obj = this.field.get(bean);
        if (this.javaToSqlMapper != null) {
            obj = this.javaToSqlMapper.apply(obj);
        }
        return obj;
    }

    //设置字段值
    public void set(Object bean, Object value) throws ReflectiveOperationException {
        if (this.sqlToJavaMapper != null) {
            value = this.sqlToJavaMapper.apply(value);
        }
        this.field.set(bean, value);
    }

    //判断是否是主键
    boolean isId() {
        return this.field.getAnnotation(Id.class) != null;
    }

    //判断是否是自增主键，通过@GeneratedValue(strategy = GenerationType.IDENTITY)注解
    boolean isIdentityId() {
        if (!isId()) {
            return false;
        }
        GeneratedValue gv = this.field.getAnnotation(GeneratedValue.class);
        if (gv == null) {
            return false;
        }
        GenerationType gt = gv.strategy();
        return gt == GenerationType.IDENTITY;
    }

    //判断字段是否插入数据库
    boolean isInsertable() {
        if (isIdentityId()) {
            return false;
        }
        Column col = this.field.getAnnotation(Column.class);
        return col == null || col.insertable();
    }

    //判断是否可以更新
    boolean isUpdatable() {
        if (isId()) {
            return false;
        }
        Column col = this.field.getAnnotation(Column.class);
        return col == null || col.updatable();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public AccessibleProperty(Field f) {
        this.field = f;
        //反射获取字段类型名称和数据库列
        this.propertyType = f.getType();
        this.propertyName = f.getName();
        this.columnDefinition = getColumnDefinition(this.propertyType);
        //枚举类就设置转换方法
        boolean isEnum = f.getType().isEnum();
        this.javaToSqlMapper = isEnum ? (obj) -> ((Enum<?>) obj).name() : null;
        this.sqlToJavaMapper = isEnum ? (obj) -> Enum.valueOf((Class<? extends Enum>) this.propertyType, (String) obj) : null;
    }

    //列定义
    private String getColumnDefinition(Class<?> type) {
        //获取注解
        Column col = this.field.getAnnotation(Column.class);
        if (col == null) {
            throw new IllegalArgumentException("@Column not found on: " + this.field.toString());
        }
        if (!col.name().isEmpty()) {
            throw new IllegalArgumentException(
                    "@Column(name=\"" + col.name() + "\") is not supported: " + this.field.toString());
        }
        String colDef = null;
        if (col == null || col.columnDefinition().isEmpty()) {
            if (type.isEnum()) {
                colDef = "VARCHAR(32)";
            } else {
                colDef = getDefaultColumnType(type, col);
            }
        } else {
            colDef = col.columnDefinition().toUpperCase();
        }
        boolean nullable = col == null ? true : col.nullable();
        colDef = colDef + " " + (nullable ? "NULL" : "NOT NULL");

        if (isIdentityId()) {
            colDef = colDef + " AUTO_INCREMENT";
        }

        if (!isId() && col != null && col.unique()) {
            colDef = colDef + " UNIQUE";
        }

        return colDef;
    }

    //获取默认的列类型
    private static String getDefaultColumnType(Class<?> type, Column col) {
        String ddl = DEFAULT_COLUMN_TYPES.get(type);
        if (ddl.equals("VARCHAR($1)")) {
            ddl = ddl.replace("$1", String.valueOf(col == null ? 255 : col.length()));
        }
        if (ddl.equals("DECIMAL($1,$2)")) {
            int preci = col == null ? 0 : col.precision();
            int scale = col == null ? 0 : col.scale();
            if (preci == 0) {
                preci = 10; // default DECIMAL precision of MySQL
            }
            ddl = ddl.replace("$1", String.valueOf(preci)).replace("$2", String.valueOf(scale));
        }
        return ddl;
    }

    static final Map<Class<?>, String> DEFAULT_COLUMN_TYPES = new HashMap<>();

    //静态代码执行块，设置默认的列类型映射，从java到sql类型
    static {
        DEFAULT_COLUMN_TYPES.put(String.class, "VARCHAR($1)");

        DEFAULT_COLUMN_TYPES.put(boolean.class, "BIT");
        DEFAULT_COLUMN_TYPES.put(Boolean.class, "BIT");

        DEFAULT_COLUMN_TYPES.put(byte.class, "TINYINT");
        DEFAULT_COLUMN_TYPES.put(Byte.class, "TINYINT");
        DEFAULT_COLUMN_TYPES.put(short.class, "SMALLINT");
        DEFAULT_COLUMN_TYPES.put(Short.class, "SMALLINT");
        DEFAULT_COLUMN_TYPES.put(int.class, "INTEGER");
        DEFAULT_COLUMN_TYPES.put(Integer.class, "INTEGER");
        DEFAULT_COLUMN_TYPES.put(long.class, "BIGINT");
        DEFAULT_COLUMN_TYPES.put(Long.class, "BIGINT");
        DEFAULT_COLUMN_TYPES.put(float.class, "REAL");
        DEFAULT_COLUMN_TYPES.put(Float.class, "REAL");
        DEFAULT_COLUMN_TYPES.put(double.class, "DOUBLE");
        DEFAULT_COLUMN_TYPES.put(Double.class, "DOUBLE");

        DEFAULT_COLUMN_TYPES.put(BigDecimal.class, "DECIMAL($1,$2)");
    }

    @Override
    public String toString() {
        return "AccessibleProperty [propertyName=" + propertyName + ", propertyType=" + propertyType
                + ", columnDefinition=" + columnDefinition + "]";
    }
}
