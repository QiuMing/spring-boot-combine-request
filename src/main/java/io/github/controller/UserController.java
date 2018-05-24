package io.github.controller;

import io.github.annotion.EnableCombineRequest;
import io.github.dto.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
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
}
