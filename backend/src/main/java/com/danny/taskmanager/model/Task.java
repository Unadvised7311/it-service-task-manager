package com.danny.taskmanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    // NEU: Das hat deinem TaskService gefehlt!
    private String description;

    private String status;

    // NEU: Echte Verknüpfung zum Projekt (für setProject / getProject)
    @ManyToOne
    @JoinColumn(name = "project_id")
    @JsonIgnore // WICHTIG: Verhindert eine Endlosschleife beim Senden an React
    private Project project;

    // Für das Frontend, falls es die ID direkt auslesen will
    @Column(name = "project_id", insertable = false, updatable = false)
    private Long projectId;

    public Task() {}

    // --- GETTER & SETTER ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
}
