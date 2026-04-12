package com.danny.taskmanager.controller;

import com.danny.taskmanager.dto.TaskRequest;
import com.danny.taskmanager.dto.TaskResponse;
import com.danny.taskmanager.dto.TaskUpdateRequest;
import com.danny.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping("/project/{projectId}")
    public List<TaskResponse> listByProject(@PathVariable Long projectId) {
        return service.listTasksForProject(projectId);
    }

    @PostMapping
    public TaskResponse addTask(@Valid @RequestBody TaskRequest req) {
        return service.createTask(req);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse changeStatus(@PathVariable Long id, @RequestParam String status) {
        return service.updateStatus(id, status);
    }

    @PatchMapping("/{id}")
    public TaskResponse updateTask(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest req) {
        return service.updateTask(id, req);
    }
}
