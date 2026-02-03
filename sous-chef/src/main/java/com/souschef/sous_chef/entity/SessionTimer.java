package com.souschef.sous_chef.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_timers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionTimer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private CookingSession session; 

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private Integer durationSeconds;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime pausedAt;

    @Builder.Default
    private Boolean completed = false;
}
