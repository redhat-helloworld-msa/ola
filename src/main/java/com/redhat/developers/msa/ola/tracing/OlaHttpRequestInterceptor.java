/*
 * Copyright 2016 Juraci Paixão Kröhling
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

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;

/**
 * This is an interceptor to be used with the Apache HTTP client. This is for the request part,
 * which is used for injecting the span data into the outgoing HTTP request.
 *
 * On a real application, more data could be added to the span, like a business ID.
 *
 * Note that this class will eventually become obsolete, once an Apache HTTP client interceptor
 * is provided by OpenTracing.
 *
 * @author Juraci Paixão Kröhling
 */
public class OlaHttpRequestInterceptor implements org.apache.http.HttpRequestInterceptor {
    private Span span;

    public OlaHttpRequestInterceptor(Span span) {
        this.span = span;
    }

    @Override
    public void process(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
        Tags.HTTP_URL.set(span, httpRequest.getRequestLine().getUri());
        Tracer tracer = TracerResolver.getTracer();
        if (null != tracer) {
            tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS, new HttpHeadersInjectAdapter(httpRequest));
        }
    }
}
