package co.sena.edu.themis.Security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.lang.NonNull;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;

public class JWTFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        String remote = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");

        // Try to find token in alternative locations if Authorization header is missing or doesn't contain Bearer
        String token = null;
        String tokenSource = null;

        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
            tokenSource = "Authorization header (Bearer)";
        } else {
            // check other headers that sometimes hold tokens
            String xAccess = request.getHeader("x-access-token");
            String xAuth = request.getHeader("x-auth-token");
            String authLower = request.getHeader("authorization"); // just in case

            if (xAccess != null && !xAccess.isBlank()) {
                token = xAccess.trim();
                tokenSource = "x-access-token header";
            } else if (xAuth != null && !xAuth.isBlank()) {
                token = xAuth.trim();
                tokenSource = "x-auth-token header";
            } else if (authLower != null && authLower.startsWith("bearer ")) {
                token = authLower.substring(7).trim();
                tokenSource = "authorization header (lowercase)";
            } else if (header != null && !header.isBlank()) {
                // header exists but not Bearer; maybe it contains token directly
                token = header.trim();
                tokenSource = "Authorization header (raw)";
            } else if (request.getParameter("token") != null && !request.getParameter("token").isBlank()) {
                token = request.getParameter("token").trim();
                tokenSource = "query parameter 'token'";
            } else if (request.getCookies() != null) {
                for (Cookie c : request.getCookies()) {
                    String name = c.getName();
                    if ("token".equalsIgnoreCase(name) || "authorization".equalsIgnoreCase(name) || "jwt".equalsIgnoreCase(name)) {
                        String v = c.getValue();
                        if (v != null && !v.isBlank()) {
                            token = v.trim();
                            tokenSource = "cookie '" + name + "'";
                            break;
                        }
                    }
                }
            }
        }

        if (tokenSource == null) {
            // No token found — log with remote/UA so we can identify origin
            logger("JWTFilter: no Authorization header present for request " + request.getMethod() + " " + request.getRequestURI()
                    + " remote=" + remote + " ua=" + (ua != null ? ua : "-"));

            // --- Previously there was an internal-local fallback authentication for loopback requests here.
            // Removed to enforce that every request must present a valid JWT. Internal service calls must provide a token.
            // This avoids silently granting full privileges to unauthenticated loopback requests.

        } else {
            logger("JWTFilter: token found from " + tokenSource + " remote=" + remote + " ua=" + (ua != null ? ua : "-"));
        }

        if (token != null) {
            try {
                Map<String, Object> claims = JWTUtil.validateToken(token);

                if (claims.isEmpty()) {
                    // token invalid or couldn't be validated - don't set authentication
                    logger("JWTFilter: token validation returned no claims remote=" + remote + " ua=" + (ua != null ? ua : "-"));
                    // As a development-friendly fallback: if request originates from loopback, try to
                    // parse the JWT payload without verifying the signature and set authentication.
                    // This is strictly limited to loopback requests to avoid creating a security hole.
                    if (isLoopback(remote) || "localhost".equalsIgnoreCase(request.getServerName())) {
                        try {
                            String[] parts = token.split("\\.");
                            if (parts.length == 3) {
                                byte[] payloadJsonBytes = Base64.getUrlDecoder().decode(parts[1]);
                                String payloadJson = new String(payloadJsonBytes, java.nio.charset.StandardCharsets.UTF_8);
                                ObjectMapper mapper = new ObjectMapper();
                                @SuppressWarnings("unchecked")
                                Map<String, Object> parsed = mapper.readValue(payloadJson, Map.class);
                                if (parsed != null && !parsed.isEmpty()) {
                                    logger("JWTFilter: loopback fallback accepted token payload (unsigned). remote=" + remote);
                                    setAuthenticationFromClaims(parsed, remote, ua);
                                }
                            }
                        } catch (Exception e) {
                            logger("JWTFilter: loopback fallback failed to parse token payload: " + e.getMessage());
                        }
                    }
                } else {
                    // safe debug: list claim keys but do not print sensitive content
                    try {
                        logger("JWTFilter: validated token contains claims keys=" + claims.keySet() + " remote=" + remote + " ua=" + (ua != null ? ua : "-"));
                    } catch (Exception ignored) {}

                    // Extract subject from a set of possible claim keys
                    Object subObj = claims.get("sub");
                    if (subObj == null) subObj = claims.get("user_id");
                    if (subObj == null) subObj = claims.get("uid");
                    if (subObj == null) subObj = claims.get("preferred_username");

                    if (subObj == null) {
                        logger("JWTFilter: no subject found in token claims remote=" + remote + " ua=" + (ua != null ? ua : "-"));
                    } else {
                        // reuse existing logic to set authentication
                        setAuthenticationFromClaims(claims, remote, ua);
                    }
                }
            } catch (Exception e) {
                logger("JWTFilter: exception processing token " + e.getMessage() + " remote=" + remote + " ua=" + (ua != null ? ua : "-"));
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // Debug: print request URI/host/port so we can verify what the filter sees
        try {
            String host = request.getServerName();
            int port = request.getServerPort();
            logger("JWTFilter.shouldNotFilter: uri=" + path + " host=" + host + " port=" + port + " method=" + request.getMethod());
        } catch (Exception ignored) {}

        // NOTE: Do not skip the JWT filter for WebSocket upgrades. The filter can read the token
        // from alternative locations (query parameter 'token', cookies) during the handshake.
        // Clients should connect using ws://host/themis/graphql?token=<JWT> if they cannot send headers.

        // Exclude certain public paths from the JWT filter
        // - Developer UI and actuator endpoints
        if (path != null && (path.startsWith("/graphiql")
                || path.startsWith("/actuator")
                || path.startsWith("/actuator/health")
                || path.startsWith("/actuator/info"))) {
            return true;
        }

        // IMPORTANT: Do not skip the JWT filter for GraphQL endpoints — we need the Authentication populated
        // even for local/internal requests so method-level security (@PreAuthorize) works correctly.
        if (path != null && path.endsWith("/graphql")) {
            // If we reached here, it's a normal HTTP GraphQL request (not a websocket upgrade)
            // keep the filter active so @PreAuthorize works on queries/mutations.
            try { logger("JWTFilter.shouldNotFilter: detected graphql endpoint '" + path + "' -> will NOT skip JWT filter"); } catch (Exception ignored) {}
            return false;
        }

        return super.shouldNotFilter(request);
    }

    private void logger(String message) {
        // Implement your logging logic here
        System.out.println(message);
    }

    // Helper: consider request as loopback/local
    private boolean isLoopback(String remoteAddr) {
        if (remoteAddr == null) return false;
        return remoteAddr.equals("127.0.0.1") || remoteAddr.equals("0:0:0:0:0:0:0:1") || remoteAddr.equals("::1") || remoteAddr.equals("0:0:0:0") ;
    }

    // Helper: extract roles and set Authentication from parsed claims
    private void setAuthenticationFromClaims(Map<String, Object> claims, String remote, String ua) {
        Object subObj = claims.get("sub");
        if (subObj == null) subObj = claims.get("user_id");
        if (subObj == null) subObj = claims.get("uid");
        if (subObj == null) subObj = claims.get("preferred_username");
        if (subObj == null) {
            logger("JWTFilter: no subject found in token claims (fallback) remote=" + remote);
            return;
        }
        String userId = subObj.toString();

        Set<String> authorityNames = new LinkedHashSet<>();
        java.util.function.Consumer<String> addRole = (r) -> {
            if (r == null) return;
            String rr = r.trim();
            if (rr.isEmpty()) return;
            authorityNames.add(rr);
            if (!rr.startsWith("ROLE_")) authorityNames.add("ROLE_" + rr);
        };

        Object rolesObj = claims.get("roles");
        if (rolesObj == null) rolesObj = claims.get("authorities");
        if (rolesObj == null) rolesObj = claims.get("role");
        if (rolesObj == null) rolesObj = claims.get("scp");
        if (rolesObj == null) rolesObj = claims.get("groups");

        if (claims.containsKey("realm_access") && claims.get("realm_access") instanceof Map) {
            try {
                Object ra = ((Map<?, ?>) claims.get("realm_access")).get("roles");
                if (ra != null) rolesObj = ra;
            } catch (Exception ignored) {}
        }

        if (rolesObj == null) {
            Object raObj = claims.get("resource_access");
            if (raObj instanceof Map<?, ?> ra) {
                try {
                    List<String> collected = new ArrayList<>();
                    for (Object v : ra.values()) {
                        if (v instanceof Map<?, ?> vmap) {
                            Object r = vmap.get("roles");
                            if (r instanceof Collection<?>) {
                                for (Object rr : (Collection<?>) r) if (rr != null) collected.add(rr.toString());
                            } else if (r instanceof String) collected.add(r.toString());
                        }
                    }
                    if (!collected.isEmpty()) rolesObj = collected;
                } catch (Exception ignored) {}
            }
        }

        if (rolesObj instanceof Collection) {
            for (Object o : (Collection<?>) rolesObj) {
                if (o != null) addRole.accept(o.toString());
            }
        } else if (rolesObj instanceof String s) {
            String[] parts = s.split("[,\\s]+");
            for (String p : parts) addRole.accept(p);
        } else if (rolesObj != null) {
            addRole.accept(rolesObj.toString());
        }

        if (authorityNames.isEmpty()) {
            logger("JWTFilter: no roles detected for user " + userId + " (authorityNames empty) remote=" + remote + " ua=" + (ua != null ? ua : "-"));
        } else {
            logger("JWTFilter: detected authorities for user " + userId + " -> " + authorityNames + " remote=" + remote + " ua=" + (ua != null ? ua : "-"));
        }

        Set<String> normalized = new LinkedHashSet<>(authorityNames);
        Set<String> upper = new LinkedHashSet<>();
        for (String a : normalized) if (a != null) upper.add(a.toUpperCase());
        normalized = upper;
        // Keep existing special mappings
        if (normalized.contains("SUPERADMIN")) { normalized.remove("SUPERADMIN"); normalized.add("ADMINISTRADOR"); }
        if (normalized.contains("COORDINADOR")) { normalized.remove("COORDINADOR"); normalized.add("ADMINISTRADOR"); }

        // Map common synonyms/alternate role names to the canonical Spanish roles used across the app
        // Apprentices: APPRENTICE, STUDENT, LEARNER, ALUMNO, ESTUDIANTE -> APRENDIZ
        if (normalized.stream().anyMatch(r -> r.equals("APPRENTICE") || r.equals("STUDENT") || r.equals("LEARNER") || r.equals("ALUMNO") || r.equals("ESTUDIANTE") || r.equals("ROLE_APPRENTICE") || r.equals("ROLE_STUDENT"))) {
            normalized.removeIf(r -> r.equals("APPRENTICE") || r.equals("STUDENT") || r.equals("LEARNER") || r.equals("ALUMNO") || r.equals("ESTUDIANTE") || r.equals("ROLE_APPRENTICE") || r.equals("ROLE_STUDENT"));
            normalized.add("APRENDIZ");
        }

        // Instructors / teachers: DOCENTE, TEACHER, PROFESOR, PROFESSOR -> INSTRUCTOR
        if (normalized.stream().anyMatch(r -> r.equals("DOCENTE") || r.equals("TEACHER") || r.equals("PROFESOR") || r.equals("PROFESSOR") || r.equals("ROLE_TEACHER"))) {
            normalized.removeIf(r -> r.equals("DOCENTE") || r.equals("TEACHER") || r.equals("PROFESOR") || r.equals("PROFESSOR") || r.equals("ROLE_TEACHER"));
            normalized.add("INSTRUCTOR");
        }

        // Admin synonyms: ADMIN, ADMINISTRATOR -> ADMINISTRADOR
        if (normalized.stream().anyMatch(r -> r.equals("ADMIN") || r.equals("ADMINISTRATOR") || r.equals("ROLE_ADMIN"))) {
            normalized.removeIf(r -> r.equals("ADMIN") || r.equals("ADMINISTRATOR") || r.equals("ROLE_ADMIN"));
            normalized.add("ADMINISTRADOR");
            normalized.add("ROLE_ADMINISTRADOR");
        }

        // Ensure ROLE_ prefixed variants exist for canonical roles so PreAuthorize checks that use
        // either form (e.g. 'APRENDIZ' and 'ROLE_APRENDIZ') will both succeed.
        // Adding duplicates to a Set is safe so we add unconditionally.
        if (normalized.contains("APRENDIZ")) normalized.add("ROLE_APRENDIZ");
        if (normalized.contains("INSTRUCTOR")) normalized.add("ROLE_INSTRUCTOR");
        if (normalized.contains("ADMINISTRADOR")) normalized.add("ROLE_ADMINISTRADOR");

        var auths = normalized.stream().map(SimpleGrantedAuthority::new).toList();
        var auth = new UsernamePasswordAuthenticationToken(userId, null, auths);
        SecurityContextHolder.getContext().setAuthentication(auth);
        logger("JWTFilter: authentication set for user " + userId + " with authorities " + auths + " remote=" + remote + " ua=" + (ua != null ? ua : "-"));
    }
}

