package com.danny.taskmanager.service;

import com.danny.taskmanager.dto.MemberAccessDto;
import com.danny.taskmanager.dto.MemberAssignment;
import com.danny.taskmanager.dto.ProjectMembersRequest;
import com.danny.taskmanager.dto.ProjectResponse;
import com.danny.taskmanager.model.Project;
import com.danny.taskmanager.model.ProjectMember;
import com.danny.taskmanager.model.User;
import com.danny.taskmanager.repository.InvitationRepository;
import com.danny.taskmanager.repository.ProjectMemberRepository;
import com.danny.taskmanager.repository.ProjectRepository;
import com.danny.taskmanager.repository.TaskRepository;
import com.danny.taskmanager.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
public class ProjectService {

    private static final Set<String> ACCESS = Set.of("READ", "WRITE");

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;
    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ProjectAuthorizationService authz;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            TaskRepository taskRepository,
            InvitationRepository invitationRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            ProjectAuthorizationService authz) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.taskRepository = taskRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.authz = authz;
    }

    public List<ProjectResponse> listVisibleProjects() {
        User me = currentUserService.requireCurrentUser();
        List<Project> all = projectRepository.findAll();
        return all.stream()
                .filter(p -> authz.canAccessProject(me, p.getId()))
                .map(p -> toResponse(p, me))
                .collect(Collectors.toList());
    }

    public ProjectResponse createProject(String name, String description) {
        User me = currentUserService.requireCurrentUser();
        if (!authz.isProjectLead(me) && !authz.isAdmin(me)) {
            throw new ResponseStatusException(FORBIDDEN, "Nur Administrator:innen oder Projektleitende können Projekte anlegen");
        }
        Project p = new Project();
        p.setName(name);
        p.setDescription(description);
        p.setStatus("ACTIVE");
        p.setOwner(me);
        Project saved = projectRepository.save(p);
        return toResponse(saved, me);
    }

    public ProjectResponse updateProject(Long id, String name, String description) {
        User me = currentUserService.requireCurrentUser();
        Project p = authz.requireProject(id);
        authz.requireOwner(me, p);
        if ("ARCHIVED".equals(p.getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "Archivierte Projekte können nicht bearbeitet werden");
        }
        p.setName(name);
        p.setDescription(description);
        return toResponse(projectRepository.save(p), me);
    }

    @Transactional
    public ProjectResponse setArchived(Long id, boolean archived) {
        User me = currentUserService.requireCurrentUser();
        Project p = authz.requireProject(id);
        authz.requireOwner(me, p);
        p.setStatus(archived ? "ARCHIVED" : "ACTIVE");
        return toResponse(projectRepository.save(p), me);
    }

    @Transactional
    public ProjectResponse setMembers(Long projectId, ProjectMembersRequest request) {
        User me = currentUserService.requireCurrentUser();
        Project p = authz.requireProject(projectId);
        authz.requireOwner(me, p);

        List<MemberAssignment> list = request.members() != null ? request.members() : List.of();
        projectMemberRepository.deleteByProjectId(projectId);

        Long ownerId = p.getOwner().getId();
        for (MemberAssignment ma : list) {
            Long uid = ma.userId();
            if (uid == null || uid.equals(ownerId)) {
                continue;
            }
            String level = ma.accessLevel() != null ? ma.accessLevel().toUpperCase() : "WRITE";
            if (!ACCESS.contains(level)) {
                throw new ResponseStatusException(BAD_REQUEST, "Ungültige Berechtigung: " + ma.accessLevel());
            }
            User u = userRepository.findById(uid)
                    .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Unbekannter Benutzer: " + uid));
            if (!"MEMBER".equals(u.getRole())) {
                throw new ResponseStatusException(BAD_REQUEST, "Nur Benutzer mit Rolle MEMBER können zugeordnet werden");
            }
            ProjectMember pm = new ProjectMember();
            pm.setProject(p);
            pm.setUser(u);
            pm.setAccessLevel(level);
            projectMemberRepository.save(pm);
        }
        return toResponse(authz.requireProject(projectId), me);
    }

    @Transactional
    public void deleteProject(Long id) {
        User me = currentUserService.requireCurrentUser();
        Project p = authz.requireProject(id);
        authz.requireOwner(me, p);
        Long pid = p.getId();
        invitationRepository.deleteByProject_Id(pid);
        taskRepository.deleteByProject_Id(pid);
        projectMemberRepository.deleteByProjectId(pid);
        projectRepository.delete(p);
    }

    private ProjectResponse toResponse(Project p, User viewer) {
        ProjectResponse r = new ProjectResponse();
        r.setId(p.getId());
        r.setName(p.getName());
        r.setDescription(p.getDescription());
        r.setStatus(p.getStatus());
        r.setOwnerId(p.getOwner().getId());
        r.setOwnerUsername(p.getOwner().getUsername());

        long total = taskRepository.countByProjectId(p.getId());
        long done = taskRepository.countByProjectIdAndStatus(p.getId(), "DONE");
        r.setTaskTotal((int) total);
        r.setTaskDone((int) done);
        r.setProgressPercent(total == 0 ? 0 : (int) Math.round((done * 100.0) / total));

        List<ProjectMember> pms = projectMemberRepository.findByProject_Id(p.getId());
        List<MemberAccessDto> memberDtos = pms.stream()
                .map(pm -> new MemberAccessDto(
                        pm.getUser().getId(),
                        pm.getUser().getUsername(),
                        pm.getAccessLevel()))
                .collect(Collectors.toList());
        r.setMembers(memberDtos);
        r.setMemberIds(memberDtos.stream().map(MemberAccessDto::getUserId).filter(Objects::nonNull).collect(Collectors.toList()));

        ProjectAccess a = authz.accessOnProject(viewer, p.getId());
        r.setMyAccess(a.name());
        return r;
    }
}
