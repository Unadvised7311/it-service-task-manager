package com.danny.taskmanager.repository;

import com.danny.taskmanager.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectIdOrderByIdAsc(Long projectId);

    @Modifying
    @Query("delete from Task t where t.project.id = :projectId")
    void deleteByProject_Id(@Param("projectId") Long projectId);

    long countByProjectId(Long projectId);

    long countByProjectIdAndStatus(Long projectId, String status);
}
