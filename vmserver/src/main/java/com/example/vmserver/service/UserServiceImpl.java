package com.example.vmserver.service;

import com.example.vmserver.model.User;
import com.example.vmserver.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    //Сохранение пользователя в БД
    @Override
    public User createUser(User user) {
        return userRepository.save(user);
    }

    //Удаление пользователя
    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден по идентификатору: " + id));
                
        userRepository.delete(user);
    }

    //Обновление полей пользователя
    @Override
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден по идентификатору: " + id));

        user.setLogin(userDetails.getLogin());
        user.setEmail(userDetails.getEmail());
        user.setHashpassword(userDetails.getHashpassword());
        
        return userRepository.save(user);
    }

    //Получение всех пользователей
    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll(); 
    }

    //Получение пользователя по ID
    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден по идентификатору: " + id));
    }
}
