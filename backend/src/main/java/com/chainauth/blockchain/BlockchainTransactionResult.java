package com.chainauth.blockchain;

import java.math.BigInteger;

public record BlockchainTransactionResult(
    String transactionHash,
    BigInteger blockNumber,
    boolean successful,
    BigInteger gasUsed
) {}
