package com.ibm.bmcshell.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.annotation.PostConstruct;
import java.util.Map;

@Component
public class RequestMappingLogger {

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @PostConstruct
    public void logRequestMappings() {
        Map<RequestMappingInfo, HandlerMethod> map = requestMappingHandlerMapping.getHandlerMethods();
        map.forEach((key, value) -> {
            RequestMapping requestMapping = value.getMethodAnnotation(RequestMapping.class);
            if (requestMapping != null) {
                RequestMethod[] methods = requestMapping.method();
                String[] paths = requestMapping.value();
                for (RequestMethod method : methods) {
                    System.out.println("Mapped URL: " + String.join(", ", paths) + " Method: " + method);
                }

            }
        });
    }
}