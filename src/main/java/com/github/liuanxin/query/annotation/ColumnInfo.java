package com.github.liuanxin.query.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ColumnInfo {

    /** table column name */
    String value() default "";

    /** table column alias, use column name if empty */
    String alias() default "";

    /** table column comment */
    String desc() default "";

    /** true: this field is not associated with a column */
    boolean ignore() default false;

    /** true: this column is primary key */
    boolean primary() default false;

    /** varchar column's length */
    int varcharLength() default 0;

    /** true: this column not null */
    boolean notNull() default false;

    /** true: this column has default value */
    boolean hasDefault() default false;

    /** logic delete: default value. for example: 0 */
    String logicValue() default "";

    /** logic delete: delete value. for example: 1, id, UNIX_TIMESTAMP() */
    String logicDeleteValue() default "";
}
