package com.chainauth.blockchain;

public enum IdentityRole {
    USER(0), MODERATOR(1), ADMIN(2);

    private final int contractValue;
    IdentityRole(int contractValue) { this.contractValue = contractValue; }
    public int contractValue() { return contractValue; }
    public static IdentityRole fromContractValue(int value) {
        return switch (value) {
            case 0 -> USER;
            case 1 -> MODERATOR;
            case 2 -> ADMIN;
            default -> throw new IllegalArgumentException("Unknown on-chain identity role: " + value);
        };
    }
}
