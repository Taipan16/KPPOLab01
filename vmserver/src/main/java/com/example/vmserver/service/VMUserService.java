package com.example.vmserver.service;

import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.vmserver.dto.VMUserDTO;
import com.example.vmserver.exception.ResourceNotFoundException;
import com.example.vmserver.mapper.VMUserMapper;
import com.example.vmserver.model.Role;
import com.example.vmserver.model.VMUser;
import com.example.vmserver.repository.RoleRepository;
import com.example.vmserver.repository.VMUserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VMUserService {

    private static final Logger logger = LoggerFactory.getLogger(VMUserService.class);
    
    private final VMUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    @Cacheable(value = "VMUser")
    public List<VMUserDTO> getVMUsers(){
        logger.debug("Получение списка всех пользователей");
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            List<VMUserDTO> users = userRepository.findAll()
                    .stream()
                    .map(VMUserMapper::userToUserDTO)
                    .toList();
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.debug("Получено {} пользователей за {} мс", users.size(), duration.toMillis());
            
            return users;
        } catch (Exception e) {
            logger.error("Ошибка при получении списка пользователей: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Cacheable(value = "VMUser", key = "#id")
    public VMUserDTO getVMUserDTO(Long id){
        logger.debug("Получение DTO пользователя по ID: {}", id);
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            VMUser user = userRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.error("Пользователь с ID {} не найден", id);
                        return new ResourceNotFoundException("VMUser with id " + id + " not found ");
                    });
            
            VMUserDTO dto = VMUserMapper.userToUserDTO(user);
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.debug("DTO пользователя с ID {} получено за {} мс: username={}, role={}", 
                    id, duration.toMillis(), dto.username(), dto.role());
            
            return dto;
        } catch (Exception e) {
            logger.error("Ошибка при получении DTO пользователя по ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Cacheable(value = "VMUser", key = "#id")
    public VMUser getVMUser(Long id){
        logger.debug("Получение пользователя по ID: {}", id);
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            VMUser user = userRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.error("Пользователь с ID {} не найден", id);
                        return new ResourceNotFoundException("VMUser with id " + id + " not found");
                    });
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.debug("Пользователь с ID {} найден за {} мс: username={}", 
                    id, duration.toMillis(), user.getUsername());
            
            return user;
        } catch (Exception e) {
            logger.error("Ошибка при получении пользователя по ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }
    
    @Cacheable(value = "VMUser", key = "#name")
    public VMUser getVMUser(String name){
        logger.debug("Получение пользователя по имени: {}", name);
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            VMUser user = userRepository.findByUsername(name)
                    .orElseThrow(() -> {
                        logger.error("Пользователь с именем {} не найден", name);
                        return new ResourceNotFoundException("VMUser with name " + name + " not found");
                    });
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.debug("Пользователь с именем {} найден за {} мс: ID={}, role={}", 
                    name, duration.toMillis(), user.getId(), user.getRole().getName());
            
            return user;
        } catch (Exception e) {
            logger.error("Ошибка при получении пользователя по имени {}: {}", name, e.getMessage(), e);
            throw e;
        }
    }
    
    @CacheEvict(value = "VMUser", allEntries = true)
    @Transactional
    public void resetPassword(String name, String oldPassword, String newPassword) {
        logger.info("Сброс пароля для пользователя: {}", name);
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            VMUser user = userRepository.findByUsername(name)
                    .orElseThrow(() -> {
                        logger.error("Пользователь {} не найден при сбросе пароля", name);
                        return new EntityNotFoundException("Пользователь не найден");
                    });
            
            logger.debug("Проверка старого пароля для пользователя {}", name);
            if(passwordEncoder.matches(oldPassword, user.getPassword())){
                logger.debug("Старый пароль верен, установка нового пароля");
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                logger.info("Пароль для пользователя {} успешно сброшен", name);
            } else {
                logger.warn("Неверный старый пароль для пользователя {}", name);
                throw new RuntimeException("Старый пароль неверен");
            }
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.debug("Сброс пароля для пользователя {} завершен за {} мс", name, duration.toMillis());
            
        } catch (Exception e) {
            logger.error("Ошибка при сбросе пароля для пользователя {}: {}", name, e.getMessage(), e);
            throw e;
        }
    }

    @CacheEvict(value = "VMUser", allEntries = true)
    @Transactional
    public VMUserDTO createUser(String username, String password) {
        logger.info("Создание нового пользователя: {}", username);
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            // Проверка на существование пользователя
            logger.debug("Проверка существования пользователя с именем: {}", username);
            if (userRepository.findByUsername(username).isPresent()) {
                logger.warn("Попытка создания уже существующего пользователя: {}", username);
                throw new RuntimeException("User already exists");
            }
            
            // Создание нового пользователя
            logger.debug("Создание объекта пользователя");
            VMUser user = new VMUser();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            
            logger.debug("Поиск роли USER для нового пользователя");
            Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> {
                    logger.error("Роль USER не найдена в системе");
                    return new RuntimeException("Default role USER not found");
                });
            user.setRole(userRole);
            user.setTokens(new HashSet<>());
            
            logger.debug("Сохранение пользователя в базе данных");
            VMUser savedUser = userRepository.save(user);
            VMUserDTO dto = VMUserMapper.userToUserDTO(savedUser);
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.info("Пользователь {} успешно создан за {} мс, ID={}", 
                    username, duration.toMillis(), savedUser.getId());
            
            return dto;
        } catch (Exception e) {
            logger.error("Ошибка при создании пользователя {}: {}", username, e.getMessage(), e);
            throw e;
        }
    }

    @CacheEvict(value = "VMUser", allEntries = true)
    @Transactional
    public void deleteVMUser(Long id) {
        logger.info("Удаление пользователя с ID: {}", id);
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            VMUser user = userRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.error("Пользователь с ID {} не найден для удаления", id);
                        return new ResourceNotFoundException("VMUser with id " + id + " not found");
                    });
            
            logger.debug("Найден пользователь для удаления: username={}, role={}", 
                    user.getUsername(), user.getRole().getName());
            
            // Проверяем, есть ли связанные токены и обрабатываем их при необходимости
            if (user.getTokens() != null && !user.getTokens().isEmpty()) {
                logger.debug("Отключение {} токенов пользователя перед удалением", user.getTokens().size());
                user.getTokens().forEach(token -> token.setDisabled(true));
            }
            
            userRepository.delete(user);
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.info("Пользователь с ID {} (username={}) успешно удален за {} мс", 
                    id, user.getUsername(), duration.toMillis());
            
        } catch (Exception e) {
            logger.error("Ошибка при удалении пользователя с ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @CacheEvict(value = "VMUser", allEntries = true)
    @Transactional
    public VMUserDTO updateVMUserDTO(Long id, VMUserDTO userDTO) {
        logger.info("Обновление пользователя с ID: {}", id);
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            VMUser existingUser = userRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.error("Пользователь с ID {} не найден для обновления", id);
                        return new ResourceNotFoundException("VMUser with id " + id + " not found");
                    });
            
            logger.debug("Текущие данные пользователя: username={}, role={}", 
                    existingUser.getUsername(), existingUser.getRole().getName());
            
            boolean changesMade = false;
            
            // Обновляем username, если он предоставлен и отличается от текущего
            if (userDTO.username() != null && 
                !userDTO.username().equals(existingUser.getUsername())) {
                
                logger.debug("Запрос на изменение username с '{}' на '{}'", 
                        existingUser.getUsername(), userDTO.username());
                
                if (userRepository.findByUsername(userDTO.username()).isPresent()) {
                    logger.warn("Username '{}' уже существует в системе", userDTO.username());
                    throw new RuntimeException("Username already exists");
                }
                existingUser.setUsername(userDTO.username());
                changesMade = true;
                logger.debug("Username изменен");
            }
            
            // Обновляем пароль, если он предоставлен и не пустой
            if (userDTO.password() != null && !userDTO.password().isEmpty()) {
                logger.debug("Запрос на изменение пароля");
                existingUser.setPassword(passwordEncoder.encode(userDTO.password()));
                changesMade = true;
                logger.debug("Пароль изменен");
            }
            
            // Обновляем роль, если она предоставлена
            if (userDTO.role() != null) {
                logger.debug("Запрос на изменение роли на '{}'", userDTO.role());
                Role role = roleRepository.findByName(userDTO.role())
                        .orElseThrow(() -> {
                            logger.error("Роль '{}' не найдена", userDTO.role());
                            return new ResourceNotFoundException("Role with name " + userDTO.role() + " not found");
                        });
                
                if (!existingUser.getRole().equals(role)) {
                    existingUser.setRole(role);
                    changesMade = true;
                    logger.debug("Роль изменена");
                }
            }
            
            VMUserDTO resultDto;
            if (changesMade) {
                VMUser updatedUser = userRepository.save(existingUser);
                resultDto = VMUserMapper.userToUserDTO(updatedUser);
                logger.info("Пользователь с ID {} успешно обновлен", id);
            } else {
                resultDto = VMUserMapper.userToUserDTO(existingUser);
                logger.debug("Изменений не обнаружено, пользователь не обновлен");
            }
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.debug("Обновление пользователя с ID {} завершено за {} мс", id, duration.toMillis());
            
            return resultDto;
        } catch (Exception e) {
            logger.error("Ошибка при обновлении пользователя с ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Cacheable(value = "VMUsers", key = "#id")
    public List<VMUser> getAllUsers() {
        logger.debug("Получение всех пользователей (сущности)");
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            List<VMUser> users = userRepository.findAll();
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.debug("Получено {} пользователей (сущности) за {} мс", 
                    users.size(), duration.toMillis());
            
            return users;
        } catch (Exception e) {
            logger.error("Ошибка при получении всех пользователей (сущности): {}", e.getMessage(), e);
            throw e;
        }
    }

    @Cacheable(value = "VMUsers", key = "#username")
    public VMUserDTO getVMUserDTOByUsername(String username) {
        logger.debug("Получение DTO пользователя по имени: {}", username);
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            VMUser user = userRepository.findByUsername(username)
                    .orElseThrow(() -> {
                        logger.error("Пользователь с именем {} не найден", username);
                        return new ResourceNotFoundException("VMUser with username " + username + " not found");
                    });
            
            VMUserDTO dto = VMUserMapper.userToUserDTO(user);
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.debug("DTO пользователя с именем {} получено за {} мс", username, duration.toMillis());
            
            return dto;
        } catch (Exception e) {
            logger.error("Ошибка при получении DTO пользователя по имени {}: {}", username, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Получить статистику пользователей
     * @return Статистика пользователей
     */
    public UserStatistics getUserStatistics() {
        logger.debug("Получение статистики пользователей");
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            long totalUsers = userRepository.count();
            
            // Получаем количество пользователей по ролям
            long adminCount = 0;
            long userCount = 0;
            
            Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
            Role userRole = roleRepository.findByName("USER").orElse(null);
            
            if (adminRole != null) {
                adminCount = userRepository.countByRole(adminRole);
            }
            if (userRole != null) {
                userCount = userRepository.countByRole(userRole);
            }
            
            UserStatistics statistics = new UserStatistics(totalUsers, adminCount, userCount);
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.debug("Статистика пользователей получена за {} мс: {}", duration.toMillis(), statistics);
            
            return statistics;
        } catch (Exception e) {
            logger.error("Ошибка при получении статистики пользователей: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Проверить существование пользователя
     * @param username Имя пользователя
     * @return true если пользователь существует
     */
    public boolean userExists(String username) {
        logger.debug("Проверка существования пользователя: {}", username);
        
        try {
            boolean exists = userRepository.findByUsername(username).isPresent();
            logger.debug("Пользователь {} существует: {}", username, exists);
            return exists;
        } catch (Exception e) {
            logger.error("Ошибка при проверке существования пользователя {}: {}", username, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Внутренний класс для статистики пользователей
     */
    public static class UserStatistics {
        private final long totalUsers;
        private final long adminCount;
        private final long userCount;
        
        public UserStatistics(long totalUsers, long adminCount, long userCount) {
            this.totalUsers = totalUsers;
            this.adminCount = adminCount;
            this.userCount = userCount;
        }
        
        public long getTotalUsers() { return totalUsers; }
        public long getAdminCount() { return adminCount; }
        public long getUserCount() { return userCount; }
        
        @Override
        public String toString() {
            return String.format(
                "UserStatistics{total=%d, admin=%d, user=%d}", 
                totalUsers, adminCount, userCount
            );
        }
    }
}