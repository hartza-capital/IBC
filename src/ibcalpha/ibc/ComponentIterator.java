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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Depth-first iterator over a Swing component hierarchy.
 */
class ComponentIterator implements Iterator<Component> {

    /** Stack of component tree nodes yet to visit. */
    private Deque<NodeState> mStack = new ArrayDeque<>();

    /** The component most recently returned by {@link #next()}. */
    private Component mCurrent;

    /**
     * Creates an iterator over the component hierarchy rooted at container.
     *
     * @param container the root container to iterate
     */
    ComponentIterator(final Container container) {
        if (container == null) {
            Utils.exitWithException(
                    ErrorCodes.UNHANDLED_EXCEPTION,
                    new NullPointerException());
        }
        mStack.push(new NodeState(container));
    }

    /**
     * Returns true if there are more components to iterate over.
     *
     * @return true if a next component is available, false otherwise
     */
    @Override
    public boolean hasNext() {
        if (mCurrent == null) {
            return moveNext();
        }
        return true;
    }

    /**
     * Returns the next component in the iteration.
     *
     * @return the next component in the iteration
     * @throws NoSuchElementException if there are no more components
     */
    @Override
    public Component next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        final Component result = mCurrent;
        mCurrent = null;
        return result;
    }

    /**
     * Removes the current element from the iteration (not supported).
     *
     * @throws UnsupportedOperationException always, as removal is not supported
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     * Advances the iterator to the next component in depth-first order.
     *
     * @return true if a next component was found, false if iteration is
     *         exhausted
     */
    private boolean moveNext() {
        if (mStack.isEmpty()) {
            return false;
        }
        while (true) {
            final NodeState currentState = mStack.peek();
            if (currentState.subComponents == null) {
                mStack.pop();
                if (mStack.isEmpty()) {
                    return false;
                }
            } else {
                if (currentState.index < currentState.subComponents.length) {
                    mCurrent = currentState.subComponents[
                            currentState.index++];
                    mStack.push(new NodeState(mCurrent));
                    return true;
                } else {
                    mStack.pop();
                    if (mStack.isEmpty()) {
                        return false;
                    }
                }
            }
        }
    }

    private class NodeState {
        /** Index of the next child component to process. */
        private int index;

        /** Child components of the current node. */
        private Component[] subComponents;

        /**
         * Captures the state for a node in the component tree.
         *
         * @param component the component whose children will be iterated
         */
        NodeState(final Component component) {
            synchronized (component.getTreeLock()) {
                this.index = 0;
                if (component instanceof Container) {
                    subComponents = ((Container) component).getComponents();
                }
            }
        }
    }

}
