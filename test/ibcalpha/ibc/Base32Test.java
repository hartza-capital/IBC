// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for the in-tree RFC 4648 Base32 decoder.

package ibcalpha.ibc;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Base32Test {

    /**
     * Decodes the given Base32 text and returns it as a US-ASCII string, so the
     * RFC 4648 test vectors can be asserted in their original readable form.
     *
     * @param encoded the Base32 text to decode
     * @return the decoded bytes interpreted as US-ASCII
     */
    private static String decodeToAscii(String encoded) {
        return new String(Base32.decode(encoded), StandardCharsets.US_ASCII);
    }

    @Test
    void decodesRfc4648ReferenceVectors() {
        // The published test vectors from RFC 4648 section 10, which between them cover
        // every padding length the encoding can produce.
        assertEquals("f", decodeToAscii("MY======"));
        assertEquals("fo", decodeToAscii("MZXQ===="));
        assertEquals("foo", decodeToAscii("MZXW6==="));
        assertEquals("foob", decodeToAscii("MZXW6YQ="));
        assertEquals("fooba", decodeToAscii("MZXW6YTB"));
        assertEquals("foobar", decodeToAscii("MZXW6YTBOI======"));
    }

    @Test
    void decodesUnpaddedInputIdenticallyToPaddedInput() {
        // TOTP secrets are commonly distributed without the '=' padding.
        assertArrayEquals(Base32.decode("MZXW6YTBOI======"), Base32.decode("MZXW6YTBOI"));
        assertArrayEquals(Base32.decode("MY======"), Base32.decode("MY"));
    }

    @Test
    void decodeIsCaseInsensitive() {
        assertArrayEquals(Base32.decode("MZXW6YTBOI"), Base32.decode("mzxw6ytboi"));
    }

    @Test
    void decodeIgnoresWhitespace() {
        // Authenticator apps often display secrets in space-separated groups of four.
        assertArrayEquals(Base32.decode("MZXW6YTBOI"), Base32.decode(" MZXW 6YTB OI "));
        assertArrayEquals(Base32.decode("MZXW6YTBOI"), Base32.decode("MZXW6\tYTB\nOI"));
    }

    @Test
    void decodeRejectsCharactersOutsideTheAlphabet() {
        // '1', '0' and '8' are deliberately absent from the Base32 alphabet because they
        // are visually confusable with I, O and B.
        assertThrows(IllegalArgumentException.class, () -> Base32.decode("MZXW1"));
        assertThrows(IllegalArgumentException.class, () -> Base32.decode("MZXW0"));
        assertThrows(IllegalArgumentException.class, () -> Base32.decode("MZXW8"));
        assertThrows(IllegalArgumentException.class, () -> Base32.decode("not-base32!"));
    }

    @Test
    void decodeRejectsNullAndEffectivelyEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> Base32.decode(null));
        assertThrows(IllegalArgumentException.class, () -> Base32.decode(""));
        assertThrows(IllegalArgumentException.class, () -> Base32.decode("   "));
        assertThrows(IllegalArgumentException.class, () -> Base32.decode("===="));
    }
}
