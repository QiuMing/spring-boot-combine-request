##  Spring 组合请求 API demo

### 背景

* app 在启动的时候，会调用服务端多个接口，随着业务迭代开发，请求接口会越来越多，致使 app 在启动的时候会有多次 io 请求，启动缓慢。
* 为解决此问题，app 端同学会要求服务端同学，在一个接口内，返回多种业务数据，或者要求在一个接口，组合响应多个接口数据。在此背景下，组合请求接口因需而生。

## 分析

* 服务端定义的 restful 接口，合理划分好粒度，标明哪些是支持组合请求接口的。
* 客户端同学，按需要，自己组合请求所需要的接口（前提是这些接口支持放在组合接口内），带上参数，一次性请求到服务端，服务端一次处理好多个接口请求后，统一返回多个接口数据。

## 实现
 
* 通过 requestMappingHandlerMapping.getHandlerMethods() 方法，可以获取到所有的方法处理器，即为 ```Map<RequestMappingInfo, HandlerMethod> HandlerMethodMap = requestMappingHandlerMapping.getHandlerMethods();```
* 根据 requestItem 中的 url 和 method ，遍历 HandlerMethodMap ，可以唯一确定一个 HandlerMethod。
* 获取到 HandlerMethod ，后面可以从里面取到该方法所在类，利用 ReflectionUtils.invokeMethod(method, object, args) 反射执行方法，获取结果。

## 举例说明：

* 为了安全控制，在支持组合接口的方法上面，要加上 ``` @EnableCombineRequest ``` 注解。告诉 Spring 容器，这方法支持放在组合接口中，如果把不支持组合接口放到组合接口中去请求，会报``` IllegalArgumentException ```


### 不带注解事例

* 请求的 request
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

* 响应的 response

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

### 带 @RequestParam 注解例子

* 请求的 request，其中处理 **[Get /liang]** 这个请求的方法中，使用了 ```(@RequestParam(name="userName",defaultValue = "明")String  name,Integer id)```

```json
[{
	"method": "GET",
	"requestId": "1111",
	"param": {
		"id": 11
	},
	"url": "/liang"
}, {
	"method": "GET",
	"requestId": "222",
	"param": {
		"param": "亮",
		"id": 12
	},
	"url": "/liang"
}]
```

* 响应的 response

``` 
[
  {
    "requestId": "1111",
    "url": "/liang",
    "httpStatus": 200,
    "entity": {
      "id": 11,
      "name": "明"
    }
  },
  {
    "requestId": "222",
    "url": "/liang",
    "httpStatus": 200,
    "entity": {
      "id": 12,
      "name": "亮"
    }
  }
]
```


## TODO

 组合方法支持 @PathVariable 等注解 
