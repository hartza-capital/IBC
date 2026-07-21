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

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;

class CommandDispatcher
        implements Runnable {

    /**
     * Channel used to read commands from and write responses to the client.
     */
    private final CommandChannel mChannel;

    /**
     * Cached modifier mask for simulated TWS keyboard shortcuts.
     */
    private static int shortcutModifiers = -1;

    /**
     * Returns the modifier mask used to simulate TWS reconnect keyboard
     * shortcuts.
     *
     * The value is computed lazily because accessing the AWT toolkit during
     * class initialisation fails in headless environments.
     *
     * @return the modifier mask combining the platform menu shortcut and Alt
     */
    private static int getShortcutModifiers() {
        if (shortcutModifiers == -1) {
            int menuShortcut = Toolkit.getDefaultToolkit()
                                     .getMenuShortcutKeyMaskEx();
            shortcutModifiers = menuShortcut | KeyEvent.ALT_DOWN_MASK;
        }
        return shortcutModifiers;
    }


    CommandDispatcher(final CommandChannel channel) {
        mChannel = channel;
    }

    @Override public void run() {
        String cmd = mChannel.getCommand();
        while (cmd != null) {
            if (cmd.equalsIgnoreCase("EXIT")) {
                mChannel.writeAck("Goodbye");
                break;
            } else if (cmd.equalsIgnoreCase("STOP")) {
                handleStopCommand();
            } else if (cmd.equalsIgnoreCase("ENABLEAPI")) {
                handleEnableAPICommand();
            } else if (cmd.equalsIgnoreCase("RECONNECTDATA")) {
                handleReconnectDataCommand();
            } else if (cmd.equalsIgnoreCase("RECONNECTACCOUNT")) {
                handleReconnectAccountCommand();
            } else if (cmd.equalsIgnoreCase("RESTART")) {
                handleRestartCommand();
            } else {
                handleInvalidCommand(cmd);
            }
            mChannel.writePrompt();
            cmd = mChannel.getCommand();
        }
        mChannel.close();
    }

    /**
     * Handles an invalid or unrecognized command by rejecting it and logging
     * an error.
     *
     * @param cmd the invalid command string
     */
    private void handleInvalidCommand(final String cmd) {
        mChannel.writeNack("Command invalid");
        Utils.logError("CommandServer: invalid command received: " + cmd);
    }

    /**
     * Handles the ENABLEAPI command by configuring the API for TWS.
     * Not valid for IB Gateway.
     */
    private void handleEnableAPICommand() {
        if (SessionManager.isGateway()) {
            mChannel.writeNack("ENABLEAPI is not valid for the IB Gateway");
            return;
        }

        // run on the current thread
        (new ConfigurationTask(new EnableApiTask(mChannel))).execute();
   }

    /**
     * Handles the RECONNECTDATA command by simulating a keyboard shortcut to
     * reconnect market data.
     * Not valid for FIX Gateway.
     */
    private void handleReconnectDataCommand() {
        if (SessionManager.isFIX()) {
            mChannel.writeNack(
                    "RECONNECTDATA is not valid for the FIX Gateway");
            return;
        }
        JFrame jf = MainWindowManager.mainWindowManager()
                             .getMainWindow(1, TimeUnit.MILLISECONDS);

        long now = System.currentTimeMillis();
        int modifiers = getShortcutModifiers();
        KeyEvent pressed = new KeyEvent(
                jf, KeyEvent.KEY_PRESSED, now, modifiers,
                KeyEvent.VK_F, KeyEvent.CHAR_UNDEFINED);
        KeyEvent typed = new KeyEvent(
                jf, KeyEvent.KEY_TYPED, now, modifiers,
                KeyEvent.VK_UNDEFINED, 'F');
        KeyEvent released = new KeyEvent(
                jf, KeyEvent.KEY_RELEASED, now, modifiers,
                KeyEvent.VK_F, KeyEvent.CHAR_UNDEFINED);
        jf.dispatchEvent(pressed);
        jf.dispatchEvent(typed);
        jf.dispatchEvent(released);

        mChannel.writeAck("");
    }

    /**
     * Handles the RECONNECTACCOUNT command by simulating a keyboard shortcut
     * to reconnect account data.
     * Not valid for FIX Gateway.
     */
    private void handleReconnectAccountCommand() {
        if (SessionManager.isFIX()) {
            mChannel.writeNack(
                    "RECONNECTACCOUNT is not valid for the FIX Gateway");
            return;
        }
        JFrame jf = MainWindowManager.mainWindowManager().getMainWindow();

        long now = System.currentTimeMillis();
        int modifiers = getShortcutModifiers();
        KeyEvent pressed = new KeyEvent(
                jf, KeyEvent.KEY_PRESSED, now, modifiers,
                KeyEvent.VK_R, KeyEvent.CHAR_UNDEFINED);
        KeyEvent typed = new KeyEvent(
                jf, KeyEvent.KEY_TYPED, now, modifiers,
                KeyEvent.VK_UNDEFINED, 'R');
        KeyEvent released = new KeyEvent(
                jf, KeyEvent.KEY_RELEASED, now, modifiers,
                KeyEvent.VK_R, KeyEvent.CHAR_UNDEFINED);
        jf.dispatchEvent(pressed);
        jf.dispatchEvent(typed);
        jf.dispatchEvent(released);

        mChannel.writeAck("");
    }

    /**
     * Handles the STOP command by shutting down TWS/Gateway.
     */
    private void handleStopCommand() {
        // run on the current thread
        (new StopTask(mChannel, false, "STOP command")).run();
    }

    /**
     * Handles the RESTART command by restarting TWS/Gateway.
     * Not valid for FIX Gateway.
     */
    private void handleRestartCommand() {
        if (SessionManager.isFIX()) {
            mChannel.writeNack("RESTART is not valid for the FIX Gateway");
            return;
        }
        // run on the current thread
        (new RestartTask(mChannel)).run();
    }
}
