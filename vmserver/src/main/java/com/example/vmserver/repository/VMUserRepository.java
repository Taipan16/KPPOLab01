package com.example.vmserver.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.vmserver.model.Role;
import com.example.vmserver.model.VMUser;

@Repository
public interface VMUserRepository extends JpaRepository<VMUser, Long> {
    Optional<VMUser> findByUsername(String username);

    long countByRole(Role role);
}
