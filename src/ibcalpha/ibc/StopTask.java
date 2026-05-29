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

import java.io.File;
import java.util.Arrays;

class StopTask
        implements Runnable {

    private static final SwitchLock _Running = new SwitchLock();

    private final CommandChannel mChannel;
    private final boolean mForceColdRestart;
    private final String mReason;

    StopTask(final CommandChannel channel, final boolean forceColdRestart, final String reason) {
        mChannel = channel;
        mForceColdRestart = forceColdRestart;
        mReason = reason;
    }

    /**
     * Executes the shutdown sequence.
     * If a shutdown is already in progress, sends an error message.
     * Otherwise, initiates the TWS/Gateway shutdown with the specified reason.
     */
    @Override
    public void run() {
        if (! _Running.set()) {
            Utils.logToConsole("STOP already in progress");
            writeNack("STOP already in progress");
            mChannel.close();
            return;
        }

        try {
            writeInfo("Closing IBC");
            if (mForceColdRestart) createColdRestartFlagFile();
            stop(mReason);
        } catch (Exception ex) {
            writeNack(ex.getMessage());
            Utils.exitWithException(ErrorCodes.UNHANDLED_EXCEPTION, ex);
        }
    }
    
    /**
     * Creates a cold restart flag file in the JTS config directory to force a cold restart on the next start.
     */
    private void createColdRestartFlagFile() {
        try {
        new File(System.getProperty("jtsConfigDir") + 
                 File.separator + 
                 "COLDRESTART" + 
                 System.getProperty("ibcsessionid"))
                .createNewFile();
        } catch (java.io.IOException e) {
            Utils.exitWithException(ErrorCodes.UNHANDLED_EXCEPTION, e);
        }
    }

    /**
     * Checks whether a shutdown is currently in progress.
     *
     * @return true if a shutdown is in progress, false otherwise
     */
    public final static boolean shutdownInProgress()
    {
        return _Running.query();
    }

    /**
     * Performs the actual shutdown of TWS/Gateway.
     * Sends acknowledgment to the client, closes the CommandServer, and either halts immediately
     * or invokes the File menu to cleanly close the application depending on login state.
     *
     * @param reason a description of why the shutdown was initiated
     */
    private void stop(String reason) {
        try {
            writeAck("Shutting down: " + reason);
            if (mChannel != null) mChannel.close();
            if (LoginManager.loginManager().getLoginState() != LoginManager.LoginState.LOGGED_IN) {
                CommandServer.commandServer().shutdown();
                Utils.logToConsole("Login has not completed: exiting immediately");
                Runtime.getRuntime().halt(0);
            } else {
                String[] closeMenuPath = SessionManager.isGateway() ? new String[] {"File", "Close"} : new String[] {"File", "Exit"};
                Utils.logToConsole("Login has completed: exiting via " + Arrays.deepToString(closeMenuPath) + " menu");
                Utils.invokeMenuItem(MainWindowManager.mainWindowManager().getMainWindow(), closeMenuPath);
            }
            
        } catch (Exception e) {
        }
    }

    /**
     * Sends a success acknowledgment message to the connected client.
     *
     * @param message the acknowledgment message
     */
    private void writeAck(String message) {if (mChannel != null) mChannel.writeAck(message);}

    /**
     * Sends an informational message to the connected client.
     *
     * @param message the informational message
     */
    private void writeInfo(String message) {if (mChannel != null) mChannel.writeInfo(message);}

    /**
     * Sends an error message to the connected client.
     *
     * @param message the error message
     */
    private void writeNack(String message) {if (mChannel != null) mChannel.writeNack(message);}

}
