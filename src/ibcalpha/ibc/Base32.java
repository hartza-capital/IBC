// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// For conditions of distribution and use, see copyright notice in COPYING.txt

// IBC is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// IBC is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with IBC.  If not, see <http://www.gnu.org/licenses/>.

package ibcalpha.ibc;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

/**
 * Decodes RFC 4648 Base32 text into raw bytes.
 */
final class Base32 {

    /**
     * Prevents instantiation of this static utility holder.
     */
    private Base32() { }

    /** The RFC 4648 alphabet; a character's index is its 5-bit value. */
    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    /** Number of bits each Base32 character contributes to the output. */
    private static final int BITS_PER_CHARACTER = 5;

    /** Number of bits in a decoded output byte. */
    private static final int BITS_PER_BYTE = 8;

    /** Mask keeping the low 8 bits when emitting a decoded byte. */
    private static final int BYTE_MASK = 0xFF;

    /**
     * Decodes a Base32-encoded string into the bytes it represents.
     *
     * <p>Whitespace, lowercase letters and '=' padding are accepted; incomplete
     * trailing bits are discarded.</p>
     *
     * @param encoded the Base32 text to decode
     * @return the decoded bytes, empty when the input holds under 8 bits
     * @throws IllegalArgumentException if encoded is null, blank, or holds a character outside the alphabet
     */
    static byte[] decode(final String encoded) {
        if (encoded == null) {
            throw new IllegalArgumentException(
                    "Base32 input must not be null");
        }

        String normalised = encoded.replaceAll("\\s+", "")
                                   .replace("=", "")
                                   .toUpperCase(Locale.ROOT);
        if (normalised.isEmpty()) {
            throw new IllegalArgumentException(
                    "Base32 input must not be blank");
        }

        int expectedBytes =
                normalised.length() * BITS_PER_CHARACTER / BITS_PER_BYTE;
        ByteArrayOutputStream decoded =
                new ByteArrayOutputStream(expectedBytes);
        int bitBuffer = 0;
        int bufferedBits = 0;

        for (int i = 0; i < normalised.length(); i++) {
            char symbol = normalised.charAt(i);
            int symbolValue = ALPHABET.indexOf(symbol);
            if (symbolValue < 0) {
                throw new IllegalArgumentException(
                        "Invalid Base32 character '" + symbol
                                + "' at position " + i);
            }

            bitBuffer = (bitBuffer << BITS_PER_CHARACTER) | symbolValue;
            bufferedBits += BITS_PER_CHARACTER;

            if (bufferedBits >= BITS_PER_BYTE) {
                bufferedBits -= BITS_PER_BYTE;
                decoded.write((bitBuffer >> bufferedBits) & BYTE_MASK);
            }
        }

        return decoded.toByteArray();
    }
}
