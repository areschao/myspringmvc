package com.chao.annoation;

import java.lang.annotation.*;

/**
 * Created by LuZichao on  2018/12/17 11:31
 */
@Target({ElementType.PARAMETER})//用于修饰在参数上
@Retention(RetentionPolicy.RUNTIME) //声明周期 运行时可以获得
@Documented //javadoc 可以
public @interface ChaoRequestParam {
    String value() default "";
}
