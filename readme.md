##  Spring 组合请求 API demo

### 背景

* app 在启动的时候，会调用服务端多个接口，随着业务迭代开发，请求接口会越来越多，致使 app 在启动的时候会有多次 io 请求，启动缓慢。
* 为解决此问题，app 端同学会要求服务在一个多口内，返回多种业务数据，或者要求在一个接口，组合响应多个接口数据。在此背景下，组合请求接口因需而生。

## 分析

* 服务端定义的 restful 接口，合理划分好粒度。
* 客户端同学，按需要，自己组合请求所需要的接口，带上参数，一次性请求到服务端，服务的一次处理好多个接口请求后，统一返回多个接口数据。

## 实现
 
* 通过 requestMappingHandlerMapping.getHandlerMethods() 方法，可以获取到所有的方法处理器,即为 ```Map<RequestMappingInfo, HandlerMethod> HandlerMethodMap = requestMappingHandlerMapping.getHandlerMethods();```
* 根据 requestItem 中的 url 和 method ，遍历 HandlerMethodMap ，可以从唯一确定一个 HandlerMethod。
* 获取到 HandlerMethod ，后面可以从里面获取到该方法所在类，利用 ReflectionUtils.invokeMethod(method, object, args) 反射执行方法，获取结果。

## 举例说明：

* request
```json
[
  {
    "method": "GET",
    "requestId": "111",
    "url": "/ming"
  },  {
    "method": "GET",
    "requestId": "222",
    "url": "/fang"
  }
]
```

* response

```json
[
  {
    "requestId": "222",
    "url": "/fang",
    "httpStatus": 200,
    "entity": {
      "id": 1,
      "name": "芳"
    }
  },
  {
    "requestId": "111",
    "url": "/ming",
    "httpStatus": 200,
    "entity": {
      "id": 2,
      "name": "明"
    }
  }
]
```

## 局限

* 不支持绑定了参数的方法，不支持 validator 参数校验
