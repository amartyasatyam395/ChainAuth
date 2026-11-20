package com.chainauth.audit;

import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Service
public class AuditService {
    private final AuditEventRepository events;
    public AuditService(AuditEventRepository events){this.events=events;}

    // Synchronized append is tamper-evident for a single backend instance. It is not WORM storage.
    public synchronized AuditEvent record(String actor,String action,String resource,int status,String source) {
        AuditEvent previous=events.findTopByOrderBySequenceDesc().orElse(null);
        AuditEvent event=new AuditEvent(); event.setSequence(previous==null?1:previous.getSequence()+1);
        event.setOccurredAt(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS)); event.setActor(safe(actor,160)); event.setAction(safe(action,80));
        event.setResource(safe(resource,240)); event.setStatus(status); event.setOutcome(status<400?"SUCCESS":"FAILURE");
        event.setSourceHash(sha256(source==null?"":source)); event.setPreviousHash(previous==null?"GENESIS":previous.getEventHash());
        event.setEventHash(hash(event)); return events.save(event);
    }
    public List<AuditEvent> recent(int limit){
        int size=Math.max(1,Math.min(limit,200));
        return events.findAll(org.springframework.data.domain.PageRequest.of(0,size,
            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,"sequence"))).getContent();
    }
    public boolean verify(int limit){
        int size=Math.max(1,Math.min(limit,5000));
        List<AuditEvent> page=new java.util.ArrayList<>(events.findAll(org.springframework.data.domain.PageRequest.of(0,size,
            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,"sequence"))).getContent());
        java.util.Collections.reverse(page);
        for(AuditEvent e:page){
            String expectedPrevious=e.getSequence()==1?"GENESIS":events.findBySequence(e.getSequence()-1).map(AuditEvent::getEventHash).orElse(null);
            if(!java.util.Objects.equals(expectedPrevious,e.getPreviousHash())||!hash(e).equals(e.getEventHash()))return false;
        }
        return true;
    }
    private static String hash(AuditEvent e){return sha256(e.getSequence()+"|"+e.getOccurredAt()+"|"+nullSafe(e.getActor())+"|"+nullSafe(e.getAction())+"|"+nullSafe(e.getResource())+"|"+e.getStatus()+"|"+nullSafe(e.getOutcome())+"|"+nullSafe(e.getSourceHash())+"|"+nullSafe(e.getPreviousHash()));}
    private static String sha256(String v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private static String safe(String v,int max){if(v==null)return "anonymous";String clean=v.replaceAll("[\\r\\n]","_");return clean.substring(0,Math.min(clean.length(),max));}
    private static String nullSafe(String v){return v==null?"":v;}
}
