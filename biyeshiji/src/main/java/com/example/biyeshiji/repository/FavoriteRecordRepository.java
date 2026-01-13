package com.example.biyeshiji.repository;

import com.example.biyeshiji.entity.FavoriteRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteRecordRepository extends JpaRepository<FavoriteRecord, Long> {
    FavoriteRecord findByUserIdAndTargetTypeAndTargetId(Long userId, Integer targetType, Long targetId);
    List<FavoriteRecord> findByUserIdAndTargetType(Long userId, Integer targetType);
    List<FavoriteRecord> findByTargetTypeAndTargetId(Integer targetType, Long targetId);
    boolean existsByUserIdAndTargetTypeAndTargetId(Long userId, Integer targetType, Long targetId);
    void deleteByUserIdAndTargetTypeAndTargetId(Long userId, Integer targetType, Long targetId);
}