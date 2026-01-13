package com.example.biyeshiji.repository;

import com.example.biyeshiji.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByUserIdOrderByCreateTimeDesc(Long userId);
    List<Message> findByUserIdAndIsReadOrderByCreateTimeDesc(Long userId, Integer isRead);
    int countByUserIdAndIsRead(Long userId, Integer isRead);
    void deleteByUserId(Long userId);
}