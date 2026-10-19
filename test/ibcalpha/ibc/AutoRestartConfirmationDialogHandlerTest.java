// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for the automatic restart confirmation dialog handler.

package ibcalpha.ibc;

import java.awt.event.WindowEvent;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoRestartConfirmationDialogHandlerTest {

    /** The message TWS/Gateway shows before restarting the trading platform. */
    private static final String DIALOG_MESSAGE = "Your trading platform restart automatically in 5 minutes";

    /**
     * Builds a stand-in for the automatic restart dialog whose message sits in a text pane.
     *
     * @param text the text of the text pane
     * @return a panel holding the text pane
     */
    private JPanel textPaneDialog(String text) {
        JPanel dialog = new JPanel();
        JTextPane messagePane = new JTextPane();
        messagePane.setText(text);
        dialog.add(messagePane);
        return dialog;
    }

    /**
     * Builds a stand-in for the automatic restart dialog whose message is the option
     * pane message.
     *
     * @param message the option pane message
     * @return a panel holding the option pane
     */
    private JPanel optionPaneDialog(Object message) {
        JPanel dialog = new JPanel();
        dialog.add(new JOptionPane(message));
        return dialog;
    }

    @Test
    void recognisesDialogByTextPaneMessage() {
        assertTrue(AutoRestartConfirmationDialogHandler.containsAutoRestartMessage(textPaneDialog(DIALOG_MESSAGE)));
    }

    @Test
    void recognisesDialogByOptionPaneMessage() {
        assertTrue(AutoRestartConfirmationDialogHandler.containsAutoRestartMessage(optionPaneDialog(DIALOG_MESSAGE)));
    }

    @Test
    void ignoresDialogWithAnotherMessage() {
        JPanel dialog = new JPanel();
        dialog.add(new JLabel("Are you sure you want to exit?"));

        assertFalse(AutoRestartConfirmationDialogHandler.containsAutoRestartMessage(dialog));
    }

    @Test
    void ignoresEmptyOptionPaneMessage() {
        assertFalse(AutoRestartConfirmationDialogHandler.containsAutoRestartMessage(optionPaneDialog(null)));
    }

    @Test
    void onlyFiltersWindowOpenedEvents() {
        AutoRestartConfirmationDialogHandler handler = new AutoRestartConfirmationDialogHandler();

        assertTrue(handler.filterEvent(null, WindowEvent.WINDOW_OPENED));
        assertFalse(handler.filterEvent(null, WindowEvent.WINDOW_CLOSED));
    }
}
