package com.uwaterloo.iqc.authservice.accounts;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
public class AccountConfiguration {

    @Bean
    UserDetailsService userDetailsService(AccountRepository accountRepository) {
        return username -> accountRepository
               .findByUsername(username)
        .map(account -> {
            boolean active = account.isActive();
            return new User(account.getUsername(), account.getPassword(), active,
            active, active, active, AuthorityUtils.createAuthorityList("ROLE_ADMIN",
            "ROLE_USER"));
        })
        .orElseThrow(
            () -> new UsernameNotFoundException(String.format("username %s not found!",
                                                username)));
    }
}
