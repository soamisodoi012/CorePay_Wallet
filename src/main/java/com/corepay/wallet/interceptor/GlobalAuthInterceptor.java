package com.corepay.wallet.interceptor;


import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;

@GrpcGlobalServerInterceptor
public class GlobalAuthInterceptor extends JwtServerInterceptor {
    // Inherits logic and registers globally via Spring Boot starter auto-configuration
}
