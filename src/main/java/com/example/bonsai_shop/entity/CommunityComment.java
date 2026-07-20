package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "community_comment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CommentID")
    private Integer commentId;

    @Column(name = "PostID", nullable = false)
    private Integer postId;

    @Column(name = "UserID")
    private Integer userId;

    @Column(name = "AuthorName", nullable = false, length = 255)
    private String authorName;

    @Column(name = "AuthorAvatar", length = 500)
    private String authorAvatar;

    @Column(name = "Content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();
}
