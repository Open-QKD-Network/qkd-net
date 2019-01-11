package com.uwaterloo.iqc.kms.apigateway;

import java.security.Principal;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


//@Profile("secure")
@RestController
class PrincipalRestController {

    @RequestMapping("/user")
    Principal principal(Principal p) {
        return p;
    }
}
