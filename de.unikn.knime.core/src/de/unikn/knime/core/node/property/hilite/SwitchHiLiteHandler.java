/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   02.06.2006 (Fabian Dill): created
 */
package de.unikn.knime.core.node.property.hilite;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.unikn.knime.core.data.DataCell;

/**
 * Allows the switching of hilite events between several hilite handlers.
 * This hilite handler/-listener listenes to some handlers, propagates the 
 * hilite events of them to the other handlers and to the registered listeners.
 * The hilite handler part of this class informs the handlers it listens to 
 * and the registered listeners.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class SwitchHiLiteHandler implements HiLiteHandler, HiLiteListener {

    private final Set<DataCell>m_hilitedKeys;
    
    private final Set<HiLiteHandler>m_handlers;
    
    private final Set<HiLiteListener>m_listeners;
    
    /**
     * Creates an empty Switch Hilite Handler.
     *
     */
    public SwitchHiLiteHandler() {
        m_handlers = new HashSet<HiLiteHandler>();
        m_hilitedKeys = Collections.synchronizedSet(new HashSet<DataCell>());
        m_listeners = new HashSet<HiLiteListener>();
    } 
    
    /**
     * @see de.unikn.knime.core.node.property.hilite.HiLiteListener#hiLite(
     * de.unikn.knime.core.node.property.hilite.KeyEvent)
     */
    public void hiLite(final KeyEvent event) {
        Set<DataCell> toBeHilited = new HashSet<DataCell>(event.keys());
        // remove all already hilited keys
        toBeHilited.removeAll(m_hilitedKeys);
        // add them to the local stored keys.
        m_hilitedKeys.addAll(toBeHilited);
        if (toBeHilited.size() > 0) {
            // create a new event
            KeyEvent newEvent = new KeyEvent(this, toBeHilited);
            // forward the newly hilited keys to the listeners
            for (HiLiteListener lis : m_listeners) {
                lis.hiLite(newEvent);
            }
            // redirect to the other handlers
            for (DataCell key : toBeHilited) {
                for (HiLiteHandler hdl : m_handlers) {
                    if (!hdl.isHiLit(key)) {
                        hdl.hiLite(key);
                    }
                }
            }
        }
    }




    /**
     * @see de.unikn.knime.core.node.property.hilite.HiLiteListener#unHiLite(
     * de.unikn.knime.core.node.property.hilite.KeyEvent)
     */
    public void unHiLite(final KeyEvent event) {
        // the keys to be unhilited
        Set<DataCell> toBeUnhilited = new HashSet<DataCell>(event.keys());
        // keep only those key which are hilited
        toBeUnhilited.retainAll(m_hilitedKeys);
        // and remove them from the hilited keys
        m_hilitedKeys.removeAll(toBeUnhilited);
        // if there are some keys to be unhilited
        if (toBeUnhilited.size() > 0) {
            KeyEvent newEvent = new KeyEvent(this, toBeUnhilited);
            for (HiLiteListener lis : m_listeners) {
                lis.unHiLite(newEvent);
            }
            // redirect to the other handlers
            for (DataCell key : toBeUnhilited) {
                for (HiLiteHandler hdl : m_handlers) {
                    if (hdl.isHiLit(key)) {
                        hdl.unHiLite(key);
                    }
                }
            }
        }
    }




    /**
     * Adds a HiLiteHandler the hilited datacells are passed to.
     * @param forwardTo - a handler the hilited keys should passed to.
     */
    public void addForwardHandler(final HiLiteHandler forwardTo) {
        if (forwardTo != null) {
            forwardTo.addHiLiteListener(this);
            m_handlers.add(forwardTo);
        }
    }
    
    /**
     * Removes the passed HiliteHandler.
     * @param forwardTo - the hilitehandler to be removed.
     */
    public void removeForwardHandler(final HiLiteHandler forwardTo) {
        if (forwardTo != null) {
            forwardTo.removeHiLiteListener(this);
            m_handlers.remove(forwardTo);
        }
    }
    
    /**
     * Removes all forward hilite handler.
     *
     */
    public void removeAllForwardHandler() {
        for (HiLiteHandler hdl : m_handlers) {
            hdl.removeHiLiteListener(this);
        }
        m_handlers.clear();
    }
    
    /**
     * @see de.unikn.knime.core.node.property.hilite.HiLiteHandler#
     * addHiLiteListener(
     * de.unikn.knime.core.node.property.hilite.HiLiteListener)
     */
    public void addHiLiteListener(final HiLiteListener listener) {
        if (listener != null) {
            m_listeners.add(listener);
        }
    }

    /**
     * @see de.unikn.knime.core.node.property.hilite.HiLiteHandler#
     * removeHiLiteListener(
     * de.unikn.knime.core.node.property.hilite.HiLiteListener)
     */
    public void removeHiLiteListener(final HiLiteListener listener) {
        if (listener != null) {
           m_listeners.remove(listener); 
        }
    }

    /**
     * @see de.unikn.knime.core.node.property.hilite.HiLiteHandler#
     * removeAllHiLiteListeners()
     */
    public void removeAllHiLiteListeners() {
        m_listeners.clear();
    }

    /**
     * @see de.unikn.knime.core.node.property.hilite.HiLiteHandler#
     * isHiLit(de.unikn.knime.core.data.DataCell...)
     */
    public boolean isHiLit(final DataCell... ids) {
        for (DataCell id : ids) {
            if (!m_hilitedKeys.contains(id)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @see de.unikn.knime.core.node.property.hilite.HiLiteHandler#
     * getHiLitKeys()
     */
    public Set<DataCell> getHiLitKeys() {
        return m_hilitedKeys;
    }

    /**
     * @see de.unikn.knime.core.node.property.hilite.HiLiteHandler#hiLite(
     * de.unikn.knime.core.data.DataCell...)
     */
    public void hiLite(final DataCell... ids) {
        for (DataCell id : ids) {
            m_hilitedKeys.add(id);
            for (HiLiteHandler hdl : m_handlers) {
                if (!hdl.isHiLit(id)) {
                    hdl.hiLite(id);
                }
            }
        }
        KeyEvent newEvent = new KeyEvent(this, ids);
        for (HiLiteListener lis : m_listeners) {
            lis.hiLite(newEvent);
        }
    }

    /**
     * @see de.unikn.knime.core.node.property.hilite.HiLiteHandler#
     * hiLite(java.util.Set)
     */
    public void hiLite(final Set<DataCell> ids) {
        for (DataCell id : ids) {
            m_hilitedKeys.add(id);
            for (HiLiteHandler hdl : m_handlers) {
                if (!hdl.isHiLit(id)) {
                    hdl.hiLite(id);
                }
            }
        }
        KeyEvent newEvent = new KeyEvent(this, ids);
        for (HiLiteListener lis : m_listeners) {
            lis.hiLite(newEvent);
        }
    }

    /**
     * @see de.unikn.knime.core.node.property.hilite.HiLiteHandler#
     * unHiLite(de.unikn.knime.core.data.DataCell...)
     */
    public void unHiLite(final DataCell... ids) {
        for (DataCell id : ids) {
            m_hilitedKeys.remove(id);
            for (HiLiteHandler hdl : m_handlers) {
                if (hdl.isHiLit(id)) {
                    hdl.unHiLite(id);
                }
            }
        }
        KeyEvent newEvent = new KeyEvent(this, ids);
        for (HiLiteListener lis : m_listeners) {
            lis.unHiLite(newEvent);
        }
    }

    /**
     * @see de.unikn.knime.core.node.property.hilite.HiLiteHandler#
     * unHiLite(java.util.Set)
     */
    public void unHiLite(final Set<DataCell> ids) {
        for (DataCell id : ids) {
            m_hilitedKeys.remove(id);
            for (HiLiteHandler hdl : m_handlers) {
                if (hdl.isHiLit(id)) {
                    hdl.unHiLite(id);
                }
            }
        }
        KeyEvent newEvent = new KeyEvent(this, ids);
        for (HiLiteListener lis : m_listeners) {
            lis.unHiLite(newEvent);
        }
    }

    /**
     * @see de.unikn.knime.core.node.property.hilite.HiLiteHandler#unHiLiteAll()
     */
    public void unHiLiteAll() {
        m_hilitedKeys.clear();
        for (HiLiteHandler hdl : m_handlers) {
            hdl.unHiLiteAll();
        }
        for (HiLiteListener lis : m_listeners) {
            lis.unHiLiteAll();
        }
    }

}
