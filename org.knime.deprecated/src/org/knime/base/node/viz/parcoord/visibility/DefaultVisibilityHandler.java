/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Jun 27, 2005 (tg): created
 */
package org.knime.base.node.viz.parcoord.visibility;


import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.knime.core.data.RowKey;
import org.knime.core.node.NodeLogger;


/**
 * 
 * @author Simona Pintilie, University of Konstanz
 */
public class DefaultVisibilityHandler implements VisibilityHandler {
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(DefaultVisibilityHandler.class);
    
    /**
     * Keeps list of registered <code>HiLiteListener</code> to fire event to.
     **/
    private final CopyOnWriteArrayList<VisibilityListener> m_listenerList;
    
    /**
     * the set with the visible elements.
     */
    private HashSet<RowKey> m_visible;
    /**
     * the set with the selected elements.
     */
    private HashSet<RowKey> m_selected;
    
    
    /**
     * the constructor.
     */
    public DefaultVisibilityHandler() {
        m_visible = new HashSet<RowKey>();
        m_selected = new HashSet<RowKey>();
        
        //inits empty event listener list
        m_listenerList = new CopyOnWriteArrayList<VisibilityListener>();
    }
    
    /** 
     * Adds the <code>VisibilityListener</code> from the list.
     * @param listener The selection listener to be removed from the list.
     */
    public void addVisibilityListener(final VisibilityListener listener) {
        if (!m_listenerList.contains(listener)) {
            m_listenerList.add(listener);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void removeVisibilityListener(final VisibilityListener listener) {
        m_listenerList.remove(listener);

    }

    /**
     * @param key the key of the row
     * @return if the row is selected
     * 
     */
    public boolean isSelected(final RowKey key) {
        return m_selected.contains(key);
    }

    /**
     * @param key the key to select
     */
    public void select(final RowKey key) {
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
    public void select(final Set<RowKey> keys) {        
        for (RowKey key : keys) {
                select(key);
        }
    }

    /**
     * @param key the key to unselect
     */
    public void unselect(final RowKey key) {
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
    public void unselect(final Set<RowKey> keys) {
        for (RowKey key : keys) {
            unselect(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void resetSelection() {
        if (m_selected == null) {
            return;
        }
        unselect(new HashSet<RowKey>(m_selected));
    }

    /**
     * @param key the id of the row key
     * @return if it is visible
     * 
     */
    public boolean isVisible(final RowKey key) {
        return m_visible.contains(key);
    }

    /**
     * @param key the id of the row to make visible
     * 
     */
    public void makeVisible(final RowKey key) {
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
    public void makeVisible(final Set<RowKey> keys) {
        for (RowKey key : keys) {
            makeVisible(key);
        }
    }

    /**
     * @param key the id of the row to make invisible
     * 
     */
    public void makeInvisible(final RowKey key) {
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
    public void makeInvisible(final Set<RowKey> keys) {
        for (RowKey key : keys) {
            makeInvisible(key);
        }
    }

    /**
     * 
     */
    public void resetVisibility() {
        for (RowKey key : m_selected) {
            makeInvisible(key);
        }
    }
    
    /** 
     * Informs all registered visibility listener to select 
     * the row keys contained 
     * in the visibility event.
     * @param event Contains all rows keys to select.
     */
    private void fireSelectionEvent(final VisibilityEvent event) {
        assert (event != null);
        for (VisibilityListener l : m_listenerList) {
            try {
                l.select(event);
            } catch (Throwable t) {
                LOGGER.error("Exception while notifying listeners", t);
            }                
        }
    }

    /** 
     * Informs all registered visibility listeners 
     * to unselect the row keys contained
     * in the visibility event.
     * @param event Contains all rows keys to unselect.
     */
    private void fireUnSelectionEvent(final VisibilityEvent event) {
        assert (event != null);
        for (VisibilityListener l : m_listenerList) {
            try {
                l.unselect(event);
            } catch (Throwable t) {
                LOGGER.error("Exception while notifying listeners", t);
            }                
        }
    }
    
    /** 
     * Informs all registered visibility listeners to 
     * select the row keys contained 
     * in the visibility event.
     * @param event Contains all rows keys to select.
     */
    private void fireMakeVisibleEvent(final VisibilityEvent event) {
        assert (event != null);
        for (VisibilityListener l : m_listenerList) {
            try {
                l.makeVisible(event);
            } catch (Throwable t) {
                LOGGER.error("Exception while notifying listeners", t);
            }                
        }
    }

    /** 
     * Informs all registered visibility listener to 
     * unselect the row keys contained
     * in the visibility event.
     * @param event Contains all rows keys to unselect.
     */
    private void fireMakeInvisibleEvent(final VisibilityEvent event) {
        assert (event != null);
        for (VisibilityListener l : m_listenerList) {
            try {
                l.makeInvisible(event);
            } catch (Throwable t) {
                LOGGER.error("Exception while notifying listeners", t);
            }
        }
    }
}
