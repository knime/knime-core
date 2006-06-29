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
 *   29.06.2006 (Fabian Dill): created
 */
package de.unikn.knime.core.node.property.hilite;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.unikn.knime.core.data.DataCell;

/**
 * The <code>DefaultHiLiteManager</code> acts as a <code>HiLiteListener</code>
 * and as a <code>HiLiteHandler</code> and
 * several <code>HiLiteHandler</code>s can be registered to it.
 * This is useful when there are n inputs and an aggregated output, then the 
 * input <code>HiLiteHandler</code>s are registered and the manager listens to 
 * HiLiteEvents from them and acts as an <code>HiLiteHandler</code> to the 
 * aggregated output.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class DefaultHiLiteManager extends DefaultHiLiteHandler implements
        HiLiteListener {

    private final List<HiLiteHandler> m_handlers 
        = new ArrayList<HiLiteHandler>();

    /**
     * Adds a HiLiteHandler the hilited datacells are passed to and the already
     * hilited keys of this handler are propageted to the other handlers and
     * listeners.
     * 
     * @param forwardTo - a handler the hilited keys should passed to.
     */
    public void addHiLiteHandler(final HiLiteHandler forwardTo) {
        if (forwardTo != null) {
            Set<DataCell> hilited = forwardTo.getHiLitKeys();
            if (hilited.size() > 0) {
                KeyEvent newEvent = new KeyEvent(this, hilited);
                this.hiLite(newEvent);
            }
            forwardTo.hiLite(getHiLitKeys());
            forwardTo.addHiLiteListener(this);
            m_handlers.add(forwardTo);
        }
    }

    /**
     * Removes the passed HiliteHandler.
     * 
     * @param forwardTo - the hilitehandler to be removed.
     */
    public void removeHiLiteHandler(final HiLiteHandler forwardTo) {
        if (forwardTo != null) {
            forwardTo.removeHiLiteListener(this);
            m_handlers.remove(forwardTo);
        }
    }

    /**
     * Removes all forward hilite handler.
     * 
     */
    public void removeAllHiLiteHandlers() {
        for (HiLiteHandler hdl : m_handlers) {
            hdl.removeHiLiteListener(this);
        }
        m_handlers.clear();
    }

    /**
     * Hilites those keys of the event which are not already hilited and 
     * forwards them to the registered handlers and listeners.
     * 
     * @see de.unikn.knime.core.node.property.hilite.HiLiteListener#hiLite(
     *      de.unikn.knime.core.node.property.hilite.KeyEvent)
     */
    public void hiLite(final KeyEvent event) {
        Set<DataCell> toBeHilited = new LinkedHashSet<DataCell>(event.keys());
        // remove all already hilited keys
        toBeHilited.removeAll(getHiLitKeys());
        // add them to the local stored keys.
        if (toBeHilited.size() > 0) {
            // forward the newly hilited keys to the listeners
            super.hiLite(toBeHilited);
            // redirect to the other handlers
            for (HiLiteHandler hdl : m_handlers) {
                hdl.hiLite(toBeHilited);
            }
        }
    }

    /**
     * Unhilites those keys which are hilited and propagates them to the 
     * registered handlers and listeners.
     * 
     * @see de.unikn.knime.core.node.property.hilite.HiLiteListener#unHiLite(
     *      de.unikn.knime.core.node.property.hilite.KeyEvent)
     */
    public void unHiLite(final KeyEvent event) {
        // the keys to be unhilited
        Set<DataCell> toBeUnhilited = new HashSet<DataCell>(event.keys());
        // keep only those key which are hilited
        toBeUnhilited.retainAll(getHiLitKeys());
        // if there are some keys to be unhilited
        if (toBeUnhilited.size() > 0) {
            super.unHiLite(toBeUnhilited);
            // redirect to the other handlers
            for (HiLiteHandler hdl : m_handlers) {
                hdl.unHiLite(toBeUnhilited);
            }
        }
    }

    /**
     * Hilites those keys of the event which are not already hilited and 
     * forwards them to the registered handlers and listeners.
     * 
     * @see de.unikn.knime.core.node.property.hilite.DefaultHiLiteHandler#
     * hiLite(de.unikn.knime.core.data.DataCell...)
     */
    @Override
    public synchronized void hiLite(final DataCell... ids) {
        this.hiLite(new KeyEvent(this, ids));
    }

    /**
     * Hilites those keys of the event which are not already hilited and 
     * forwards them to the registered handlers and listeners.
     * 
     * @see de.unikn.knime.core.node.property.hilite.DefaultHiLiteHandler#
     * hiLite(java.util.Set)
     */
    @Override
    public synchronized void hiLite(final Set<DataCell> ids) {
        this.hiLite(new KeyEvent(this, ids));
    }

    /**
     * Unhilites those keys which are hilited and propagates them to the 
     * registered handlers and listeners.
     *      
     * @see de.unikn.knime.core.node.property.hilite.DefaultHiLiteHandler#
     * unHiLite(de.unikn.knime.core.data.DataCell...)
     */
    @Override
    public synchronized void unHiLite(final DataCell... ids) {
        this.unHiLite(new KeyEvent(this, ids));
    }

    /**
     * Unhilites those keys which are hilited and propagates them to the 
     * registered handlers and listeners.
     * 
     * @see de.unikn.knime.core.node.property.hilite.DefaultHiLiteHandler#
     * unHiLite(java.util.Set)
     */
    @Override
    public synchronized void unHiLite(final Set<DataCell> ids) {
        this.unHiLite(new KeyEvent(this, ids));
    }

    /**
     * @see de.unikn.knime.core.node.property.hilite.DefaultHiLiteHandler#
     * unHiLiteAll()
     */
    @Override
    public synchronized void unHiLiteAll() {
        this.unHiLite(new KeyEvent(this, getHiLitKeys()));
    }
    
    

}
