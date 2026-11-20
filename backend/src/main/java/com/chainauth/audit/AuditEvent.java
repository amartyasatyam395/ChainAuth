package com.chainauth.audit;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document("security_audit_events")
public class AuditEvent {
    @Id private String id;
    @Indexed(unique = true) private long sequence;
    @Indexed private Instant occurredAt;
    private String actor;
    private String action;
    private String resource;
    private int status;
    private String outcome;
    private String sourceHash;
    private String previousHash;
    @Indexed private String eventHash;
    public String getId(){return id;} public void setId(String v){id=v;}
    public long getSequence(){return sequence;} public void setSequence(long v){sequence=v;}
    public Instant getOccurredAt(){return occurredAt;} public void setOccurredAt(Instant v){occurredAt=v;}
    public String getActor(){return actor;} public void setActor(String v){actor=v;}
    public String getAction(){return action;} public void setAction(String v){action=v;}
    public String getResource(){return resource;} public void setResource(String v){resource=v;}
    public int getStatus(){return status;} public void setStatus(int v){status=v;}
    public String getOutcome(){return outcome;} public void setOutcome(String v){outcome=v;}
    public String getSourceHash(){return sourceHash;} public void setSourceHash(String v){sourceHash=v;}
    public String getPreviousHash(){return previousHash;} public void setPreviousHash(String v){previousHash=v;}
    public String getEventHash(){return eventHash;} public void setEventHash(String v){eventHash=v;}
}
