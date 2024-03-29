/*
       Copyright 2023 Kyndryl, All Rights Reserved

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

import com.ibm.hybrid.cloud.sample.stocktrader.broker.json.CashAccount;

import javax.enterprise.context.ApplicationScoped;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;


@ApplicationPath("/")
@Path("/")
@ApplicationScoped
@RegisterRestClient
/** mpRestClient "remote" interface for the CashAccount microservice */
public interface CashAccountClient {
	@GET
	@Path("/{owner}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public CashAccount getCashAccount(@HeaderParam("Authorization") String jwt, @PathParam("owner") String owner);

	@POST
	@Path("/{owner}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public CashAccount createCashAccount(@HeaderParam("Authorization") String jwt, @PathParam("owner") String owner, CashAccount cashAccount);

	@PUT
	@Path("/{owner}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public CashAccount updateCashAccount(@HeaderParam("Authorization") String jwt, @PathParam("owner") String owner, CashAccount cashAccount);

	@DELETE
	@Path("/{owner}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public CashAccount deleteCashAccount(@HeaderParam("Authorization") String jwt, @PathParam("owner") String owner);

	@PUT
	@Path("/{owner}/debit")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public CashAccount debit(@HeaderParam("Authorization") String jwt, @PathParam("owner") String owner, @QueryParam("amount") double amount);

	@PUT
	@Path("/{owner}/credit")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public CashAccount credit(@HeaderParam("Authorization") String jwt, @PathParam("owner") String owner, @QueryParam("amount") double amount);
}
