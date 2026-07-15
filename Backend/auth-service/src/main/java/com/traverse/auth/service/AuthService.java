package com.traverse.auth.service;

import com.traverse.auth.dto.RegisterRequest;
import com.traverse.auth.dto.LoginRequest;
import com.traverse.auth.entity.Role;
import com.traverse.auth.entity.User;
import com.traverse.auth.exception.EmailAlreadyExistsException;
import com.traverse.auth.exception.InvalidCredentialsException;
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
     * admin exists yet to grant that role). After that, a caller can only
     * hand out ADMIN if they're already authenticated as ADMIN themselves --
     * otherwise a public registration silently gets downgraded to USER,
     * regardless of what role it asked for.
     */
    private Role resolveRole(Role requestedRole, Role callerRole) {
        if (userRepository.count() == 0) {
            return Role.ADMIN;
        }
        if (requestedRole == Role.ADMIN && callerRole != Role.ADMIN) {
            return Role.USER;
        }
        return requestedRole == null ? Role.USER : requestedRole;
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
}
