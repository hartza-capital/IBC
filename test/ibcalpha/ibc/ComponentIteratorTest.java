// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for ComponentIterator.

package ibcalpha.ibc;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentIteratorTest {

    @Test
    void iteratesFlatContainer() {
        JPanel panel = new JPanel();
        JButton button = new JButton("Click");
        JLabel label = new JLabel("Label");
        panel.add(button);
        panel.add(label);

        ComponentIterator it = new ComponentIterator(panel);
        List<Component> found = new ArrayList<>();
        while (it.hasNext()) {
            found.add(it.next());
        }

        assertEquals(2, found.size());
        assertEquals(button, found.get(0));
        assertEquals(label, found.get(1));
    }

    @Test
    void iteratesNestedContainer() {
        JPanel outer = new JPanel();
        JPanel inner = new JPanel();
        JTextField field = new JTextField("text");
        inner.add(field);
        outer.add(inner);

        ComponentIterator it = new ComponentIterator(outer);
        List<Component> found = new ArrayList<>();
        while (it.hasNext()) {
            found.add(it.next());
        }

        assertEquals(2, found.size());
        assertEquals(inner, found.get(0));
        assertEquals(field, found.get(1));
    }

    @Test
    void hasNextIsIdempotent() {
        JPanel panel = new JPanel();
        panel.add(new JButton("B"));

        ComponentIterator it = new ComponentIterator(panel);
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertEquals("B", ((JButton) it.next()).getText());
        assertFalse(it.hasNext());
    }

    @Test
    void nextThrowsWhenExhausted() {
        JPanel panel = new JPanel();
        ComponentIterator it = new ComponentIterator(panel);
        assertThrows(java.util.NoSuchElementException.class, it::next);
    }

    @Test
    void removeIsUnsupported() {
        JPanel panel = new JPanel();
        panel.add(new JButton("B"));
        ComponentIterator it = new ComponentIterator(panel);
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove);
    }
}
