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

import org.knime.core.data.RowKey;

/**
 * A translator for hilite events between one source {@link HiLiteHandler} and a
 * number of target handlers. The source hilite handler is passed through the
 * constructor of this class. The target hilite handlers can be set
 * independently, as well as the mapping which is defined between
 * {@link RowKey} row keys and {@link RowKey} sets. This class hosts two
 * listeners one which is registered with the source handler and one which is
 * registered with all target handlers. These listeners are called when
 * something changes either on the source or target side, and then invoke the
 * corresponding handler(s) on the other side to hilite, unhilite, and clear 
 * mapped keys.
 * <p>
 * The source listener forwards all events to the target handlers, that is,
 * hilite, unhilite, and clear hilite. The target listener on the other side
 * only forwards the clear hilite event to the source handler, and therefore
 * ignores all other events. The clear hilite event is only fired to the source
 * handler, if at least one key is hilit.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class HiLiteTranslator {
    
    /** Target handler used for hiliting on the aggregation side. */
    private final Set<HiLiteHandler> m_targetHandlers;
    
    /** Source handlers used for hiliting for single items. */
    private final HiLiteHandler m_sourceHandler;
    
    /** Contains the mapping between aggregation and single items. */
    private HiLiteMapper m_mapper;
    
    /** 
     * Listener on the source handler used to forward events  
     * to all registered target handlers.
     */
    private final HiLiteListener m_sourceListener = new HiLiteListener() {
        /**
         * {@inheritDoc}
         */
        public void hiLite(final KeyEvent event) {
            if (m_mapper != null && m_targetHandlers.size() > 0) {
                Set<RowKey> fireSet = new LinkedHashSet<RowKey>();
                for (RowKey key : event.keys()) {
                    Set<RowKey> s = m_mapper.getKeys(key);
                    if (s != null && !s.isEmpty()) {
                        fireSet.addAll(s);
                    }  
                }
                if (!fireSet.isEmpty()) {
                    for (HiLiteHandler h : m_targetHandlers) {
                        //if (h != event.getSource()) {
                            h.fireHiLiteEvent(fireSet);
                        //}
                    }
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void unHiLite(final KeyEvent event) {
            if (m_mapper != null && m_targetHandlers.size() > 0) {
                Set<RowKey> fireSet = new LinkedHashSet<RowKey>();
                for (RowKey key : event.keys()) {
                    Set<RowKey> s = m_mapper.getKeys(key);
                    if (s != null && !s.isEmpty()) {
                        fireSet.addAll(s);
                    }
                }
                if (!fireSet.isEmpty()) {
                    for (HiLiteHandler h : m_targetHandlers) {
                        //if (h != event.getSource()) {
                            h.fireUnHiLiteEvent(fireSet);
                        //}
                    }
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void unHiLiteAll(final KeyEvent event) {
            for (HiLiteHandler h : m_targetHandlers) {
                //if (h != event.getSource()) {
                    h.fireClearHiLiteEvent();
                //}
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
            if (m_mapper != null) {
                try {
                    // remove source listener temporarily to ensure that the 
                    // fired event is not propagates back to this target handler
                    m_sourceHandler.removeHiLiteListener(m_sourceListener);
                    // add all hilite keys from the event and all hilit keys
                    // from the target hilite handlers
                    final Set<RowKey> all = new LinkedHashSet<RowKey>(
                            event.keys());
                    for (HiLiteHandler hdl : m_targetHandlers) {
                        all.addAll(hdl.getHiLitKeys());
                    }
                    // check overlap with all mappings  
                    for (RowKey key : m_mapper.keySet()) {
                        final Set<RowKey> keys = m_mapper.getKeys(key);
                        // if all mapped keys are hilit then fire event
                        if (all.containsAll(keys)) {
                            m_sourceHandler.fireHiLiteEvent(key);
                        }
                    }
                } finally {
                    m_sourceHandler.addHiLiteListener(m_sourceListener);
                }
            }
        }
        /**
         * {@inheritDoc}
         */
        public void unHiLite(final KeyEvent event) {
            if (m_mapper != null) {
                try {
                    // remove source listener temporarily to ensure that the 
                    // fired event is not propagates back to this target handler
                    m_sourceHandler.removeHiLiteListener(m_sourceListener);
                    // check all mappings
                    for (RowKey key : m_mapper.keySet()) {
                        final Set<RowKey> keys = m_mapper.getKeys(key);
                        // if at least one item is unhilit then fire event
                        for (RowKey hilite : event.keys()) {
                            if (keys.contains(hilite)) {
                                m_sourceHandler.fireUnHiLiteEvent(key);
                                break;
                            }
                        }
                    }
                } finally {
                    m_sourceHandler.addHiLiteListener(m_sourceListener);
                }
            }
        }
        /**
         * {@inheritDoc}
         */
        public void unHiLiteAll(final KeyEvent event) {
            m_sourceHandler.fireClearHiLiteEvent();
        }
    };
    
    /**
     * Creates a new translator with the given source handler and an null 
     * (empty) mapping. 
     * 
     * @param sourceHandler the handler on the aggregation side
     * @throws IllegalArgumentException if the handler is null
     */
    public HiLiteTranslator(final HiLiteHandler sourceHandler) {
        this(sourceHandler, null);
    }
    
    /**
     * Creates a new translator with the given source handler and the given 
     * mapping which can be null.
     * 
     * @param sourceHandler the handler on the aggregation side
     * @param mapper mapping from aggregation to single patterns
     * @throws IllegalArgumentException if the handler is null
     */
    public HiLiteTranslator(
            final HiLiteHandler sourceHandler, final HiLiteMapper mapper) {
        if (sourceHandler == null) {
            throw new IllegalArgumentException(
                    "Source HiLiteHandler must not be null!");
        }
        m_sourceHandler = sourceHandler;
        m_targetHandlers = new LinkedHashSet<HiLiteHandler>();
        m_mapper = mapper;
    }
    
    /**
     * Sets a new hilite mapper which can be <code>null</code> in case no
     * hilite translation is available.
     * 
     * @param mapper the new hilite mapper
     */
    public void setMapper(final HiLiteMapper mapper) {
        m_sourceHandler.fireClearHiLiteEvent();
        m_mapper = mapper;
    }
    
    /**
     * @return mapper which contains the mapping, can be null
     */
    public HiLiteMapper getMapper() {
        return m_mapper;
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
