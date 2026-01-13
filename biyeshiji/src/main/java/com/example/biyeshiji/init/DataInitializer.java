package com.example.biyeshiji.init;

import com.example.biyeshiji.entity.Category;
import com.example.biyeshiji.entity.Comment;
import com.example.biyeshiji.entity.Post;
import com.example.biyeshiji.entity.User;
import com.example.biyeshiji.repository.CategoryRepository;
import com.example.biyeshiji.repository.CommentRepository;
import com.example.biyeshiji.repository.PostRepository;
import com.example.biyeshiji.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer {

    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        // 检查分类表是否为空，如果为空则初始化默认分类
        if (categoryRepository.count() == 0) {
            List<Category> categories = new ArrayList<>();
            
            // 添加默认分类
            Category category1 = new Category();
            category1.setName("公告");
            category1.setDescription("论坛公告");
            category1.setParentId(0L);
            category1.setSortOrder(1);
            category1.setIsActive(1);
            categories.add(category1);
            
            Category category2 = new Category();
            category2.setName("小说讨论");
            category2.setDescription("小说创作与讨论");
            category2.setParentId(0L);
            category2.setSortOrder(2);
            category2.setIsActive(1);
            categories.add(category2);
            
            Category category3 = new Category();
            category3.setName("求助");
            category3.setDescription("寻求帮助");
            category3.setParentId(0L);
            category3.setSortOrder(3);
            category3.setIsActive(1);
            categories.add(category3);
            
            Category category4 = new Category();
            category4.setName("建议");
            category4.setDescription("论坛建议");
            category4.setParentId(0L);
            category4.setSortOrder(4);
            category4.setIsActive(1);
            categories.add(category4);
            
            Category category5 = new Category();
            category5.setName("灌水");
            category5.setDescription("闲聊灌水");
            category5.setParentId(0L);
            category5.setSortOrder(5);
            category5.setIsActive(1);
            categories.add(category5);
            
            Category category6 = new Category();
            category6.setName("分享");
            category6.setDescription("资源分享");
            category6.setParentId(0L);
            category6.setSortOrder(6);
            category6.setIsActive(1);
            categories.add(category6);
            
            // 保存分类数据
            categoryRepository.saveAll(categories);
        }
        
        // 添加小说分类（如果不存在）
        List<String> novelCategoryNames = List.of("玄幻", "科幻", "武侠", "都市", "历史", "奇幻", "悬疑", "轻小说");
        for (String name : novelCategoryNames) {
            if (!categoryRepository.findByName(name).isPresent()) {
                Category novelCategory = new Category();
                novelCategory.setName(name);
                novelCategory.setDescription(name + "类小说");
                novelCategory.setParentId(0L);
                novelCategory.setSortOrder((int)(categoryRepository.count() + 1));
                novelCategory.setIsActive(1);
                categoryRepository.save(novelCategory);
            }
        }

        // 检查用户表是否为空，如果为空则初始化测试用户
        if (userRepository.count() == 0) {
            // 创建高级管理员用户
            User superAdmin = new User();
            superAdmin.setUsername("superadmin");
            superAdmin.setUserPassword(passwordEncoder.encode("superadmin123"));
            superAdmin.setNickname("超级管理员");
            superAdmin.setRole(2); // 2为高级管理员
            superAdmin.setStatus(1);
            userRepository.save(superAdmin);
            
            // 创建管理员用户
            User admin = new User();
            admin.setUsername("admin");
            admin.setUserPassword(passwordEncoder.encode("admin123"));
            admin.setNickname("管理员");
            admin.setRole(1); // 1为管理员
            admin.setStatus(1);
            userRepository.save(admin);
            
            // 创建普通用户
            User user1 = new User();
            user1.setUsername("user1");
            user1.setUserPassword(passwordEncoder.encode("user123"));
            user1.setNickname("用户1");
            user1.setRole(0); // 0为普通用户
            user1.setStatus(1);
            userRepository.save(user1);
            
            User user2 = new User();
            user2.setUsername("user2");
            user2.setUserPassword(passwordEncoder.encode("user123"));
            user2.setNickname("用户2");
            user2.setRole(0);
            user2.setStatus(1);
            userRepository.save(user2);
        } else {
            // 如果用户表不为空，检查是否存在高级管理员，若不存在则创建
            if (userRepository.findByUsername("superadmin") == null) {
                User superAdmin = new User();
                superAdmin.setUsername("superadmin");
                superAdmin.setUserPassword(passwordEncoder.encode("superadmin123"));
                superAdmin.setNickname("超级管理员");
                superAdmin.setRole(2);
                superAdmin.setStatus(1);
                userRepository.save(superAdmin);
            }
        }
        
        // 检查帖子表是否为空，如果为空则初始化测试帖子
        if (postRepository.count() == 0) {
            // 获取分类和用户
            Category category1 = categoryRepository.findByName("小说讨论").orElse(null);
            Category category2 = categoryRepository.findByName("公告").orElse(null);
            User admin = userRepository.findByUsername("admin");
            User user1 = userRepository.findByUsername("user1");
            
            if (category1 != null && category2 != null && admin != null && user1 != null) {
                // 创建测试帖子1
                Post post1 = new Post();
                post1.setTitle("测试帖子1");
                post1.setContent("这是一个测试帖子，用于测试评论功能。");
                post1.setAuthorId(admin.getId());
                post1.setCategoryId(category1.getId());
                post1.setStatus(0); // 已审核
                post1.setViewCount(0);
                post1.setLikeCount(0);
                post1.setCommentCount(0);
                post1.setFavoriteCount(0);
                post1.setIsTop(0);
                post1.setIsLocked(0);
                post1.setIsDeleted(0);
                post1.setCreateTime(LocalDateTime.now().minusDays(1));
                post1.setUpdateTime(LocalDateTime.now().minusDays(1));
                postRepository.save(post1);
                
                // 创建测试帖子2
                Post post2 = new Post();
                post2.setTitle("测试帖子2");
                post2.setContent("这是第二个测试帖子，用于测试评论列表显示。");
                post2.setAuthorId(user1.getId());
                post2.setCategoryId(category2.getId());
                post2.setStatus(0); // 已审核
                post2.setViewCount(0);
                post2.setLikeCount(0);
                post2.setCommentCount(0);
                post2.setFavoriteCount(0);
                post2.setIsTop(0);
                post2.setIsLocked(0);
                post2.setIsDeleted(0);
                post2.setCreateTime(LocalDateTime.now().minusHours(12));
                post2.setUpdateTime(LocalDateTime.now().minusHours(12));
                postRepository.save(post2);
                
                // 添加测试评论
                Comment comment1 = new Comment();
                comment1.setContent("这是第一条测试评论。");
                comment1.setAuthorId(admin.getId());
                comment1.setPostId(post1.getId());
                comment1.setParentId(0L);
                comment1.setStatus(0);
                comment1.setFloorNumber(1);
                comment1.setCreateTime(LocalDateTime.now().minusHours(23));
                comment1.setUpdateTime(LocalDateTime.now().minusHours(23));
                comment1.setIsDeleted(0);
                commentRepository.save(comment1);
                
                Comment comment2 = new Comment();
                comment2.setContent("这是第二条测试评论。");
                comment2.setAuthorId(user1.getId());
                comment2.setPostId(post1.getId());
                comment2.setParentId(0L);
                comment2.setStatus(0);
                comment2.setFloorNumber(2);
                comment2.setCreateTime(LocalDateTime.now().minusHours(22));
                comment2.setUpdateTime(LocalDateTime.now().minusHours(22));
                comment2.setIsDeleted(0);
                commentRepository.save(comment2);
                
                Comment comment3 = new Comment();
                comment3.setContent("这是第三条测试评论，回复第一条评论。");
                comment3.setAuthorId(admin.getId());
                comment3.setPostId(post1.getId());
                comment3.setParentId(comment1.getId());
                comment3.setStatus(0);
                comment3.setFloorNumber(3);
                comment3.setCreateTime(LocalDateTime.now().minusHours(21));
                comment3.setUpdateTime(LocalDateTime.now().minusHours(21));
                comment3.setIsDeleted(0);
                commentRepository.save(comment3);
                
                // 更新帖子评论数
                post1.setCommentCount(3);
                postRepository.save(post1);
            }
        }
    }
}