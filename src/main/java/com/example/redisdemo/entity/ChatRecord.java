package com.example.redisdemo.entity;
import	java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

/**
 * @Author: wang
 * @Date: 2020/2/28 14:11
 */
@Data
@JsonNaming
@RedisHash("live:chatRecord")
public class ChatRecord {

    @Id
    private Long id;

    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss", timezone = "GMT+8")
    @Indexed
    private LocalDateTime sendTime;
}
