package com.redhat.developers.msa.ola;

import java.util.Collections;
import java.util.regex.Pattern;

import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Logger;
import feign.httpclient.ApacheHttpClient;
import feign.hystrix.HystrixFeign;
import feign.jackson.JacksonDecoder;
import feign.opentracing.TracingClient;
import feign.opentracing.hystrix.TracingConcurrencyStrategy;
import io.jaegertracing.tracerresolver.internal.JaegerTracerFactory;
import io.opentracing.NoopTracerFactory;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.autoconfig.WebTracingConfiguration;

/**
 * @author Pavol Loffay
 */
@Configuration
public class TracingConfiguration {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TracingConfiguration.class);

    @Bean
    public Tracer tracer() {
        String jaegerURL = System.getenv("JAEGER_SERVICE_NAME");
        if (jaegerURL != null) {
            log.info("Using Jaeger tracer");
            return jaegerTracer(jaegerURL);
        }

        log.info("Using Noop tracer");
        return NoopTracerFactory.create();
    }

    private Tracer jaegerTracer(String url) {
        return new JaegerTracerFactory().getTracer();
    }

    @Bean
    public WebTracingConfiguration webTracingConfiguration() {
        return WebTracingConfiguration.builder()
                .withSkipPattern(Pattern.compile("/api/health"))
                .build();
    }

    /**
     *
     * This is were the "magic" happens: it creates a Feign, which is a proxy
     * interface for remote calling a
     * REST endpoint with Hystrix fallback support.
     */
    @Bean
    public HolaService holaService(Tracer tracer) {
        // bind current span to Hystrix thread
        TracingConcurrencyStrategy.register();

        return HystrixFeign.builder()
                .client(new TracingClient(new ApacheHttpClient(HttpClientBuilder.create().build()), tracer))
                .logger(new Logger.ErrorLogger()).logLevel(Logger.Level.BASIC)
                .decoder(new JacksonDecoder())
                .target(HolaService.class, "http://hola:8080/",
                        () -> Collections.singletonList("Hola response (fallback)"));
    }
}
