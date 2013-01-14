/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
