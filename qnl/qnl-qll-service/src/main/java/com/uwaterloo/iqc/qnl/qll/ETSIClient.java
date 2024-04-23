package com.uwaterloo.iqc.qnl.qll;

import org.springframework.http.MediaType;

/*
 * Expected API from ETSI Server
 * A calls POST http://localhost:3000/initQKD with an empty body
 * A receives a seed in the response.
 * B calls POST http://localhost:3001/initQKD with the seed in the body
 * A calls POST http://localhost:3000/api/v1/keys/nodeBID/enc_keys
 *  with body: {number: 3, size: 64}
 * 
 * B calls http://localhost:3001/api/v1/keys/nodeAID/dec_keys
 *  with body: {
 "ids": [
        {"keyID": "1:64"},
        {"keyID": "2:64"},
        {"keyID": "3:64"}
        ]
    }
 */

/**
 * An HTTP client that interacts with an ETSI server.
 */

import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

public class ETSIClient {
    // [@rahul temp]: should this be static?
    final WebClient client;
    
    public ETSIClient() {
        // [@rahul temp]: revisit this and fix the magic URL.
        this.client = WebClient.create("http://localhost:8000");
    }
    public static void main(String[] args) {
        System.out.println("Hello world. Rn ETSI Client doesn't do much.");
    }

    public Mono<Integer> initAndGetSeedFromA() {
        /**
         * We expect this method to be called at site A.
         */
        return this.client.post().uri("/initQKD")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(null)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(QLLResponse.class)
                .map(QLLResponse::getSeed);
    }
    public Mono<String> initAndGetConfirmationMsgfromB(int seed) {
        return this.client.post().uri("/initQKD")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new QLLRequest(seed))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(QLLResponse.class)
                .map(QLLResponse::getMessage);
    }

    public Mono<Key []> generateKeysAtA() {
        return this.client.post().uri("/api/v1/keys/nodeBID/enc_keys")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new KeyRequest(Optional.of(5), Optional.of(64)))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(KeyResponse.class)
                .map(KeyResponse::getKeys);

    }

    public Mono<KeyResponse> getKeysWithInfoFromB(KeyIDsContainer container) {
        return this.client.post().uri("/api/v1/keys/nodeAID/dec_keys")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(container)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(KeyResponse.class);
    }

    
}
