package com.example.biyeshiji.repository;

import com.example.biyeshiji.entity.LikeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LikeRecordRepository extends JpaRepository<LikeRecord, Long> {
    LikeRecord findByUserIdAndTargetTypeAndTargetId(Long userId, Integer targetType, Long targetId);
    void deleteByUserIdAndTargetTypeAndTargetId(Long userId, Integer targetType, Long targetId);
    List<LikeRecord> findByUserIdAndTargetType(Long userId, Integer targetType);
}
