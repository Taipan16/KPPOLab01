package com.example.vmserver.repositori;

//import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.vmserver.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    //Optional<User> findById(Long id);
    //List<User> findAll();
    Optional<User> findByLogin(String userLogin);
    Optional<User> findByEmail(String email);

    //Boolean deleteById(User user);

    //Boolean updateById(User user);
}
