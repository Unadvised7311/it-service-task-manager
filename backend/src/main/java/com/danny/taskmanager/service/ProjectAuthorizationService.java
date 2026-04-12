package com.danny.taskmanager.service;

import com.danny.taskmanager.model.Project;
import com.danny.taskmanager.model.User;
import com.danny.taskmanager.repository.ProjectMemberRepository;
import com.danny.taskmanager.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ProjectAuthorizationService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public ProjectAuthorizationService(ProjectRepository projectRepository, ProjectMemberRepository projectMemberRepository) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    public Project requireProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Projekt nicht gefunden"));
    }

    public boolean isAdmin(User user) {
        return "ADMIN".equals(user.getRole());
    }

    public boolean isProjectLead(User user) {
        return "PROJECT_LEAD".equals(user.getRole());
    }

    public ProjectAccess accessOnProject(User user, Long projectId) {
        if (isAdmin(user)) {
            return ProjectAccess.WRITE;
        }
        Project p = projectRepository.findById(projectId).orElse(null);
        if (p == null) {
            return ProjectAccess.NONE;
        }
        if (p.getOwner().getId().equals(user.getId())) {
            return ProjectAccess.WRITE;
        }
        return projectMemberRepository.findByProject_IdAndUser_Id(projectId, user.getId())
                .map(pm -> normalizeAccess(pm.getAccessLevel()))
                .orElse(ProjectAccess.NONE);
    }

    private static ProjectAccess normalizeAccess(String level) {
        if ("READ".equalsIgnoreCase(level)) {
            return ProjectAccess.READ;
        }
        return ProjectAccess.WRITE;
    }

    public boolean canAccessProject(User user, Long projectId) {
        return accessOnProject(user, projectId) != ProjectAccess.NONE;
    }

    public boolean isOwner(User user, Project project) {
        return project.getOwner().getId().equals(user.getId());
    }

    public void requireProjectAccess(User user, Long projectId) {
        if (!canAccessProject(user, projectId)) {
            throw new ResponseStatusException(FORBIDDEN, "Kein Zugriff auf dieses Projekt");
        }
    }

    public void requireRead(User user, Long projectId) {
        if (accessOnProject(user, projectId) == ProjectAccess.NONE) {
            throw new ResponseStatusException(FORBIDDEN, "Kein Zugriff auf dieses Projekt");
        }
    }

    public void requireWrite(User user, Long projectId) {
        if (accessOnProject(user, projectId) != ProjectAccess.WRITE) {
            throw new ResponseStatusException(FORBIDDEN, "Nur Lesezugriff: Aufgaben können nicht geändert werden");
        }
    }

    public void requireOwner(User user, Project project) {
        if (isAdmin(user)) {
            return;
        }
        if (!isProjectLead(user) || !isOwner(user, project)) {
            throw new ResponseStatusException(FORBIDDEN, "Nur die Projektleitung dieses Projekts darf das");
        }
    }
}
