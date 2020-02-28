package com.example.redisdemo.controller;

import com.example.redisdemo.entity.ChatRecord;
import com.example.redisdemo.repository.ChatRecordRepository;
import com.example.redisdemo.service.ChatRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author: wang
 * @Date: 2020/2/28 14:49
 */
@RestController
@Slf4j
@RequestMapping("/chat")
public class ChatRecordController {

    @Resource
    private ChatRecordRepository chatRecordRepository;
    private static final AtomicLong ID = new AtomicLong(0);

    @GetMapping
    public Page<ChatRecord> findAll(){
//        Pageable pageable = PageRequest.of(0, 1);
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "sendTime"));
        return chatRecordRepository.findAll(pageable);
    }

    @GetMapping("/put")
    public String put(@RequestParam("content") String content){

        ChatRecord record = new ChatRecord();
        record.setContent(content);
        record.setId(ID.incrementAndGet());
        record.setSendTime(LocalDateTime.now());
        chatRecordRepository.save(record);
        return "ok";
    }
    @GetMapping("/list")
    public Iterable<ChatRecord> list(){
        return chatRecordRepository.findAll();
    }
}
