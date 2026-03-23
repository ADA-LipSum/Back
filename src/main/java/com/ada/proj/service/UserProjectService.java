package com.ada.proj.service;

import com.ada.proj.dto.UserProjectRequest;
import com.ada.proj.dto.UserProjectResponse;
import com.ada.proj.entity.UserProject;
import com.ada.proj.exception.ForbiddenException;
import com.ada.proj.exception.UnauthenticatedException;
import com.ada.proj.exception.UserNotFoundException;
import com.ada.proj.repository.UserProjectRepository;
import com.ada.proj.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserProjectService {

    private final UserProjectRepository userProjectRepository;
    private final UserRepository userRepository;

    public UserProjectService(UserProjectRepository userProjectRepository,
                              UserRepository userRepository) {
        this.userProjectRepository = userProjectRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserProjectResponse> listProjects(String uuid) {
        userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return userProjectRepository.findByUserUuidOrderByCreatedAtDesc(uuid)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private static final int PROJECT_LIMIT = 3;

    public UserProjectResponse addProject(String uuid, UserProjectRequest req, Authentication auth) {
        ensureSelfOrAdmin(auth, uuid);
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new IllegalArgumentException("title은 필수입니다.");
        }
        if (userProjectRepository.countByUserUuid(uuid) >= PROJECT_LIMIT) {
            throw new IllegalStateException("프로젝트는 최대 " + PROJECT_LIMIT + "개까지 등록할 수 있습니다.");
        }
        UserProject project = UserProject.builder()
                .userUuid(uuid)
                .title(req.getTitle())
                .description(req.getDescription())
                .githubUrl(req.getGithubUrl())
                .isLookingForTeam(req.isLookingForTeam())
                .build();
        return toResponse(userProjectRepository.save(project));
    }

    public UserProjectResponse updateProject(String uuid, Long projectId, UserProjectRequest req, Authentication auth) {
        ensureSelfOrAdmin(auth, uuid);
        UserProject project = userProjectRepository.findByIdAndUserUuid(projectId, uuid)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        if (req.getTitle() != null) {
            project.setTitle(req.getTitle());
        }
        if (req.getDescription() != null) {
            project.setDescription(req.getDescription());
        }
        if (req.getGithubUrl() != null) {
            project.setGithubUrl(req.getGithubUrl());
        }
        project.setLookingForTeam(req.isLookingForTeam());
        return toResponse(project);
    }

    public void deleteProject(String uuid, Long projectId, Authentication auth) {
        ensureSelfOrAdmin(auth, uuid);
        UserProject project = userProjectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        if (!project.getUserUuid().equals(uuid)) {
            throw new ForbiddenException("Forbidden");
        }
        userProjectRepository.delete(project);
    }

    private UserProjectResponse toResponse(UserProject p) {
        return UserProjectResponse.builder()
                .id(p.getId())
                .userUuid(p.getUserUuid())
                .title(p.getTitle())
                .description(p.getDescription())
                .githubUrl(p.getGithubUrl())
                .lookingForTeam(p.isLookingForTeam())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private void ensureSelfOrAdmin(Authentication auth, String uuid) {
        if (auth == null) {
            throw new UnauthenticatedException("Unauthenticated");
        }
        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));
        if (!isAdmin && !uuid.equals(auth.getName())) {
            throw new ForbiddenException("Forbidden");
        }
    }

}
