package com.souschef.sous_chef.entity;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.core.annotation.Order;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.souschef.sous_chef.entity.enums.Cuisine;
import com.souschef.sous_chef.entity.enums.DifficultyLevel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set; 

@Entity
@Table(name = "recipes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Recipe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable=false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "Text")
    private String description;

    @Enumerated(EnumType.STRING)
    private Cuisine cuisine;

    private Integer prepTimeMinutes;
    private Integer cookTimeMinutes;
    private Integer servings; 

    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficulty; 

    @Builder.Default
    private Boolean isPublic = False;

    private String imageUrl;

    @ElementCollection
    @CollectionTable(name = "recipe_ingredients", joinColumns = @JoinColumn(name = "recipe_id"))
    @Column(name = "ingredient")
    @Builder.Default
    private List<String> ingredients = new ArrayList<>(); 

    @ElementCollection
    @CollectionTable(name = "recipe_tags", joinColumns = @JoinColumn(name = "recipe_id"))
    @Column(name = "recipe_tags")
    @Builder.Default
    private Set<String> tags = new HashSet<>(); 

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval =  true)
    @Order("stepNumber ASC")
    @Builder.Default
    private List<RecipeStep> steps = new ArrayList<>();
}
