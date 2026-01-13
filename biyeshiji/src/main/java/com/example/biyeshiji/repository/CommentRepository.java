package com.example.biyeshiji.repository;

import com.example.biyeshiji.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("SELECT DISTINCT c FROM Comment c LEFT JOIN FETCH c.author WHERE c.postId = :postId AND c.isDeleted = :isDeleted ORDER BY c.floorNumber ASC")
    List<Comment> findByPostIdAndIsDeletedOrderByFloorNumberAsc(@Param("postId") Long postId, @Param("isDeleted") Integer isDeleted);
    @Query("SELECT DISTINCT c FROM Comment c LEFT JOIN FETCH c.author WHERE c.authorId = :authorId AND c.isDeleted = :isDeleted ORDER BY c.createTime DESC")
    List<Comment> findByAuthorIdAndIsDeletedOrderByCreateTimeDesc(@Param("authorId") Long authorId, @Param("isDeleted") Integer isDeleted);
    Integer countByPostIdAndIsDeleted(Long postId, Integer isDeleted);
    @Query("SELECT MAX(c.floorNumber) FROM Comment c WHERE c.postId = :postId AND c.isDeleted = 0")
    Integer findMaxFloorNumberByPostId(@Param("postId") Long postId);
}
