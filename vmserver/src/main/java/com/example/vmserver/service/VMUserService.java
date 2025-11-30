package com.example.vmserver.service;

import java.util.HashSet;
import java.util.List;

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

@Service
@RequiredArgsConstructor
public class VMUserService {

    private final VMUserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final RoleRepository roleRepository;

    @Cacheable(value = "VMUser")
    public List<VMUserDTO> getVMUsers(){
        return userRepository.findAll().stream().map(VMUserMapper::userToUserDTO).toList();
    }

    @Cacheable(value = "VMUser", key = "#id")
    public VMUserDTO getVMUserDTO(Long id){
        VMUser user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("VMUser with id " + id + " not found "));
        return VMUserMapper.userToUserDTO(user);
    }

    @Cacheable(value = "VMUser", key = "#id")
    public VMUser getVMUser(Long id){
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("VMUser with id " + id + " not found"));
    }
    
    @Cacheable(value = "VMUser", key = "#name")
    public VMUser getVMUser(String name){
        return userRepository.findByUsername(name).orElseThrow(() -> new ResourceNotFoundException("VMUser with name " + name + " not found"));
    }
    
    @CacheEvict(value = "VMUser", allEntries = true)
    @Transactional
    public void resetPassword(String name, String oldPassword, String newPassword) {
        VMUser user = userRepository.findByUsername(name)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));
        
        if(passwordEncoder.matches(oldPassword, user.getPassword())){
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
        }
        else{
            throw new RuntimeException("Старый пароль неверен");
        }
    }

    @CacheEvict(value = "VMUser", allEntries = true)
    @Transactional
    public VMUser createUser(String username, String password) {
        // Проверка на существование пользователя
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("User already exists");
        }
        
        // Создание нового пользователя
        VMUser user = new VMUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        
        Role userRole = roleRepository.findByName("USER")
            .orElseThrow(() -> new RuntimeException("Default role USER not found"));
        user.setRole(userRole);
        user.setTokens(new HashSet<>());
        
        return userRepository.save(user);
    }

    @CacheEvict(value = "VMUser", allEntries = true)
    @Transactional
    public void deleteVMUser(Long id) {
        VMUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VMUser with id " + id + " not found"));
        
        // Проверяем, есть ли связанные токены и обрабатываем их при необходимости
        if (user.getTokens() != null && !user.getTokens().isEmpty()) {
            // Отключаем все токены пользователя перед удалением
            user.getTokens().forEach(token -> token.setDisabled(true));
        }
        
        userRepository.delete(user);
    }

    @CacheEvict(value = "VMUser", allEntries = true)
    @Transactional
    public VMUserDTO updateVMUserDTO(Long id, VMUserDTO userDTO) {
        VMUser existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VMUser with id " + id + " not found"));
        
        // Обновляем username, если он предоставлен и отличается от текущего
        if (userDTO.username() != null && 
            !userDTO.username().equals(existingUser.getUsername())) {
            
            if (userRepository.findByUsername(userDTO.username()).isPresent()) {
                throw new RuntimeException("Username already exists");
            }
            existingUser.setUsername(userDTO.username());
        }
        
        // Обновляем пароль, если он предоставлен и не пустой
        if (userDTO.password() != null && !userDTO.password().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(userDTO.password()));
        }
        
        // Обновляем роль, если она предоставлена
        if (userDTO.role() != null) {
            Role role = roleRepository.findByName(userDTO.role())
                    .orElseThrow(() -> new ResourceNotFoundException("Role with name " + userDTO.role() + " not found"));
            existingUser.setRole(role);
        }
        
        VMUser updatedUser = userRepository.save(existingUser);
        return VMUserMapper.userToUserDTO(updatedUser);
    }

    @Cacheable(value = "VMUsers", key = "#id")
    public List<VMUser> getAllUsers() {
        return userRepository.findAll();
    }

    @Cacheable(value = "VMUser", key = "#username")
    public VMUserDTO getVMUserDTOByUsername(String username) {
        VMUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("VMUser with username " + username + " not found"));
        return VMUserMapper.userToUserDTO(user);
    }   
}


