/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 */
package org.knime.core.node.property.hilite;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.knime.core.data.RowKey;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.ViewUtils;

/**
 * <code>HiLiteHandler</code> implementation which receives hilite change
 * requests, answers, queries, and notifies registered listeners.
 * <p>
 * This implementation keeps a list of row keys only for the hilit items.
 * Furthermore, an event is only sent for items whose status actually changed.
 * The list of hilite keys is modified (delete or add keys) before the actual
 * event is send.
 * <p>
 * Do NOT derive this class which intended to be final but can't due to the
 * historical <code>DefaultHiLiteHandler</code> class.
 *
 * @see HiLiteListener
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public class HiLiteHandler {

    /**
     * Constant for the Menu entry 'HiLite'.
     */
    public static final String HILITE = "HiLite";

    /**
     * Constant for the menu entry 'HiLite Selected'.
     */
    public static final String HILITE_SELECTED = "HiLite selected";

    /**
     * Constant for the menu entry 'UnHiLite Selected'.
     */
    public static final String UNHILITE_SELECTED = "UnHiLite selected";

    /**
     * Constant for the menu entry 'Clear HiLite'.
     */
    public static final String CLEAR_HILITE = "Clear HiLite";

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(HiLiteHandler.class);

    /** List of registered <code>HiLiteListener</code>s to fire event to. */
    private final CopyOnWriteArrayList<HiLiteListener> m_listenerList;

    /** Set of non-<code>null</code> hilit items. */
    private final Set<RowKey> m_hiLitKeys;

    /**
     * Creates a new default hilite handler with an empty set of registered
     * listeners and an empty set of hilit items.
     */
    public HiLiteHandler() {
        m_listenerList = new CopyOnWriteArrayList<HiLiteListener>();
        // initialize item list
        m_hiLitKeys = new LinkedHashSet<RowKey>();
    }

    /**
     * Appends a new hilite listener at the end of the list, if the
     * listener has not been added before. This method does not send a
     * hilite event to the new listener.
     *
     * @param listener the hilite listener to append to the list
     */
    public void addHiLiteListener(final HiLiteListener listener) {
        if (!m_listenerList.contains(listener)) {
            m_listenerList.add(listener);
        }
    }

    /**
     * Removes the given hilite listener from the list.
     *
     * @param listener the hilite listener to remove from the list
     */
    public void removeHiLiteListener(final HiLiteListener listener) {
        m_listenerList.remove(listener);
    }

    /**
     * Removes all hilite listeners from the list.
     */
    public void removeAllHiLiteListeners() {
        m_listenerList.clear();
    }

    /**
     * Returns <code>true</code> if the specified row IDs are hilit.
     *
     * @param ids the row IDs to check the hilite status for
     * @return <code>true</code> if all row IDs are hilit
     * @throws IllegalArgumentException if this array or one of its elements is
     *         <code>null</code>.
     */
    public boolean isHiLit(final RowKey... ids) {
        if (ids == null) {
            throw new IllegalArgumentException("Key array must not be null.");
        }
        for (final RowKey c : ids) {
            if (c == null) {
                throw new IllegalArgumentException(
                        "Key array must not contain null elements.");
            }
            if (!m_hiLitKeys.contains(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sets the status of the specified row IDs to 'hilit'. It will send a
     * hilite event to all registered listeners - only if the keys were not
     * hilit before.
     *
     * @param ids the row IDs to set hilited.
     */
    public synchronized void fireHiLiteEvent(final RowKey... ids) {
        this.fireHiLiteEvent(new LinkedHashSet<RowKey>(Arrays.asList(ids)));
    }

    /**
     * Sets the status of the specified row IDs to 'unhilit'. It will send a
     * unhilite event to all registered listeners - only if the keys were hilit
     * before.
     *
     * @param ids the row IDs to set unhilited
     */
    public synchronized void fireUnHiLiteEvent(final RowKey... ids) {
        this.fireUnHiLiteEvent(new LinkedHashSet<RowKey>(Arrays.asList(ids)));
    }

    /**
     * Sets the status of all specified row IDs in the set to 'hilit'.
     * It will send a hilite event to all registered listeners - only for the
     * IDs that were not hilit before.
     *
     * @param ids a set of row IDs to set hilited
     * @throws IllegalArgumentException if the set or one of its elements is
     *      <code>null</code>
     */
    public synchronized void fireHiLiteEvent(final Set<RowKey> ids) {
        if (ids == null) {
            throw new IllegalArgumentException("Key array must not be null.");
        }
        fireHiLiteEvent(new KeyEvent(this, ids));
    }

    /**
     * Sets the status of all specified row IDs in the set to 'unhilit'.
     * It will send a unhilite event to all registered listeners - only for
     * the IDs that were hilit before.
     *
     * @param ids a set of row IDs to set unhilited
     * @throws IllegalArgumentException if the set or one of its elements is
     *      <code>null</code>
     */
    public synchronized void fireUnHiLiteEvent(final Set<RowKey> ids) {
        if (ids == null) {
            throw new IllegalArgumentException("Key array must not be null.");
        }
        /*
         * Do not change this implementation, see #fireHiLiteEvent for
         * more details.
         */
        fireUnHiLiteEvent(new KeyEvent(this, ids));
    }

    /**
     * Resets the hilit status of all row IDs. Every row ID will be unhilit
     * after the call to this method. Sends an event to all registered listeners
     * with all previously hilit row IDs, if at least one key was effected
     * by this call.
     */
    public synchronized void fireClearHiLiteEvent() {
        if (!m_hiLitKeys.isEmpty()) {
            fireClearHiLiteEvent(new KeyEvent(this));
        }
    }

    /**
     * Informs all registered hilite listener to hilite the row keys contained
     * in the key event.
     *
     * @param event Contains all rows keys to hilite.
     * @deprecated Replaced by public method {@link #fireHiLiteEvent(KeyEvent)}.
     */
    @Deprecated
    protected void fireHiLiteEventInternal(final KeyEvent event) {
        fireHiLiteEvent(event);
    }

    /**
     * Informs all registered hilite listener to hilite the row keys contained
     * in the key event.
     *
     * @param event Contains all rows keys to hilite.
     */
    public synchronized void fireHiLiteEvent(final KeyEvent event) {
        if (event == null) {
            throw new NullPointerException("KeyEvent must not be null");
        }
        /*
         * Do not change this implementation, unless you are aware of the
         * following problem:
         * To ensure that no intermediate event interrupts the current hiliting
         * procedure, all hilite events are queued into the AWT event queue.
         * That means, just  before the event is processed the hilite key set is
         * modified and then the event is fired. That ensures that the keys are
         * not marked as hilit, before the actual event is sent. You must not
         * care about the current thread (e.g. EDT) to queue this event, since
         * the event must be queued in both cases to avoid nested events to be
         * waiting on each other.
         */
        final Set<RowKey> ids = event.keys();
        // check if at least one id is present
        if (ids.isEmpty()) {
            return;
        }
        // create list of row keys from input key array
        final Set<RowKey> changedIDs = new LinkedHashSet<RowKey>();
        // iterates over all keys and adds them to the changed set
        for (final RowKey id : ids) {
            if (id == null) {
                throw new IllegalArgumentException(
                        "Key array must not contains null elements.");
            }
            // if the key is already hilit, do not add it
            if (m_hiLitKeys.add(id)) {
                changedIDs.add(id);
            }
        }
        // if at least on key changed
        if (changedIDs.size() > 0) {
            final KeyEvent fireEvent =
                new KeyEvent(event.getSource(), changedIDs);
            final Runnable r = new Runnable() {
                public void run() {
                    for (final HiLiteListener l : m_listenerList) {
                        try {
                            l.hiLite(fireEvent);
                        } catch (final Throwable t) {
                            LOGGER.coding(
                                    "Exception while notifying listeners, "
                                        + "reason: " + t.getMessage(), t);
                        }
                    }
                }
            };
            ViewUtils.runOrInvokeLaterInEDT(r);
        }
    }

    /**
     * Informs all registered hilite listener to unhilite the row keys contained
     * in the key event.
     *
     * @param event Contains all rows keys to unhilite.
     * @deprecated Replaced by {@link #fireUnHiLiteEvent(KeyEvent)}.
     */
    @Deprecated
    protected void fireUnHiLiteEventInternal(final KeyEvent event) {
        fireUnHiLiteEvent(event);
    }

    /**
     * Informs all registered hilite listener to unhilite the row keys contained
     * in the key event.
     *
     * @param event Contains all rows keys to unhilite.
     */
    public synchronized void fireUnHiLiteEvent(final KeyEvent event) {
        if (event == null) {
            throw new NullPointerException("KeyEvent must not be null");
        }
        /*
         * Do not change this implementation, see #fireHiLiteEvent for
         * more details.
         */
        final Set<RowKey> ids = event.keys();
        // check if at least one id is present
        if (ids.isEmpty()) {
            return;
        }
        // create list of row keys from input key array
        final Set<RowKey> changedIDs = new LinkedHashSet<RowKey>();
        // iterate over all keys and removes all not hilit ones
        for (final RowKey id : ids) {
            if (id == null) {
                throw new IllegalArgumentException(
                        "Key array must not contains null elements.");
            }
            if (m_hiLitKeys.remove(id)) {
                changedIDs.add(id);
            }
        }
        // if at least on key changed
        if (changedIDs.size() > 0) {
            // throw unhilite event
            final KeyEvent fireEvent = new KeyEvent(
                    event.getSource(), changedIDs);
            final Runnable r = new Runnable() {
                public void run() {
                    for (final HiLiteListener l : m_listenerList) {
                        try {
                            l.unHiLite(fireEvent);
                        } catch (final Throwable t) {
                            LOGGER.coding(
                                 "Exception while notifying listeners, reason: "
                                        + t.getMessage(), t);
                        }
                    }
                }
            };
            ViewUtils.runOrInvokeLaterInEDT(r);
        }
    }

    /**
     * Informs all registered hilite listener to reset all hilit rows.
     * @param event the event fired for clear hilite
     * @deprecated Replaced by {@link #fireClearHiLiteEvent(KeyEvent)}
     */
    @Deprecated
    protected void fireClearHiLiteEventInternal(final KeyEvent event) {
        fireClearHiLiteEvent(event);
    }

    /**
     * Informs all registered hilite listener to reset all hilit rows.
     * @param event the event fired for clear hilite
     */
    public synchronized void fireClearHiLiteEvent(final KeyEvent event) {
        if (event == null) {
            throw new NullPointerException("KeyEvent must not be null");
        }
        /*
         * Do not change this implementation, see #fireHiLiteEvent for
         * more details.
         */
        if (m_hiLitKeys.size() > 0) {
            m_hiLitKeys.clear();
            final Runnable r = new Runnable() {
                public void run() {
                    for (final HiLiteListener l : m_listenerList) {
                        try {
                            l.unHiLiteAll(event);
                        } catch (final Throwable t) {
                            LOGGER.coding(
                                "Exception while notifying listeners, reason: "
                                    + t.getMessage(), t);
                        }
                    }
                }
            };
            ViewUtils.runOrInvokeLaterInEDT(r);
        }
    }

    /**
     * Returns a copy of all hilit keys.
     * @return a set of hilit row keys
     * @see HiLiteHandler#getHiLitKeys()
     */
    public Set<RowKey> getHiLitKeys() {
        return new LinkedHashSet<RowKey>(m_hiLitKeys);
    }
}
