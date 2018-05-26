package io.github.controller;

import io.github.annotion.EnableCombineRequest;
import io.github.dto.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 功能：测试 user controller
 *
 * @author liaoming
 * @since 2018年05月22日
 */
@Slf4j
@RestController
public class UserController {

    @EnableCombineRequest
    @GetMapping(path = "/ming")
    public UserDto getMing() {
        return new UserDto(1,"明");
    }

    @EnableCombineRequest
    @GetMapping(path = "/fang")
    public UserDto getFang() {
        return new UserDto(2,"芳");
    }


    @EnableCombineRequest
    @GetMapping(path = "/liang")
    public UserDto getLiang(@RequestParam(name="userName",defaultValue = "明")String  name,Integer id) {
        log.info("name:{},age:{}",name,id);
        return new UserDto(id,name);
    }

    @EnableCombineRequest
    @GetMapping(path = "/qiu/{id}")
    public UserDto getQiu(@PathVariable(name="id")Integer id,@RequestParam(name="userName",defaultValue = "丘")String  name) {
        log.info("name:{},age:{}",name,id);
        return new UserDto(id,name);
    }
}
