package com.hooppicks.backendapplication.push;

import com.hooppicks.backendapplication.push.PushController.SubscribeRequest;
import com.hooppicks.backendapplication.push.PushController.SubscriptionKeys;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PushControllerTest {

    private static SubscribeRequest request(String endpoint) {
        return new SubscribeRequest(endpoint, new SubscriptionKeys("p256dh", "auth"));
    }

    @Test
    void accepte_les_services_push_des_navigateurs() {
        assertThat(PushController.isValid(request("https://fcm.googleapis.com/fcm/send/abc"))).isTrue();
        assertThat(PushController.isValid(request("https://updates.push.services.mozilla.com/wpush/v2/abc"))).isTrue();
        assertThat(PushController.isValid(request("https://web.push.apple.com/QGx"))).isTrue();
        assertThat(PushController.isValid(request("https://wns2-par02p.notify.windows.com/w/?token=x"))).isTrue();
    }

    /** Le backend appelle l'endpoint : tout autre hôte permettrait de lui faire viser une adresse arbitraire. */
    @Test
    void refuse_tout_autre_hote_ou_protocole() {
        assertThat(PushController.isValid(request("https://169.254.169.254/latest/meta-data"))).isFalse();
        assertThat(PushController.isValid(request("https://localhost:3001/admin/sync"))).isFalse();
        assertThat(PushController.isValid(request("http://fcm.googleapis.com/fcm/send/abc"))).isFalse();
        assertThat(PushController.isValid(request("https://fcm.googleapis.com.evil.example/x"))).isFalse();
        assertThat(PushController.isValid(request("https://evilfcm.googleapis.com.example/x"))).isFalse();
    }

    @Test
    void refuse_une_requete_incomplete() {
        assertThat(PushController.isValid(null)).isFalse();
        assertThat(PushController.isValid(new SubscribeRequest("https://fcm.googleapis.com/x", null))).isFalse();
        assertThat(PushController.isValid(new SubscribeRequest(null, new SubscriptionKeys("a", "b")))).isFalse();
    }
}
