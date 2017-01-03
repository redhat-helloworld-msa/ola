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

import brave.Tracer;
import brave.opentracing.BraveTracer;
import io.opentracing.NoopTracerFactory;
import zipkin.Span;
import zipkin.reporter.AsyncReporter;
import zipkin.reporter.Reporter;
import zipkin.reporter.urlconnection.URLConnectionSender;

@SuppressWarnings("Duplicates")
public class TracerResolver {
    public static io.opentracing.Tracer getTracer() {
        if ("true".equalsIgnoreCase(System.getenv("ENABLE_ZIPKIN"))) {
            String zipkinServerUrl = String.format("%s/api/v1/spans", System.getenv("ZIPKIN_SERVER_URL"));
            Reporter<Span> reporter = AsyncReporter.builder(URLConnectionSender.create(zipkinServerUrl)).build();
            Tracer tracer = Tracer.newBuilder().localServiceName("ola").reporter(reporter).build();
            System.out.println("Using ZipKin Tracer");
            return BraveTracer.wrap(tracer);
        } else {
            System.out.println("Using Noop Tracer");
            return NoopTracerFactory.create();
        }
    }
}
