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
 * 2006-06-08 (tm): reviewed
 */
package org.knime.core.node.property.hilite;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A manager for hilite events between one source (from) {@link HiLiteHandler} 
 * and a number of target handlers (to). This class provides one source hilite 
 * handler instantiated within the constructor. The target hilite handlers can 
 * be added individually.
 * <p>
 * This class hosts two listeners one which is registered with the source
 * handler and one which is registered with all target handlers. These listeners
 * are called when something changes either on the source (from) or target side 
 * (to), and then invoke the corresponding handlers on the other side to hilite,
 * unhilite, and clear hilite.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class HiLiteManager {
    
    /** Target handler used for hiliting on the aggregation side. */
    private final Set<HiLiteHandler> m_targetHandlers;
    
    /** Source handlers used for hiliting for single items. */
    private final HiLiteHandler m_sourceHandler;
    
    private final Object m_eventSource = this;
    
    /** 
     * Listener on the source handler used to forward events  
     * to all registered target handlers.
     */
    private final HiLiteListener m_sourceListener = new HiLiteListener() {
        /**
         * {@inheritDoc}
         */
        public void hiLite(final KeyEvent event) {
            if (event.getSource() == m_eventSource) {
                return;
            }
            for (HiLiteHandler h : m_targetHandlers) {
                h.fireHiLiteEventInternal(new KeyEvent(
                        m_eventSource, event.keys()));
            }
        }

        /**
         * {@inheritDoc}
         */
        public void unHiLite(final KeyEvent event) {
            if (event.getSource() == m_eventSource) {
                return;
            }
            for (HiLiteHandler h : m_targetHandlers) {
                h.fireUnHiLiteEventInternal(
                        new KeyEvent(m_eventSource, event.keys()));
            }
        }

        /**
         * {@inheritDoc}
         */
        public void unHiLiteAll(final KeyEvent event) {
            if (event.getSource() == m_eventSource) {
                return;
            }
            for (HiLiteHandler h : m_targetHandlers) {
                h.fireClearHiLiteEventInternal(
                        new KeyEvent(m_eventSource, event.keys()));
            }
        }
    };
        
    /** 
     * Listener to all target handlers that send clear hilite
     * events to the source handler.
     */
    private final HiLiteListener m_targetListener = new HiLiteListener() {
        /**
         * {@inheritDoc}
         */
        public void hiLite(final KeyEvent event) {
            if (event.getSource() == m_eventSource) {
                return;
            }
            m_sourceHandler.fireHiLiteEventInternal(
                    new KeyEvent(m_eventSource, event.keys()));
        }
        /**
         * {@inheritDoc}
         */
        public void unHiLite(final KeyEvent event) {
            if (event.getSource() == m_eventSource) {
                return;
            }
            m_sourceHandler.fireUnHiLiteEventInternal(
                                new KeyEvent(m_eventSource, event.keys()));
 
        }
        /**
         * {@inheritDoc}
         */
        public void unHiLiteAll(final KeyEvent event) {
            if (event.getSource() == m_eventSource) {
                return;
            }
            m_sourceHandler.fireClearHiLiteEventInternal(
                    new KeyEvent(m_eventSource, event.keys()));
        }
    };
    
    /**
     * Creates a new manager.
     */
    public HiLiteManager() {
        m_sourceHandler = new HiLiteHandler();
        m_targetHandlers = new LinkedHashSet<HiLiteHandler>();
    }
    
    /**
     * Removes the given target <code>HiLiteHandler</code> from the list of 
     * registered hilite handlers and removes the private target listener from 
     * if the list of hilit keys is empty.
     * 
     * @param targetHandler the target hilite handler to remove
     */
    public void removeToHiLiteHandler(final HiLiteHandler targetHandler) {
        if (targetHandler != null) {
            m_targetHandlers.remove(targetHandler);
            targetHandler.removeHiLiteListener(m_targetListener);
            if (m_targetHandlers.isEmpty()) {
                m_sourceHandler.removeHiLiteListener(m_sourceListener);
            }
        }
    }    

    /**
     * Adds a new target <code>HiLiteHandler</code> to the list of registered 
     * hilite handlers and adds the private target listener if the list of hilit
     * keys is empty.
     * 
     * @param targetHandler the target hilite handler to add
     */
    public void addToHiLiteHandler(final HiLiteHandler targetHandler) {
        if (targetHandler != null) {
            if (m_targetHandlers.isEmpty()) {
                m_sourceHandler.addHiLiteListener(m_sourceListener);
            }
            m_targetHandlers.add(targetHandler);
            targetHandler.addHiLiteListener(m_targetListener);
        }
    }
    
    /**
     * An unmodifiable set of target hilite handlers.
     * 
     * @return the set of target hilite handlers
     */
    public Set<HiLiteHandler> getToHiLiteHandlers() {
        return Collections.unmodifiableSet(m_targetHandlers);
    }
    
    /**
     * Removes all target hilite handlers from this translator. To be
     * used from the node that instantiates this instance when a new 
     * connection is made. 
     */
    public void removeAllToHiliteHandlers() {
        for (HiLiteHandler hh : m_targetHandlers) {
            hh.removeHiLiteListener(m_targetListener);
        }
        m_targetHandlers.clear();
        m_sourceHandler.removeHiLiteListener(m_sourceListener);
    }

    /**
     * The source hilite handler.
     * 
     * @return source hilite handler
     */
    public HiLiteHandler getFromHiLiteHandler() {
        return m_sourceHandler;
    }

}
