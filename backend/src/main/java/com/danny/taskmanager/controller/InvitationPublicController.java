package com.danny.taskmanager.controller;

import com.danny.taskmanager.dto.AcceptInvitationRequest;
import com.danny.taskmanager.dto.InvitationPreviewDto;
import com.danny.taskmanager.service.InvitationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invitations")
public class InvitationPublicController {

    private final InvitationService invitationService;

    public InvitationPublicController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping("/{token}")
    public InvitationPreviewDto preview(@PathVariable String token) {
        return invitationService.preview(token);
    }

    @PostMapping("/{token}/accept")
    public ResponseEntity<Void> accept(@PathVariable String token, @Valid @RequestBody AcceptInvitationRequest req) {
        invitationService.accept(token, req);
        return ResponseEntity.ok().build();
    }
}
