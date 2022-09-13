package com.uwaterloo.iqc.qnl.qll;

import com.uwaterloo.iqc.qnl.RouteConfig;
import nl.altindag.ssl.util.PemUtils;
import nl.altindag.ssl.SSLFactory;
import okhttp3.OkHttpClient;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.KeyApi;
import org.openapitools.client.api.StatusApi;
import org.openapitools.client.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.*;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.net.ssl.X509ExtendedTrustManager;

public class QLLETSIReader implements QLLReader {

    private ApiClient client;
    private KeyApi api;

    private String otherSAEId;

    private Map<UUID, Key> keys;

    private static Logger LOGGER = LoggerFactory.getLogger(QLLETSIReader.class);

    private static InputStream loadResource(String res) throws IOException {
        File configFile = new File(res);
        if (!configFile.isAbsolute()) {
            String configLoc = System.getProperty("user.home") + "/.qkd/qnl";
            configFile = new File(configLoc, res);
        }
        return Files.newInputStream(configFile.toPath());
    }

    public static OkHttpClient loadClient(RouteConfig.KMEConfig config) throws IOException {
        SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder();

        // Set up the client identity
        X509ExtendedKeyManager keyManager = PemUtils.loadIdentityMaterial(
                loadResource(config.client_cert),
                loadResource(config.client_key),
                config.client_key_password != null ? config.client_key_password.toCharArray() : null);
        sslFactoryBuilder.withIdentityMaterial(keyManager);

        // If a root CA is configured, use that
        if (config.server_cert != null) {
            X509ExtendedTrustManager trustManager = PemUtils.loadTrustMaterial(loadResource(config.server_cert));
            sslFactoryBuilder.withTrustMaterial(trustManager);
        } else {
            sslFactoryBuilder.withSystemTrustMaterial();
        }
        SSLFactory sslFactory = sslFactoryBuilder.build();

        return new OkHttpClient.Builder()
                .sslSocketFactory(sslFactory.getSslSocketFactory(), sslFactory.getTrustManager().get())
                // Uncomment to disable hostname verification for debugging
                // .hostnameVerifier((hostname, session) -> true)
                .build();
    }

    public QLLETSIReader(RouteConfig.KMEConfig config) {
        this.client = new ApiClient();
        this.api = new KeyApi(this.client);
        this.keys = new HashMap<>();

        this.otherSAEId = config.remote_sae;

        client.setBasePath("https://" + config.address + "/api/v1");
        LOGGER.info("Connecting to API endpoint: {}", client.getBasePath());
        try {
            client.setHttpClient(loadClient(config));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // TODO: Use status info to set key size bounds etc.
        try {
            Status status = new StatusApi(this.client).getStatus(this.otherSAEId);
            LOGGER.info("ETSI server status:\n{}", status);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private Key getKey(int len) {
        KeyRequest request = new KeyRequest();
        request.setSize(len * 8);
        try {
            KeyContainer container = api.getKey(otherSAEId, request);
            LOGGER.info("Received keys:\n{}", container);
            return container.getKeys().get(0);
        } catch (ApiException e) {
            LOGGER.error("getKey failed", e);
            throw new RuntimeException(e);
        }
    }

    private Key getKeyById(String identifier) {
        KeyIds ids = new KeyIds();
        KeyInformationDto keyDTO = new KeyInformationDto();
        keyDTO.setKeyID(UUID.fromString(identifier));
        ids.addKeyIDsItem(keyDTO);
        try {
            KeyContainer container = api.getKeyWithKeyIds(otherSAEId, ids);
            LOGGER.info("Received keys by id:\nP{}", container);
            return container.getKeys().get(0);
        } catch (ApiException e) {
            LOGGER.error("getKeyWithKeyIds failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getNextKeyId(int len) {
        Key key = getKey(len);
        keys.put(key.getKeyID(), key);
        return key.getKeyID().toString();
    }

    @Override
    public String readNextKey(byte[] dst, int len) {
        Key key = getKey(len);
        byte[] keyData = Base64.getDecoder().decode(key.getKey());
        System.arraycopy(keyData, 0, dst, 0, len);
        return key.getKeyID().toString();
    }

    @Override
    public byte[] read(String identifier) {
        UUID keyUUID = UUID.fromString(identifier);
        if (keys.containsKey(keyUUID)) {
            return Base64.getDecoder().decode(keys.remove(keyUUID).getKey());
        }
        Key key = getKeyById(identifier);
        return Base64.getDecoder().decode(key.getKey());
    }
}
