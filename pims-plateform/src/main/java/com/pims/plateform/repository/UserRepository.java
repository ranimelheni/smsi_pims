package com.pims.plateform.repository;

import com.pims.plateform.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    

    @EntityGraph(attributePaths = {"organism"})
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"organism"})
    Optional<User> findById(Long id);

    @EntityGraph(attributePaths = {"organism"})
    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"organism"})
    List<User> findByOrganismId(Long organismId);

    @EntityGraph(attributePaths = {"organism"})
    List<User> findByOrganismIdAndRole(Long organismId, String role);

    @EntityGraph(attributePaths = {"organism"})
    List<User> findByRoleNot(String role);
    
    
}