package com.chainauth.blockchain;

import com.chainauth.wallet.Wallet;
import com.chainauth.wallet.WalletRepository;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class IdentityReconciliationService {
    private final WalletRepository wallets;
    private final BlockchainService blockchain;
    public IdentityReconciliationService(WalletRepository wallets, BlockchainService blockchain) {
        this.wallets = wallets; this.blockchain = blockchain;
    }
    public Map<String,Object> reconcile() {
        List<Map<String,Object>> missing = new ArrayList<>();
        List<Map<String,Object>> inactive = new ArrayList<>();
        List<Map<String,Object>> roleMismatch = new ArrayList<>();
        int checked = 0;
        for (Wallet wallet : wallets.findAll()) {
            if (checked >= 500) break;
            checked++;
            IdentitySnapshot snapshot = blockchain.getIdentity(wallet.getAddress());
            if (!snapshot.registered()) missing.add(Map.of("walletAddress",wallet.getAddress(),"userId",wallet.getUserId()));
            else {
                if (!snapshot.active()) inactive.add(Map.of("walletAddress",wallet.getAddress(),"userId",wallet.getUserId()));
                if (snapshot.role() == null) roleMismatch.add(Map.of("walletAddress",wallet.getAddress(),"userId",wallet.getUserId(),"reason","Registered identity has no role"));
            }
        }
        return Map.of("checkedWallets",checked,"truncated",wallets.count()>checked,"missingOnChain",missing,
            "inactiveOnChain",inactive,"roleDataIssues",roleMismatch,
            "applicationAuthorizationSource","MongoDB; on-chain roles do not grant Spring Security privileges");
    }
}
