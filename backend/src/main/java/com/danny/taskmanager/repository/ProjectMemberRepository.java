package com.danny.taskmanager.repository;

import com.danny.taskmanager.model.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    List<ProjectMember> findByProject_Id(Long projectId);

    Optional<ProjectMember> findByProject_IdAndUser_Id(Long projectId, Long userId);

    @Modifying
    @Query("delete from ProjectMember pm where pm.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") Long projectId);

    @Modifying
    @Query("delete from ProjectMember pm where pm.user.id = :userId")
    void deleteByUser_Id(@Param("userId") Long userId);

    boolean existsByProject_IdAndUser_Id(Long projectId, Long userId);
}
