package org.acme;

import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
// other imports...

// This class is an INTERFACE which you implement and optionally extend
// to provide the methods that execute the REST requests.

@Path("/q/health")
//@Path("/myhealth")
@ClientHeaderParam(name = "Content-Type", value = "application/json")
public interface OrgHealthProxy {
    // We dont need a pojo, just use JsonObject so we have more flexibility about what we might get back.

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Uni<String> GETAsUni();

    @ClientExceptionMapper
    static WebApplicationException toExceptionWithJson(Response response) {
        if (response.getStatus() == 503) {
            // health DOWN json received
            return new WebApplicationException("Proceed with the received JSON", response);
        }

        return null;
    }
}
