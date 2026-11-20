package com.chainauth.wallet;
import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Component;import java.time.Instant;import java.time.temporal.ChronoUnit;
@Component public class SiweMessageFactory{private final String domain,uri;private final long ttl;
public SiweMessageFactory(@Value("${app.wallet.siwe-domain}")String d,@Value("${app.wallet.siwe-uri}")String u,@Value("${app.wallet.nonce-ttl-seconds:300}")long t){domain=d;uri=u;ttl=t;}
public Instant expiry(Instant i){return i.plus(ttl,ChronoUnit.SECONDS);}
public String create(String a,long c,String n,Instant i,Instant e){return domain+" wants you to sign in with your Ethereum account:\n"+a+"\n\nSign in to ChainAuth with Ethereum.\n\nURI: "+uri+"\nVersion: 1\nChain ID: "+c+"\nNonce: "+n+"\nIssued At: "+i+"\nExpiration Time: "+e;}}
