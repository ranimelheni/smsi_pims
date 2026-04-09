package com.pims.plateform;

import com.pims.plateform.entity.User;
import com.pims.plateform.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
@EnableJpaRepositories("com.pims.plateform.repository")
@SpringBootApplication
@EnableConfigurationProperties
public class PimsPlateformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PimsPlateformApplication.class, args);
    }

    @Bean
    CommandLineRunner initAdmin(UserRepository userRepository) {
        return args -> {
            if (!userRepository.existsByEmail("admin@organisation.fr")) {
                User admin = User.builder()
                        .email("admin@organisation.fr")
                        .nom("Administrateur")
                        .prenom("Système")
                        .role("super_admin")
                        .isActive(true)
                        .mustChangePassword(false)
                        .build();
                admin.setPassword("Admin@2026");
                userRepository.save(admin);
                System.out.println("✅ Admin créé : admin@organisation.fr / Admin@2026");
            } else {
                System.out.println("ℹ️  Admin déjà existant");
            }
        };
    }
}