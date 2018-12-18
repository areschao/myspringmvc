package com.chao.annoation;

import java.lang.annotation.*;

/**
 * Created by LuZichao on  2018/12/17 11:31
 */
@Target({ElementType.METHOD,ElementType.TYPE})//用于修饰在成员变量,类上
@Retention(RetentionPolicy.RUNTIME) //声明周期 运行时可以获得
@Documented //javadoc 可以
public @interface ChaoRequestMapping {
    String value() default "";
}
