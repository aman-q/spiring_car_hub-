package com.carhub.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokensTest {

    @Test
    void sha256_isDeterministicAnd64Hex() {
        String a = Tokens.sha256Hex("hello");
        assertEquals(a, Tokens.sha256Hex("hello"), "hashing must be deterministic");
        assertEquals(64, a.length());
        assertTrue(a.matches("[0-9a-f]{64}"));
        assertNotEquals(a, Tokens.sha256Hex("world"));
    }

    @Test
    void randomHex_hasExpectedLengthAndIsRandom() {
        String token = Tokens.randomHex(40);
        assertEquals(80, token.length(), "40 bytes -> 80 hex chars");
        assertNotEquals(token, Tokens.randomHex(40));
    }
}
