package com.uwaterloo.iqc.authservice.clients;

import java.util.Optional;

import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.stereotype.Service;

@Service
@Primary
public class ClientConfiguration implements ClientDetailsService {

    private final LoadBalancerClient loadBalancerClient;

    private final ClientRepository clientRepository;

    public ClientConfiguration(ClientRepository clientRepository, LoadBalancerClient client) {
        this.clientRepository = clientRepository;
        this.loadBalancerClient = client;
    }

    public ClientDetails loadClientByClientId(String clientId) {
        Optional<Client> oClient = 	clientRepository.findByClientId(clientId);
        Client client = oClient.get();

        BaseClientDetails details = new BaseClientDetails(client.getClientId(),
                null, client.getScopes(), client.getAuthorizedGrantTypes(), client
                .getAuthorizedGrantTypes());
        details.setClientSecret(client.getSecret());
        return details;
    }
}
