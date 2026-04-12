package com.danny.taskmanager.repository;

import com.danny.taskmanager.model.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByToken(String token);

    @Modifying
    @Query("delete from Invitation i where i.project.id = :projectId")
    void deleteByProject_Id(@Param("projectId") Long projectId);
}
