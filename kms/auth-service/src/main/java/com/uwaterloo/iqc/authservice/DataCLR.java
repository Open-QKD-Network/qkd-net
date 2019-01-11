package com.uwaterloo.iqc.authservice;

import com.uwaterloo.iqc.authservice.accounts.Account;
import com.uwaterloo.iqc.authservice.accounts.AccountRepository;
import com.uwaterloo.iqc.authservice.clients.Client;
import com.uwaterloo.iqc.authservice.clients.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@Component
class DataCLR implements CommandLineRunner {

    private final AccountRepository accountRepository;

    private final ClientRepository clientRepository;

    @Autowired
    public DataCLR(AccountRepository accountRepository,
                   ClientRepository clientRepository) {
        this.accountRepository = accountRepository;
        this.clientRepository = clientRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        Stream
        .of("dsyer,cloud1", "pwebb,bot", "mminela,batch0", "rgwinch,security",
            "jlonog,solstice")
        .map(s -> s.split(","))
        .forEach(
            tuple -> accountRepository.save(new Account(tuple[0], tuple[1], true)));

        Stream.of("html5,password", "qtox,seachat", "android,secret").map(x -> x.split(","))
        .forEach(x -> clientRepository.save(new Client(x[0], x[1])));
    }
}