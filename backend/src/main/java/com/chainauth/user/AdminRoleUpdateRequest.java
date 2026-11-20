package com.chainauth.user;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
public record AdminRoleUpdateRequest(@NotEmpty Set<Role> roles) {}
