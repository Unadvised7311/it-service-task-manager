package com.danny.taskmanager.service;

import com.danny.taskmanager.dto.AcceptInvitationRequest;
import com.danny.taskmanager.dto.CreateInvitationRequest;
import com.danny.taskmanager.dto.InvitationPreviewDto;
import com.danny.taskmanager.model.Invitation;
import com.danny.taskmanager.model.Project;
import com.danny.taskmanager.model.ProjectMember;
import com.danny.taskmanager.model.User;
import com.danny.taskmanager.repository.InvitationRepository;
import com.danny.taskmanager.repository.ProjectMemberRepository;
import com.danny.taskmanager.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class InvitationService {

    private static final Set<String> ACCESS = Set.of("READ", "WRITE");

    private final InvitationRepository invitationRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ProjectAuthorizationService authz;
    private final MailNotificationService mailNotificationService;
    private final PasswordEncoder passwordEncoder;

    public InvitationService(
            InvitationRepository invitationRepository,
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            ProjectAuthorizationService authz,
            MailNotificationService mailNotificationService,
            PasswordEncoder passwordEncoder) {
        this.invitationRepository = invitationRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.authz = authz;
        this.mailNotificationService = mailNotificationService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void createInvitation(Long projectId, CreateInvitationRequest req) {
        User me = currentUserService.requireCurrentUser();
        Project p = authz.requireProject(projectId);
        authz.requireOwner(me, p);
        if ("ARCHIVED".equals(p.getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "Archivierte Projekte können nicht eingeladen werden");
        }
        String level = req.accessLevel().toUpperCase();
        if (!ACCESS.contains(level)) {
            throw new ResponseStatusException(BAD_REQUEST, "Ungültige Berechtigung");
        }

        String token = UUID.randomUUID().toString();
        Invitation inv = new Invitation();
        inv.setProject(p);
        inv.setEmail(req.email().trim().toLowerCase());
        inv.setToken(token);
        inv.setAccessLevel(level);
        inv.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        invitationRepository.save(inv);

        mailNotificationService.sendProjectInvitation(inv.getEmail(), p.getName(), token);
    }

    public InvitationPreviewDto preview(String token) {
        return invitationRepository.findByToken(token)
                .map(this::toPreview)
                .orElseGet(() -> new InvitationPreviewDto(false, null, null, null, "Unbekannter Link"));
    }

    private InvitationPreviewDto toPreview(Invitation inv) {
        if (inv.getAcceptedAt() != null) {
            return new InvitationPreviewDto(false, inv.getProject().getName(), inv.getAccessLevel(), inv.getEmail(), "Einladung bereits verwendet");
        }
        if (inv.getExpiresAt().isBefore(Instant.now())) {
            return new InvitationPreviewDto(false, inv.getProject().getName(), inv.getAccessLevel(), inv.getEmail(), "Einladung abgelaufen");
        }
        return new InvitationPreviewDto(true, inv.getProject().getName(), inv.getAccessLevel(), inv.getEmail(), null);
    }

    @Transactional
    public void accept(String token, AcceptInvitationRequest req) {
        Invitation inv = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Ungültiger Link"));
        if (inv.getAcceptedAt() != null) {
            throw new ResponseStatusException(BAD_REQUEST, "Einladung bereits verwendet");
        }
        if (inv.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(BAD_REQUEST, "Einladung abgelaufen");
        }
        if (userRepository.findByUsername(req.username().trim()).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "Benutzername bereits vergeben");
        }
        userRepository.findByEmailIgnoreCase(inv.getEmail()).ifPresent(u -> {
            throw new ResponseStatusException(BAD_REQUEST, "Diese E-Mail ist bereits registriert. Bitte anmelden und die Projektleitung bitten, Sie zuzuordnen.");
        });

        User u = new User();
        u.setUsername(req.username().trim());
        u.setEmail(inv.getEmail());
        u.setPassword(passwordEncoder.encode(req.password()));
        u.setRole("MEMBER");
        User saved = userRepository.save(u);

        ProjectMember pm = new ProjectMember();
        pm.setProject(inv.getProject());
        pm.setUser(saved);
        pm.setAccessLevel(inv.getAccessLevel());
        projectMemberRepository.save(pm);

        inv.setAcceptedAt(Instant.now());
        invitationRepository.save(inv);
    }
}
