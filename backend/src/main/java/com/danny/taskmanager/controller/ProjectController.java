package com.danny.taskmanager.controller;

import com.danny.taskmanager.dto.ProjectMembersRequest;
import com.danny.taskmanager.dto.ProjectResponse;
import com.danny.taskmanager.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProjectResponse> list() {
        return service.listVisibleProjects();
    }

    @PostMapping
    public ProjectResponse create(@RequestBody ProjectResponse request) {
        return service.createProject(request.getName(), request.getDescription());
    }

    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable Long id, @RequestBody ProjectResponse request) {
        return service.updateProject(id, request.getName(), request.getDescription());
    }

    @PatchMapping("/{id}/archive")
    public ProjectResponse archive(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        boolean archived = Boolean.TRUE.equals(body.get("archived"));
        return service.setArchived(id, archived);
    }

    @PutMapping("/{id}/members")
    public ProjectResponse setMembers(@PathVariable Long id, @RequestBody @Valid ProjectMembersRequest body) {
        return service.setMembers(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.deleteProject(id);
    }
}
