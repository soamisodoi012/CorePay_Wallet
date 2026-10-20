package com.corepay.wallet;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@SpringBootApplication
public class CorePayWalletApplication {

    public static void main(String[] args) {
        SpringApplication.run(CorePayWalletApplication.class, args);
        String secret = "CorePayEngineJwtValidationSecretKey2026!!";
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .subject("system_admin_user")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000)) // 1 day
                .signWith(key)
                .compact();

        System.out.println("TOKEN: " + token);
    }
    }

