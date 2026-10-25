// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for the login warning dialog handler.

package ibcalpha.ibc;

import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginWarningDialogHandlerTest {

    /** Caption of the button that acknowledges the warning. */
    private static final String ACKNOWLEDGEMENT_CAPTION = "I understand and accept";

    /**
     * Builds a stand-in for the login warning dialog: a panel holding a button nested
     * one level down, the way the vendor's dialog nests its controls.
     *
     * @param buttonCaption the caption of the acknowledgement button
     * @return the panel standing in for the dialog
     */
    private JPanel warningDialog(String buttonCaption) {
        JPanel buttonRow = new JPanel();
        buttonRow.add(new JButton(buttonCaption));
        JPanel dialog = new JPanel();
        dialog.add(new JLabel("This is not a brokerage account."));
        dialog.add(buttonRow);
        return dialog;
    }

    @Test
    void findsAcknowledgementButtonInNestedPanel() {
        JButton button = LoginWarningDialogHandler.findAcknowledgementButton(
                warningDialog(ACKNOWLEDGEMENT_CAPTION));

        assertNotNull(button);
    }

    @Test
    void matchesAcknowledgementCaptionIgnoringCase() {
        JButton button = LoginWarningDialogHandler.findAcknowledgementButton(
                warningDialog("I UNDERSTAND AND ACCEPT"));

        assertNotNull(button);
    }

    @Test
    void ignoresDialogWithoutTheAcknowledgementButton() {
        JPanel dialog = new JPanel();
        dialog.add(new JLabel("This is not a brokerage account."));
        dialog.add(new JButton("Cancel"));

        assertNull(LoginWarningDialogHandler.findAcknowledgementButton(dialog));
    }

    @Test
    void onlyFiltersOpenAndCloseEvents() {
        LoginWarningDialogHandler handler = new LoginWarningDialogHandler();

        assertTrue(handler.filterEvent(null, WindowEvent.WINDOW_OPENED));
        assertTrue(handler.filterEvent(null, WindowEvent.WINDOW_CLOSED));
        assertFalse(handler.filterEvent(null, WindowEvent.WINDOW_ACTIVATED));
        assertFalse(handler.filterEvent(null, WindowEvent.WINDOW_GAINED_FOCUS));
    }
}
