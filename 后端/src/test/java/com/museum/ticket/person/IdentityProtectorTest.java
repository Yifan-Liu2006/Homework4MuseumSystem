package com.museum.ticket.person;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class IdentityProtectorTest {
    private final IdentityProtector identityProtector = new IdentityProtector();

    @Test
    void normalizesBeforeHashing() {
        assertEquals(identityProtector.hash("护照", "ab 12345"),
                identityProtector.hash("护照", "AB12345"));
    }

    @Test
    void separatesDifferentDocumentTypes() {
        assertNotEquals(identityProtector.hash("护照", "AB12345"),
                identityProtector.hash("港澳台通行证", "AB12345"));
    }

    @Test
    void masksIdentityNumber() {
        assertEquals("110***********1234", identityProtector.mask("110101199001011234"));
    }
}
