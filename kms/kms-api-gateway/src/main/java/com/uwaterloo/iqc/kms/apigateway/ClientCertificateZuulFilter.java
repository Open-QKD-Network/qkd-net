package com.uwaterloo.iqc.kms.apigateway;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.cert.X509Certificate;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

@Component
public class ClientCertificateZuulFilter extends ZuulFilter {

    private static final Logger logger = LoggerFactory.getLogger(ClientCertificateZuulFilter.class);

    public ClientCertificateZuulFilter() {
        logger.info("ClientCertificateZuulFilter CREATED");
    }

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        // Execute before RibbonRoutingFilter (order 10)
        return 6;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {

        RequestContext ctx = RequestContext.getCurrentContext();

        HttpServletRequest request = ctx.getRequest();

        try {
            logger.info("STEP 1: Zuul filter executing");
            X509Certificate[] certs =
                (X509Certificate[]) request.getAttribute(
                    "javax.servlet.request.X509Certificate"
                );

            if (certs == null || certs.length == 0) {
                logger.info("ERROR: Client certificate not found");
                reject(ctx, "Client certificate not found");
                return null;
            }

            // Get CN from the validated client certificate
            String clientCN = getClientCN(certs[0]);

            logger.info("CN in client certificate " + clientCN);
            // Add authenticated client identity to Zuul request
            // This replaces any incoming header with the same name.
            ctx.addZuulRequestHeader("X-Client-CN", clientCN);

            return null;

        } catch (Exception e) {
            reject(ctx, "Invalid client certificate");
            return null;
        }
    }

    private String getClientCN(X509Certificate cert)
            throws Exception {

        String subject =
            cert.getSubjectX500Principal().getName();

	logger.info("SUBJECT:" + subject);
        LdapName ldapName = new LdapName(subject);

        for (Rdn rdn : ldapName.getRdns()) {
	    logger.info("RDN Type:" + rdn.getType());
	    logger.info("RDN Value:" + rdn.getValue());

            if ("CN".equalsIgnoreCase(rdn.getType())) {
                return String.valueOf(rdn.getValue());
            }
        }

        throw new Exception("CN not found in client certificate");
    }

    private void reject(RequestContext ctx, String message) {

        ctx.setSendZuulResponse(false);
        ctx.setResponseStatusCode(
            HttpServletResponse.SC_UNAUTHORIZED
        );
        ctx.setResponseBody(message);
        ctx.getResponse().setContentType("text/plain");
    }
}
