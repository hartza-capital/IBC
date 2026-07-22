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

/**
 * IBC automates IB Trader Workstation and IB Gateway.
 *
 * <p>The package hooks into the Swing event system rather than driving the
 * applications from outside: {@link ibcalpha.ibc.TwsListener} is registered as
 * an {@code AWTEventListener} on the toolkit, and every window event is offered
 * to a list of {@link ibcalpha.ibc.WindowHandler} implementations. The first
 * handler whose {@code recogniseWindow} matches takes the event, and its
 * {@code handleWindow} performs the automation. Handler work is posted through
 * {@link ibcalpha.ibc.GuiDeferredExecutor} so the event dispatch thread is
 * never blocked.
 *
 * <p>The entry points are {@link ibcalpha.ibc.IbcTws} for TWS and
 * {@link ibcalpha.ibc.IbcGateway} for the Gateway. Both load configuration
 * through {@link ibcalpha.ibc.Settings}, drive the login state machine in
 * {@link ibcalpha.ibc.LoginManager}, and expose remote control over TCP via
 * {@link ibcalpha.ibc.CommandServer}.
 *
 * <p>After login, the {@code Configure*Task} classes apply settings
 * programmatically by opening the Global Configuration dialog, while
 * {@link ibcalpha.ibc.JtsIniManager} persists the changes that TWS reads back
 * from {@code jts.ini} on the next start.
 */
package ibcalpha.ibc;
