package com.ada.proj.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_data")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uuid", referencedColumnName = "uuid", nullable = false)
    private User user;

    @Column(length = 255)
    private String intro;

    @Column(length = 255)
    private String techStack;

    @Column(length = 255)
    private String githubUrl;

    @Column(length = 255)
    private String otherUrl;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
