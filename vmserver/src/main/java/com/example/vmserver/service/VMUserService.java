package com.example.vmserver.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.vmserver.dto.VMUserDTO;
import com.example.vmserver.exception.ResourceNotFoundException;
import com.example.vmserver.mapper.VMUserMapper;
import com.example.vmserver.model.VMUser;
import com.example.vmserver.repository.VMUserRepository;

@Service
public class VMUserService {

    private final VMUserRepository userRepository;

    public VMUserService(VMUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<VMUserDTO> getVMUsers(){
        return userRepository.findAll().stream().map(VMUserMapper::userToUserDTO).toList();
    }

    public VMUserDTO getVMUser(Long id){
        VMUser user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("VMUser with id " + id + " not found"));
        return VMUserMapper.userToUserDTO(user);
    }


    
}
