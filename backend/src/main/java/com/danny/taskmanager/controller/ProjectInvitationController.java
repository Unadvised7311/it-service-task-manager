package com.danny.taskmanager.controller;

import com.danny.taskmanager.dto.CreateInvitationRequest;
import com.danny.taskmanager.service.InvitationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/invitations")
public class ProjectInvitationController {

    private final InvitationService invitationService;

    public ProjectInvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping
    public ResponseEntity<Void> invite(@PathVariable Long projectId, @Valid @RequestBody CreateInvitationRequest body) {
        invitationService.createInvitation(projectId, body);
        return ResponseEntity.ok().build();
    }
}
