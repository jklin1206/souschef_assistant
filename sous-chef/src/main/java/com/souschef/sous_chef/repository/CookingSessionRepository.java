package com.souschef.sous_chef.repository;

import com.souschef.sous_chef.entity.CookingSession;
import com.souschef.sous_chef.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface CookingSessionRepository extends JpaRepository<CookingSession, Long> {
    Optional<CookingSession> findByUserAndCompletedAtIsNull(User user);
    List<CookingSession> findByUserOrderByStartedAtDesc(User user); 
}
