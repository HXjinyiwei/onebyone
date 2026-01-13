package com.example.biyeshiji.controller;

import com.example.biyeshiji.entity.Novel;
import com.example.biyeshiji.entity.Chapter;
import com.example.biyeshiji.service.NovelService;
import com.example.biyeshiji.service.ChapterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ViewController {

    @Autowired
    private NovelService novelService;

    @Autowired
    private ChapterService chapterService;

    @GetMapping("/")
    public String index() {
        return "login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/home")
    public String home() {
        return "index";
    }

    @GetMapping("/index")
    public String indexPage() {
        return "index";
    }
    
    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }
    
    @GetMapping("/profile/novels")
    public String myNovels() {
        return "profile";
    }
    
    @GetMapping("/profile/posts")
    public String myPosts() {
        return "profile";
    }
    
    @GetMapping("/profile/messages")
    public String myMessages() {
        return "profile";
    }
    
    // 管理员审核页面路由
    @GetMapping("/profile/approval/posts")
    public String postApproval() {
        return "profile";
    }
    
    @GetMapping("/profile/approval/novels")
    public String novelApproval() {
        return "profile";
    }
    
    @GetMapping("/profile/approval/chapters")
    public String chapterApproval() {
        return "profile";
    }
    
    // 帖子相关页面路由
    @GetMapping("/post/list")
    public String postList() {
        return "post-list";
    }
    
    @GetMapping("/post/{id}")
    public String postDetail(@PathVariable Long id) {
        return "post-detail";
    }
    
    @GetMapping("/post/create")
    public String postCreate() {
        return "post-create";
    }
    
    @GetMapping("/post/edit/{id}")
    public String postEdit(@PathVariable Long id) {
        return "post-create";
    }

    // 小说相关页面路由
    @GetMapping("/novel/list")
    public String novelList() {
        return "novel-list";
    }

    @GetMapping("/novel/view/{id}")
    public String novelView(@PathVariable Long id) {
        return "novel-detail";
    }

    @GetMapping("/novel/create")
    public String novelCreate() {
        return "novel-create";
    }

    @GetMapping("/novel/edit/{id}")
    public String novelEdit(@PathVariable Long id, Model model) {
        Novel novel = novelService.getNovelById(id);
        if (novel != null && novel.getIsDeleted() == 0) {
            model.addAttribute("novel", novel);
            // 生成逗号分隔的分类ID字符串，用于前端data-selected-categories属性
            if (novel.getCategoryIds() != null && !novel.getCategoryIds().isEmpty()) {
                String categoryIdsStr = novel.getCategoryIds().stream()
                        .map(Object::toString)
                        .collect(java.util.stream.Collectors.joining(","));
                model.addAttribute("categoryIdsStr", categoryIdsStr);
            } else {
                model.addAttribute("categoryIdsStr", "");
            }
        }
        return "novel-create";
    }

    @GetMapping("/chapter/create")
    public String chapterCreate(@RequestParam(required = false) Long id,
                                @RequestParam(required = false) Long novelId,
                                Model model) {
        if (id != null) {
            Chapter chapter = chapterService.getChapterById(id);
            if (chapter != null) {
                model.addAttribute("chapter", chapter);
                if (novelId == null) {
                    novelId = chapter.getNovelId();
                }
            }
        }
        if (novelId != null) {
            model.addAttribute("novelId", novelId);
        }
        return "chapter-create";
    }

    @GetMapping("/chapter/edit/{id}")
    public String chapterEdit(@PathVariable Long id, Model model) {
        Chapter chapter = chapterService.getChapterById(id);
        if (chapter != null) {
            model.addAttribute("chapter", chapter);
            model.addAttribute("novelId", chapter.getNovelId());
        }
        return "chapter-create";
    }

    @GetMapping("/chapter/view/{id}")
    public String chapterView(@PathVariable Long id) {
        return "chapter-detail";
    }

    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }

    @GetMapping("/unban-appeal")
    public String unbanAppeal() {
        return "unban-appeal";
    }
}