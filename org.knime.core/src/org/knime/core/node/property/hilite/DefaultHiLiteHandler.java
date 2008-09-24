/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 * 
 * 2006-06-08 (tm): reviewed 
 */
package org.knime.core.node.property.hilite;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.SwingUtilities;

import org.knime.core.data.RowKey;
import org.knime.core.node.NodeLogger;

/**
 * Default implementation for a <code>HiLiteHandler</code> which receives
 * hilite change requests, answers queries and notifies registered listeners. 
 * <p>
 * This implementation keeps a list of row keys only for the hilit items. 
 * Furthermore, an event is only sent for items whose status actually changed.
 * The list of hilite keys is modified (delete or add keys) before
 * the actual event is send.
 * <p> 
 * 
 * @see HiLiteListener
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DefaultHiLiteHandler implements HiLiteHandler {
    
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(DefaultHiLiteHandler.class);

    /** List of registered <code>HiLiteListener</code>s to fire event to. */
    private final CopyOnWriteArrayList<HiLiteListener> m_listenerList;

    /** Set of non-<code>null</code> hilit items. */
    private final Set<RowKey> m_hiLitKeys;

    /** 
     * Creates a new default hilite handler with an empty set of registered 
     * listeners and an empty set of hilit items.
     */
    public DefaultHiLiteHandler() {
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
        for (RowKey c : ids) {
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
        // create list of row keys from input key array
        final Set<RowKey> changedIDs = new LinkedHashSet<RowKey>();
        // iterates over all keys and adds them to the changed set
        for (RowKey id : ids) {
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
            // throw hilite event
            fireHiLiteEventInternal(new KeyEvent(this, changedIDs));
        }
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
        // create list of row keys from input key array
        final Set<RowKey> changedIDs = new LinkedHashSet<RowKey>();
        // iterate over all keys and removes all not hilit ones
        for (RowKey id : ids) {
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
            fireUnHiLiteEventInternal(new KeyEvent(this, changedIDs));
        }
    }

    /**
     * Resets the hilit status of all row IDs. Every row ID will be unhilit
     * after the call to this method. Sends an event to all registered listeners
     * with all previously hilit row IDs, if at least one key was effected 
     * by this call.
     */
    public synchronized void fireClearHiLiteEvent() {
        if (!m_hiLitKeys.isEmpty()) {
            /*
             * Do not change this implementation, see #fireHiLiteEvent for
             * more details.
             */
            m_hiLitKeys.clear();
            fireClearHiLiteEventInternal();
        }
    } 
    
    /** 
     * Informs all registered hilite listener to hilite the row keys contained 
     * in the key event.
     * 
     * @param event Contains all rows keys to hilite.
     */
    protected void fireHiLiteEventInternal(final KeyEvent event) {
        final Runnable r = new Runnable() {
            public void run() {
                for (HiLiteListener l : m_listenerList) {
                    try {
                        l.hiLite(event);
                    } catch (Throwable t) {
                        LOGGER.coding("Exception while notifying listeners", t);
                    }
                }
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            if (SwingUtilities.isEventDispatchThread()) {
                r.run();
            } else {
                SwingUtilities.invokeLater(r);
            }
        }
    }

    /**
     * Informs all registered hilite listener to unhilite the row keys contained
     * in the key event.
     * 
     * @param event Contains all rows keys to unhilite.
     */
    protected void fireUnHiLiteEventInternal(final KeyEvent event) {
        final Runnable r = new Runnable() {
            public void run() {
                for (HiLiteListener l : m_listenerList) {
                    try {
                        l.unHiLite(event);
                    } catch (Throwable t) {
                        LOGGER.coding("Exception while notifying listeners", t);
                    }
                }
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }
    
    /** used to fire empty key event during clear hilite. */
    private final KeyEvent EMPTY_KEY_EVENT = new KeyEvent(this,
            (Set<RowKey>) Collections.EMPTY_SET);
    
    /**
     * Informs all registered hilite listener to reset all hilit rows.
     */
    protected void fireClearHiLiteEventInternal() {
        final Runnable r = new Runnable() {
            public void run() {
                for (HiLiteListener l : m_listenerList) {
                    try {
                        l.unHiLiteAll(EMPTY_KEY_EVENT);
                    } catch (Throwable t) {
                        LOGGER.coding("Exception while notifying listeners", t);
                    }
                }
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            if (SwingUtilities.isEventDispatchThread()) {
                r.run();
            } else {
                SwingUtilities.invokeLater(r);
            }
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
