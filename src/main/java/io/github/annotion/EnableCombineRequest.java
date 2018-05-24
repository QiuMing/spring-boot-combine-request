package io.github.annotion;

import java.lang.annotation.*;

/**
 * 功能：接口支付支持组合请求
 *
 * @author liaoming
 * @since 2018年05月23日
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableCombineRequest {

    String name() default "";
}
