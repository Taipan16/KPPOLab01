package com.example.vmserver.service;

import com.example.vmserver.model.User;
import java.util.List;

public interface UserService {
    //Создать пользователя
    User createUser(User user);

    //Удалить пользователя
    void deleteUser(Long id);

    //Обновить пользователя
    User updateUser(Long id, User userDetails);

    //Получить всех пользователей
    List<User> getAllUsers();

    // Получить пользователя по ID
    User getUserById(Long id);
}
