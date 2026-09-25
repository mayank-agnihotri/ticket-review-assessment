package com.atl.ticketmgmt.common.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.atl.ticketmgmt")
@EntityScan(basePackages = "com.atl.ticketmgmt")
public class JpaConfig {
}
