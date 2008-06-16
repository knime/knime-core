/*
 * ------------------------------------------------------------------
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
 *   29.06.2006 (Fabian Dill): created
 */
package org.knime.core.node.property.hilite;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.knime.core.data.RowKey;


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
     * Adds a HiLiteHandler the hilited RowKeys are passed to and the already
     * hilited keys of this handler are propagated to the other handlers and
     * listeners.
     * 
     * @param forwardTo - a handler the hilited keys should passed to.
     */
    public void addHiLiteHandler(final HiLiteHandler forwardTo) {
        if (forwardTo != null) {
            Set<RowKey> hilited = forwardTo.getHiLitKeys();
            fireHiLiteEvent(hilited);
            forwardTo.fireHiLiteEvent(getHiLitKeys());
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
     * @param event holding the keys to hilite
     * @see org.knime.core.node.property.hilite.HiLiteListener#hiLite(
     *      org.knime.core.node.property.hilite.KeyEvent)
     */
    public void hiLite(final KeyEvent event) {
        Set<RowKey> toBeHilited = new LinkedHashSet<RowKey>(event.keys());
        // remove all already hilited keys
        toBeHilited.removeAll(getHiLitKeys());
        // add them to the local stored keys.
        if (toBeHilited.size() > 0) {
            // forward the newly hilited keys to the listeners
            super.fireHiLiteEvent(toBeHilited);
            // redirect to the other handlers
            for (HiLiteHandler hdl : m_handlers) {
                hdl.fireHiLiteEvent(toBeHilited);
            }
        }
    }

    /**
     * Unhilites those keys which are hilited and propagates them to the 
     * registered handlers and listeners.
     * @param event holding the keys to unhilite
     * @see org.knime.core.node.property.hilite.HiLiteListener#unHiLite(
     *      org.knime.core.node.property.hilite.KeyEvent)
     */
    public void unHiLite(final KeyEvent event) {
        // the keys to be unhilited
        Set<RowKey> toBeUnhilited = new LinkedHashSet<RowKey>(event.keys());
        // keep only those key which are hilited
        toBeUnhilited.retainAll(getHiLitKeys());
        // if there are some keys to be unhilited
        if (toBeUnhilited.size() > 0) {
            super.fireUnHiLiteEvent(toBeUnhilited);
            // redirect to the other handlers
            for (HiLiteHandler hdl : m_handlers) {
                hdl.fireUnHiLiteEvent(toBeUnhilited);
            }
        }
    }

    /**
     * Hilites those keys of the event which are not already hilited and 
     * forwards them to the registered handlers and listeners.
     * @param ids holding the keys to hilite
     * @see org.knime.core.node.property.hilite.DefaultHiLiteHandler
     * #fireHiLiteEvent(org.knime.core.data.RowKey...)
     */
    @Override
    public synchronized void fireHiLiteEvent(final RowKey... ids) {
        if (ids.length > 0) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { 
                    hiLite(new KeyEvent(this, ids));
                }
            });
        }
    }

    /**
     * Hilites those keys of the event which are not already hilited and 
     * forwards them to the registered handlers and listeners.
     * @param ids holding the keys to hilite
     * @see org.knime.core.node.property.hilite.DefaultHiLiteHandler
     * #fireHiLiteEvent(java.util.Set)
     */
    @Override
    public synchronized void fireHiLiteEvent(final Set<RowKey> ids) {
        if (ids.size() > 0) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { 
                    hiLite(new KeyEvent(this, ids));
                }
            });
        }
    }

    /**
     * Unhilites those keys which are hilited and propagates them to the 
     * registered handlers and listeners.
     * @param ids holding the keys to hilite
     * @see org.knime.core.node.property.hilite.DefaultHiLiteHandler
     * #fireUnHiLiteEvent(org.knime.core.data.RowKey...)
     */
    @Override
    public synchronized void fireUnHiLiteEvent(final RowKey... ids) {
        if (ids.length > 0) {
            SwingUtilities.invokeLater(new Runnable() {
               public void run() { 
                   unHiLite(new KeyEvent(this, ids));
               }
            });
        }
    }

    /**
     * Unhilites those keys which are hilited and propagates them to the 
     * registered handlers and listeners.
     * @param ids holding the keys to unhilite
     * @see org.knime.core.node.property.hilite.DefaultHiLiteHandler
     * #fireUnHiLiteEvent(java.util.Set)
     */
    @Override
    public synchronized void fireUnHiLiteEvent(final Set<RowKey> ids) {
        if (ids.size() > 0) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { 
                    unHiLite(new KeyEvent(this, ids));
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void fireClearHiLiteEvent() {
        if (!getHiLitKeys().isEmpty()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { 
                    if (!getHiLitKeys().isEmpty()) {
                        unHiLite(new KeyEvent(this, getHiLitKeys()));
                    }
                }
            });
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiLiteAll(final KeyEvent event) {
        this.fireClearHiLiteEvent();
    }

}
