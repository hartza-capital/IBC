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
 * Wraps a {@link CommandChannel} backed by in-memory streams for unit tests.
 */
final class FakeCommandChannel {

    private final ByteArrayOutputStream output;
    private final CommandChannel channel;

    FakeCommandChannel(String... inputLines) {
        String input = String.join("\n", inputLines);
        if (!input.isEmpty()) input += "\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        output = new ByteArrayOutputStream();
        Socket socket = new NoOpSocket(inputStream, output);
        channel = new CommandChannel(socket);
    }

    CommandChannel channel() {
        return channel;
    }

    String output() {
        return output.toString(StandardCharsets.UTF_8);
    }

    /**
     * Socket subclass that ignores shutdown/close and uses the supplied streams.
     */
    private static final class NoOpSocket extends Socket {
        private final ByteArrayInputStream inputStream;
        private final ByteArrayOutputStream outputStream;

        NoOpSocket(ByteArrayInputStream inputStream, ByteArrayOutputStream outputStream) {
            this.inputStream = inputStream;
            this.outputStream = outputStream;
        }

        @Override
        public java.io.InputStream getInputStream() {
            return inputStream;
        }

        @Override
        public java.io.OutputStream getOutputStream() {
            return outputStream;
        }

        @Override
        public void shutdownInput() throws IOException {
            // no-op
        }

        @Override
        public void shutdownOutput() throws IOException {
            // no-op
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public synchronized void close() throws IOException {
            // no-op
        }

        @Override
        public SocketAddress getRemoteSocketAddress() {
            return null;
        }
    }
}
