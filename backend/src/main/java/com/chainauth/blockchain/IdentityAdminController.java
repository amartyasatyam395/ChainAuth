package com.chainauth.blockchain;

import com.chainauth.blockchain.dto.IdentityRegistrationRequest;
import com.chainauth.blockchain.dto.IdentityRoleRequest;
import com.chainauth.wallet.Wallet;
import com.chainauth.wallet.WalletRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigInteger;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/identities")
@PreAuthorize("hasRole('ADMIN')")
public class IdentityAdminController {
    private final BlockchainService blockchain;
    private final WalletRepository wallets;

    public IdentityAdminController(BlockchainService blockchain, WalletRepository wallets) {
        this.blockchain = blockchain;
        this.wallets = wallets;
    }

    @GetMapping("/{address}")
    public IdentitySnapshot getIdentity(@PathVariable String address) {
        return blockchain.getIdentity(address);
    }

    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody IdentityRegistrationRequest request) {
        String address = normalize(request.address());
        Wallet linked = wallets.findByAddress(address)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Wallet must be linked to a ChainAuth account before on-chain registration"));
        BlockchainTransactionResult tx = blockchain.registerIdentity(address, request.role());
        return Map.of("walletAddress", address, "chainAuthUserId", linked.getUserId(),
            "role", request.role().name(), "transaction", tx);
    }

    @PostMapping("/{address}/role")
    public Map<String, Object> assignRole(@PathVariable String address,
                                          @Valid @RequestBody IdentityRoleRequest request) {
        String normalized = normalize(address);
        requireLinkedWallet(normalized);
        BlockchainTransactionResult tx = blockchain.assignRole(normalized, request.role());
        return Map.of("walletAddress", normalized, "role", request.role().name(), "transaction", tx);
    }

    @PostMapping("/{address}/deactivate")
    public Map<String, Object> deactivate(@PathVariable String address) {
        String normalized = normalize(address);
        requireLinkedWallet(normalized);
        return Map.of("walletAddress", normalized, "transaction",
            blockchain.deactivateIdentity(normalized));
    }

    @PostMapping("/{address}/reactivate")
    public Map<String, Object> reactivate(@PathVariable String address) {
        String normalized = normalize(address);
        requireLinkedWallet(normalized);
        return Map.of("walletAddress", normalized, "transaction",
            blockchain.reactivateIdentity(normalized));
    }

    @GetMapping("/events")
    public Object events(@RequestParam(defaultValue = "0") BigInteger fromBlock) {
        if (fromBlock.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromBlock cannot be negative");
        }
        return blockchain.getEvents(fromBlock);
    }

    @GetMapping("/network")
    public Map<String, Object> network() {
        return blockchain.networkStatus();
    }

    private Wallet requireLinkedWallet(String address) {
        return wallets.findByAddress(address)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Wallet is not linked to a ChainAuth account"));
    }

    private static String normalize(String address) {
        if (address == null || !address.matches("(?i)^0x[0-9a-f]{40}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Ethereum address");
        }
        return "0x" + address.substring(2).toLowerCase();
    }
}
