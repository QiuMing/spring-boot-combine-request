package io.github.controller;

import io.github.annotion.EnableCombineRequest;
import io.github.pojo.RequestItem;
import io.github.pojo.ResponseItem;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cglib.core.CollectionUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.annotation.Resource;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 功能：组合接口
 *
 * @author liaoming
 * @since 2017年06月15日
 */
@Api(value = "组合接口") @Slf4j @RestController @RequestMapping("${combine.request.path}") public class CombineRequestController {

    @Resource private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Resource private ApplicationContext context;

    /**
     * 组合请求处理器缓存，key=url+method
     */
    private static Map<String, HandlerMethod> requestItemHandlerMethodMap = new ConcurrentHashMap<>(
            8);

    /**
     * 组合请求RequestMappingInfo缓存，key=url+method
     */
    private static Map<String, RequestMappingInfo> requestItemRequestMappingInfoMap = new ConcurrentHashMap<>(
            8);

    /**
     * 是否支持处理 List<RequestItem>，即是否有找到所有[url+method] 的方法处理器，以及对应的方法中，是否加了 EnableCombineRequest 注解
     */
    private static ThreadLocal<Boolean> supportAllRequestItems = new ThreadLocal<>();

    @PostMapping public ResponseEntity<List<ResponseItem>> combine(
            @RequestBody List<RequestItem> requestItems) {
        //获取所有方法处理器
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping
                .getHandlerMethods();

        //此 map 存放当前请求，对应的方法处理器
        Map<RequestItem, HandlerMethod> handlerMethodMap = new HashMap<>(requestItems.size());

        //此 map 存放当前请求，参数映射信息
        Map<RequestItem, RequestMappingInfo> requestMappingInfoMap = new HashMap<>();

        supportAllRequestItems.set(Boolean.TRUE);
        requestItems.forEach(requestItem -> {
            String url = requestItem.getUrl().startsWith("/") ?
                    requestItem.getUrl() :
                    "/" + requestItem.getUrl();
            String strRequestMethod = requestItem.getMethod().toUpperCase();
            RequestMethod requestMethod = RequestMethod.valueOf(strRequestMethod);
            String requestUrlAndMethod = url + strRequestMethod;

            //先尝试从缓存中获取方法处理器和参数映射信息
            if (requestItemHandlerMethodMap.get(requestUrlAndMethod) != null
                    && requestItemRequestMappingInfoMap.get(requestUrlAndMethod) != null) {
                if (!supportAllRequestItems.get()) {
                    return;
                }

                handlerMethodMap
                        .put(requestItem, requestItemHandlerMethodMap.get(requestUrlAndMethod));
                requestMappingInfoMap.put(requestItem,
                        requestItemRequestMappingInfoMap.get(requestUrlAndMethod));

            } else {

                handlerMethods.forEach((requestMappingInfo, handlerMethod) -> {
                    if (!supportAllRequestItems.get()) {
                        return;
                    }
                    requestMappingInfo.getPatternsCondition().getPatterns();
                    List<String> matchingPatterns = requestMappingInfo.getPatternsCondition()
                            .getMatchingPatterns(url);
                    Set<RequestMethod> methods = requestMappingInfo.getMethodsCondition()
                            .getMethods();

                    if (matchingPatterns.size() > 0 && methods.contains(requestMethod)) {

                        EnableCombineRequest enableCombineRequest = handlerMethod
                                .getMethodAnnotation(EnableCombineRequest.class);
                        if (enableCombineRequest == null) {
                            log.warn("not support combine request,method:{},it's url:{}",
                                    requestItem.getMethod(), requestItem.getUrl());
                            supportAllRequestItems.set(Boolean.FALSE);
                        }
                        handlerMethodMap.put(requestItem, handlerMethod);
                        requestMappingInfoMap.put(requestItem, requestMappingInfo);

                        //设置缓存
                        requestItemHandlerMethodMap.put(requestUrlAndMethod, handlerMethod);
                        requestItemRequestMappingInfoMap
                                .put(requestUrlAndMethod, requestMappingInfo);
                        return;
                    }
                });
            }
        });

        if (!supportAllRequestItems.get()) {
            throw new IllegalArgumentException("exist not support request");
        }

        List<ResponseItem> responseItems = new ArrayList<>(requestItems.size());

        //通过反射调用获取执行方法的结果
        requestItems.parallelStream().forEach(requestItem -> {
            ResponseItem responseItem = new ResponseItem();
            responseItem.setRequestId(requestItem.getRequestId());
            responseItem.setUrl(requestItem.getUrl());

            HandlerMethod handlerMethod = handlerMethodMap.get(requestItem);
            RequestMappingInfo requestMappingInfo = requestMappingInfoMap.get(requestItem);
            if (null == handlerMethod) {
                log.warn("not found handlerMethod for request url:{}", requestItem.getUrl());
                responseItem.setHttpStatus(HttpStatus.NOT_FOUND.value());
            } else {
                //方法所在类
                Object object = context.getBean(handlerMethod.getBean().toString());
                Method method = handlerMethod.getMethod();

                //组装请求参数
                Object[] args = getArgs(method, requestItem, requestMappingInfo);

                Object result;
                try {
                    result = ReflectionUtils.invokeMethod(method, object, args);
                    responseItem.setEntity(result);
                    responseItem.setHttpStatus(HttpStatus.OK.value());
                } catch (IllegalArgumentException e) {
                    log.error("invoke method error method={},args={},exception msg={}", method,
                            args, e.getMessage());
                    responseItem.setEntity("bad request");
                    responseItem.setHttpStatus(HttpStatus.BAD_REQUEST.value());
                }
            }
            responseItems.add(responseItem);
        });
        return ResponseEntity.ok(responseItems);
    }

    private String parseDefaultValueAttribute(String value) {
        return (ValueConstants.DEFAULT_NONE.equals(value) ? null : value);
    }

    /**
     * @param method          处理请求的方法
     * @param requestItem 请求Item  对象
     * @param requestMappingInfo 处理请求  Item 方法，参数映射信息
     * @return 组装好顺序，传给方法的值
     */
    private Object[] getArgs(Method method, RequestItem requestItem,
            RequestMappingInfo requestMappingInfo) {
        Map<String, Object> requestParamMap = requestItem.getParam();
        String requestUrl = requestItem.getUrl();
        DefaultParameterNameDiscoverer defaultParameterNameDiscoverer = new DefaultParameterNameDiscoverer();
        String[] methodParameterNames = defaultParameterNameDiscoverer.getParameterNames(method);

        if (methodParameterNames.length == 0)  {
            log.debug("method：{} not need parameter", method.getName());
            return null;
        }
        Object[] objects = new Object[] {};

        LinkedList<Object> collect = new LinkedList<>();

        for (int i = 0; i < methodParameterNames.length; i++) {
            //方法参数，字面量的名称
            String methodParameterName = methodParameterNames[i];
            //方法参数对象
            MethodParameter methodParam = new SynthesizingMethodParameter(method, i);
            Class<?>[] parameterTypes = method.getParameterTypes();

            Annotation[] paramAnns = methodParam.getParameterAnnotations();
            if (paramAnns.length != 0) {
                //TODO foreach 遍历处理注解
                Annotation paramAnn = paramAnns[0];

                if (RequestParam.class.isInstance(paramAnn)) {
                    RequestParam requestParam = (RequestParam) paramAnn;
                    String paramName = requestParam.name();
                    boolean required = requestParam.required();
                    String defaultValue = parseDefaultValueAttribute(requestParam.defaultValue());

                    if (requestParamMap == null) {
                        if (required) {
                            collect.add(defaultValue);
                            continue;
                        } else {
                            throw new IllegalArgumentException(
                                    "get param value fail,require param:" + requestParam.value());
                        }
                    }
                    Object param = requestParamMap.get(paramName);

                    if (required) {
                        Assert.isTrue(param != null || defaultValue != null, paramName
                                + "is require,but not found from request and not default value");
                    }
                    defaultValue = param == null ? defaultValue : param.toString();
                    collect.add(defaultValue);

                    if (defaultValue == null) {
                        log.warn(
                                "param has RequestParam Annotation,required = false,not default value,and not found from value request");
                    }
                    continue;
                } else if (PathVariable.class.isInstance(paramAnn)) {
                    PathVariable pathVariable = (PathVariable) paramAnn;

                    String bestPattern;
                    Map<String, String> uriVariables;
                    Map<String, String> decodedUriVariables;

                    Set<String> patterns = requestMappingInfo.getPatternsCondition().getPatterns();
                    if (patterns.isEmpty()) {
                        log.info("pattern is empty");
                    } else {
                        bestPattern = patterns.iterator().next();
                        uriVariables = new AntPathMatcher()
                                .extractUriTemplateVariables(bestPattern, requestUrl);
                        Class<?> parameterType = parameterTypes[i];
                        String pathVariableValue = uriVariables.get(pathVariable.value());
                        Object value;
                        if (parameterType == Integer.class) {
                            value = Integer.valueOf(pathVariableValue);
                        } else if (parameterType == Long.class) {
                            value = Long.valueOf(pathVariableValue);
                        } else {
                            value = pathVariableValue;
                        }
                        collect.add(value);
                        continue;
                        //decodedUriVariables = getUrlPathHelper().decodePathVariables(request, uriVariables);
                    }
                }
            }

            //没有注解，直接按照字面量尝试取值
            Object param = requestParamMap.get(methodParameterName);
            if (param != null) {
                collect.add(param);
            } else {
                log.info("not found method param value from request,param name:{}",
                        methodParameterName);
            }
        }
        return collect.toArray(objects);
    }
}
