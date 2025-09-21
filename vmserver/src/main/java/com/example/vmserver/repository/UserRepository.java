package com.example.vmserver.repository;

import com.example.vmserver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    //Получить пользователя по логину
    User findByLogin(String login);

    //Получить пользователя по почте
    User findByEmail(String email);
}
