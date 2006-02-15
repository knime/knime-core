/*
 * @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 */
package de.unikn.knime.core.node.property.hilite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.unikn.knime.core.data.DataCell;

/**
 * Default implementation for a <code>HiLiteHandler</code> which recieves
 * hilite change request, answers queries and notifies registered listeners. 
 *
 * <p>This implementation keeps a list of row keys only for the hilit items. 
 * Furthermore, an event is only send with items whose status actually changed.
 * The list of hilite keys is modified (delete or add keys) before
 * the actual event is send.
 * 
 * @see HiLiteListener
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DefaultHiLiteHandler implements HiLiteHandler {

    /* 
     * Keeps list of registered <code>HiLiteListener</code> to fire event to.
     */
    private final List<HiLiteListener> m_listenerList;

    /* 
     * Keeps list of all hilit items. 
     */
    private final HashSet<DataCell> m_hiLitKeys;

    /** 
     * Creates a new default hilite handler with an empty list of registered 
     * listeners and an empty list of hilit items.
     */
    public DefaultHiLiteHandler() {
        // inits empty event listener list
        m_listenerList = 
            Collections.synchronizedList(new ArrayList<HiLiteListener>());
        // initialize item list
        m_hiLitKeys = new HashSet<DataCell>();
    }
    
    /**
     * Appends a new hilite listener at the end of the list, if the 
     * listener has not been added before. This method does not send a 
     * hilite event to the new listener.
     * @param  listener The hilite listener to append to the list.
     */
    public void addHiLiteListener(final HiLiteListener listener) {
        m_listenerList.add(listener);
    }

    /**
     * Removes the given hilite listener from the list. 
     * @param  listener The hilite listener to remove from the list.
     */
    public void removeHiLiteListener(final HiLiteListener listener) {
        m_listenerList.remove(listener);
    }

    /**
     * Returns <b>true</b> if the specified row key is hilit.
     * @param  id The row ID to check the hilite status for.
     * @return <b>true</b> if the row key is hilit.
     * @throws NullPointerException If the given row key is <code>null</code>. 
     */
    public boolean isHiLit(final DataCell id) {
        return m_hiLitKeys.contains(id);
    } 

    /**
     * Sets the status of the specified row key to 'hilit'. It will send a
     * hilite event to all registered listeners - only if the key was not hilit
     * before.
     * @param  id The row ID to set hilited.
     * @throws NullPointerException If the given row key is <code>null</code>. 
     */
    public synchronized void hiLite(final DataCell id) {
        if (id == null) { throw new NullPointerException(); } 
        if (m_hiLitKeys.add(id)) {
            fireHiLiteEvent(new KeyEvent(this, id));
        }
    }

    /**
     * Sets the status of the specified row key to 'unhilit'. It will send a
     * unhilite event to all registered listeners - only if the key was hilit
     * before.
     * @param  id The row ID to set unhilited.
     * @throws NullPointerException If the given row key is <code>null</code>. 
     */    
    public synchronized void unHiLite(final DataCell id) {
        if (id == null) { throw new NullPointerException(); } 
        if (m_hiLitKeys.remove(id)) {
            fireUnHiLiteEvent(new KeyEvent(this, id));
        }
    }

    /**
     * Sets the status of all specified row IDs in the set to 'hilit'. 
     * It will send a hilite event to all registered listeners - only for the 
     * IDs that were not hilit before.
     * @param  ids A set of row IDs to set hilited.
     * @throws NullPointerException If the given set is <code>null</code> or
     *           contains a null element. 
     */
     public synchronized void hiLite(final Set<DataCell> ids) {
        // check key set
        if (ids == null) { throw new NullPointerException(); } 
        // synchronize this block
        // create list of row keys from input key array
        final HashSet<DataCell> changedIDs = new HashSet<DataCell>();
        // iterates over all keys and adds them to the changed set
        for (DataCell id : ids) {
            if (id == null) { throw new NullPointerException(); }
            // if the key is already hilit, remove it from the cleaned list
            if (m_hiLitKeys.add(id)) {
                changedIDs.add(id);
                assert (ids.contains(id));
            }
        }
        // if at least on key changed
        if (changedIDs.size() > 0) {
            // throw hilite event
            fireHiLiteEvent(new KeyEvent(this, changedIDs));
        }
    } // hiLite(DataCellSet)

     /**
      * Sets the status of all specified row IDs in the set to 'unhilit'. 
      * It will send a unhilite event to all registered listeners - only for 
      * the IDs that were hilit before.
      * @param  ids A set of row IDs to set unhilited.
      * @throws NullPointerException If the given set is <code>null</code> or
      *           contains a null element. 
      */
     public synchronized void unHiLite(final Set<DataCell> ids) {
        // check IDs set
        if (ids == null) { throw new NullPointerException(); } 
        // create list of row keys from input key array
        final HashSet<DataCell> changedIDs = new HashSet<DataCell>();
        // iterate over all keys and removes all not hilit ones
        for (DataCell id : ids) {
            if (id == null) { throw new NullPointerException(); }
            // if the ID has not been hilit remove from the cleaned list 
            if (m_hiLitKeys.remove(id)) {
                changedIDs.add(id);
                assert (ids.contains(id));
            }
        }
        // if at least on key changed
        if (changedIDs.size() > 0) {
            // throw hilite event
            fireUnHiLiteEvent(new KeyEvent(this, changedIDs));
        }
    }   // unHiLite(DataCellSet)
        
    /**
     * Resets the hilit status of all row IDs. Every row ID will be unhilit
     * after the call to this method. Sends an event to all registered listeners
     * with all previously hilit row IDs.
     */
    public synchronized void resetHiLite() {
            // TODO fireResetHiLiteEvent()
        Set<DataCell> clone = 
            Collections.unmodifiableSet(new HashSet<DataCell>(m_hiLitKeys));
        m_hiLitKeys.clear();
        fireUnHiLiteEvent(new KeyEvent(this, clone));
    } 
    
    /** 
     * Informs all registered hilite listener to hilite the row keys contained 
     * in the key event.
     * @param event Contains all rows keys to hilite.
     */
    private synchronized void fireHiLiteEvent(final KeyEvent event) {
        assert (event != null);
        for (int l = m_listenerList.size() - 1; l >= 0; l--) {
            HiLiteListener listener = m_listenerList.get(l);
            listener.hiLite(event);
        }
    }

    /** 
     * Informs all registered hilite listener to unhilite the row keys contained
     * in the key event.
     * @param event Contains all rows keys to unhilite.
     */
    private synchronized void fireUnHiLiteEvent(final KeyEvent event) {
        assert (event != null);
        for (int l = m_listenerList.size() - 1; l >= 0; l--) {
            HiLiteListener listener = m_listenerList.get(l);
            listener.unHiLite(event);
        }
    }
    
    /** 
     * Informs all registered hilite listener to reset all hilit rows.
     */
//    private synchronized void fireResetHiLiteEvent() {
//        for (Iterator it = m_listenerList.iterator(); it.hasNext();) {
//            HiLiteListener l = (HiLiteListener) it.next();
//            l.resetHiLite();
//        }
//    }

} // DefaultHiLiteHandler
