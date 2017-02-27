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

import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpRequest;

import io.opentracing.propagation.TextMap;

/**
 * This is an inject adapter. This is to be used to propagate span state into a payload that
 * goes to a downstream server. On our case, the downstream server is called via an HTTP request,
 * so, we map the {@link #put(String, String)} operation to the {@link HttpRequest#addHeader(String, String)}
 * method. This means that the Tracer will put data as headers, which will need to be parsed by the downstream
 * server, like we do at {@link HttpHeadersExtractAdapter}
 *
 * Note that this class might not be needed in the future, once OpenTracing provides a Spring Boot or
 * Servlet integration library.
 *
 * @author Juraci Paixão Kröhling
 */
public class HttpHeadersInjectAdapter implements TextMap {
    private final HttpRequest httpRequest;

    public HttpHeadersInjectAdapter(final HttpRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        throw new UnsupportedOperationException("HttpHeadersInjectAdapter should only be used with Tracer.inject()");
    }

    @Override
    public void put(String key, String value) {
        this.httpRequest.addHeader(key, value);
    }
}
