package com.chainauth.blockchain;

import java.math.BigInteger;

public record IdentitySnapshot(
    String address,
    boolean registered,
    boolean active,
    IdentityRole role,
    BigInteger registeredAt
) {}
