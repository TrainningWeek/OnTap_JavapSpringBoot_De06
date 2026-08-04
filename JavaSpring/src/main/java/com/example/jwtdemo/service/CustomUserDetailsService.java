package com.example.jwtdemo.service;

import com.example.jwtdemo.entity.User;
import com.example.jwtdemo.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Load UserDetails từ database.
 * Được inject vào JwtAuthenticationFilter để kiểm tra trạng thái account.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Không tìm thấy user với username: " + username
                ));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority(user.getRole())))
                .disabled(!user.isEnabled())
                .accountLocked(!user.isAccountNonLocked())
                .build();
    }
}
