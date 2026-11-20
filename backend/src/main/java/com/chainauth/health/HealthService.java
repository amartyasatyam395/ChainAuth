package com.chainauth.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class HealthService {
    private final MongoTemplate mongoTemplate;
    private final String rpcUrl;
    private final long expectedChainId;

    public HealthService(MongoTemplate mongoTemplate,
        @Value("${app.blockchain.rpc-url}") String rpcUrl,
        @Value("${app.blockchain.chain-id:31337}") long expectedChainId) {
        this.mongoTemplate = mongoTemplate;
        this.rpcUrl = rpcUrl;
        this.expectedChainId = expectedChainId;
    }

    public Map<String, Object> database() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("service", "mongodb");
        result.put("timestamp", Instant.now().toString());
        try {
            String ping = mongoTemplate.getDb().runCommand(new org.bson.Document("ping", 1))
                .get("ok").toString();
            result.put("status", "1.0".equals(ping) || "1".equals(ping) ? "UP" : "DOWN");
            result.put("ping", ping);
            result.put("database", mongoTemplate.getDb().getName());
        } catch (Exception ex) {
            result.put("status", "DOWN");
            result.put("error", ex.getClass().getSimpleName());
        }
        return result;
    }

    public Map<String, Object> blockchain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("service", "ethereum-rpc");
        result.put("timestamp", Instant.now().toString());
        Web3j web3j = Web3j.build(new HttpService(rpcUrl));
        try {
            var version = web3j.web3ClientVersion().send();
            var chain = web3j.ethChainId().send();
            var block = web3j.ethBlockNumber().send();
            long actualChainId = chain.getChainId().longValueExact();
            result.put("status", "UP");
            result.put("clientVersion", version.getWeb3ClientVersion());
            result.put("chainId", actualChainId);
            result.put("expectedChainId", expectedChainId);
            result.put("chainIdMatches", actualChainId == expectedChainId);
            result.put("latestBlock", block.getBlockNumber().longValueExact());
        } catch (Exception ex) {
            result.put("status", "DOWN");
            result.put("error", ex.getClass().getSimpleName());
        } finally {
            web3j.shutdown();
        }
        return result;
    }
}
