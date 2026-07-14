package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "community_post")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PostID")
    private Integer postId;

    @Column(name = "Title", nullable = false, length = 255)
    private String title;

    @Column(name = "Content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "Summary", length = 1000)
    private String summary;

    @Column(name = "AuthorName", length = 255)
    private String authorName;

    @Column(name = "AuthorAvatar", length = 500)
    private String authorAvatar;

    @Column(name = "Category", length = 100)
    private String category;

    @Column(name = "ImageUrl", length = 1000)
    private String imageUrl;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "ReadTime")
    private Integer readTime = 5;

    @Column(name = "LikesCount")
    private Integer likesCount = 0;

    @Column(name = "CommentsCount")
    private Integer commentsCount = 0;
}
