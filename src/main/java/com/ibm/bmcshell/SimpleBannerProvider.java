package com.ibm.bmcshell;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SimpleBannerProvider  {
    public String getBanner() {
        StringBuffer buf = new StringBuffer();
        buf.append("=======================================")
                .append("\n");
        buf.append("*          BMC Shell             *")
                .append("\n");
        buf.append("=======================================")
                .append("\n");
        buf.append("Version:")
                .append(this.getVersion());
        return buf.toString();
    }

    public String getVersion() {
        return "1.0.1";
    }

    public String getWelcomeMessage() {
        return "Welcome to BMC CLI";
    }

    public String getProviderName() {
        return "BMC Banner";
    }
}
