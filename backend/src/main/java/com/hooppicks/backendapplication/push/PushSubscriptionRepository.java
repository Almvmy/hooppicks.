package com.hooppicks.backendapplication.push;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, String> {

    List<PushSubscription> findByUserId(String userId);

    Optional<PushSubscription> findByEndpoint(String endpoint);

    @Transactional
    long deleteByEndpointAndUserId(String endpoint, String userId);

    @Transactional
    void deleteByUserId(String userId);
}
