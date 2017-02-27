/*
 * Copyright 2017 Juraci Paixão Kröhling
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.developers.msa.ola.tracing;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;

/**
 * This is a Spring Boot filter, responsible for starting and finishing spans, as well as adding the span
 * to the request as an attribute. This means that every request will become a span.
 *
 * On a real world application, this would serve to create a "base" span for the request. Business spans would
 * then retrieve the span from the request attribute, using it as the parent span.
 *
 * This class might eventually become obsolete, once a Spring Boot integration library is available.
 *
 * @author Juraci Paixão Kröhling
 */
@SuppressWarnings("Duplicates")
@Configuration
public class OpenTracingConfiguration {

    @Bean
    public FilterRegistrationBean getOpenTracingFilter() {
        return new FilterRegistrationBean(new Filter() {
            @Override public void init(FilterConfig filterConfig) throws ServletException {
            }

            @Override public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
                Tracer tracer = TracerResolver.getTracer();
                Span requestSpan = null;
                if (servletRequest instanceof HttpServletRequest) {
                    HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
                    SpanContext spanContext = tracer.extract(Format.Builtin.HTTP_HEADERS, new HttpHeadersExtractAdapter(httpServletRequest));
                    requestSpan = tracer.buildSpan(httpServletRequest.getMethod())
                            .asChildOf(spanContext)
                            .start();
                    Tags.HTTP_URL.set(requestSpan, httpServletRequest.getRequestURI());
                    servletRequest.setAttribute("tracing.requestSpan", requestSpan);
                }

                filterChain.doFilter(servletRequest, servletResponse);

                if (null != requestSpan) {
                    if (servletResponse instanceof HttpServletResponse) {
                        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
                        Tags.HTTP_STATUS.set(requestSpan, httpServletResponse.getStatus());
                        requestSpan.finish();
                    }
                }
            }

            @Override public void destroy() {
            }
        });
    }

}
