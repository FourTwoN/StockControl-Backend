package com.fortytwo.demeter.common.auth;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.jwt.JsonWebToken;
import java.util.Set;

@RequestScoped
public class CurrentUser {

    @Inject
    JsonWebToken jwt;

    @Inject
    SecurityIdentity identity;

    public String getUserId() {
        try {
            String subject = jwt.getSubject();
            if (subject != null) return subject;
        } catch (Exception ignored) {}
        return identity.getPrincipal().getName();
    }

    public String getEmail() {
        try {
            String email = jwt.getClaim("email");
            if (email != null) return email;
        } catch (Exception ignored) {}
        return identity.getPrincipal().getName() + "@test.local";
    }

    public String getTenantId() {
        try {
            return jwt.getClaim("tenant_id");
        } catch (Exception ignored) {}
        return null;
    }

    public Set<String> getRoles() {
        try {
            Set<String> groups = jwt.getGroups();
            if (groups != null && !groups.isEmpty()) return groups;
        } catch (Exception ignored) {}
        return identity.getRoles();
    }

    public String getName() {
        try {
            String name = jwt.getClaim("name");
            if (name != null) return name;
        } catch (Exception ignored) {}
        return identity.getPrincipal().getName();
    }
}
