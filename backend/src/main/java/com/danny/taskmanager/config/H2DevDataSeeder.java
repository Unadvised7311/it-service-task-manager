package com.danny.taskmanager.config;

import com.danny.taskmanager.model.Project;
import com.danny.taskmanager.model.ProjectMember;
import com.danny.taskmanager.model.Task;
import com.danny.taskmanager.model.User;
import com.danny.taskmanager.repository.ProjectMemberRepository;
import com.danny.taskmanager.repository.ProjectRepository;
import com.danny.taskmanager.repository.TaskRepository;
import com.danny.taskmanager.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Legt beim ersten Start (leere DB) Demo-Benutzer und ein Beispielprojekt an.
 * Bei persistenter H2-Datei wird nichts überschrieben — Daten bleiben erhalten.
 */
@Component
@Profile("h2")
@Order(100)
public class H2DevDataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    public H2DevDataSeeder(
            UserRepository userRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            TaskRepository taskRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.taskRepository = taskRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        userRepository.save(buildUser("admin", "ADMIN", "admin@example.com"));
        User bob = userRepository.save(buildUser("bob", "PROJECT_LEAD", "bob@example.com"));
        User charlie = userRepository.save(buildUser("charlie", "MEMBER", "charlie@example.com"));

        Project project = new Project();
        project.setName("Server-Migration");
        project.setDescription("Wechsel auf neue Cloud-Server");
        project.setStatus("ACTIVE");
        project.setOwner(bob);
        project = projectRepository.save(project);

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(charlie);
        member.setAccessLevel("WRITE");
        projectMemberRepository.save(member);

        saveTask(project, "Backups erstellen", "", "DONE");
        saveTask(project, "Datenbank exportieren", "", "IN_PROGRESS");
        saveTask(project, "DNS umstellen", "", "OPEN");
    }

    private User buildUser(String username, String role, String email) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("password"));
        u.setRole(role);
        u.setEmail(email);
        return u;
    }

    private void saveTask(Project project, String title, String description, String status) {
        Task t = new Task();
        t.setProject(project);
        t.setTitle(title);
        t.setDescription(description);
        t.setStatus(status);
        taskRepository.save(t);
    }
}
