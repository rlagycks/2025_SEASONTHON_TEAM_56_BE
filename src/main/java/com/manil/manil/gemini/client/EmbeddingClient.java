// com.manil.manil.ai.EmbeddingClient.java
package com.manil.manil.gemini.client;

public interface EmbeddingClient {
    float[] embed(String text);
    int dimension();
    String modelName();
}