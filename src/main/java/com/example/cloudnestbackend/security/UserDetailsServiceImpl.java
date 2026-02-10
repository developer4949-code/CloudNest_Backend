package com.example.cloudnestbackend.security;

import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final com.example.cloudnestbackend.repository.UserRepository userRepository;

    public UserDetailsServiceImpl(com.example.cloudnestbackend.repository.UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        com.example.cloudnestbackend.entity.User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return User.withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getRole().name()) // mapping Role enum to authority
                .build();
    }
}
