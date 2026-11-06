package com.bfts.bfts;

import com.bfts.entity.Role;
import com.bfts.entity.user;
import com.bfts.repository.RoleRepository;
import com.bfts.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.bfts")
@EntityScan("com.bfts.entity")
@EnableJpaRepositories("com.bfts.repository")
public class BftsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BftsApplication.class, args);
    }

    @Bean
    CommandLineRunner initializeUsers(
            RoleRepository roleRepository,
            UserRepository userRepository) {

        return args -> {

            // =========================
            // CREATE ROLES
            // =========================

            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName("ADMIN");
                        return roleRepository.save(role);
                    });

            Role projectManagerRole = roleRepository.findByName("PROJECT_MANAGER")
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName("PROJECT_MANAGER");
                        return roleRepository.save(role);
                    });

            Role financeOfficerRole = roleRepository.findByName("FINANCE_OFFICER")
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName("FINANCE_OFFICER");
                        return roleRepository.save(role);
                    });


            // =========================
            // CREATE ADMIN USER
            // =========================

            if (!userRepository.existsByUsername("admin")) {

                user admin = new user();

                admin.setUsername("admin");
                admin.setEmail("admin@projecttrack.ph");
                admin.setPassword("admin123");
                admin.setFullName("System Administrator");
                admin.setActive(true);
                admin.setRole(adminRole);

                userRepository.save(admin);
            }


            // =========================
            // CREATE PROJECT MANAGER
            // =========================

            if (!userRepository.existsByUsername("manager")) {

                user manager = new user();

                manager.setUsername("manager");
                manager.setEmail("manager@projecttrack.ph");
                manager.setPassword("manager123");
                manager.setFullName("Project Manager");
                manager.setActive(true);
                manager.setRole(projectManagerRole);

                userRepository.save(manager);
            }


            // =========================
            // CREATE FINANCE OFFICER
            // =========================

            if (!userRepository.existsByUsername("finance")) {

                user finance = new user();

                finance.setUsername("finance");
                finance.setEmail("finance@projecttrack.ph");
                finance.setPassword("finance123");
                finance.setFullName("Finance Officer");
                finance.setActive(true);
                finance.setRole(financeOfficerRole);

                userRepository.save(finance);
            }


            System.out.println("========================================");
            System.out.println("BFTS DEFAULT USERS INITIALIZED");
            System.out.println("========================================");
            System.out.println("ADMIN");
            System.out.println("Username: admin");
            System.out.println("Password: admin123");
            System.out.println("----------------------------------------");
            System.out.println("PROJECT MANAGER");
            System.out.println("Username: manager");
            System.out.println("Password: manager123");
            System.out.println("----------------------------------------");
            System.out.println("FINANCE OFFICER");
            System.out.println("Username: finance");
            System.out.println("Password: finance123");
            System.out.println("========================================");
        };
    }
}