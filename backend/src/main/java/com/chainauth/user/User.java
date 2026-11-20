package com.chainauth.user;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.Set;

@Document("users")
public class User {
    @Id private String id;
    private String name;
    @Indexed(unique = true) private String email;
    private String passwordHash;
    private Set<Role> roles = Set.of(Role.USER);
    private boolean enabled = true;
    private long tokenVersion = 0;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    public String getId(){return id;} public void setId(String v){id=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;}
    public Set<Role> getRoles(){return roles;} public void setRoles(Set<Role> v){roles=v;}
    public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;}
    public long getTokenVersion(){return tokenVersion;} public void setTokenVersion(long v){tokenVersion=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}
