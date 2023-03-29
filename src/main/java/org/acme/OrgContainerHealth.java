package org.acme;

import io.quarkus.logging.Log;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;

// other imports...

@ApplicationScoped
@Path("/test")
public class OrgContainerHealth {

    Boolean isDev = LaunchMode.current().toString().equals("DEVELOPMENT");

    /**
     * Get the health status of the container
     * @param containerName
     * @param portNumberStr
     * @return
     */
    @GET
    public Uni<String> getHealthDataFromOrgContainer() {

        String networkHost = "";
        String portStr = "";

//        if (isDev) {
            // This works when this app is running in DEV mode.
            // Talk to the Org container via its port number on localhost.
            networkHost = "http://localhost";
//            portStr = portNumberStr;
//        } else {
//             We are running from inside a container so use the docker network and container name
//            networkHost = "http://" + containerName;
            portStr = "8080";
//        }

        URI uri = URI.create(networkHost + ":" + portStr);

        OrgHealthProxy orgHealthProxy = RestClientBuilder.newBuilder()
            .baseUri(uri)
            .build(OrgHealthProxy.class);

        // Start the timer
        Instant startReq = Instant.now();

        // The JsonObject of health data we will return
        JsonObject healthJsonObject = new JsonObject();

        // Our timeout in ms
        int healthCheckTimeout = 6000;

        return orgHealthProxy.GETAsUni()
            .onItem().transform(orgHealthJsonObject -> {
                Log.info("getHealthDataFromOrgContainer REST request received response =" + orgHealthJsonObject);
//
//                healthJsonObject.clear();
//                healthJsonObject.mergeIn(orgHealthJsonObject, true);

                // Get the overall status string: UP | DOWN
//                String status = orgHealthJsonObject.getString("status");

//                if (status.equals("UP")) {
//                    Instant endReq = Instant.now(); // Stop the timer
//                    long durationMs = Duration.between(startReq, endReq).toMillis(); // Calculate the time taken
//
//                    // Add these properties for the caller
//                    healthJsonObject.put("timeoutMs", healthCheckTimeout);
//                    healthJsonObject.put("timeToStartMs", durationMs);
//
//                    // Return the health data as a JsonObject so the caller has the actual data.
//                    return healthJsonObject;
//                } else {
//                    String errMsg = "ERROR: OrgContainerHealth.getHealthDataFromOrgContainer() the Organisation container is running but not ready. Status=" + status;
//                    Log.error(errMsg);
//
//                    // Throw the error so we retry onFailure()
//                    throw new Error(errMsg);
//                }

                return orgHealthJsonObject.toString();
            })

            // Log the failure
            .onFailure().invoke(throwable ->  {
                Log.info("getHealthDataFromOrgContainer REST request " + " failed because: " + throwable.getMessage());
                Log.info("Received DOWN JSON: " + ((WebApplicationException) throwable).getResponse().readEntity(String.class));
            })

            // Retry the request every 500ms
            .onFailure().retry().withBackOff(Duration.ofMillis(500), Duration.ofMillis(500))

            // Keep trying for eg 6 seconds
            .expireIn(healthCheckTimeout)

            // The FINAL failure, if / when we expire.
            .onFailure().recoverWithItem(throwable -> {
                Log.info("getHealthDataFromOrgContainer HEALTH request for " +  " finally failed after "
                    + healthCheckTimeout + "ms because: " + throwable);

                healthJsonObject.put("timeoutMs", healthCheckTimeout);
                healthJsonObject.put("timeToStartMs", healthCheckTimeout);

                // Add the failure reason in case it is useful.
                healthJsonObject.put("finalFailureReason", throwable.toString());

                // Return the health data we got from the Org container.
                // Your caller can decide what to do.
                return healthJsonObject.toString();
            });

    } // getHealthDataFromOrgContainer

}
