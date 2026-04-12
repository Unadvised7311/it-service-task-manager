package com.danny.taskmanager.repository;

import com.danny.taskmanager.model.Project;
import org.springframework.data.jpa.repository.JpaRepository; // [cite: 3437]

import java.util.List;

// Erbt automatisch CRUD-Operationen wie save() oder findById() [cite: 3448]
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByOwner_Id(Long ownerId);
}
