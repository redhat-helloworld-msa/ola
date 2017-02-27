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
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

/**
 * This is an interceptor to be used with the Apache HTTP client. This is for the response part,
 * which is used for finishing the span, adding the resulting HTTP response code to it.
 *
 * On a real application, this could be used to add more data to the span, like a specific HTTP
 * header sent from the downstream server.
 *
 * Note that this class will eventually become obsolete, once an Apache HTTP client interceptor
 * is provided by OpenTracing.
 *
 * @author Juraci Paixão Kröhling
 */
public class OlaHttpResponseInterceptor implements HttpResponseInterceptor {
    private Span span;

    public OlaHttpResponseInterceptor(Span span) {
        this.span = span;
    }

    @Override
    public void process(HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
        Tags.HTTP_STATUS.set(span, httpResponse.getStatusLine().getStatusCode());
        span.finish();
    }
}
