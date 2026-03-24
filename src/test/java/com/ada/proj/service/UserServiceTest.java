package com.ada.proj.service;

import com.ada.proj.dto.CreateUserRequest;
import com.ada.proj.entity.User;
import com.ada.proj.enums.Role;
import com.ada.proj.repository.CommentRepository;
import com.ada.proj.repository.PostRepository;
import com.ada.proj.repository.UserDataRepository;
import com.ada.proj.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Test
    void createUserByAdmin_savesNickname_whenProvided() {
        UserRepository userRepository = mock(UserRepository.class);
        UserDataRepository userDataRepository = mock(UserDataRepository.class);
        PostRepository postRepository = mock(PostRepository.class);
        CommentRepository commentRepository = mock(CommentRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        ObjectMapper objectMapper = new ObjectMapper();

        when(userRepository.findByAdminId("st12345")).thenReturn(Optional.empty());
        when(userRepository.existsByCustomId(any())).thenReturn(false);
        when(passwordEncoder.encode("1234567890")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserService userService = new UserService(userRepository, userDataRepository, postRepository, commentRepository, passwordEncoder, objectMapper);

        CreateUserRequest req = new CreateUserRequest();
        req.setAdminId("st12345");
        req.setUserRealname("김학생");
        req.setUserNickname("김코딩");
        req.setRole(Role.STUDENT);
        req.setPassword("1234567890");

        var auth = new TestingAuthenticationToken(
                "admin",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        User created = userService.createUserByAdmin(req, auth);

        ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedCaptor.capture());

        assertThat(savedCaptor.getValue().getUserNickname()).isEqualTo("김코딩");
        assertThat(created.getUserNickname()).isEqualTo("김코딩");
        assertThat(created.getUserRealname()).isEqualTo("김학생");
        assertThat(created.getAdminId()).isEqualTo("st12345");
    }

    @Test
    void createUserByAdmin_throwsBadRequest_whenNicknameMissing() {
        UserRepository userRepository = mock(UserRepository.class);
        UserDataRepository userDataRepository = mock(UserDataRepository.class);
        PostRepository postRepository = mock(PostRepository.class);
        CommentRepository commentRepository = mock(CommentRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        ObjectMapper objectMapper = new ObjectMapper();

        when(userRepository.findByAdminId("st12345")).thenReturn(Optional.empty());

        UserService userService = new UserService(userRepository, userDataRepository, postRepository, commentRepository, passwordEncoder, objectMapper);

        CreateUserRequest req = new CreateUserRequest();
        req.setAdminId("st12345");
        req.setUserRealname("김학생");
        req.setRole(Role.STUDENT);
        req.setPassword("1234567890");
        // userNickname intentionally missing

        var auth = new TestingAuthenticationToken(
                "admin",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        assertThatThrownBy(() -> userService.createUserByAdmin(req, auth))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userNickname");

        verify(userRepository, never()).save(any(User.class));
    }
}
