/**
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.developers.msa.ola;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.redhat.developers.msa.ola.tracing.OlaHttpRequestInterceptor;
import com.redhat.developers.msa.ola.tracing.OlaHttpResponseInterceptor;
import com.redhat.developers.msa.ola.tracing.TracerResolver;

import feign.Logger;
import feign.Logger.Level;
import feign.httpclient.ApacheHttpClient;
import feign.hystrix.HystrixFeign;
import feign.jackson.JacksonDecoder;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api")
public class OlaController {
    private final Tracer tracer = TracerResolver.getTracer();

    @CrossOrigin
    @RequestMapping(method = RequestMethod.GET, value = "/ola", produces = "text/plain")
    @ApiOperation("Returns the greeting in Portuguese")
    public String ola() {
        String hostname = System.getenv().getOrDefault("HOSTNAME", "Unknown");
        return String.format("Olá de %s", hostname);
    }

    @CrossOrigin
    @RequestMapping(method = RequestMethod.GET, value = "/ola-chaining", produces = "application/json")
    @ApiOperation("Returns the greeting plus the next service in the chain")
    public List<String> sayHelloChaining() {
        List<String> greetings = new ArrayList<>();
        greetings.add(ola());
        greetings.addAll(getNextService().hola());
        return greetings;
    }

    @CrossOrigin
    @RequestMapping(method = RequestMethod.GET, value = "/ola-secured", produces = "text/plain")
    @ApiOperation("Returns a message that is only available for authenticated users")
    public String olaSecured(KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal) {
        AccessToken token = principal.getKeycloakSecurityContext().getToken();
        return "This is a Secured resource. You are logged as " + token.getName();
    }

    @CrossOrigin
    @RequestMapping(method = RequestMethod.GET, value = "/logout", produces = "text/plain")
    @ApiOperation("Logout")
    public String logout() throws ServletException {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        request.logout();
        return "Logged out";
    }

    @RequestMapping(method = RequestMethod.GET, value = "/health")
    @ApiOperation("Used to verify the health of the service")
    public String health() {
        return "I'm ok";
    }

    /**
     * This is were the "magic" happens: it creates a Feign, which is a proxy interface for remote calling a REST endpoint with
     * Hystrix fallback support.
     *
     * @return The feign pointing to the service URL and with Hystrix fallback.
     */
    private HolaService getNextService() {
        HttpServletRequest servletRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        Span parentSpan = (Span) servletRequest.getAttribute("tracing.requestSpan");

        final CloseableHttpClient httpclient;
        Span span = tracer.buildSpan("GET").asChildOf(parentSpan).start();
        httpclient = HttpClients.custom()
                .addInterceptorFirst(new OlaHttpRequestInterceptor(span))
                .addInterceptorFirst(new OlaHttpResponseInterceptor(span))
                .build();

        return HystrixFeign.builder()
                .logger(new Logger.ErrorLogger()).logLevel(Level.BASIC)
                .client(new ApacheHttpClient(httpclient))
                .decoder(new JacksonDecoder())
                .target(HolaService.class, holaServiceUrl(), () -> Collections.singletonList("Hola response (fallback)"));
    }

    /**
     * Returns the base URL for the Hola service.
     * @return  the base URL for the Hola Service
     */
    private static String holaServiceUrl() {
        // we have two possibilities here:
        // the first is to let OpenShift tell us passively what is the HOLA host and port, via the
        // HOLA_SERVICE_HOST / HOLA_SERVICE_PORT env vars. These are set by OpenShift for a service named `hola`, so,
        // we don't need to set it anywhere ourselves.
        // If we want to override that, or if we are not running on OpenShift, the HOLA_SERVER_URL can be used, which
        // overrides the OpenShift ones.

        String url = System.getenv("HOLA_SERVER_URL");
        if (null == url || url.isEmpty()) {
            String host = System.getenv("HOLA_SERVICE_HOST");
            String port = System.getenv("HOLA_SERVICE_PORT");
            if (null == host) {
                // at this point, we have no env vars at all, so, we default to "something".
                url = "http://hola:8080/";
            } else {
                url = String.format("http://%s:%s", host, port);
            }
        }
        return url;
    }
}
