package com.ibm.bmcshell.rest;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "server.ssl")
public class SslProperties {

    private String keyStore;
    private String keyStorePassword;
    private String keyStoreType;
    private String keyAlias;

    // getters and setters...

    // rest of the class...
}
