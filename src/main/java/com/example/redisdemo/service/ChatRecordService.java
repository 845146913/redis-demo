package com.example.redisdemo.service;

import com.example.redisdemo.entity.ChatRecord;
import com.example.redisdemo.repository.ChatRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Author: wang
 * @Date: 2020/2/28 14:47
 */
@Service
@Slf4j
public class ChatRecordService {

    @Resource
    private ChatRecordRepository chatRecordRepository;

    public Page<ChatRecord> findPage(Pageable pageable){
        return chatRecordRepository.findAll(pageable);
    }

    public void save(ChatRecord record) {
        chatRecordRepository.save(record);
    }
}
