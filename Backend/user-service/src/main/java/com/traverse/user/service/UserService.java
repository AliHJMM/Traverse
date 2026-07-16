package com.traverse.user.service;

import com.traverse.user.client.AuthRegisterRequest;
import com.traverse.user.client.AuthServiceClient;
import com.traverse.user.client.AuthUpdateCredentialsRequest;
import com.traverse.user.client.AuthUserResponse;
import com.traverse.user.dto.CreateUserRequest;
import com.traverse.user.dto.UpdateUserRequest;
import com.traverse.user.entity.UserProfile;
import com.traverse.user.exception.EmailAlreadyExistsException;
import com.traverse.user.exception.UserNotFoundException;
import com.traverse.user.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserProfileRepository userProfileRepository;
    private final AuthServiceClient authServiceClient;

    public UserService(UserProfileRepository userProfileRepository, AuthServiceClient authServiceClient) {
        this.userProfileRepository = userProfileRepository;
        this.authServiceClient = authServiceClient;
    }

    /**
     * Creates the login credential in Auth Service first (source of truth
     * for identity/role), then stores the profile locally under the id Auth
     * Service assigned. If the local save fails, we intentionally don't try
     * to compensate by deleting the just-created credential -- an orphaned
     * credential with no profile is recoverable by retrying the profile
     * creation, whereas silently losing a credential is not.
     */
    public UserProfile create(CreateUserRequest request) {
        if (userProfileRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        AuthUserResponse authUser = authServiceClient.register(
                new AuthRegisterRequest(request.email(), request.password(), request.role()));

        UserProfile profile = new UserProfile(
                authUser.id(), authUser.email(), authUser.role(),
                request.fullName(), request.phone(), request.address());
        return userProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public List<UserProfile> findAll() {
        return userProfileRepository.findAll();
    }

    @Transactional(readOnly = true)
    public UserProfile findById(Long id) {
        return userProfileRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    public UserProfile update(Long id, UpdateUserRequest request) {
        UserProfile profile = findById(id);

        boolean credentialsChanged = request.email() != null || request.role() != null || request.enabled() != null;
        if (credentialsChanged) {
            AuthUserResponse authUser = authServiceClient.updateCredentials(id,
                    new AuthUpdateCredentialsRequest(request.email(), request.role(), request.enabled()));
            profile.setEmail(authUser.email());
            profile.setRole(authUser.role());
        }
        if (request.enabled() != null) {
            profile.setEnabled(request.enabled());
        }
        if (request.fullName() != null) {
            profile.setFullName(request.fullName());
        }
        if (request.phone() != null) {
            profile.setPhone(request.phone());
        }
        if (request.address() != null) {
            profile.setAddress(request.address());
        }
        return userProfileRepository.save(profile);
    }

    /**
     * Cascades the delete: removes the credential in Auth Service first,
     * then the local profile. (Cascading into travels/payments happens once
     * those services exist -- Phases 5/6.)
     */
    public void delete(Long id) {
        if (!userProfileRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        authServiceClient.deleteCredentials(id);
        userProfileRepository.deleteById(id);
    }
}
