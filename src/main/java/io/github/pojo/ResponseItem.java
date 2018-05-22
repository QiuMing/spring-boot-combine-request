package io.github.pojo;

import lombok.Data;

/**
 * 功能：组合请求返回的item
 * @author liaoming
 * @since 2017年06月15日
 */
@Data
public class ResponseItem {

    /**
     * 请求id
     */
    private String requestId;

    /**
     * 请求url
     */
    private String url;

    /**
     * 请求响应状态码
     */
    private Integer httpStatus;

    /**
     * 请求响应实体
     */
    private Object entity;

}