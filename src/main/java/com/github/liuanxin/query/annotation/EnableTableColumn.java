package com.github.liuanxin.query.annotation;

import com.github.liuanxin.query.TcConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({TcConfig.class})
public @interface EnableTableColumn {
}
