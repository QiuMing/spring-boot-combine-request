package io.github.pojo;

import lombok.Data;

import java.util.Map;
import java.util.Objects;

/**
 * 功能：组合请求中的item
 *
 * @author liaoming
 * @since 2017年06月15日
 */
@Data
public class RequestItem {

    /**
     * 请求url
     */
    private String url;

    /**
     * 请求方式，对应 spring  mvc 中 RequestMethod 枚举类型：GET,HEAD,POST,PUT 等
     */
    private String method;

    /**
     * 请求id
     */
    private String requestId;

    /**
     * 请求的参数
     */
    private Map<String, Object> param;

    /**
     * 请求的header
     */
    private Map<String, Object> header;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RequestItem that = (RequestItem) o;
        return Objects.equals(url, that.url) &&
                Objects.equals(method, that.method) &&
                Objects.equals(requestId, that.requestId) &&
                Objects.equals(param, that.param) &&
                Objects.equals(header, that.header);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, method, requestId, param, header);
    }

}