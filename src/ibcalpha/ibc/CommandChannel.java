// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2018 Richard L King (rlking@aultan.com)
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;

/**
 * Handles I/O for a single client connection to the command server.
 */
final class CommandChannel {

    private static final String _Prompt = Settings.settings().getString("CommandPrompt", "");
    private static final boolean _SuppressInfo = Settings.settings().getBoolean("SuppressInfoMessages", true);

    private Socket mSocket;
    private BufferedReader mInstream = null;
    private BufferedWriter mOutstream = null;

    /**
     * Creates a command channel for an accepted client connection and sets up
     * its input and output streams.
     *
     * @param socket the connected client socket
     */
    CommandChannel(Socket socket) {

        mSocket = socket;
        if (! setupStreams()) return;

        writeInfo("IBC Command Server");
    }

    /**
     * Closes the streams and the client socket; does nothing if the channel is
     * already closed.
     */
    void close() {
        try {
            if (mSocket == null || mSocket.isClosed()) return;
            
            Utils.logToConsole("Closing command channel");
            mSocket.shutdownInput();
            mSocket.shutdownOutput();

            mInstream.close();
            mInstream = null;

            mOutstream.close();
            mOutstream = null;

            mSocket.close();
            mSocket = null;
        } catch (SocketException e) {
            // the socket was reset by the client - ignore
            Utils.logException(e);
        } catch (IOException e) {
            // ignore
            Utils.logException(e);
        }
    }

    /**
     * Reads and returns the next non-blank command line sent by the client,
     * prompting again after blank lines, and closes the channel on read errors.
     *
     * @return the command line, or null if the client disconnected
     */
    String getCommand() {
        String cmd = null;

        if (mInstream == null) return null;

        try {
            cmd = mInstream.readLine();
            while (cmd != null && cmd.trim().isEmpty()) {
                writePrompt();
                cmd = mInstream.readLine();
            }

            if (cmd != null) Utils.logToConsole("CommandServer received command: " + cmd);
        } catch (SocketException e) {
            // the socket was reset by the client
            Utils.logException(e);
            close();
        } catch (IOException e) {
            Utils.logException(e);
            close();
        }
        return cmd;
    }

    /**
     * Sends an acknowledgement line to the client.
     *
     * @param info detail text appended to the acknowledgement
     */
    void writeAck(String info) {
        replyLine("OK " + info);
    }

    /**
     * Sends an informational line to the client unless info messages are suppressed.
     *
     * @param info the informational text
     */
    final void writeInfo(String info) {
        if (! _SuppressInfo) replyLine("INFO " + info);
    }

    /**
     * Sends an error line to the client.
     *
     * @param info the error text
     */
    void writeNack(String info) {
        replyLine("ERROR " + info);
    }

    /**
     * Sends the configured command prompt to the client, if a prompt is configured.
     */
    void writePrompt() {
        if (! _Prompt.isEmpty()) reply(_Prompt);
    }

    /**
     * Sends a message to the connected client without a trailing newline.
     *
     * @param message the message text to send
     */
    private void reply(String message) {
        reply(message, false);
    }

    /**
     * Sends a message to the connected client with optional trailing newline.
     *
     * @param message the message text to send
     * @param addNewline if true, appends a newline after the message
     */
    private void reply(String message, boolean addNewline) {
        if (mOutstream == null) return;
        try {
            mOutstream.write(message);
            if (addNewline) mOutstream.newLine();
            mOutstream.flush();
        } catch (SocketException e) {
            // the socket was reset by the client
            Utils.logException(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a message to the connected client followed by a newline.
     *
     * @param message the message text to send
     */
    private void replyLine(String message) {
        reply(message,true);
    }

    /**
     * Initializes input and output streams for the socket connection.
     *
     * @return true if streams were successfully created, false otherwise
     */
    private boolean setupStreams() {
        try {
            mInstream = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            mOutstream = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
            return true;
        } catch (IOException e) {
            // this is most likely a result of the user closing the command connection
            Utils.logException(e);
            return false;
        }
    }

}
