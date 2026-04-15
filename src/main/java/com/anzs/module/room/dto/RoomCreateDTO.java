package com.anzs.module.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoomCreateDTO {
    @NotBlank(message = "讨论室名称不能为空")
    private String name;

    private String password;

    @NotNull(message = "最大人数不能为空")
    private Integer maxMembers = 50;

    @NotNull(message = "过期时间不能为空")
    private Integer expireMinutes = 120;
}
