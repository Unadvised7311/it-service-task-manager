package com.danny.taskmanager.service;

import com.danny.taskmanager.dto.TaskRequest;
import com.danny.taskmanager.dto.TaskResponse;
import com.danny.taskmanager.dto.TaskUpdateRequest;
import com.danny.taskmanager.model.Project;
import com.danny.taskmanager.model.Task;
import com.danny.taskmanager.model.User;
import com.danny.taskmanager.repository.ProjectRepository;
import com.danny.taskmanager.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class TaskService {

    private static final Set<String> STATUSES = Set.of("OPEN", "IN_PROGRESS", "DONE");

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserService currentUserService;
    private final ProjectAuthorizationService authz;

    public TaskService(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            CurrentUserService currentUserService,
            ProjectAuthorizationService authz) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.currentUserService = currentUserService;
        this.authz = authz;
    }

    public List<TaskResponse> listTasksForProject(Long projectId) {
        User me = currentUserService.requireCurrentUser();
        authz.requireRead(me, projectId);
        return taskRepository.findByProjectIdOrderByIdAsc(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    public TaskResponse createTask(TaskRequest req) {
        User me = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(req.projectId())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Projekt nicht gefunden"));
        authz.requireWrite(me, project.getId());
        if ("ARCHIVED".equals(project.getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "Projekt ist archiviert");
        }

        Task task = new Task();
        task.setTitle(req.title());
        task.setDescription(req.description() != null ? req.description() : "");
        task.setStatus("OPEN");
        task.setProject(project);
        Task saved = taskRepository.save(task);
        return toResponse(saved);
    }

    public TaskResponse updateStatus(Long taskId, String newStatus) {
        if (!STATUSES.contains(newStatus)) {
            throw new ResponseStatusException(BAD_REQUEST, "Ungültiger Status");
        }
        User me = currentUserService.requireCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Aufgabe nicht gefunden"));
        authz.requireWrite(me, task.getProject().getId());
        if ("ARCHIVED".equals(task.getProject().getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "Projekt ist archiviert");
        }
        task.setStatus(newStatus);
        return toResponse(taskRepository.save(task));
    }

    public TaskResponse updateTask(Long taskId, TaskUpdateRequest req) {
        User me = currentUserService.requireCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Aufgabe nicht gefunden"));
        authz.requireWrite(me, task.getProject().getId());
        if ("ARCHIVED".equals(task.getProject().getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "Projekt ist archiviert");
        }
        task.setTitle(req.title());
        task.setDescription(req.description() != null ? req.description() : "");
        return toResponse(taskRepository.save(task));
    }

    private TaskResponse toResponse(Task t) {
        return new TaskResponse(
                t.getId(),
                t.getTitle(),
                t.getDescription() != null ? t.getDescription() : "",
                t.getStatus(),
                t.getProject().getId());
    }
}
