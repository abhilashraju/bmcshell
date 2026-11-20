package com.ibm.bmcshell.redfish.validator;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates HTTP transport and protocol level requirements according to DSP0266 (Redfish Specification).
 */
public class RedfishProtocolValidator {

    public static List<ValidationIssue> validateResponse(String uri, HttpStatusCode statusCode, HttpHeaders headers) {
        List<ValidationIssue> issues = new ArrayList<>();

        if (statusCode == null) {
            issues.add(ValidationIssue.fail("HTTP_PROTOCOL", "", "No HTTP response status code received"));
            return issues;
        }

        int code = statusCode.value();

        // 1. Status code validation
        if (code != 200 && code != 204) {
            issues.add(ValidationIssue.fail("HTTP_STATUS", "",
                    "Unexpected HTTP status code for GET request", "200 OK", String.valueOf(code)));
        }

        if (headers == null) {
            return issues;
        }

        // 2. Content-Type Header
        String contentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);
        if (contentType == null) {
            issues.add(ValidationIssue.fail("HTTP_HEADER", "Content-Type", "Missing Content-Type header in response"));
        } else {
            String cleanContentType = contentType.toLowerCase().split(";")[0].trim();
            if (!cleanContentType.equals("application/json")) {
                issues.add(ValidationIssue.fail("HTTP_HEADER", "Content-Type",
                        "Content-Type must be 'application/json'", "application/json", contentType));
            }
        }

        // 3. OData-Version Header (DSP0266 specifies OData-Version must be 4.0)
        String odataVersion = headers.getFirst("OData-Version");
        if (odataVersion == null) {
            issues.add(ValidationIssue.warn("ODATA_HEADER", "OData-Version",
                    "Missing 'OData-Version' response header. DSP0266 recommends 'OData-Version: 4.0'"));
        } else if (!odataVersion.startsWith("4.0")) {
            issues.add(ValidationIssue.fail("ODATA_HEADER", "OData-Version",
                    "Invalid OData-Version header value", "4.0", odataVersion));
        }

        // 4. Link Header check (RFC 5988 describedby link)
        String linkHeader = headers.getFirst("Link");
        if (linkHeader != null && !linkHeader.contains("rel=describedby") && !linkHeader.contains("rel=\"describedby\"")) {
            issues.add(ValidationIssue.warn("ODATA_HEADER", "Link",
                    "Link header does not contain expected rel=describedby relation"));
        }

        return issues;
    }
}
