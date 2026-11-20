package com.chainauth.wallet.dto;import jakarta.validation.constraints.NotBlank;
public record WalletVerifyRequest(@NotBlank String address,@NotBlank String message,@NotBlank String signature){}
