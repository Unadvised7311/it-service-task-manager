package com.danny.taskmanager.controller;

import com.danny.taskmanager.dto.CreateUserRequest;
import com.danny.taskmanager.dto.UpdateRoleRequest;
import com.danny.taskmanager.dto.UserSummaryDto;
import com.danny.taskmanager.model.Project;
import com.danny.taskmanager.model.User;
import com.danny.taskmanager.repository.ProjectMemberRepository;
import com.danny.taskmanager.repository.ProjectRepository;
import com.danny.taskmanager.repository.UserRepository;
import com.danny.taskmanager.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private static final Set<String> ROLES = Set.of("ADMIN", "PROJECT_LEAD", "MEMBER");
    private static final Pattern EMAIL_LIKE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public AdminUserController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            CurrentUserService currentUserService,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    private static UserSummaryDto toSummary(User u) {
        return new UserSummaryDto(u.getId(), u.getUsername(), u.getRole(), u.getEmail());
    }

    @GetMapping
    public List<UserSummaryDto> list() {
        return userRepository.findAll().stream().map(AdminUserController::toSummary).collect(Collectors.toList());
    }

    @PostMapping
    public UserSummaryDto create(@Valid @RequestBody CreateUserRequest req) {
        if (!ROLES.contains(req.role())) {
            throw new ResponseStatusException(BAD_REQUEST, "Ungültige Rolle");
        }
        if (userRepository.findByUsername(req.username()).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "Benutzername bereits vergeben");
        }
        String emailNorm = null;
        if (req.email() != null && !req.email().isBlank()) {
            emailNorm = req.email().trim().toLowerCase();
            if (!EMAIL_LIKE.matcher(emailNorm).matches()) {
                throw new ResponseStatusException(BAD_REQUEST, "Ungültige E-Mail-Adresse");
            }
            if (userRepository.findByEmailIgnoreCase(emailNorm).isPresent()) {
                throw new ResponseStatusException(BAD_REQUEST, "E-Mail bereits vergeben");
            }
        }
        User u = new User();
        u.setUsername(req.username().trim());
        u.setPassword(passwordEncoder.encode(req.password()));
        u.setRole(req.role());
        u.setEmail(emailNorm);
        User saved = userRepository.save(u);
        return toSummary(saved);
    }

    @PatchMapping("/{id}/role")
    public UserSummaryDto updateRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest req) {
        if (!ROLES.contains(req.role())) {
            throw new ResponseStatusException(BAD_REQUEST, "Ungültige Rolle");
        }
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Benutzer nicht gefunden"));
        u.setRole(req.role());
        User saved = userRepository.save(u);
        return toSummary(saved);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Long id) {
        User admin = currentUserService.requireCurrentUser();
        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Benutzer nicht gefunden"));
        if (target.getId().equals(admin.getId())) {
            throw new ResponseStatusException(BAD_REQUEST, "Eigenes Konto kann nicht gelöscht werden");
        }
        List<Project> owned = projectRepository.findByOwner_Id(id);
        for (Project p : owned) {
            p.setOwner(admin);
        }
        projectRepository.saveAll(owned);
        projectMemberRepository.deleteByUser_Id(id);
        userRepository.delete(target);
    }
}
