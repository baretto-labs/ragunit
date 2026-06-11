package org.ragunit.embedding;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OllamaEmbeddingProviderTest {

    private static final String MODEL = "nomic-embed-text";

    private HttpServer server;
    private int port;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void should_return_embedding_when_ollama_responds() throws Exception {
        server.createContext("/api/embeddings", exchange -> {
            String body = "{\"embedding\":[0.1,0.2,0.3]}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        });

        OllamaEmbeddingProvider provider = new OllamaEmbeddingProvider(MODEL, "localhost", port);
        float[] embedding = provider.embed("Hello world");

        assertThat(embedding).hasSize(3);
        assertThat(embedding[0]).isCloseTo(0.1f, org.assertj.core.data.Offset.offset(1e-5f));
        assertThat(embedding[1]).isCloseTo(0.2f, org.assertj.core.data.Offset.offset(1e-5f));
        assertThat(embedding[2]).isCloseTo(0.3f, org.assertj.core.data.Offset.offset(1e-5f));
    }

    @Test
    void should_send_model_and_prompt_in_request_body() throws Exception {
        var captured = new String[1];
        server.createContext("/api/embeddings", exchange -> {
            captured[0] = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String body = "{\"embedding\":[0.5]}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        });

        OllamaEmbeddingProvider provider = new OllamaEmbeddingProvider(MODEL, "localhost", port);
        provider.embed("test input");

        assertThat(captured[0]).contains("\"model\"");
        assertThat(captured[0]).contains(MODEL);
        assertThat(captured[0]).contains("test input");
    }

    @Test
    void should_throw_when_ollama_returns_error() {
        server.createContext("/api/embeddings", exchange -> {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().close();
        });

        OllamaEmbeddingProvider provider = new OllamaEmbeddingProvider(MODEL, "localhost", port);
        assertThatThrownBy(() -> provider.embed("text"))
                .isInstanceOf(EmbeddingException.class);
    }

    @Test
    void should_throw_when_model_is_null() {
        assertThatThrownBy(() -> new OllamaEmbeddingProvider(null, "localhost", port))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_use_default_host_and_port_when_single_arg_constructor() {
        // Just verifies the constructor does not throw
        OllamaEmbeddingProvider provider = new OllamaEmbeddingProvider(MODEL);
        assertThat(provider).isNotNull();
    }
}
