package com.jobautomation.user.controller;

import com.jobautomation.common.dto.UserDto;
import com.jobautomation.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Public endpoint: Register or update a user */
    @PostMapping("/add-user-info")
    public ResponseEntity<UserDto> addUserInfo(@RequestBody UserDto userDto) {
        return ResponseEntity.ok(userService.saveUser(userDto));
    }

    /** Public endpoint: Get user by ID */
    @GetMapping("/get-user-info/{userId}")
    public ResponseEntity<UserDto> getUserInfo(@PathVariable("userId") Long userId) {
        UserDto user = userService.getUser(userId);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }

    /** Public endpoint: Delete user by name and email */
    @DeleteMapping("/delete-user-info/{name}/{email}")
    public ResponseEntity<String> deleteUserInfo(@PathVariable("name") String name,
                                                  @PathVariable("email") String email) {
        userService.deleteUserByNameAndEmail(name, email);
        return ResponseEntity.ok("User deleted successfully");
    }

    // ── Internal endpoints called by other microservices ──────────────────

    /** Internal: Called by linkedin-service and naukri-service to fetch credentials */
    @GetMapping("/by-email/{email}")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable("email") String email) {
        UserDto user = userService.getUserByEmail(email);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }

    /** Internal: Called by ai-recommendation-service */
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable("id") Long id) {
        UserDto user = userService.getUser(id);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }
}
