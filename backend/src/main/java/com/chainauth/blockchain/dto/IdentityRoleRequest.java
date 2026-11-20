package com.chainauth.blockchain.dto;

import com.chainauth.blockchain.IdentityRole;
import jakarta.validation.constraints.NotNull;

public record IdentityRoleRequest(@NotNull IdentityRole role) {}
