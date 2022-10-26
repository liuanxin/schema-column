package com.github.liuanxin.query.util;

import com.github.liuanxin.query.annotation.ColumnInfo;
import com.github.liuanxin.query.annotation.TableInfo;
import com.github.liuanxin.query.function.FunctionSerialize;
import com.github.liuanxin.query.function.SupplierSerialize;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class QueryLambdaUtil {

    private static final Map<String, Class<?>> CLASS_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Field> CLASS_FIELD_MAP = new ConcurrentHashMap<>();

    private static SerializedLambda getLambdaMataInfo(Serializable obj) {
        try {
            Method lambdaMethod = obj.getClass().getDeclaredMethod("writeReplace");
            // noinspection deprecation
            boolean accessible = lambdaMethod.isAccessible();
            if (!accessible) {
                lambdaMethod.setAccessible(true);
            }
            SerializedLambda lambda = (SerializedLambda) lambdaMethod.invoke(obj);
            if (!accessible) {
                lambdaMethod.setAccessible(false);
            }
            return lambda;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("get lambda method exception", e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("invoke lambda method exception", e);
        }
    }

    private static Class<?> toClass(SerializedLambda lambda) {
        String className = lambda.getImplClass();
        try {
            Class<?> clazz = CLASS_MAP.get(className);
            if (clazz == null) {
                Class<?> useClazz = Class.forName(className.replace("/", "."));
                CLASS_MAP.put(className, useClazz);
                return useClazz;
            }
            return clazz;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("no Class(" + className + ")", e);
        }
    }

    private static Field methodToField(Class<?> clazz, String methodName) {
        String className = clazz.getName();
        if (!methodName.startsWith("is") && !methodName.startsWith("get")) {
            throw new RuntimeException("method(" + methodName + ") in(" + className + ") is not a get-method of a property");
        }

        String fieldMethodName = methodName.substring(methodName.startsWith("is") ? 2 : 3);
        String fieldName = fieldMethodName.substring(0, 1).toLowerCase() + fieldMethodName.substring(1);
        try {
            String key = className + "-->" + fieldName;
            Field field = CLASS_FIELD_MAP.get(key);
            if (field == null) {
                Field useField = clazz.getDeclaredField(fieldName);
                CLASS_FIELD_MAP.put(key, useField);
                return useField;
            }
            return field;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Class(" + className + ") no field(" + fieldName + ")", e);
        }
    }

    public static <T> Class<?> toClass(SupplierSerialize<T> supplier) {
        return toClass(getLambdaMataInfo(supplier));
    }
    public static <T> Class<?> toClass(FunctionSerialize<T, ?> function) {
        return toClass(getLambdaMataInfo(function));
    }

    public static <T> Field toField(SupplierSerialize<T> supp) {
        SerializedLambda lambda = getLambdaMataInfo(supp);
        return methodToField(toClass(lambda), lambda.getImplMethodName());
    }
    public static <T> Field toField(FunctionSerialize<T, ?> function) {
        SerializedLambda lambda = getLambdaMataInfo(function);
        return methodToField(toClass(lambda), lambda.getImplMethodName());
    }

    public static <T> String toTableName(SupplierSerialize<T> supplier) {
        return toTableName("", supplier);
    }
    public static <T> String toTableName(String tablePrefix, SupplierSerialize<T> supplier) {
        return toTableName(tablePrefix, toClass(supplier));
    }
    private static String toTableName(String tablePrefix, Class<?> clazz) {
        TableInfo tableInfo = clazz.getAnnotation(TableInfo.class);
        if (QueryUtil.isNull(tableInfo)) {
            return QueryUtil.classToTableName(tablePrefix, clazz.getSimpleName());
        } else {
            return tableInfo.ignore() ? "" : tableInfo.value();
        }
    }
    public static <T> String toTableName(FunctionSerialize<T, ?> function) {
        return toTableName("", function);
    }
    public static <T> String toTableName(String tablePrefix, FunctionSerialize<T, ?> function) {
        return toTableName(tablePrefix, toClass(function));
    }

    public static <T> String toColumnName(SupplierSerialize<T> supplier) {
        return toColumnName(toField(supplier));
    }
    private static String toColumnName(Field field) {
        ColumnInfo columnInfo = field.getAnnotation(ColumnInfo.class);
        if (QueryUtil.isNull(columnInfo)) {
            return QueryUtil.fieldToColumnName(field.getName());
        } else {
            return columnInfo.ignore() ? "" : columnInfo.value();
        }
    }
    public static <T> String toColumnName(FunctionSerialize<T, ?> function) {
        return toColumnName(toField(function));
    }
}
