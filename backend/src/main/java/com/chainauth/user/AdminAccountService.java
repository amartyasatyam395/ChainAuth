package com.chainauth.user;

import com.chainauth.auth.TokenService;
import com.chainauth.blockchain.BlockchainService;
import com.chainauth.blockchain.IdentitySnapshot;
import com.chainauth.blockchain.IdentitySyncJob;
import com.chainauth.blockchain.IdentitySyncService;
import com.chainauth.wallet.Wallet;
import com.chainauth.wallet.WalletRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AdminAccountService {
    private final UserRepository users;
    private final WalletRepository wallets;
    private final TokenService tokens;
    private final BlockchainService blockchain;
    private final IdentitySyncService sync;

    public AdminAccountService(UserRepository users, WalletRepository wallets, TokenService tokens,
                              BlockchainService blockchain, IdentitySyncService sync) {
        this.users=users; this.wallets=wallets; this.tokens=tokens; this.blockchain=blockchain; this.sync=sync;
    }

    public List<Map<String,Object>> listUsers() {
        return users.findAll().stream().map(u -> Map.<String,Object>of(
            "id",u.getId(),"name",u.getName(),"email",u.getEmail(),"roles",u.getRoles(),
            "enabled",u.isEnabled(),"createdAt",u.getCreatedAt(),"updatedAt",u.getUpdatedAt())).toList();
    }

    public Map<String,Object> updateRoles(String userId, java.util.Set<Role> requested) {
        User target=users.findById(userId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found"));
        if(requested==null||requested.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"At least one role is required");
        boolean targetIsEnabledAdmin=target.isEnabled()&&target.getRoles().contains(Role.ADMIN);
        boolean remainsAdmin=target.isEnabled()&&requested.contains(Role.ADMIN);
        if(targetIsEnabledAdmin&&!remainsAdmin){
            long admins=users.findAll().stream().filter(u->u.isEnabled()&&u.getRoles().contains(Role.ADMIN)).count();
            if(admins<=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Cannot remove the last enabled administrator");
        }
        target.setRoles(java.util.Set.copyOf(requested));target.setUpdatedAt(Instant.now());users.save(target);
        return Map.of("userId",userId,"roles",target.getRoles(),"updatedAt",target.getUpdatedAt());
    }

    public Map<String,Object> suspend(String userId) {
        User user=users.findById(userId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found"));
        boolean newlySuspended=user.isEnabled();
        if(newlySuspended&&user.getRoles().contains(Role.ADMIN)){
            long admins=users.findAll().stream().filter(u->u.isEnabled()&&u.getRoles().contains(Role.ADMIN)).count();
            if(admins<=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Cannot suspend the last enabled administrator");
        }
        if(newlySuspended){user.setEnabled(false); user.setTokenVersion(user.getTokenVersion()+1); user.setUpdatedAt(Instant.now()); users.save(user);}
        tokens.revokeUser(userId);
        List<IdentitySyncJob> queued=new ArrayList<>(); List<String> skipped=new ArrayList<>();
        for(Wallet wallet:wallets.findAllByUserIdOrderByLinkedAtDesc(userId)) {
            try {
                IdentitySnapshot identity=blockchain.getIdentity(wallet.getAddress());
                if(identity.registered() && identity.active()) {
                    String key="suspend-"+userId+"-"+wallet.getAddress()+"-"+user.getTokenVersion();
                    queued.add(sync.enqueueStatusChange(wallet.getAddress(),false,key));
                }
            } catch(Exception ex) { skipped.add(wallet.getAddress()); }
        }
        return Map.of("userId",userId,"enabled",false,"newlySuspended",newlySuspended,"refreshSessionsRevoked",true,
            "walletDeactivationJobs",queued,"walletsNotQueued",skipped,
            "note","Access tokens are rejected by the disabled-account check; on-chain deactivation is asynchronous.");
    }

    public Map<String,Object> reactivate(String userId) {
        User user=users.findById(userId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found"));
        user.setEnabled(true); user.setUpdatedAt(Instant.now()); users.save(user);
        return Map.of("userId",userId,"enabled",true,
            "note","Existing sessions remain revoked. On-chain wallet identities remain inactive until an administrator explicitly queues reactivation.");
    }
}
