package io.github.controller;

import com.alibaba.fastjson.JSON;
import io.github.annotion.EnableCombineRequest;
import io.github.pojo.RequestItem;
import io.github.pojo.ResponseItem;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 功能：组合接口
 *
 * @author liaoming
 * @since 2017年06月15日
 */
@Api(value = "组合接口")
@Slf4j
@RestController
@RequestMapping("${combine.request.path}")
public class CombineRequestController {

    @Resource
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Resource
    private ApplicationContext context;

    /**
     * 组合请求处理器缓存，key=url+method
     */
    private static Map<String, HandlerMethod> requestItemHandlerMethodMap = new ConcurrentHashMap<>(8);

    private ThreadLocal<Boolean> supportAllRequestItems = new ThreadLocal<>();

    @PostMapping
    public ResponseEntity<List<ResponseItem>> combine(@RequestBody List<RequestItem> requestItems) {
        //获取所有方法处理器
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();

        //此 map 存放当前请求，对应的方法处理器
        Map<RequestItem, HandlerMethod> handlerMethodMap = new HashMap<>(requestItems.size());

        supportAllRequestItems.set(Boolean.TRUE);
        requestItems.forEach(requestItem -> {
            String url = requestItem.getUrl().startsWith("/") ? requestItem.getUrl() : "/" + requestItem.getUrl();
            String strRequestMethod = requestItem.getMethod().toUpperCase();
            RequestMethod requestMethod = RequestMethod.valueOf(strRequestMethod);
            String requestUrlAndMethod = url + strRequestMethod;


            if (requestItemHandlerMethodMap.get(requestUrlAndMethod) != null) {
                if(!supportAllRequestItems.get()){
                    return;
                }

                handlerMethodMap.put(requestItem, requestItemHandlerMethodMap.get(requestUrlAndMethod));
            } else {

                handlerMethods.forEach((requestMappingInfo, handlerMethod) -> {

                    if(!supportAllRequestItems.get()){
                        return;
                    }

                    List<String> matchingPatterns = requestMappingInfo.getPatternsCondition().getMatchingPatterns(url);

                    Set<RequestMethod> methods = requestMappingInfo.getMethodsCondition().getMethods();
                    if (matchingPatterns.size() > 0 && methods.contains(requestMethod)) {

                        EnableCombineRequest enableCombineRequest = handlerMethod.getMethodAnnotation(EnableCombineRequest.class);
                        if(enableCombineRequest == null){
                            log.warn("not support combine request,url:{},method:{}",requestItem.getUrl(),requestItem.getMethod());
                            supportAllRequestItems.set(Boolean.FALSE);

                        }

                        handlerMethodMap.put(requestItem, handlerMethod);
                        requestItemHandlerMethodMap.put(requestUrlAndMethod, handlerMethod);
                    }
                });
            }
        });

        if(!supportAllRequestItems.get()){
            throw new IllegalArgumentException("exist not support request");
        }


        List<ResponseItem> responseItems = new ArrayList<>(requestItems.size());

        //通过反射调用获取执行方法的结果
        requestItems.parallelStream().forEach(requestItem -> {
            ResponseItem responseItem = new ResponseItem();
            responseItem.setRequestId(requestItem.getRequestId());
            responseItem.setUrl(requestItem.getUrl());

            HandlerMethod handlerMethod = handlerMethodMap.get(requestItem);
            if (null == handlerMethod) {
                log.warn("not found handlerMethod for request url:{}", requestItem.getUrl());
                responseItem.setHttpStatus(HttpStatus.NOT_FOUND.value());
            } else {
                //方法所在类
                Object object = context.getBean(handlerMethod.getBean().toString());
                Method method = handlerMethod.getMethod();

                //组装请求参数
                Object[] args = getArgs(method, requestItem.getParam());

                Object result;
                try {
                    result = ReflectionUtils.invokeMethod(method, object, args);
                } catch (IllegalArgumentException e) {
                    log.error("invoke method error method={},args={},exception msg={}", method, args, e.getMessage());
                    responseItem.setEntity(null);
                    responseItem.setHttpStatus(HttpStatus.BAD_REQUEST.value());
                    return;
                }
                responseItem.setEntity(result);
                responseItem.setHttpStatus(HttpStatus.OK.value());
            }
            responseItems.add(responseItem);
        });
        return ResponseEntity.ok(responseItems);
    }


    /**
     * @param method          处理请求的方法
     * @param requestParamMap 请求参数 Map
     * @return
     */
    private Object[] getArgs(Method method, Map<String, Object> requestParamMap) {
        DefaultParameterNameDiscoverer defaultParameterNameDiscoverer = new DefaultParameterNameDiscoverer();
        String[] methodParameterNames = defaultParameterNameDiscoverer.getParameterNames(method);

        if (methodParameterNames == null) {
            log.debug("method：{} not need parameter", method.getName());
            return null;
        }
        Object[] objects = new Object[]{};

        LinkedList<Object> collect = new LinkedList<>();
        for (String methodParameterName : methodParameterNames) {
            if (!CollectionUtils.isEmpty(requestParamMap)) {
                Object param = requestParamMap.get(methodParameterName);
                collect.add(param);
            } else {
                log.info("not found method param value from request,param name:{}", methodParameterName);
            }
        }
        return collect.toArray(objects);
    }
}
