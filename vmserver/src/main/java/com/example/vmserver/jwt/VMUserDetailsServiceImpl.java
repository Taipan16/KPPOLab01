package com.example.vmserver.jwt;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.vmserver.exception.ResourceNotFoundException;
import com.example.vmserver.repository.VMUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VMUserDetailsServiceImpl implements UserDetailsService{
    private final VMUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException(""));
    }

}
