package com.corepay.wallet.interceptor;
import io.grpc.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtServerInterceptor implements ServerInterceptor {

    // In production, load this securely from environment variables or Vault
    private static final String JWT_SECRET = "YourSuperSecureSecretKeyForCorePayEngineJwtValidation2026!";

    public static final Context.Key<String> CLIENT_ID_CONTEXT_KEY = Context.key("clientId");
    private static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall,
            Metadata metadata,
            ServerCallHandler<ReqT, RespT> next) {

        String authHeader = metadata.get(AUTHORIZATION_METADATA_KEY);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            serverCall.close(
                    Status.UNAUTHENTICATED.withDescription("Missing or invalid Authorization Bearer token metadata"),
                    metadata
            );
            return new ServerCall.Listener<>() {};
        }

        String token = authHeader.substring(7);
        try {
            SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            String clientId = claims.getSubject();

            // Inject client identity into gRPC Context
            Context context = Context.current().withValue(CLIENT_ID_CONTEXT_KEY, clientId);
            return Contexts.interceptCall(context, serverCall, metadata, next);

        } catch (JwtException e) {
            serverCall.close(
                    Status.PERMISSION_DENIED.withDescription("Token verification failed: " + e.getMessage()),
                    metadata
            );
            return new ServerCall.Listener<>() {};
        }
    }
}