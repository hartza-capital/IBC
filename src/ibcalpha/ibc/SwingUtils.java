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

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.MenuElement;
import javax.swing.tree.TreeModel;

/** Utility methods for inspecting and interacting with Swing components. */
final class SwingUtils {

    /** The system-dependent line separator. */
    static final String NEWLINE = System.lineSeparator();

    /** Placeholder used when a window has no title. */
    static final String NO_TITLE = "** no title **";

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private SwingUtils() {
        // utility class: prevent instantiation
    }

    /**
     * Performs a click on the button labelled with the specified text.
     *
     * @param container the container to search for the button
     * @param buttonText the button's label
     * @return true if the button was found; false if the button was not found
     */
    static boolean clickButton(final Container container,
                               final String buttonText) {
        final JButton button = findButton(container, buttonText);
        if (button == null) {
            return false;
        }
        clickButton(button);
        return true;
    }

    /**
     * Performs a click on the supplied button.
     *
     * @param button the button to click
     */
    static void clickButton(final JButton button) {
        if (!button.isEnabled()) {
            button.setEnabled(true);
            Utils.logToConsole(
                    "Button was disabled, has been enabled: "
                            + button.getText());
        }

        Utils.logToConsole("Click button: " + button.getText());
        button.doClick();
        if (!button.isEnabled()) {
            Utils.logToConsole(
                    "Button now disabled: " + button.getText());
        }
    }

    /**
     * Traverses a container hierarchy and returns the button with the given
     * text.
     *
     * @param container the Container to search in
     * @param text the label of the button to be found
     * @return the button, if it was found; otherwise null
     */
    static JButton findButton(final Container container, final String text) {
        final ComponentIterator iterator = new ComponentIterator(container);
        while (iterator.hasNext()) {
            final Component component = iterator.next();
            if (component instanceof JButton
                    && text.equalsIgnoreCase(
                            ((JButton) component).getText())) {
                return (JButton) component;
            }
        }
        return null;
    }

    /**
     * Traverses a container hierarchy and returns the checkbox with the given
     * text.
     *
     * @param container the Container to search in
     * @param text the label of the checkbox to be found
     * @return the checkbox, if it was found; otherwise null
     */
    static JCheckBox findCheckBox(final Container container,
                                  final String text) {
        final ComponentIterator iterator = new ComponentIterator(container);
        while (iterator.hasNext()) {
            final Component component = iterator.next();
            if (component instanceof JCheckBox
                    && text.equalsIgnoreCase(
                            ((JCheckBox) component).getText())) {
                return (JCheckBox) component;
            }
        }
        return null;
    }

    /**
     * Traverses a container hierarchy and returns the ith JComboBox
     * (0 based indexing).
     *
     * @param container the Container to search in
     * @param index specifies which JComboBox to return (the first one is
     *     specified by 0, the next by 1, etc)
     * @return the required JComboBox if it is found, otherwise null
     */
    static JComboBox<?> findComboBox(final Container container,
                                     final int index) {
        final ComponentIterator iterator = new ComponentIterator(container);
        int i = 0;
        while (iterator.hasNext()) {
            final Component component = iterator.next();
            if (component instanceof JComboBox<?>
                    && i++ == index) {
                return (JComboBox<?>) component;
            }
        }
        return null;
    }

    /**
     * Traverses a container hierarchy and returns the Component with the given
     * name.
     *
     * @param container the Container to search in
     * @param text the name of the Component to be found
     * @return the Component, if it was found; otherwise null
     */
    static Component findComponent(final Container container,
                                  final String text) {
        final ComponentIterator iterator = new ComponentIterator(container);
        while (iterator.hasNext()) {
            final Component component = iterator.next();
            if (text.equalsIgnoreCase(component.getName())) {
                return component;
            }
        }
        return null;
    }

    /**
     * Traverses a container hierarchy and returns the radio button with the
     * given text.
     *
     * @param container the Container to search in
     * @param text the label of the radio button to be found
     * @return the radio button, if it was found; otherwise null
     */
    static JRadioButton findRadioButton(final Container container,
                                        final String text) {
        final ComponentIterator iterator = new ComponentIterator(container);
        while (iterator.hasNext()) {
            final Component component = iterator.next();
            if (component instanceof JRadioButton
                    && text.equalsIgnoreCase(
                            ((JRadioButton) component).getText())) {
                return (JRadioButton) component;
            }
        }
        return null;
    }

    /**
     * Traverses a container hierarchy and returns the JLabel that contains the
     * given substring.
     *
     * @param container the Container to search in
     * @param text the substring to find in a JLabel
     * @return the JLabel, if it was found; otherwise null
     */
    static JLabel findLabel(final Container container, final String text) {
        final ComponentIterator iterator = new ComponentIterator(container);
        while (iterator.hasNext()) {
            final Component component = iterator.next();
            if (component instanceof JLabel) {
                final String content = ((JLabel) component).getText();
                if (content != null
                        && content.toLowerCase().contains(
                                text.toLowerCase())) {
                    return (JLabel) component;
                }
            }
        }
        return null;
    }

    /**
     * Traverses a container hierarchy and returns the ith JList (0 based
     * indexing).
     *
     * @param container the Container to search in
     * @param index specifies which JList to return (the first one is specified
     *     by 0, the next by 1, etc)
     * @return the required JList if it is found, otherwise null
     */
    static JList<?> findList(final Container container, final int index) {
        final ComponentIterator iterator = new ComponentIterator(container);
        int i = 0;
        while (iterator.hasNext()) {
            final Component component = iterator.next();
            if (component instanceof JList<?>
                    && i++ == index) {
                return (JList<?>) component;
            }
        }
        return null;
    }

    /**
     * Traverses a container hierarchy and returns the JTextArea that contains
     * the given substring.
     *
     * @param container the Container to search in
     * @param text the substring to find in a JTextArea
     * @return the JTextArea, if it was found; otherwise null
     */
    static JTextArea findTextArea(final Container container,
                                  final String text) {
        final ComponentIterator iterator = new ComponentIterator(container);
        while (iterator.hasNext()) {
            final Component component = iterator.next();
            if (component instanceof JTextArea) {
                final String content = ((JTextArea) component).getText();
                if (content != null
                        && content.toLowerCase().contains(
                                text.toLowerCase())) {
                    return (JTextArea) component;
                }
            }
        }
        return null;
    }

    /**
     * Returns a string containing the text of all labels in the container,
     * one per line.
     *
     * @param container the Container to search in
     * @return the concatenated label texts
     */
    static String getLabelTexts(final Container container) {
        final ComponentIterator iterator = new ComponentIterator(container);
        String texts = "";
        while (iterator.hasNext()) {
            final Component component = iterator.next();
            if (component instanceof JLabel) {
                final String content = ((JLabel) component).getText();
                if (content != null) {
                    if (!texts.isEmpty()) {
                        texts += NEWLINE;
                    }
                    texts += content;
                }
            }
        }
        return texts;
    }

    /**
     * Returns a string containing the text of all text areas in the container,
     * one per line.
     *
     * @param container the Container to search in
     * @return the concatenated text area contents
     */
    static String getTexts(final Container container) {
        final ComponentIterator iterator = new ComponentIterator(container);
        String texts = "";
        while (iterator.hasNext()) {
            final Component component = iterator.next();
            if (component instanceof JTextArea) {
                final String content = ((JTextArea) component).getText();
                if (content != null) {
                    if (!texts.isEmpty()) {
                        texts += NEWLINE;
                    }
                    texts += content;
                }
            }
        }
        return texts;
    }

    /**
     * Traverses a container hierarchy and returns the ith JTextField (0 based
     * indexing).
     *
     * @param container the Container to search in
     * @param index specifies which JTextField to return (the first one is
     *     specified by 0, the next by 1, etc)
     * @return the required JTextField if it is found, otherwise null
     */
    static JTextField findTextField(final Container container,
                                    final int index) {
        final ComponentIterator iterator = new ComponentIterator(container);
        int i = 0;
        while (iterator.hasNext()) {
            final Component component = iterator.next();
            if (component instanceof JTextField
                    && i++ == index) {
                return (JTextField) component;
            }
        }
        return null;
    }

    /**
     * Traverses a container hierarchy and returns the JTextPane that contains
     * the given substring.
     *
     * @param container the Container to search in
     * @param text the substring to find in a JTextPane
     * @return the JTextPane, if it was found; otherwise null
     */
    static JTextPane findTextPane(final Container container,
                                  final String text) {
        final ComponentIterator iterator = new ComponentIterator(container);
        while (iterator.hasNext()) {
            final Component component = iterator.next();
            if (component instanceof JTextPane) {
                final String content = ((JTextPane) component).getText();
                if (content != null
                        && content.toLowerCase().contains(
                                text.toLowerCase())) {
                    return (JTextPane) component;
                }
            }
        }
        return null;
    }

    /**
     * Traverses a container hierarchy and returns the toggle button with the
     * given text.
     *
     * @param container the Container to search in
     * @param text the label of the toggle button to be found
     * @return the toggle button, if it was found; otherwise null
     */
    static JToggleButton findToggleButton(final Container container,
                                          final String text) {
        final ComponentIterator iterator = new ComponentIterator(container);
        while (iterator.hasNext()) {
            final Component component = iterator.next();
            if (component instanceof JToggleButton
                    && text.equalsIgnoreCase(
                            ((JToggleButton) component).getText())) {
                return (JToggleButton) component;
            }
        }
        return null;
    }

    /**
     * Traverses a container hierarchy and returns the first JMenuBar it
     * finds.
     *
     * @param container the Container to search in
     * @return the first JMenuBar found, if any; otherwise null
     */
    static JMenuBar findMenuBar(final Container container) {
        final ComponentIterator iterator = new ComponentIterator(container);
        while (iterator.hasNext()) {
            final Component component = iterator.next();
            if (component instanceof JMenuBar) {
                return (JMenuBar) component;
            }
        }
        return null;
    }

    /**
     * Traverses a container hierarchy and returns the ith JMenuBar (0 based
     * indexing).
     *
     * @param container the Container to search in
     * @param index specifies which JMenuBar to return (the first one is
     *     specified by 0, the next by 1, etc)
     * @return the required JMenuBar if it is found, otherwise null
     */
    static JMenuBar findMenuBar(final Container container, final int index) {
        final ComponentIterator iterator = new ComponentIterator(container);
        int i = 0;
        while (iterator.hasNext()) {
            final Component component = iterator.next();
            if (component instanceof JMenuBar
                    && i++ == index) {
                return (JMenuBar) component;
            }
        }
        return null;
    }

    /**
     * Searches a MenuElement's subelements for the JMenuItem with the given
     * text.
     *
     * @param container the MenuElement to search in
     * @param text the label of the JMenuItem to be found
     * @return the JMenuItem, if it was found; otherwise null
     */
    static JMenuItem findMenuItem(final MenuElement container,
                                  final String text) {
        final MenuElement[] elements = container.getSubElements();

        for (MenuElement element : elements) {
            if (element instanceof JMenuItem) {
                final JMenuItem button = (JMenuItem) element;
                if (button.getText().equalsIgnoreCase(text)) {
                    return button;
                }
            } else {
                final JMenuItem button = findMenuItem(element, text);
                if (button != null) {
                    return button;
                }
            }
        }
        return null;
    }

    /**
     * Traverses a container hierarchy and returns the JMenuItem with the given
     * path from the first JMenuBar encountered, or null if the first JMenuBar
     * doesn't contain an item with that path.
     *
     * @param container the Container to search in
     * @param path the required menu path
     * @return the JMenuItem at the specified path, if found; otherwise null
     */
    static JMenuItem findMenuItem(final Container container,
                                  final String[] path) {
        if (path.length == 0) {
            return null;
        }

        final JMenuBar menuBar = findMenuBar(container);
        if (menuBar == null) {
            return null;
        }

        return findMenuItem(menuBar, path);
    }

    /**
     * Traverses a container hierarchy and returns the JMenuItem with the given
     * path from the first JMenuBar that contains it.
     *
     * @param container the Container to search in
     * @param path the required menu path
     * @return the JMenuItem at the specified path, if found; otherwise null
     */
    static JMenuItem findMenuItemInAnyMenuBar(final Container container,
                                              final String[] path) {
        if (path.length == 0) {
            return null;
        }

        int i = 0;
        while (true) {
            final JMenuBar menuBar = findMenuBar(container, i);
            if (menuBar == null) {
                return null;
            }
            final JMenuItem menuItem = findMenuItem(menuBar, path);
            if (menuItem != null) {
                return menuItem;
            }
            i++;
        }
    }

    /**
     * Traverses a JMenuBar's menu structure for a JMenuItem with the specified
     * path.
     *
     * @param menuBar the JMenuBar to search
     * @param path the required menu path
     * @return the JMenuItem at the specified path, if found; otherwise null
     */
    static JMenuItem findMenuItem(final JMenuBar menuBar,
                                  final String[] path) {
        if (path.length == 0) {
            return null;
        }

        MenuElement currentItem = menuBar;
        for (String pathElement : path) {
            currentItem = findMenuItem(currentItem, pathElement);
            if (currentItem == null) {
                return null;
            }
        }
        return (JMenuItem) currentItem;
    }

    /**
     * Traverses a container hierarchy and returns the first JOptionPane it
     * finds.
     *
     * @param container the Container to search in
     * @return the first JOptionPane, if one was found; otherwise null
     */
    static JOptionPane findOptionPane(final Container container) {
        final ComponentIterator iterator = new ComponentIterator(container);
        while (iterator.hasNext()) {
            final Component component = iterator.next();
            if (component instanceof JOptionPane) {
                return (JOptionPane) component;
            }
        }
        return null;
    }

    /**
     * Traverses a container hierarchy and returns the first JTree it finds.
     *
     * @param container the Container to search in
     * @return the first JTree, if one was found; otherwise null
     */
    static JTree findTree(final Container container) {
        final ComponentIterator iterator = new ComponentIterator(container);
        while (iterator.hasNext()) {
            final Component component = iterator.next();
            if (component instanceof JTree) {
                return (JTree) component;
            }
        }
        return null;
    }

    /**
     * Returns the node with the given text below the given node in the
     * specified TreeModel.
     *
     * @param model the TreeModel to search
     * @param node the node to search below
     * @param text the text associated with the required node
     * @return the required node, if found; otherwise null
     */
    static Object findChildNode(final TreeModel model,
                                final Object node,
                                final String text) {
        for (int i = 0; i < model.getChildCount(node); i++) {
            final Object currentNode = model.getChild(node, i);
            if (currentNode.toString() != null
                    && currentNode.toString().equalsIgnoreCase(text)) {
                return currentNode;
            }
        }
        return null;
    }

    /**
     * Indicates whether the specified JButton is enabled.
     *
     * @param window the window in which to search for the required JButton
     * @param buttonText the label of the required JButton
     * @return true if the JButton is enabled; false if there is no such
     *     JButton, or it is disabled
     */
    static boolean isButtonEnabled(final Window window,
                                   final String buttonText) {
        final JButton button = findButton(window, buttonText);
        if (button == null) {
            return false;
        }

        return button.isEnabled();
    }

    /**
     * Indicates whether the specified JCheckBox is selected.
     *
     * @param window the window in which to search for the required JCheckBox
     * @param buttonText the label of the required JCheckBox
     * @return true if the JCheckBox is selected; false if there is no such
     *     JCheckBox, or it is not selected
     */
    static boolean isCheckBoxSelected(final Window window,
                                      final String buttonText) {
        final JCheckBox checkbox = findCheckBox(window, buttonText);
        if (checkbox == null) {
            return false;
        }

        return checkbox.isSelected();
    }

    /**
     * Indicates whether the specified JRadioButton is selected.
     *
     * @param window the window in which to search for the required JRadioButton
     * @param buttonText the label of the required JRadioButton
     * @return true if the JRadioButton is selected; false if there is no such
     *     JRadioButton, or it is not selected
     */
    static boolean isRadioButtonSelected(final Window window,
                                         final String buttonText) {
        final JRadioButton radioButton = findRadioButton(window, buttonText);
        if (radioButton == null) {
            return false;
        }

        return radioButton.isSelected();
    }

    /**
     * Returns a string representing the structure of the specified window.
     *
     * <p>Details of each component in the window are included, indented to
     * reflect the component's position in the hierarchy. If an exception
     * occurs while walking the component tree, its stack trace is appended to
     * the returned string instead of being thrown.
     *
     * @param window the Window whose structure is to be returned
     * @return a human-readable, indented description of the window's component
     *     hierarchy
     */
    static String getWindowStructure(final Window window) {
        final StringBuilder builder = new StringBuilder();
        try {
            for (Component component : window.getComponents()) {
                appendComponentStructure(component, builder);
            }
        } catch (Exception e) {
            builder.append(
                    "Exception occurred while generating window structure: ");
            builder.append(e.toString());
            final StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            builder.append(stringWriter.toString());
        }
        builder.append(NEWLINE);
        builder.append(NEWLINE);
        return builder.toString();
    }

    /**
     * Sets or clears the specified JCheckBox.
     *
     * @param window the window in which to search for the required JCheckBox
     * @param buttonText the label for the required JCheckBox
     * @param value true to set the JCheckBox; false to clear it
     * @return true if the JCheckBox was found; otherwise false
     */
    static boolean setCheckBoxSelected(final Window window,
                                         final String buttonText,
                                         final boolean value) {
        final JCheckBox checkbox = findCheckBox(window, buttonText);
        if (checkbox == null) {
            return false;
        }
        checkbox.setSelected(value);
        return true;
    }

    /**
     * Selects the specified JRadioButton if it is not already selected, by
     * simulating a click.
     * Radio buttons cannot be programmatically cleared (only another button in
     * the same group can deselect them), so this method only ever selects,
     * never clears.
     *
     * @param window the window in which to search for the required JRadioButton
     * @param buttonText the label for the required JRadioButton
     * @return true if the JRadioButton was found; otherwise false
     */
    static boolean setRadioButtonSelected(final Window window,
                                          final String buttonText) {
        final JRadioButton radioButton = findRadioButton(window, buttonText);
        if (radioButton == null) {
            return false;
        }

        if (radioButton.isSelected()) {
            return true;
        }

        radioButton.doClick();
        return true;
    }

    /**
     * Sets the specified JTextField to the given value.
     *
     * @param container the container in which to search for the JTextField
     * @param fieldNumber the number of the required JTextField in the
     *     container, counting from 0
     * @param value the value to be set in the JTextField
     * @return true if the required JTextField was found; otherwise false
     */
    static boolean setTextField(final Container container,
                                final int fieldNumber,
                                final String value) {
        final JTextField textField = findTextField(container, fieldNumber);
        if (textField != null) {
            textField.setText(value);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets the password field at the specified index to the given value.
     * Prefers a {@link JPasswordField}; if no JPasswordField is found at the
     * requested index, falls back to the JTextField at that index.
     * This handles dialogs that expose a plain text field rather than a
     * password component.
     *
     * @param window the window in which to search for the password field
     * @param fieldNumber the number of the required text field in the window,
     *     counting from 0
     * @param value the password characters to set
     * @return true if a matching field was found and updated; otherwise false
     */
    static boolean setPasswordField(final Window window,
                                    final int fieldNumber,
                                    final char[] value) {
        return setPasswordField((Container) window, fieldNumber, value);
    }

    /**
     * Sets the password field at the specified index to the given value.
     * Prefers a {@link JPasswordField}; if no JPasswordField is found at the
     * requested index, falls back to the JTextField at that index.
     * This overload accepts any container so it can be exercised with
     * lightweight panels in tests.
     *
     * @param container the container to search for the password field
     * @param fieldNumber the number of the required text field in the
     *     container, counting from 0
     * @param value the password characters to set
     * @return true if a matching field was found and updated; otherwise false
     */
    static boolean setPasswordField(final Container container,
                                    final int fieldNumber,
                                    final char[] value) {
        final ComponentIterator iterator = new ComponentIterator(container);
        int i = 0;
        JTextField fallbackField = null;
        while (iterator.hasNext()) {
            final Component component = iterator.next();
            if (component instanceof JTextField) {
                if (i == fieldNumber) {
                    if (component instanceof JPasswordField) {
                        ((JPasswordField) component).setText(
                                new String(value));
                        return true;
                    }
                    fallbackField = (JTextField) component;
                }
                i++;
            }
        }
        if (fallbackField != null) {
            fallbackField.setText(new String(value));
            return true;
        }
        return false;
    }

    /**
     * Indicates whether the specified window's title contains the given
     * string.
     *
     * @param window the window to be checked
     * @param text the text to be searched for
     * @return true if the window's title contains text, otherwise false
     */
    static boolean titleContains(final Window window, final String text) {
        final String title = getWindowTitle(window);
        return title != null
                && title.toLowerCase().contains(text.toLowerCase());
    }

    /**
     * Indicates whether the specified window's title equals the given string
     * (ignoring case).
     *
     * @param window the window to be checked
     * @param text the text to be compared
     * @return true if the window's title equals text, otherwise false
     */
    static boolean titleEquals(final Window window, final String text) {
        final String title = getWindowTitle(window);
        return title != null && title.equalsIgnoreCase(text);
    }

    /**
     * Returns a human-readable name for the given window event ID.
     *
     * @param eventID the AWT window event ID
     * @return the descriptive name of the event
     */
    static String windowEventToString(final int eventID) {
        switch (eventID) {
            case WindowEvent.WINDOW_ACTIVATED:
                return "Activated";
            case WindowEvent.WINDOW_CLOSED:
                return "Closed";
            case WindowEvent.WINDOW_CLOSING:
                return "Closing";
            case WindowEvent.WINDOW_DEACTIVATED:
                return "Deactivated";
            case WindowEvent.WINDOW_DEICONIFIED:
                return "Deiconified";
            case WindowEvent.WINDOW_GAINED_FOCUS:
                return "Focused";
            case WindowEvent.WINDOW_ICONIFIED:
                return "Iconified";
            case WindowEvent.WINDOW_LOST_FOCUS:
                return "Lost focus";
            case WindowEvent.WINDOW_OPENED:
                return "Opened";
            case WindowEvent.WINDOW_STATE_CHANGED:
                return "State changed";
            default:
                return "???";
        }
    }

    /**
     * Returns the title of the given window, or {@link #NO_TITLE} if it has
     * none.
     *
     * @param window the window whose title is required
     * @return the window title, or a placeholder if none is set
     */
    static String getWindowTitle(final Window window) {
        String title = NO_TITLE;
        if (window instanceof JDialog) {
            title = ((JDialog) window).getTitle();
        } else if (window instanceof JFrame) {
            title = ((JFrame) window).getTitle();
        }
        if (title == null) {
            title = NO_TITLE;
        }
        return title;
    }

    /**
     * Returns a string describing the details of a Swing component.
     *
     * <p>Includes component type, text content, and enabled/disabled status.
     * Used for diagnostic logging and debugging Swing component hierarchies.
     *
     * @param component the component to describe
     * @return a formatted string with component details
     */
    private static String getComponentDetails(final Component component) {
        String details = component.isEnabled() ? "" : "[Disabled]";
        if (component instanceof JButton) {
            details += "JButton: ";
            details += ((JButton) component).getText();
        } else if (component instanceof JCheckBox) {
            details += "JCheckBox: ";
            details += ((JCheckBox) component).getText();
            details += "("
                    + (((JCheckBox) component).isSelected()
                            ? "selected" : "unselected")
                    + ")";
        } else if (component instanceof JLabel) {
            details += "JLabel: ";
            details += ((JLabel) component).getText();
        } else if (component instanceof JOptionPane) {
            details += "JOptionPane: ";
            details += ((JOptionPane) component).getMessage().toString();
        } else if (component instanceof JRadioButton) {
            details += "JRadioButton: ";
            details += ((JRadioButton) component).getText();
        } else if (component instanceof JPasswordField) {
            details += "JPasswordField: ";
            details += "***";
        } else if (component instanceof JTextArea) {
            details += "JTextArea: ";
            details += ((JTextArea) component).getText();
        } else if (component instanceof JTextField) {
            details += "JTextField: ";
            details += ((JTextField) component).getText();
        } else if (component instanceof JTextPane) {
            details += "JTextPane: ";
            details += ((JTextPane) component).getText();
        } else if (component instanceof JMenuBar) {
            details += "JMenuBar: ";
            details += ((JMenuBar) component).getName();
        } else if (component instanceof JMenuItem) {
            details += "JMenuItem: ";
            details += ((JMenuItem) component).getText();
        } else if (component instanceof JTree) {
            details += "JTree: ";
        } else if (component instanceof JComboBox) {
            details += "JComboBox: ";
            details += ((JComboBox<?>) component).getSelectedItem().toString();
        } else if (component instanceof JList) {
            details += "JList: ";
        } else if (component instanceof JToggleButton) {
            details += "JToggleButton: ";
            details += ((JToggleButton) component).getText();
        } else {
            details += getClassDerivation(component);
        }
        return details;
    }

    /**
     * Returns a string showing the class inheritance hierarchy of an object.
     *
     * <p>Shows the full derivation path from the root class to the actual
     * class, separated by dots.
     *
     * @param object the object to analyze
     * @return a string representing the class hierarchy
     */
    private static String getClassDerivation(final Object object) {
        String derivation = object.getClass().getSimpleName();
        Class<?> currentClass = object.getClass().getSuperclass();
        while (currentClass != null) {
            derivation = currentClass.getSimpleName() + "." + derivation;
            currentClass = currentClass.getSuperclass();
        }
        return derivation;
    }

    /**
     * Appends a hierarchical representation of a component and its children to
     * a StringBuilder.
     *
     * @param component the component to describe
     * @param builder the StringBuilder to append to
     */
    private static void appendComponentStructure(final Component component,
                                                 final StringBuilder builder) {
        appendComponentStructure(component, builder, "");
    }

    /**
     * Appends a hierarchical representation of a component and its children to
     * a StringBuilder with indentation.
     *
     * <p>Recursively processes containers, trees, lists, combo boxes, and menu
     * bars. Each nested level is indented to show the hierarchy.
     *
     * @param component the component to describe
     * @param builder the StringBuilder to append to
     * @param indent the indentation string for this level of nesting
     */
    private static void appendComponentStructure(final Component component,
                                                 final StringBuilder builder,
                                                 final String indent) {
        builder.append(NEWLINE);
        builder.append(indent);
        builder.append(component.getName());
        builder.append("(");
        builder.append(component.getClass().getName());
        builder.append(")");
        builder.append("{");
        builder.append(getComponentDetails(component));
        builder.append("}");
        if (component instanceof JTree) {
            appendTreeNodes(
                    ((JTree) component).getModel(),
                    ((JTree) component).getModel().getRoot(),
                    builder,
                    "|   " + indent);
        }
        if (component instanceof JList<?>) {
            appendListItems(
                    ((JList<?>) component).getModel(),
                    builder,
                    "|   " + indent);
        }
        if (component instanceof JComboBox<?>) {
            appendComboItems(
                    ((JComboBox<?>) component).getModel(),
                    builder,
                    "|   " + indent);
        }
        if (component instanceof JMenuBar) {
            appendMenuItem(component, builder, "|   " + indent);
        } else if (component instanceof Container) {
            for (Component subComponent : ((Container) component)
                    .getComponents()) {
                appendComponentStructure(subComponent, builder,
                        "|   " + indent);
            }
        }
    }

    /**
     * Appends all items in a combo box model to a StringBuilder.
     *
     * @param model the ComboBoxModel to process
     * @param builder the StringBuilder to append to
     * @param indent the indentation string for this level
     */
    private static void appendComboItems(final ComboBoxModel<?> model,
                                         final StringBuilder builder,
                                         final String indent) {
        for (int i = 0; i < model.getSize(); i++) {
            builder.append(NEWLINE);
            builder.append(indent);
            builder.append(model.getElementAt(i).toString());
        }
    }

    /**
     * Appends all items in a list model to a StringBuilder.
     *
     * @param model the ListModel to process
     * @param builder the StringBuilder to append to
     * @param indent the indentation string for this level
     */
    private static void appendListItems(final ListModel<?> model,
                                        final StringBuilder builder,
                                        final String indent) {
        for (int i = 0; i < model.getSize(); i++) {
            builder.append(NEWLINE);
            builder.append(indent);
            builder.append(model.getElementAt(i).toString());
        }
    }

    /**
     * Appends a tree node and its children to a StringBuilder recursively.
     *
     * @param model the TreeModel containing the node
     * @param node the tree node to process
     * @param builder the StringBuilder to append to
     * @param indent the indentation string for this level
     */
    private static void appendTreeNodes(final TreeModel model,
                                        final Object node,
                                        final StringBuilder builder,
                                        final String indent) {
        builder.append(NEWLINE);
        builder.append(indent);
        if (node instanceof Component) {
            builder.append(node.toString());
            appendComponentStructure((Component) node, builder,
                    "|   " + indent);
            builder.append(getClassDerivation(node));
        } else {
            builder.append(node.toString());
            builder.append("  (");
            builder.append(getClassDerivation(node));
            builder.append(")");
        }
        for (int i = 0; i < model.getChildCount(node); i++) {
            appendTreeNodes(model, model.getChild(node, i), builder,
                    "|   " + indent);
        }
    }

    /**
     * Appends a menu item and its sub-elements to a StringBuilder.
     *
     * <p>Handles JMenuBar, JPopupMenu, JMenuItem, and JSeparator components.
     * Recursively processes sub-menus and items.
     *
     * @param menuItem the menu component to process
     * @param builder the StringBuilder to append to
     * @param indent the indentation string for this level
     */
    private static void appendMenuItem(final Component menuItem,
                                       final StringBuilder builder,
                                       final String indent) {
        if (menuItem instanceof JMenuBar) {
            appendMenuSubElements((MenuElement) menuItem, builder, indent);
        } else if (menuItem instanceof JPopupMenu) {
            appendMenuSubElements((MenuElement) menuItem, builder, indent);
        } else if (menuItem instanceof JMenuItem) {
            builder.append(NEWLINE);
            builder.append(indent);
            builder.append(((JMenuItem) menuItem).getText());
            builder.append(((JMenuItem) menuItem).isEnabled()
                    ? "" : "[Disabled]");
            appendMenuSubElements((JMenuItem) menuItem, builder,
                    "|   " + indent);
        } else if (menuItem instanceof JSeparator) {
            builder.append(NEWLINE);
            builder.append(indent);
            builder.append("--------");
        }
    }

    /**
     * Appends all sub-elements of a menu element to a StringBuilder.
     *
     * @param element the MenuElement whose sub-elements to process
     * @param builder the StringBuilder to append to
     * @param indent the indentation string for this level
     */
    private static void appendMenuSubElements(final MenuElement element,
                                              final StringBuilder builder,
                                              final String indent) {
        for (MenuElement subItem : element.getSubElements()) {
            appendMenuItem((Component) subItem, builder, indent);
        }
    }

}
