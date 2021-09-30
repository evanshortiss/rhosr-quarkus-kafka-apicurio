package org.acme.kafka.producer;


import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.acme.kafka.quarkus.Quote;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import io.smallrye.mutiny.Multi;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestSseElementType;

@Path("/quotes")
public class QuotesResource {

    private static final Logger Log = Logger.getLogger(QuotesResource.class);

    @Channel("quote-requests")
    Emitter<Quote> quoteRequestEmitter;

    /**
     * Endpoint to generate a new quote request id and send it to "quote-requests" Kafka topic using the emitter.
     */
    @POST
    @Path("/request")
    @Produces(MediaType.TEXT_PLAIN)
    public Quote createRequest() {
        UUID uuid = UUID.randomUUID();
        Quote quote = new Quote(uuid.toString(), ThreadLocalRandom.current().nextInt());
        quoteRequestEmitter.send(quote);
        Log.info("Sent quote request with ID: " + quote.getId());
        return quote;
    }

    @Channel("quotes")
    Multi<Quote> quotes;

    /**
     * Endpoint retrieving the "quotes" Kafka topic and sending the items to a server sent event.
     */
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS) // denotes that server side events (SSE) will be produced
    @RestSseElementType(MediaType.TEXT_PLAIN)
    public Multi<Quote> stream() {
        return quotes.onItem().invoke(q -> Log.info("Received processed quote: " + q.getId())).log();
    }
}

