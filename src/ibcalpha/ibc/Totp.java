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

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Generates one-time passcodes for IBKR two-factor authentication,
 * using HOTP (RFC 4226) dynamic truncation over HMAC-SHA1 with a TOTP
 * (RFC 6238) time step.
 *
 * <p>Only the SHA1 / 6-digit / 30-second-step combination is supported,
 * which is what IBKR expects and what IBC has always used in practice:
 * it matches the implicit defaults of the {@code dev.samstevens.totp}
 * library that this class replaced. Nothing here is configurable
 * because nothing in IBC ever configured it.
 *
 * <p>No cryptography is implemented in this class. The HMAC-SHA1
 * primitive comes from the JDK ({@link Mac} / {@link SecretKeySpec});
 * the code below only performs the counter encoding, truncation, and
 * decimal formatting steps defined by the public IETF specifications.
 */
final class Totp {

    /**
     * Prevents instantiation of this static utility holder.
     */
    private Totp() { }

    /** JCE name of the HMAC used by RFC 4226; present on every JVM. */
    private static final String HMAC_ALGORITHM = "HmacSHA1";

    /** Number of decimal digits in the passcodes IBKR expects. */
    private static final int CODE_DIGITS = 6;

    /** Width of the big-endian counter block fed to the HMAC. */
    private static final int COUNTER_BYTES = 8;

    /** Number of hash bytes combined into the truncated binary code. */
    private static final int TRUNCATED_BYTES = 4;

    /** Number of bits contributed by each truncated hash byte. */
    private static final int BITS_PER_BYTE = 8;

    /** Mask selecting the truncation offset from the last hash byte. */
    private static final int OFFSET_MASK = 0x0F;

    /** Mask clearing the sign bit of the truncated code's first byte. */
    private static final int SIGN_BIT_MASK = 0x7F;

    /** Mask undoing sign extension when widening a hash byte to int. */
    private static final int BYTE_MASK = 0xFF;

    /** Powers of ten, indexed by digit count, used to truncate. */
    private static final int[] DIGITS_POWER =
            {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000};

    /**
     * Generates the 6-digit TOTP passcode for a Base32 secret at a
     * given time step.
     *
     * <p>This method performs no time arithmetic of its own: the caller
     * supplies the already-computed time-step counter, so that the
     * caller owns the clock and this method stays deterministic and
     * directly testable.
     *
     * @param base32Secret the shared secret, Base32-encoded (RFC 4648),
     *                as supplied by IBKR and configured through IBC's
     *                {@code Ib2FA} setting
     * @param timeStepCounter the number of 30-second time steps elapsed
     *                since the Unix epoch, that is
     *                {@code System.currentTimeMillis() / 30000}
     * @return the 6-digit passcode, left-padded with zeros as needed
     * @throws IllegalArgumentException if base32Secret is null, blank,
     *                or not valid Base32
     * @throws GeneralSecurityException if the JVM rejects HMAC-SHA1 or
     *                the decoded secret as a key; unreachable on a
     *                standard JVM, but declared rather than swallowed
     *                so a broken security provider cannot fail silently
     */
    static String generate(final String base32Secret,
                           final long timeStepCounter)
            throws GeneralSecurityException {
        byte[] secretKey = Base32.decode(base32Secret);
        return generateHotp(secretKey, timeStepCounter, CODE_DIGITS);
    }

    /**
     * Computes an HOTP passcode using the dynamic truncation defined in
     * RFC 4226 section 5.3.
     *
     * <p>The moving factor is encoded as an 8-byte big-endian integer
     * and passed through HMAC-SHA1. The low nibble of the last hash
     * byte selects a 4-byte window, whose high bit is cleared so the
     * value is always a positive 31-bit integer, and that value is
     * reduced modulo a power of ten to yield the passcode.
     *
     * @param secretKey the raw, already-decoded shared secret
     * @param counter the moving factor: an event counter for HOTP, or a
     *                time-step count for TOTP
     * @param digits the number of decimal digits to produce; must be
     *                between 0 and 7 inclusive
     * @return the passcode, left-padded with zeros to the requested
     *                number of digits
     * @throws IllegalArgumentException if digits is outside the
     *                supported range
     * @throws GeneralSecurityException if HMAC-SHA1 is unavailable or
     *                the key is rejected
     */
    static String generateHotp(final byte[] secretKey,
                               final long counter,
                               final int digits)
            throws GeneralSecurityException {
        if (digits < 0 || digits >= DIGITS_POWER.length) {
            throw new IllegalArgumentException(
                    "Unsupported digit count " + digits + " (must be 0 to "
                            + (DIGITS_POWER.length - 1) + ")");
        }

        byte[] counterBlock =
                ByteBuffer.allocate(COUNTER_BYTES).putLong(counter).array();

        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secretKey, HMAC_ALGORITHM));
        byte[] hash = mac.doFinal(counterBlock);

        // Dynamic truncation: the last nibble of the hash picks which
        // 4-byte window to read.
        int offset = hash[hash.length - 1] & OFFSET_MASK;

        // The first byte is masked with SIGN_BIT_MASK to discard its
        // sign bit, so the result is a positive 31-bit value; the
        // remaining bytes use BYTE_MASK to undo the sign extension Java
        // applies when promoting a signed byte to int.
        int binaryCode = hash[offset] & SIGN_BIT_MASK;
        for (int i = 1; i < TRUNCATED_BYTES; i++) {
            binaryCode = (binaryCode << BITS_PER_BYTE)
                    | (hash[offset + i] & BYTE_MASK);
        }

        int passcode = binaryCode % DIGITS_POWER[digits];
        return String.format("%0" + digits + "d", passcode);
    }
}
