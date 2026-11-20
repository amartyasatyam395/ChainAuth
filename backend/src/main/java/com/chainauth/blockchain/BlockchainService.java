package com.chainauth.blockchain;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.crypto.Credentials;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@Service
public class BlockchainService {
    private static final BigInteger ZERO = BigInteger.ZERO;
    private static final BigInteger DEFAULT_GAS_PRICE = BigInteger.valueOf(2_000_000_000L);

    private final Web3j web3j;
    private final String contractAddress;
    private final String privateKey;
    private final long expectedChainId;
    private final BigInteger configuredGasLimit;

    public BlockchainService(
        @Value("${app.blockchain.rpc-url}") String rpcUrl,
        @Value("${app.blockchain.chain-id:31337}") long expectedChainId,
        @Value("${app.blockchain.contract-address:}") String contractAddress,
        @Value("${app.blockchain.signer-private-key:}") String privateKey,
        @Value("${app.blockchain.gas-limit:600000}") BigInteger gasLimit
    ) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.expectedChainId = expectedChainId;
        this.contractAddress = contractAddress == null ? "" : contractAddress.trim();
        this.privateKey = privateKey == null ? "" : privateKey.trim();
        this.configuredGasLimit = gasLimit;
    }

    public boolean isConfigured() {
        return isAddress(contractAddress);
    }

    public boolean hasTransactionSigner() {
        return StringUtils.hasText(privateKey);
    }

    public Map<String, Object> networkStatus() {
        try {
            long actual = web3j.ethChainId().send().getChainId().longValueExact();
            BigInteger block = web3j.ethBlockNumber().send().getBlockNumber();
            boolean codePresent = false;
            if (isConfigured()) {
                String code = web3j.ethGetCode(contractAddress, DefaultBlockParameterName.LATEST).send().getCode();
                codePresent = code != null && !code.equalsIgnoreCase("0x") && !code.equalsIgnoreCase("0x0");
            }
            return Map.of(
                "connected", true,
                "chainId", actual,
                "expectedChainId", expectedChainId,
                "chainIdMatches", actual == expectedChainId,
                "latestBlock", block,
                "contractConfigured", isConfigured(),
                "contractCodePresent", codePresent,
                "transactionSignerConfigured", hasTransactionSigner()
            );
        } catch (Exception ex) {
            return Map.of(
                "connected", false,
                "expectedChainId", expectedChainId,
                "contractConfigured", isConfigured(),
                "transactionSignerConfigured", hasTransactionSigner(),
                "error", ex.getClass().getSimpleName()
            );
        }
    }

    public IdentitySnapshot getIdentity(String rawAddress) {
        String address = normalizeAddress(rawAddress);
        Function function = function("getIdentity", List.of(new Address(address)), List.of(
            new TypeReference<Bool>() {},
            new TypeReference<Bool>() {},
            new TypeReference<Uint8>() {},
            new TypeReference<org.web3j.abi.datatypes.generated.Uint64>() {}
        ));
        List<Type> values = call(function);
        boolean registered = (Boolean) values.get(0).getValue();
        boolean active = (Boolean) values.get(1).getValue();
        int role = ((BigInteger) values.get(2).getValue()).intValueExact();
        BigInteger registeredAt = (BigInteger) values.get(3).getValue();
        return new IdentitySnapshot(address, registered, active,
            registered ? IdentityRole.fromContractValue(role) : null, registeredAt);
    }

    public BlockchainTransactionResult registerIdentity(String address, IdentityRole role) {
        return transact(function("registerIdentity",
            List.of(new Address(normalizeAddress(address)), new Uint8(BigInteger.valueOf(role.contractValue()))), List.of()));
    }

    public BlockchainTransactionResult assignRole(String address, IdentityRole role) {
        return transact(function("assignRole",
            List.of(new Address(normalizeAddress(address)), new Uint8(BigInteger.valueOf(role.contractValue()))), List.of()));
    }

    public BlockchainTransactionResult deactivateIdentity(String address) {
        return transact(function("deactivateIdentity", List.of(new Address(normalizeAddress(address))), List.of()));
    }

    public BlockchainTransactionResult reactivateIdentity(String address) {
        return transact(function("reactivateIdentity", List.of(new Address(normalizeAddress(address))), List.of()));
    }

    public List<EthLog.LogResult<?>> getEvents(BigInteger fromBlock) {
        ensureContract();
        verifyNetwork();
        try {
            EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(fromBlock.max(ZERO)),
                DefaultBlockParameterName.LATEST,
                contractAddress
            );
            return web3j.ethGetLogs(filter).send().getLogs();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to retrieve IdentityRegistry events", ex);
        }
    }

    public List<EthLog.LogResult<?>> getEvents(BigInteger fromBlock, BigInteger toBlock) {
        ensureContract(); verifyNetwork();
        try {
            EthFilter filter = new EthFilter(DefaultBlockParameter.valueOf(fromBlock.max(ZERO)),
                DefaultBlockParameter.valueOf(toBlock.max(fromBlock)), contractAddress);
            return web3j.ethGetLogs(filter).send().getLogs();
        } catch (Exception ex) { throw new IllegalStateException("Unable to retrieve IdentityRegistry event range", ex); }
    }

    /** Submit without waiting for a receipt; durable worker owns confirmation. */
    public String submitIdentityOperation(String operation, String address, IdentityRole role) {
        ensureContract();
        if (!hasTransactionSigner()) throw new IllegalStateException("Blockchain transaction signer is not configured");
        verifyNetwork();
        List<Type> inputs = switch(operation) {
            case "registerIdentity", "assignRole" -> List.of(new Address(normalizeAddress(address)), new Uint8(BigInteger.valueOf(role.contractValue())));
            case "deactivateIdentity", "reactivateIdentity" -> List.of(new Address(normalizeAddress(address)));
            default -> throw new IllegalArgumentException("Unsupported IdentityRegistry operation");
        };
        try {
            Credentials credentials = Credentials.create(privateKey);
            TransactionManager manager = new RawTransactionManager(web3j, credentials, expectedChainId);
            String encoded = FunctionEncoder.encode(function(operation, inputs, List.of()));
            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
            var estimate = web3j.ethEstimateGas(Transaction.createEthCallTransaction(credentials.getAddress(), contractAddress, encoded)).send();
            if (estimate.hasError()) throw new IllegalStateException("Gas estimation failed: " + estimate.getError().getMessage());
            BigInteger gasLimit = estimate.getAmountUsed().multiply(BigInteger.valueOf(120)).divide(BigInteger.valueOf(100));
            if (gasLimit.compareTo(configuredGasLimit) > 0) throw new IllegalStateException("Estimated gas exceeds configured BLOCKCHAIN_GAS_LIMIT");
            EthSendTransaction sent = manager.sendTransaction(gasPrice == null ? DEFAULT_GAS_PRICE : gasPrice,
                gasLimit.max(BigInteger.valueOf(21_000)), contractAddress, encoded, ZERO);
            if (sent.hasError()) throw new IllegalStateException("Transaction rejected: " + sent.getError().getMessage());
            return sent.getTransactionHash();
        } catch (Exception ex) { throw new IllegalStateException("Unable to submit identity operation", ex); }
    }

    public TransactionReceipt findReceipt(String transactionHash) {
        try { var response = web3j.ethGetTransactionReceipt(transactionHash).send(); return response.getTransactionReceipt().orElse(null); }
        catch (Exception ex) { throw new IllegalStateException("Unable to retrieve transaction receipt", ex); }
    }
    public BigInteger getLatestBlock() {
        try { return web3j.ethBlockNumber().send().getBlockNumber(); }
        catch (Exception ex) { throw new IllegalStateException("Unable to retrieve latest block", ex); }
    }
    public String getBlockHash(long number) {
        try { var block=web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(BigInteger.valueOf(number)), false).send().getBlock();
            if(block==null || block.getHash()==null) throw new IllegalStateException("Block unavailable"); return block.getHash(); }
        catch (Exception ex) { throw new IllegalStateException("Unable to retrieve canonical block hash", ex); }
    }

    private List<Type> call(Function function) {
        ensureContract();
        verifyNetwork();
        try {
            String encoded = FunctionEncoder.encode(function);
            var response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, contractAddress, encoded),
                DefaultBlockParameterName.LATEST
            ).send();
            if (response.hasError()) throw new IllegalStateException(response.getError().getMessage());
            return FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        } catch (Exception ex) {
            throw new IllegalStateException("IdentityRegistry read call failed", ex);
        }
    }

    private BlockchainTransactionResult transact(Function function) {
        ensureContract();
        if (!hasTransactionSigner()) {
            throw new IllegalStateException("Blockchain transaction signer is not configured");
        }
        verifyNetwork();
        try {
            Credentials credentials = Credentials.create(privateKey);
            TransactionManager manager = new RawTransactionManager(web3j, credentials, expectedChainId);
            String encoded = FunctionEncoder.encode(function);

            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
            var estimate = web3j.ethEstimateGas(
                Transaction.createEthCallTransaction(credentials.getAddress(), contractAddress, encoded)
            ).send();
            if (estimate.hasError()) throw new IllegalStateException("Gas estimation failed: " + estimate.getError().getMessage());
            BigInteger estimatedGas = estimate.getAmountUsed();
            BigInteger gasLimit = estimatedGas.multiply(BigInteger.valueOf(120)).divide(BigInteger.valueOf(100));
            if (gasLimit.compareTo(configuredGasLimit) > 0) {
                throw new IllegalStateException("Estimated gas exceeds configured BLOCKCHAIN_GAS_LIMIT");
            }

            EthSendTransaction sent = manager.sendTransaction(
                gasPrice == null ? DEFAULT_GAS_PRICE : gasPrice,
                gasLimit.max(BigInteger.valueOf(21_000)),
                contractAddress,
                encoded,
                ZERO
            );
            if (sent.hasError()) throw new IllegalStateException("Transaction rejected: " + sent.getError().getMessage());

            TransactionReceipt receipt = new PollingTransactionReceiptProcessor(web3j, 1_000, 60)
                .waitForTransactionReceipt(sent.getTransactionHash());
            boolean successful = receipt.isStatusOK();
            return new BlockchainTransactionResult(
                sent.getTransactionHash(),
                receipt.getBlockNumber(),
                successful,
                receipt.getGasUsed()
            );
        } catch (Exception ex) {
            throw new IllegalStateException("IdentityRegistry transaction failed", ex);
        }
    }

    private void verifyNetwork() {
        try {
            long actual = web3j.ethChainId().send().getChainId().longValueExact();
            if (actual != expectedChainId) {
                throw new IllegalStateException("Connected to chain " + actual + "; expected " + expectedChainId);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to verify Ethereum network", ex);
        }
    }

    private void ensureContract() {
        if (!isConfigured()) throw new IllegalStateException("IDENTITY_REGISTRY_ADDRESS is not configured");
        try {
            String code = web3j.ethGetCode(contractAddress, DefaultBlockParameterName.LATEST).send().getCode();
            if (code == null || code.equalsIgnoreCase("0x") || code.equalsIgnoreCase("0x0")) {
                throw new IllegalStateException("No contract bytecode found at IDENTITY_REGISTRY_ADDRESS");
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to verify IdentityRegistry deployment", ex);
        }
    }

    private static Function function(String name, List<Type> inputs, List<TypeReference<?>> outputs) {
        return new Function(name, inputs, outputs);
    }

    private static boolean isAddress(String address) {
        return address != null && address.matches("(?i)^0x[0-9a-f]{40}$");
    }

    private static String normalizeAddress(String address) {
        if (!isAddress(address)) throw new IllegalArgumentException("Invalid Ethereum address");
        return "0x" + address.substring(2).toLowerCase();
    }

    @PreDestroy
    public void shutdown() {
        web3j.shutdown();
    }
}
