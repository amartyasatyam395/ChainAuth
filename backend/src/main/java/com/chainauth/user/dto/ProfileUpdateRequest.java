package com.chainauth.user.dto;
import jakarta.validation.constraints.*;
public record ProfileUpdateRequest(@NotBlank @Size(max=80) String name) {}
