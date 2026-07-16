package com.traverse.user.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-service", configuration = FeignAuthForwardingConfig.class)
public interface AuthServiceClient {

    @PostMapping("/api/auth/register")
    AuthUserResponse register(@RequestBody AuthRegisterRequest request);

    @PatchMapping("/api/auth/users/{id}")
    AuthUserResponse updateCredentials(@PathVariable("id") Long id, @RequestBody AuthUpdateCredentialsRequest request);

    @DeleteMapping("/api/auth/users/{id}")
    void deleteCredentials(@PathVariable("id") Long id);
}
