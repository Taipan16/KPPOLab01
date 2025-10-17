package com.example.vmserver.service;

import com.example.vmserver.model.User;
import com.example.vmserver.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    //Сохранение пользователя в БД
    @Transactional
    @CacheEvict(value = "User", allEntries = true)
    @Override
    public User createUser(User user) {
        return userRepository.save(user);
    }

    //Удаление пользователя
    @Transactional
    @CacheEvict(value = "User", allEntries = true)
    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден по идентификатору: " + id));
                
        userRepository.delete(user);
    }

    //Обновление полей пользователя
    @Transactional
    @CacheEvict(value = "User", allEntries = true)
    @Override
    public User updateUser(Long id, User userDetails) {
    User user = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден по идентификатору: " + id));

    user.setLogin(userDetails.getLogin());
    user.setEmail(userDetails.getEmail());
    
    // Обновляем пароль ТОЛЬКО если пришло новое значение
    if (userDetails.getHashpassword() != null) {
        user.setHashpassword(userDetails.getHashpassword());
    }
    
    return userRepository.save(user);
    }

    //Получение всех пользователей
    @Transactional
    @Cacheable(value = "Users")
    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll(); 
    }

    //Получение пользователя по ID
    @Transactional
    @Cacheable(value = "Users", key = "#id")
    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден по идентификатору: " + id));
    }

    @Override
    public void resetPassword(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        String standardPassword = "Q12werty";
        user.setHashpassword(standardPassword);
        userRepository.save(user);
    }
}
