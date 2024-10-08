/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.property.hilite;

import static org.knime.core.node.property.hilite.HiLiteTranslator.FORCE_SYNC_FIRE;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

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

    /** Unique ID for this {@link HiLiteHandler} */
    private final UUID m_hiliteHandlerID;

    /** List of registered <code>HiLiteListener</code>s to fire event to. */
    private final CopyOnWriteArrayList<HiLiteListener> m_listenerList;

    /** Set of non-<code>null</code> hilit items. */
    private Set<RowKey> m_hiLitKeys;

    /** Not-null if this {@link HiLiteHandler} is associated with one or more {@link HiLiteTranslator}s */
    private Set<HiLiteTranslator> m_hiliteTranslators;

    /** Not-null if this {@link HiLiteHandler} is associated with one or more {@link HiLiteManager}s */
    private Set<HiLiteManager> m_hiliteManagers;

    /**
     * Creates a new default hilite handler with an empty set of registered
     * listeners and an empty set of hilit items.
     */
    public HiLiteHandler() {
        m_hiliteHandlerID = UUID.randomUUID();
        m_listenerList = new CopyOnWriteArrayList<>();
        // initialize item list
        m_hiLitKeys = new LinkedHashSet<>();
        m_hiliteTranslators = new LinkedHashSet<>();
        m_hiliteManagers = new LinkedHashSet<>();
    }

    /**
     * Returns a unique ID for this hilite handler instance
     * @return a unique hiliteHandler ID
     * @since 3.4
     */
    public UUID getHiliteHandlerID() {
        return m_hiliteHandlerID;
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
     * Returns a set of {@link HiLiteTranslator}, if this {@link HiLiteHandler} instance is associated with it, never null.
     * @return A set of {@link HiLiteTranslator}s, or null
     * @since 3.4
     * @noreference This method is not intended to be referenced by clients.
     */
    public Set<HiLiteTranslator> getHiLiteTranslators() {
        return m_hiliteTranslators;
    }

    /**
     * Associates a {@link HiLiteTranslator} with this {@link HiLiteHandler} instance.
     *
     * @param hiliteTranslator the {@link HiLiteTranslator} to add
     * @throws IllegalArgumentException when trying to associate more than two {@link HiLiteTranslator} with this
     *             {@link HiLiteHandler} instance
     */
    final void addHiLiteTranslator(final HiLiteTranslator hiliteTranslator) {
        if (hiliteTranslator != null) {
            LOGGER.debug("Adding hilite translator to handler " + m_hiliteHandlerID);
            m_hiliteTranslators.add(hiliteTranslator);
        }
    }

    /**
     * Removes an associated {@link HiLiteTranslator} from this {@link HiLiteHandler} instance
     *
     * @param hiliteTranslator the {@link HiLiteTranslator} to remove
     * @return true, if the given {@link HiLiteTranslator} was associated with this {@link HiLiteHandler} instance and
     *         removed, false otherwise
     */
    final boolean removeHiLiteTranslator(final HiLiteTranslator hiliteTranslator) {
        if (hiliteTranslator != null) {
            LOGGER.debug("Removing hilite translator from handler " + m_hiliteHandlerID);
            return m_hiliteTranslators.remove(hiliteTranslator);
        }
        return false;
    }

    /**
     * Returns a set of {@link HiLiteManager}, if this {@link HiLiteHandler} instance is associated with it, never null.
     * @return A set of {@link HiLiteManager}s
     * @since 3.4
     * @noreference This method is not intended to be referenced by clients.
     */
    public final Set<HiLiteManager> getHiLiteManagers() {
        return m_hiliteManagers;
    }

    /**
     * Associates a {@link HiLiteManager} with this {@link HiLiteHandler} instance.
     *
     * @param hiliteManager the {@link HiLiteManager} to add
     * @throws IllegalArgumentException when trying to associate more than two {@link HiLiteManager} with this
     *             {@link HiLiteHandler} instance
     */
    final void addHiLiteManager(final HiLiteManager hiliteManager) {
        if (hiliteManager != null) {
            LOGGER.debug("Adding hilite manager to handler " + m_hiliteHandlerID);
            m_hiliteManagers.add(hiliteManager);
        }
    }

    /**
     * Removes an associated {@link HiLiteManager} from this {@link HiLiteHandler} instance
     *
     * @param hiliteManager the {@link HiLiteManager} to remove
     * @return true, if the given {@link HiLiteManager} was associated with this {@link HiLiteHandler} instance and
     *         removed, false otherwise
     */
    final boolean removeHiLiteManager(final HiLiteManager hiliteManager) {
        if (hiliteManager != null) {
            LOGGER.debug("Removing hilite manager from handler " + m_hiliteHandlerID);
            return m_hiliteManagers.remove(hiliteManager);
        }
        return false;
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
        fireHiLiteEvent(event, true);
    }

    /**
     * Informs all registered hilite listener to hilite the row keys contained in the key event.
     *
     * @param event Contains all rows keys to hilite.
     * @param async if {@code false} this method will return until all registered {@link HiLiteListener HiLiteListeners}
     *            have been called; if {@code true} calling the listeners will be carried out asynchronously
     */
    public synchronized void fireHiLiteEvent(final KeyEvent event, final boolean async) {
        if (event == null) {
            throw new NullPointerException("KeyEvent must not be null");
        }

        Set<RowKey> newHilitKeys = new LinkedHashSet<RowKey>(m_hiLitKeys);

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
            if (newHilitKeys.add(id)) {
                changedIDs.add(id);
            }
        }

        // if at least on key changed
        if (!changedIDs.isEmpty()) {
            m_hiLitKeys = newHilitKeys;
            final KeyEvent fireEvent = new KeyEvent(event.getSource(), changedIDs);
            runOnListeners(l -> l.hiLite(fireEvent), async);
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
        fireUnHiLiteEvent(event, true);
    }

    /**
     * Informs all registered hilite listener to unhilite the row keys contained in the key event.
     *
     * @param event Contains all rows keys to unhilite.
     * @param async if {@code false} this method will return until all registered {@link HiLiteListener HiLiteListeners}
     *            have been called; if {@code true} calling the listeners will be carried out asynchronously
     */
    public synchronized void fireUnHiLiteEvent(final KeyEvent event, final boolean async) {
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

        Set<RowKey> newHilitKeys = new LinkedHashSet<RowKey>(m_hiLitKeys);

        // create list of row keys from input key array
        final Set<RowKey> changedIDs = new LinkedHashSet<RowKey>();
        // iterate over all keys and removes all not hilit ones
        for (final RowKey id : ids) {
            if (id == null) {
                throw new IllegalArgumentException(
                        "Key array must not contains null elements.");
            }
            if (newHilitKeys.remove(id)) {
                changedIDs.add(id);
            }
        }
        // if at least on key changed
        if (!changedIDs.isEmpty()) {
            m_hiLitKeys = newHilitKeys;
            // throw unhilite event
            final KeyEvent fireEvent = new KeyEvent(
                    event.getSource(), changedIDs);
            runOnListeners(l -> l.unHiLite(fireEvent), async);
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
     *
     * @param event the event fired for clear hilite
     */
    public synchronized void fireClearHiLiteEvent(final KeyEvent event) {
        fireClearHiLiteEvent(event, true);
    }

    /**
     * Informs all registered hilite listener to reset all hilit rows.
     *
     * @param event the event fired for clear hilite
     * @param async if {@code false} this method will return until all registered {@link HiLiteListener
     *            HiLiteListeners} have been called; if {@code true} calling the listeners will be carried out
     *            asynchronously
     * @since 5.4
     */
    public synchronized void fireClearHiLiteEvent(final KeyEvent event, final boolean async) {
        if (event == null) {
            throw new NullPointerException("KeyEvent must not be null");
        }
        /*
         * Do not change this implementation, see #fireHiLiteEvent for
         * more details.
         */
        if (!m_hiLitKeys.isEmpty()) {
            m_hiLitKeys = new LinkedHashSet<RowKey>();
            runOnListeners(l -> l.unHiLiteAll(event), async);
        }
    }

    /**
     * Informs all registered hilite listener to replace all hilit rows with the specified row keys.
     *
     * @param ids a set of row keys to hilite
     * @throws NullPointerException if the set of row keys to hilite is <code>null</code>
     * @since 4.5
     */
    public synchronized void fireReplaceHiLiteEvent(final RowKey... ids) {
        Objects.requireNonNull(ids, "Key array must not be null.");
        fireReplaceHiLiteEvent(new KeyEvent(this, ids));

    }

    /**
     * Informs all registered hilite listener to replace all hilit rows with the specified row keys.
     *
     * @param ids a set of row keys to hilite
     * @throws NullPointerException if the set of row keys to hilite is <code>null</code>
     * @since 4.5
     */
    public synchronized void fireReplaceHiLiteEvent(final Set<RowKey> ids) {
        Objects.requireNonNull(ids, "Key set must not be null.");
        fireReplaceHiLiteEvent(new KeyEvent(this, ids));
    }

    /**
     * Informs all registered hilite listener to replace all hilit rows.
     *
     * @param event the event fired for hilite replacement
     * @throws NullPointerException if the key event is <code>null</code>
     * @since 4.5
     */
    public synchronized void fireReplaceHiLiteEvent(final KeyEvent event) {
        fireReplaceHiLiteEvent(event, true);
    }

    /**
     * Informs all registered hilite listener to replace all hilit rows.
     *
     * @param event the event fired for hilite replacement
     * @param async if {@code false} this method will return until all registered {@link HiLiteListener HiLiteListeners}
     *            have been called; if {@code true} calling the listeners will be carried out asynchronously
     * @throws NullPointerException if the key event is <code>null</code>
     * @since 4.5
     */
    public synchronized void fireReplaceHiLiteEvent(final KeyEvent event, final boolean async) {
        Objects.requireNonNull(event, "KeyEvent must not be null");

        final var keys = event.keys();
        if (!m_hiLitKeys.equals(keys)) {
            m_hiLitKeys = new LinkedHashSet<>(keys);
            runOnListeners(l -> l.replaceHiLite(event), async);
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

    private void runOnListeners(final Consumer<HiLiteListener> callOnListener, final boolean async) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                for (final HiLiteListener l : m_listenerList) {
                    try {
                        callOnListener.accept(l);
                    } catch (final Throwable t) {
                        LOGGER.coding("Exception while notifying listeners, reason: " + t.getMessage(), t);
                    }
                }
            }
        };

        if (async) {
            ViewUtils.runOrInvokeLaterInEDT(r);
        } else {
            // Only allow the flag to be removed again by the 'setter'.
            // Otherwise it will be removed too early since this method is called recursively.
            var removeForceSyncFireFlag = false;
            try {
                if (FORCE_SYNC_FIRE.get() == null) {
                    FORCE_SYNC_FIRE.set(Boolean.TRUE);
                    removeForceSyncFireFlag = true;
                }
                r.run();
            } finally {
                if (removeForceSyncFireFlag) {
                    FORCE_SYNC_FIRE.remove();
                }
            }
        }
    }

}
