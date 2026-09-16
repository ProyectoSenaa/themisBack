package co.sena.edu.themis.Security;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;

@Component
public class JWTUtil {
    // MISMO secret que Cerberos para validar tokens
    private static final Logger logger = LoggerFactory.getLogger(JWTUtil.class);


    private static String SECRET;

    @Value("${jwt.secret:}")
    private String injectedSecret;

    @PostConstruct
    private void initSecret() {
        String secret = injectedSecret == null ? "" : injectedSecret;
        JWTUtil.SECRET = secret;
        try {
            byte[] keyBytes;
            try {
                keyBytes = Decoders.BASE64.decode(secret);
            } catch (Exception e) {
                // not valid base64 - use raw UTF-8 bytes
                logger.debug("jwt.secret is not valid Base64, using raw UTF-8 bytes");
                keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            }

            // Ensure at least 32 bytes for HMAC-SHA256
            if (keyBytes.length < 32) {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                keyBytes = md.digest(keyBytes);
            }

            JWTUtil.key = Keys.hmacShaKeyFor(keyBytes);
            JWTUtil.rawKeyBytes = JWTUtil.key.getEncoded();
        } catch (Exception ex) {
            logger.error("Failed to initialize JWT key from secret: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Invalid jwt.secret configuration", ex);
        }
    }

    private static SecretKey key;
    private static byte[] rawKeyBytes;

    public static Map<String, Object> validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) return Map.of();

            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                logger.warn("Invalid JWT structure");
                return Map.of();
            }

            String headerB64 = parts[0];
            String payloadB64 = parts[1];
            String signatureB64 = parts[2];

            Base64.Decoder urlDecoder = Base64.getUrlDecoder();

            // Parse header JSON for diagnostics (non-fatal)
            Map<String, Object> header = null;
            try {
                byte[] headerJsonBytes = urlDecoder.decode(headerB64);
                String headerJson = new String(headerJsonBytes, StandardCharsets.UTF_8);
                ObjectMapper headerMapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> parsedHeader = headerMapper.readValue(headerJson, Map.class);
                header = parsedHeader;
            } catch (Exception ex) {
                logger.debug("Failed to parse JWT header for diagnostics: {}", ex.getMessage());
            }

            // Debug/testing: allow skipping signature verification when explicit environment flag is set.
            // WARNING: this is insecure and must be used ONLY for local debugging/testing environments.
            String acceptUnsigned = System.getenv("JWT_ACCEPT_UNSIGNED");
            boolean allowUnsigned = acceptUnsigned != null && (acceptUnsigned.equalsIgnoreCase("1") || acceptUnsigned.equalsIgnoreCase("true"));
            if (allowUnsigned) {
                try {
                    byte[] payloadJsonBytes = urlDecoder.decode(payloadB64);
                    String payloadJson = new String(payloadJsonBytes, StandardCharsets.UTF_8);
                    ObjectMapper mapper = new ObjectMapper();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> claims = mapper.readValue(payloadJson, Map.class);
                    if (claims == null) claims = Map.of();
                    logger.warn("JWT signature verification skipped due to JWT_ACCEPT_UNSIGNED=true. Using claimsKeys={}", claims.keySet());
                    // optional exp check even in unsigned mode
                    if (claims != null && claims.containsKey("exp")) {
                        try {
                            long exp = ((Number) claims.get("exp")).longValue();
                            long nowSec = System.currentTimeMillis() / 1000L;
                            if (exp < nowSec) {
                                logger.warn("JWT token expired (exp={})", exp);
                                return Map.of();
                            }
                        } catch (Exception ignored) {}
                    }
                    return claims;
                } catch (Exception e) {
                    logger.warn("Failed to parse payload in unsigned-accept mode: {}", e.getMessage());
                    // continue to normal validation flow
                }
            }

            byte[] signingInput = (headerB64 + "." + payloadB64).getBytes(StandardCharsets.US_ASCII);
            byte[] sigBytes;
            try {
                sigBytes = urlDecoder.decode(signatureB64);
            } catch (Exception e) {
                logger.warn("Failed to decode JWT signature: {}", e.getMessage());
                return Map.of();
            }

            // Try multiple reasonable key derivations to maximize compatibility with different ways the secret may be provided
            java.util.List<byte[]> candidateKeys = new java.util.ArrayList<>();
            if (SECRET != null) {
                // try Base64-decoded form
                try {
                    byte[] b = Decoders.BASE64.decode(SECRET);
                    candidateKeys.add(b);
                } catch (Exception ignored) {}

                // try Base64 URL-safe decoding (some services use URL-safe base64)
                try {
                    byte[] b = Base64.getUrlDecoder().decode(SECRET);
                    candidateKeys.add(b);
                } catch (Exception ignored) {}

                // try hex decoding (secret provided as hex string)
                try {
                    byte[] b = hexToBytes(SECRET);
                    candidateKeys.add(b);
                } catch (Exception ignored) {}

                // raw UTF-8 bytes
                try {
                    candidateKeys.add(SECRET.getBytes(StandardCharsets.UTF_8));
                } catch (Exception ignored) {}
            }

            // also include the previously computed rawKeyBytes if available
            if (rawKeyBytes != null) candidateKeys.add(rawKeyBytes);

            // expand candidates by including SHA-256 of each (some libraries hash shorter secrets)
            java.util.List<byte[]> expanded = new java.util.ArrayList<>(candidateKeys);
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                for (byte[] k : candidateKeys) {
                    if (k != null && k.length > 0) {
                        byte[] h = md.digest(k);
                        expanded.add(h);
                    }
                }
            } catch (Exception ignored) {}

            // remove duplicates (by hex)
            java.util.Map<String, byte[]> uniq = new java.util.LinkedHashMap<>();
            for (byte[] k : expanded) {
                if (k == null) continue;
                String hex = bytesToHex(k);
                if (!uniq.containsKey(hex)) uniq.put(hex, k);
            }

            boolean match = false;
            for (byte[] candidate : uniq.values()) {
                try {
                    // debug: log candidate length only (do not log key material)
                    logger.debug("JWTUtil: trying candidate key of length {} bytes", candidate == null ? 0 : candidate.length);
                    Mac mac = Mac.getInstance("HmacSHA256");
                    SecretKeySpec signingKey = new SecretKeySpec(candidate, "HmacSHA256");
                    mac.init(signingKey);
                    byte[] expected = mac.doFinal(signingInput);
                    if (constantTimeArrayEquals(expected, sigBytes)) { match = true; break; }
                } catch (Exception ignored) {}
            }

            if (!match) {
                // Signature mismatch: log safe diagnostics (alg header and payload keys) to help debug without revealing secrets
                try {
                    String alg = header != null && header.get("alg") != null ? header.get("alg").toString() : "(unknown)";
                    // Decode payload to inspect claim keys
                    Map<String, Object> tmpClaims = Map.of();
                    try {
                        byte[] payloadJsonBytes = urlDecoder.decode(payloadB64);
                        String payloadJson = new String(payloadJsonBytes, StandardCharsets.UTF_8);
                        ObjectMapper mapper = new ObjectMapper();
                        @SuppressWarnings("unchecked")
                        Map<String, Object> parsedClaims = mapper.readValue(payloadJson, Map.class);
                        tmpClaims = parsedClaims != null ? parsedClaims : Map.of();
                    } catch (Exception ignored) {}

                    logger.warn("JWT signature mismatch. header.alg={}, payloadKeys={}", alg, tmpClaims.keySet());
                } catch (Exception ignored) {
                    logger.warn("JWT signature mismatch (no extra diagnostics)");
                }
                return Map.of();
            }

            // decode payload
            byte[] payloadJsonBytes = urlDecoder.decode(payloadB64);
            String payloadJson = new String(payloadJsonBytes, StandardCharsets.UTF_8);

            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = mapper.readValue(payloadJson, Map.class);
            if (claims == null) claims = Map.of();

            // optional: verify exp claim
            if (claims.containsKey("exp")) {
                try {
                    long exp = ((Number) claims.get("exp")).longValue();
                    long nowSec = System.currentTimeMillis() / 1000L;
                    if (exp < nowSec) {
                        logger.warn("JWT token expired (exp={})", exp);
                        return Map.of();
                    }
                } catch (Exception ignored) {}
            }

            return claims;
        } catch (Exception e) {
            logger.warn("Token validation failed: {}", e.getMessage());
            return Map.of();
        }
    }

    private static boolean constantTimeArrayEquals(byte[] a, byte[] b) {
        if (a == null || b == null) return false;
        if (a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    // Helper: convert bytes to uppercase hex string (equivalent to DatatypeConverter.printHexBinary)
    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) return null;
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    // Helper: parse hex string (both upper and lower case) into bytes
    private static byte[] hexToBytes(String hex) {
        if (hex == null) return null;
        String s = hex.trim();
        if (s.length() % 2 != 0) throw new IllegalArgumentException("Invalid hex length");
        int len = s.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(s.charAt(i), 16);
            int lo = Character.digit(s.charAt(i+1), 16);
            if (hi == -1 || lo == -1) throw new IllegalArgumentException("Invalid hex char");
            out[i/2] = (byte) ((hi << 4) + lo);
        }
        return out;
    }
}