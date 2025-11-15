package com.ada.proj.security;

import com.ada.proj.entity.User;
import com.ada.proj.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Collection;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String uuid) throws UsernameNotFoundException {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + uuid));

        // 권한을 TEACHER, ADMIN, STUDENT 그대로 넣는다 (ROLE_ prefix 제거)
        Collection<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority(user.getRole().name()));

        // 비밀번호는 JWT 인증이므로 실제 사용되지 않음
        return new org.springframework.security.core.userdetails.User(
                user.getUuid(),
                "N/A",
                authorities
        );
    }
}