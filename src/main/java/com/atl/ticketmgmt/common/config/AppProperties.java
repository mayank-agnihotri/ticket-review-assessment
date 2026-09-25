package com.atl.ticketmgmt.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Rag rag = new Rag();
    private final Tickets tickets = new Tickets();

    public Rag getRag() {
        return rag;
    }

    public Tickets getTickets() {
        return tickets;
    }

    public static class Rag {

        private final VectorStore vectorStore = new VectorStore();

        /**
         * Max chunks retrieved for RAG (spec: configurable, not hardcoded).
         */
        private int topK = 5;

        /**
         * Minimum similarity score 0–1 (spec: configurable, not hardcoded).
         */
        private double similarityThreshold = 0.65;

        private int maxChunkChars = 1500;

        /**
         * Embedding vector size; must match PGVector / Spring AI configuration.
         */
        private int embeddingDimensions = 768;

        public VectorStore getVectorStore() {
            return vectorStore;
        }

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public double getSimilarityThreshold() {
            return similarityThreshold;
        }

        public void setSimilarityThreshold(double similarityThreshold) {
            this.similarityThreshold = similarityThreshold;
        }

        public int getMaxChunkChars() {
            return maxChunkChars;
        }

        public void setMaxChunkChars(int maxChunkChars) {
            this.maxChunkChars = maxChunkChars;
        }

        public int getEmbeddingDimensions() {
            return embeddingDimensions;
        }

        public void setEmbeddingDimensions(int embeddingDimensions) {
            this.embeddingDimensions = embeddingDimensions;
        }

        public static class VectorStore {

            /**
             * {@code pgvector} for PostgreSQL vector storage, {@code noop} for tests without PGVector.
             */
            private String type = "pgvector";

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public boolean isPgVectorEnabled() {
                return "pgvector".equalsIgnoreCase(type);
            }

            public boolean isNoopEnabled() {
                return "noop".equalsIgnoreCase(type);
            }
        }
    }

    public static class Tickets {

        /**
         * Optional cap for GET /api/tickets list (design decision).
         */
        private int listMax = 500;

        public int getListMax() {
            return listMax;
        }

        public void setListMax(int listMax) {
            this.listMax = listMax;
        }
    }
}
