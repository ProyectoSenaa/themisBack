 package co.sena.edu.themis.Security;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {
        try {
            // Buscar token en query param 'token'
            URI uri = request.getURI();
            String query = uri.getQuery();
            String token = null;
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] kv = param.split("=", 2);
                    if (kv.length == 2 && "token".equalsIgnoreCase(kv[0])) {
                        token = java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
                        break;
                    }
                }
            }

            // Si no está en query, intentar header Authorization
            if (token == null) {
                List<String> auth = request.getHeaders().getOrDefault("Authorization", List.of());
                if (!auth.isEmpty()) {
                    String header = auth.get(0);
                    if (header.startsWith("Bearer ")) token = header.substring(7);
                }
            }

            // Si aún no, buscar en cookies
            if (token == null) {
                List<String> cookies = request.getHeaders().getOrDefault("Cookie", List.of());
                if (!cookies.isEmpty()) {
                    String cookieHeader = cookies.get(0);
                    for (String c : cookieHeader.split(";")) {
                        String[] kv = c.trim().split("=", 2);
                        if (kv.length == 2 && ("token".equalsIgnoreCase(kv[0]) || "jwt".equalsIgnoreCase(kv[0]) || "authorization".equalsIgnoreCase(kv[0]))) {
                            token = kv[1];
                            break;
                        }
                    }
                }
            }

            if (token == null) {
                // No token found: do not allow handshake
                return true; // allow handshake but anonymous; JWTFilter will try to handle if applicable
            }

            // Validate token (using same JWTUtil used elsewhere)
            try {
                java.util.Map<String, Object> claims = JWTUtil.validateToken(token);
                if (claims != null && !claims.isEmpty()) {
                    Object sub = claims.get("sub");
                    if (sub == null) sub = claims.get("user_id");
                    if (sub == null) sub = claims.get("uid");
                    if (sub == null) sub = claims.get("preferred_username");
                    if (sub != null) {
                        attributes.put("ws-user", sub.toString());
                        // also put the raw token so other handlers can use it
                        attributes.put("ws-token", token);
                    }
                }
            } catch (Exception e) {
                // invalid token: allow handshake to continue anonymously; the app may reject subscriptions later
            }

        } catch (Exception e) {
            // keep handshake from failing due to interceptor errors
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}

