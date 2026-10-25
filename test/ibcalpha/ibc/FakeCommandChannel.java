// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Test-only fake socket-based CommandChannel that captures output.

package ibcalpha.ibc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * In-memory test double for {@link CommandChannel}: serves the supplied input
 * lines and captures everything the channel writes.
 */
final class FakeCommandChannel {

    /** Buffer holding the bytes written by the wrapped channel. */
    private final ByteArrayOutputStream output;
    /** The channel under test, reading from the in-memory input. */
    private final CommandChannel channel;

    /**
     * Creates a fake channel that reads the supplied lines as its input.
     *
     * @param inputLines input lines presented to the channel, in order
     */
    FakeCommandChannel(String... inputLines) {
        String input = String.join("\n", inputLines);
        if (!input.isEmpty()) input += "\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        output = new ByteArrayOutputStream();
        Socket socket = new NoOpSocket(inputStream, output);
        channel = new CommandChannel(socket);
    }

    /**
     * Returns the wrapped channel.
     *
     * @return the channel reading from the fake input
     */
    CommandChannel channel() {
        return channel;
    }

    /**
     * Returns everything the channel has written so far.
     *
     * @return the captured output decoded as UTF-8
     */
    String output() {
        return output.toString(StandardCharsets.UTF_8);
    }

    /**
     * Socket double backed by the supplied memory streams whose shutdown and
     * close operations are no-ops.
     */
    private static final class NoOpSocket extends Socket {
        /** Stream supplying the channel's input, returned by {@link #getInputStream()}. */
        private final ByteArrayInputStream inputStream;
        /** Stream capturing the channel's output, returned by {@link #getOutputStream()}. */
        private final ByteArrayOutputStream outputStream;

        /**
         * Creates a socket backed by the given memory streams.
         *
         * @param inputStream the stream returned by {@link #getInputStream()}
         * @param outputStream the stream returned by {@link #getOutputStream()}
         */
        NoOpSocket(ByteArrayInputStream inputStream, ByteArrayOutputStream outputStream) {
            this.inputStream = inputStream;
            this.outputStream = outputStream;
        }

        /**
         * Returns the supplied input stream.
         *
         * @return the in-memory input stream
         */
        @Override
        public java.io.InputStream getInputStream() {
            return inputStream;
        }

        /**
         * Returns the supplied output stream.
         *
         * @return the in-memory output stream
         */
        @Override
        public java.io.OutputStream getOutputStream() {
            return outputStream;
        }

        /**
         * Does nothing; the input stream stays readable.
         */
        @Override
        public void shutdownInput() throws IOException {
            // no-op
        }

        /**
         * Does nothing; the output stream stays writable.
         */
        @Override
        public void shutdownOutput() throws IOException {
            // no-op
        }

        /**
         * Reports the socket as open.
         *
         * @return false, always
         */
        @Override
        public boolean isClosed() {
            return false;
        }

        /**
         * Does nothing; the streams stay open after the channel closes the socket.
         */
        @Override
        public synchronized void close() throws IOException {
            // no-op
        }

        /**
         * Returns no address for the fake peer.
         *
         * @return null, always
         */
        @Override
        public SocketAddress getRemoteSocketAddress() {
            return null;
        }
    }
}
