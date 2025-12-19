/*
       Copyright 2020-2021 IBM Corp All Rights Reserved
       Copyright 2022-2025 Kyndryl, All Rights Reserved

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.ibm.hybrid.cloud.sample.stocktrader.broker.client;

import com.ibm.hybrid.cloud.sample.stocktrader.broker.json.Sentiment;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.faulttolerance.Timeout;

@Path("/")
@RegisterRestClient
@RegisterClientHeaders //To enable JWT propagation
// JWT is propagated.  See src/main/resources/META-INF/microprofile-config.properties
/** mpRestClient "remote" interface for the Sentiment Analysis microservice */
public interface SentimentClient {
	@GET
	@Path("/sentiment/{symbol}")
	@Produces(MediaType.APPLICATION_JSON)
	@Timeout(5000) // 5 second timeout
	@WithSpan(kind = SpanKind.CLIENT, value="SentimentClient.getSentiment")
	public Sentiment getSentiment(@PathParam("symbol") String symbol);
}

