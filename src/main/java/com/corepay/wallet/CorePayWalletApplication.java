package com.corepay.wallet;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@SpringBootApplication
public class CorePayWalletApplication {

    public static void main(String[] args) {
        SpringApplication.run(CorePayWalletApplication.class, args);
        String secret = "YourSuperSecureSecretKeyForCorePayEngineJwtValidation2026!";
        String token = Jwts.builder()
                .subject("system_admin_user")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000)) // 1 day
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
        System.out.println("TOKEN: " + token);
    }

}
