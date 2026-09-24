package com.coffeeshop.app.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SupabaseStorageClientTest {

    private static final String URL = "https://ref.supabase.co";

    @Test
    void upload_legacyJwtKey_sendsApikeyAndBearer() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SupabaseStorageClient client = new SupabaseStorageClient(builder, URL, "eyJ.legacy.key", "products");

        server.expect(requestTo(URL + "/storage/v1/object/products/a.png"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("apikey", "eyJ.legacy.key"))
                .andExpect(header("Authorization", "Bearer eyJ.legacy.key"))
                .andRespond(withSuccess());

        String url = client.upload("a.png", new byte[]{1}, "image/png");

        assertThat(url).isEqualTo(URL + "/storage/v1/object/public/products/a.png");
        server.verify();
    }

    @Test
    void upload_newSecretKey_sendsOnlyApikey() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SupabaseStorageClient client = new SupabaseStorageClient(builder, URL, "sb_secret_abc", "products");

        server.expect(requestTo(URL + "/storage/v1/object/products/a.png"))
                .andExpect(header("apikey", "sb_secret_abc"))
                .andExpect(headerDoesNotExist("Authorization"))
                .andRespond(withSuccess());

        client.upload("a.png", new byte[]{1}, "image/png");

        server.verify();
    }
}
