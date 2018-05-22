package io.github.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 功能：UserDto
 *
 * @author liaoming
 * @since 2017年06月15日
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserDto {
    private Integer id;
    private String name;
}
