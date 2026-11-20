package com.chainauth.blockchain.dto;

import com.chainauth.blockchain.IdentityRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record IdentityRegistrationRequest(
    @NotBlank @Pattern(regexp = "(?i)^0x[0-9a-f]{40}$") String address,
    @NotNull IdentityRole role
) {}
