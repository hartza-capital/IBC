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

import static ibcalpha.ibc.IbcTws.checkArguments;
import static ibcalpha.ibc.IbcTws.setupDefaultEnvironment;

public final class IbcGateway {
    /**
     * Utility class constructor.
     * Prevents instantiation of this utility class.
     */
    private IbcGateway() {
    }

    /**
     * Starts the IB Gateway automation agent.
     * Sets up exception handling, validates arguments, initializes the
     * environment, and loads the Gateway.
     *
     * @param args command-line arguments (optional: config file,
     *     credentials, trading mode).
     * @throws Exception if an error occurs during Gateway initialization.
     */
    public static void main(final String[] args) throws Exception {
        if (Thread.getDefaultUncaughtExceptionHandler() == null) {
            Thread.setDefaultUncaughtExceptionHandler(
                new ibcalpha.ibc.UncaughtExceptionHandler());
        }
        checkArguments(args);
        setupDefaultEnvironment(args, true);
        IbcTws.load();
    }

    /**
     * Prints IBC version information to the console.
     */
    public static void printVersionInfo() {
        IbcTws.printVersionInfo();
    }

}
