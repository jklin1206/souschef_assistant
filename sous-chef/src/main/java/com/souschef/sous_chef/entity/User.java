package com.souschef.sous_chef.entity;

// database mapping 
import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder; 
import lombok.Data;
import lombok.NoArgsConstructor;
 
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate; 
import org.springframework.data.jpa.domain.support.AuditingEntityListener; 

import java.time.LocalDateTime; 
import java.util.ArrayList; 
import java.util.List; 
import java.util.Set;
import java.util.HashSet;

@Entity
@Table(name = "users") 
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class) 
public class User {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 

    @Column(nullable = false, unique = true)
    private String username; 

    @Column(nullable = false, unique = true)
    private String email; 

    @Column(nullable = false)
    private String passwordHash; 

    private String firstName;
    private String lastName; 

    @CreatedDate
    @Column(nullable = false, updatable = false) 
    private LocalDateTime createdAt; 

    @LastModifiedDate
    private LocalDateTime updatedAt; 

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Recipe> recipes = new ArrayList<>(); 
    
    @ElementCollection
    @CollectionTable(name = "user_dietary_restrictions", joinColumns= @JoinColumn(name="user_id"))
    @Column(name="restriction")
    @Builder.Default
    private Set<String> dietaryRestrictions = new HashSet<>(); 
}


