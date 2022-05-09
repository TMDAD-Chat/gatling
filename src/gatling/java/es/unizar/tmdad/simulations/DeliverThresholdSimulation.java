package es.unizar.tmdad.simulations;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.sse;

public class DeliverThresholdSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
            .baseUrl("https://gateway:8443") // Here is the root for all relative URLs
            .acceptHeader("text/html,text/event-stream,application/json,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
            .acceptEncodingHeader("gzip, deflate")
            .acceptLanguageHeader("en-US,en;q=0.5")
            .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0");

    String receiverOfMessage = "dummy@test.io";
    String sentMsg = "MSG_SENT_BY_GATLING";

    Iterator<Map<String, Object>> feeder =
            Stream.generate((Supplier<Map<String, Object>>) () -> {
                        String email = RandomStringUtils.randomAlphanumeric(20) + "@foo.com";
                        return Collections.singletonMap("email", email);
                    }
            ).iterator();

    ScenarioBuilder scnListener = scenario("SSE Scenario Listener")
            .feed(feeder)
            .exec(http("Create user")
                    .put(session -> "/v1/user/user/" + session.get("email"))
                    .header("content-type", "application/json")
                    .body(StringBody(session -> "{\"name\":\"Dummy user\", \"email\":\""+session.get("email")+"\", \"photoUri\": \"https://www.google.com/images/branding/googlelogo/1x/googlelogo_light_color_272x92dp.png\"}")))
            .pause(Duration.ofSeconds(3))
            .exec(sse("Connect")
                    .connect(session -> "/v1/messagepush/user/" + session.get("email")))
            .exec(http("Send new message")
                    .post("/v1/messagereceive/user/" + receiverOfMessage + "/message")
                    .header("content-type", "application/json")
                    .body(StringBody(session -> "{\"sender\":\""+session.get("email")+"\", \"content\": \""+sentMsg+"\"}")))
            .exec(sse("WaitForMessage").setCheck()
                    .await(Duration.of(60, ChronoUnit.SECONDS))
                            .on(
                                    sse.checkMessage("Contains msg")
                                            .matching(substring(sentMsg))
                            )
                    )
            .exec(sse("Close").close())
            .exec(http("Delete user")
                    .delete(session -> "/v1/user/user/" + session.get("email")));

    public DeliverThresholdSimulation() {
        super();
        setUp(scnListener.injectClosed(rampConcurrentUsers(1).to(1000).during(Duration.ofSeconds(90)))
                .protocols(httpProtocol));
    }
}