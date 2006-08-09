/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Jun 27, 2005 (tg): created
 */
package org.knime.base.node.viz.parcoord.visibility;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataCell;


/**
 * 
 * @author Simona Pintilie, University of Konstanz
 */
public class DefaultVisibilityHandler implements VisibilityHandler {

    
    /**
     * Keeps list of registered <code>HiLiteListener</code> to fire event to.
     **/
    private final List<VisibilityListener> m_listenerList;
    
    /**
     * the set with the visible elements.
     */
    private HashSet<DataCell> m_visible;
    /**
     * the set with the selected elements.
     */
    private HashSet<DataCell> m_selected;
    
    
    /**
     * the constructor.
     */
    public DefaultVisibilityHandler() {
        m_visible = new HashSet<DataCell>();
        m_selected = new HashSet<DataCell>();
        
        //inits empty event listener list
        m_listenerList = Collections.synchronizedList(
                new ArrayList<VisibilityListener>());
    }
    
    /** 
     * Adds the <code>VisibilityListener</code> from the list.
     * @param listener The selection listener to be removed from the list.
     */
    public void addVisibilityListener(final VisibilityListener listener) {
        m_listenerList.add(listener);
    }
    
    /**
     * @see VisibilityHandler#removeVisibilityListener(VisibilityListener)
     */
    public void removeVisibilityListener(final VisibilityListener listener) {
        m_listenerList.remove(listener);

    }

    /**
     * @param key the key of the row
     * @return if the row is selected
     * 
     */
    public boolean isSelected(final DataCell key) {
        return m_selected.contains(key);
    }

    /**
     * @param key the key to select
     */
    public void select(final DataCell key) {
        if (key == null) { throw new NullPointerException(); } 
        // synchronize this block
        synchronized (m_selected) {
            if (m_selected.add(key)) {
                fireSelectionEvent(new VisibilityEvent(this, key));
            }
        }  
    }

    /**
     * @param keys the set of keys to select
     */
    public void select(final Set<DataCell> keys) {        
        for (DataCell key : keys) {
                select(key);
        }
    }

    /**
     * @param key the key to unselect
     */
    public void unselect(final DataCell key) {
        if (key == null) { throw new NullPointerException(); } 
        // synchronize this block
        synchronized (m_selected) {
            if (m_selected.remove(key)) {
                fireUnSelectionEvent(new VisibilityEvent(this, key));
            }
        }  
    }

    /**
     * @param keys the set of keys to unselect
     * 
     */
    public void unselect(final Set<DataCell> keys) {
        for (DataCell key : keys) {
            unselect(key);
        }
    }

    /**
     * @see VisibilityHandler#resetSelection()
     */
    public void resetSelection() {
        if (m_selected == null) {
            return;
        }
        unselect(new HashSet<DataCell>(m_selected));
    }

    /**
     * @param key the id of the row key
     * @return if it is visible
     * 
     */
    public boolean isVisible(final DataCell key) {
        return m_visible.contains(key);
    }

    /**
     * @param key the id of the row to make visible
     * 
     */
    public void makeVisible(final DataCell key) {
        if (key == null) { throw new NullPointerException(); } 
        // synchronize this block
        synchronized (m_visible) {
            if (m_visible.add(key)) {
                fireMakeVisibleEvent(new VisibilityEvent(this, key));
            }
        }       
    }

    /**
     * @param keys a set of the keys to make visible
     * 
     */
    public void makeVisible(final Set<DataCell> keys) {
        for (DataCell key : keys) {
            makeVisible(key);
        }
    }

    /**
     * @param key the id of the row to make invisible
     * 
     */
    public void makeInvisible(final DataCell key) {
        if (key == null) { throw new NullPointerException(); } 
        // synchronize this block
        synchronized (m_visible) {
            if (m_visible.remove(key)) {
                fireMakeInvisibleEvent(new VisibilityEvent(this, key));
            }   
           
        }    
    }

    /**
     * @param keys a hash set of the ids to make invisible
     * 
     */
    public void makeInvisible(final Set<DataCell> keys) {
        for (DataCell key : keys) {
            makeInvisible(key);
        }
    }

    /**
     * 
     */
    public void resetVisibility() {
        for (DataCell key : m_selected) {
            makeInvisible(key);
        }
    }
    
    /** 
     * Informs all registered visibility listener to select 
     * the row keys contained 
     * in the visibility event.
     * @param event Contains all rows keys to select.
     */
    private synchronized void fireSelectionEvent(final VisibilityEvent event) {
        assert (event != null);
        for (int vl = 0; vl < m_listenerList.size(); vl++) {
            VisibilityListener l = m_listenerList.get(vl);
            l.select(event);
        }
    }

    /** 
     * Informs all registered visibility listeners 
     * to unselect the row keys contained
     * in the visibility event.
     * @param event Contains all rows keys to unselect.
     */
    private synchronized void fireUnSelectionEvent(final 
            VisibilityEvent event) {
        assert (event != null);
        for (int vl = 0; vl < m_listenerList.size(); vl++) {
            VisibilityListener l = m_listenerList.get(vl);
            l.unselect(event);
        }
    }
    
    /** 
     * Informs all registered visibility listeners to 
     * select the row keys contained 
     * in the visibility event.
     * @param event Contains all rows keys to select.
     */
    private synchronized void fireMakeVisibleEvent(final 
            VisibilityEvent event) {
        assert (event != null);
        for (int vl = 0; vl < m_listenerList.size(); vl++) {
            VisibilityListener l = m_listenerList.get(vl);
            l.makeVisible(event);
        }
    }

    /** 
     * Informs all registered visibility listener to 
     * unselect the row keys contained
     * in the visibility event.
     * @param event Contains all rows keys to unselect.
     */
    private synchronized void fireMakeInvisibleEvent(final 
            VisibilityEvent event) {
        assert (event != null);
        for (int vl = 0; vl < m_listenerList.size(); vl++) {
            VisibilityListener l = m_listenerList.get(vl);
            l.makeInvisible(event);
        }
    }
}
