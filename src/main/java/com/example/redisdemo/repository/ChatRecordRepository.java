package com.example.redisdemo.repository;

import com.example.redisdemo.entity.ChatRecord;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @Author: wang
 * @Date: 2020/2/28 14:40
 */
public interface ChatRecordRepository extends PagingAndSortingRepository<ChatRecord, Long> {
}
