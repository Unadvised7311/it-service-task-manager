package com.danny.taskmanager.controller;

import com.danny.taskmanager.dto.UserSummaryDto;
import com.danny.taskmanager.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserDirectoryController {

    private final UserRepository userRepository;

    public UserDirectoryController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<UserSummaryDto> listForAssignment() {
        return userRepository.findAll().stream()
                .filter(u -> "MEMBER".equals(u.getRole()))
                .map(u -> new UserSummaryDto(u.getId(), u.getUsername(), u.getRole(), u.getEmail()))
                .collect(Collectors.toList());
    }
}
