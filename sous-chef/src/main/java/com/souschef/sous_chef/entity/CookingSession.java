package com.souschef.sous_chef.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cooking_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CookingSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(nullable = false)
    private String state; 

    @Builder.Default
    private Integer currentStep = 1;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime lastActiveAt;
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SessionTimer> timers = new ArrayList<>(); 
}
