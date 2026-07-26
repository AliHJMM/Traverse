package com.traverse.auth.service;

import com.traverse.auth.dto.RegisterRequest;
import com.traverse.auth.dto.LoginRequest;
import com.traverse.auth.dto.UpdateCredentialsRequest;
import com.traverse.auth.entity.Role;
import com.traverse.auth.entity.User;
import com.traverse.auth.exception.EmailAlreadyExistsException;
import com.traverse.auth.exception.InvalidCredentialsException;
import com.traverse.auth.exception.UserNotFoundException;
import com.traverse.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(RegisterRequest request, Role callerRole) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        Role assignedRole = resolveRole(request.role(), callerRole);
        User user = new User(request.email(), passwordEncoder.encode(request.password()), assignedRole);
        return userRepository.save(user);
    }

    /**
     * The very first account ever created becomes ADMIN (bootstrap, since no
     * admin exists yet to grant that role). After that, only an existing
     * ADMIN may hand out a privileged role (ADMIN or TRAVEL_MANAGER) --
     * a public registration asking for one silently falls back to TRAVELER,
     * the default for ordinary signups.
     */
    private Role resolveRole(Role requestedRole, Role callerRole) {
        if (userRepository.count() == 0) {
            return Role.ADMIN;
        }
        boolean privileged = requestedRole == Role.ADMIN || requestedRole == Role.TRAVEL_MANAGER;
        if (privileged && callerRole != Role.ADMIN) {
            return Role.TRAVELER;
        }
        return requestedRole == null ? Role.TRAVELER : requestedRole;
    }

    @Transactional(readOnly = true)
    public User authenticate(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);
        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return user;
    }

    public User updateCredentials(Long id, UpdateCredentialsRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));

        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new EmailAlreadyExistsException(request.email());
            }
            user.setEmail(request.email());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        return userRepository.save(user);
    }

    public void deleteById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }
}
