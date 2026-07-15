package com.traverse.user.controller;

import com.traverse.user.dto.CreateUserRequest;
import com.traverse.user.dto.UpdateUserRequest;
import com.traverse.user.dto.UserResponse;
import com.traverse.user.entity.UserProfile;
import com.traverse.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserProfile profile = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(profile));
    }

    @GetMapping
    public List<UserResponse> findAll() {
        return userService.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable Long id) {
        return toResponse(userService.findById(id));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return toResponse(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private UserResponse toResponse(UserProfile profile) {
        return new UserResponse(profile.getId(), profile.getEmail(), profile.getRole(), profile.getFullName(),
                profile.getPhone(), profile.getAddress(), profile.isEnabled(), profile.getCreatedAt());
    }
}
