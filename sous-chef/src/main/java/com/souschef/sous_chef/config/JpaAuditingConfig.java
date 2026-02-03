package com.souschef.sous_chef.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration to enable JPA Auditing
 * This allows automatic population of @CreatedDate and @LastModifiedDate annotations
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    // No additional configuration needed
    // The @EnableJpaAuditing annotation does all the work
}
