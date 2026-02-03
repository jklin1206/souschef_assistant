package com.souschef.sous_chef.repository;

import com.souschef.sous_chef.entity.Recipe;
import com.souschef.sous_chef.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long>{
    List<Recipe> findByUser(User user);
    List<Recipe> findByIsPublicTrue();
    List<Recipe> findByTitleContainingIgnoreCase(String title);    
}


