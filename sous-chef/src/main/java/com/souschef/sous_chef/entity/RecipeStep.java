package com.souschef.sous_chef.entity;

import jakarta.persistence.*;

import lombok.*;

@Entity
@Table(name = "recipe_steps")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(nullable=false)
    private Integer stepNumber;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String instruction; 

    private Integer timerMinutes; 
    private String timerLabel; 
}