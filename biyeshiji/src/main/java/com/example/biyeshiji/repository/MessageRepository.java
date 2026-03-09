package com.example.biyeshiji.repository;

import com.example.biyeshiji.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByUserIdOrderByCreateTimeDesc(Long userId);
    Page<Message> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);
    List<Message> findByUserIdAndIsReadOrderByCreateTimeDesc(Long userId, Integer isRead);
    Page<Message> findByUserIdAndIsReadOrderByCreateTimeDesc(Long userId, Integer isRead, Pageable pageable);
    int countByUserIdAndIsRead(Long userId, Integer isRead);
    void deleteByUserId(Long userId);
}