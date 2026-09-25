package com.atl.ticketmgmt.ai.support;

public final class RagTestFixtures {

    public static final String PREFIX = "RAGE2E";

    private RagTestFixtures() {}

    public static String token(String name) {
        return PREFIX + "-" + name + "-9f2c1d";
    }
}
