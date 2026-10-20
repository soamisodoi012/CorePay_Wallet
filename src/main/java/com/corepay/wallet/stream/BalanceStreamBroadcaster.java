package com.corepay.wallet.stream;

import com.corepay.wallet.grpc.BalanceUpdateEvent;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BalanceStreamBroadcaster {

    // Maps walletId -> Set of active client stream observers
    private final Map<String, Set<StreamObserver<BalanceUpdateEvent>>> observersMap = new ConcurrentHashMap<>();

    public void register(String walletId, StreamObserver<BalanceUpdateEvent> observer) {
        observersMap.computeIfAbsent(walletId, k -> ConcurrentHashMap.newKeySet()).add(observer);
    }

    public void unregister(String walletId, StreamObserver<BalanceUpdateEvent> observer) {
        Set<StreamObserver<BalanceUpdateEvent>> observers = observersMap.get(walletId);
        if (observers != null) {
            observers.remove(observer);
            if (observers.isEmpty()) {
                observersMap.remove(walletId);
            }
        }
    }

    public void broadcast(BalanceUpdateEvent event) {
        Set<StreamObserver<BalanceUpdateEvent>> observers = observersMap.get(event.getWalletId());
        if (observers != null) {
            for (StreamObserver<BalanceUpdateEvent> observer : observers) {
                try {
                    observer.onNext(event);
                } catch (Exception e) {
                    observers.remove(observer);
                }
            }
        }
    }
}
