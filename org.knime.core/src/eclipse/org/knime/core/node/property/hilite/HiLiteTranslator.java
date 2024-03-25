/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * 2006-06-08 (tm): reviewed
 */
package org.knime.core.node.property.hilite;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;

import org.knime.core.data.RowKey;

/**
 * A translator for hilite events between one source (from)
 * {@link HiLiteHandler} and a number of target handlers (to). The source hilite
 * handler is passed through the constructor of this class. The target hilite
 * handlers can be set independently, as well as the mapping which is defined
 * between {@link RowKey} row keys and {@link RowKey} sets.
 * <p>
 * This class hosts two listeners one which is registered with the source
 * handler and one which is registered with all target handlers. These listeners
 * are called when something changes either on the source or target side, and
 * then invoke the corresponding handlers on the other side to hilite, unhilite,
 * and clear mapped keys.
 * <p>
 * <strong>Note:</strong> If you create an instance of a {@link HiLiteTranslator} make sure to {@linkplain #dispose()}
 * it when done, e.g. during reset, setting new input hilite handler or during disposal of the NodeModel.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public final class HiLiteTranslator extends HiLiteManager {

    /** Contains the mapping between aggregation and single items. */
    private HiLiteMapper m_mapper;

    /** Propagates hilite events downstream. */
    private final HiLiteListener m_sourceListener = new AbstractPropagatingHiLiteListener() {
        // apply the mapper to compute affected row keys and invoke a function on all target handlers
        @Override
        protected void propagate(final KeyEvent event, final BiConsumer<HiLiteHandler, KeyEvent> consumer) {
            if (event.getSource() == m_eventSource) {
                return;
            }
            if (m_mapper != null && !m_targetHandlers.isEmpty()) {
                final var union = m_mapper.applyUnion(event.keys());
                if (!union.isEmpty()) {
                    m_targetHandlers.forEach(h -> consumer.accept(h, new KeyEvent(m_eventSource, union)));
                }
            }
        }
    };

    /** Propagates hilite events upstream. */
    private final HiLiteListener m_targetListener = new AbstractPropagatingHiLiteListener() {
        // apply the mapper to compute affected row keys and invoke a function on all target handlers
        @Override
        protected void propagate(final KeyEvent event, final BiConsumer<HiLiteHandler, KeyEvent> consumer) {
            if (event.getSource() == m_eventSource) {
                return;
            }
            if (m_mapper != null) {
                // hilite is additive: add all keys from the event to the currently hilit keys by all handlers
                final var hilitOutput = new LinkedHashSet<RowKey>(event.keys());
                m_targetHandlers.forEach(hdl -> hilitOutput.addAll(hdl.getHiLitKeys()));

                // compute hilit input row keys
                final var hilitInput = m_mapper.inverseCovered(hilitOutput);
                consumer.accept(m_sourceHandler, new KeyEvent(m_eventSource, hilitInput));
            }
        }
    };

    /**
     * Creates a translator with an empty mapping and a default hilite
     * handler.
     */
    public HiLiteTranslator() {
        this((HiLiteMapper) null);
    }

    /**
     * Creates a new translator.
     * @param handler a given source <code>HiLiteHandler</code>
     */
    public HiLiteTranslator(final HiLiteHandler handler) {
        this(handler, null);
    }

    /**
     * Creates a new translator.
     * @param mapper mapping from aggregation to single patterns
     */
    public HiLiteTranslator(final HiLiteMapper mapper) {
        this(new HiLiteHandler(), mapper);
    }

    /**
     * Creates a new translator.
     * @param handler a given source <code>HiLiteHandler</code>
     * @param mapper mapping from aggregation to single patterns
     */
    public HiLiteTranslator(final HiLiteHandler handler,
            final HiLiteMapper mapper) {
        if (handler == null) {
            throw new IllegalArgumentException(
                    "Source HiLiteHandler must not be null.");
        }
        m_sourceHandler = handler;
        m_sourceHandler.addHiLiteTranslator(this);
        m_targetHandlers = new LinkedHashSet<HiLiteHandler>();
        m_mapper = mapper;
    }

    /**
     * Sets a new hilite mapper which can be <code>null</code> in case no
     * hilite translation is available.
     * @param mapper the new hilite mapper
     */
    public void setMapper(final HiLiteMapper mapper) {
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
     * if the list of hilite keys is empty.
     *
     * @param targetHandler the target hilite handler to remove
     */
    @Override
    public void removeToHiLiteHandler(final HiLiteHandler targetHandler) {
        if (targetHandler != null) {
            m_sourceListener.unHiLite(new KeyEvent(targetHandler,
                    m_sourceHandler.getHiLitKeys()));
            m_targetHandlers.remove(targetHandler);
            targetHandler.removeHiLiteListener(m_targetListener);
            targetHandler.removeHiLiteTranslator(this);
            if (m_targetHandlers.isEmpty()) {
                m_sourceHandler.removeHiLiteListener(m_sourceListener);
            }
        }
    }

    /**
     * Adds a new target <code>HiLiteHandler</code> to the list of registered
     * hilite handlers and adds the private target listener if the list of
     * hilite keys is empty.
     *
     * @param targetHandler the target hilite handler to add
     */
    @Override
    public void addToHiLiteHandler(final HiLiteHandler targetHandler) {
        if (targetHandler != null) {
            if (m_targetHandlers.isEmpty()) {
                m_sourceHandler.addHiLiteListener(m_sourceListener);
            }
            m_targetHandlers.add(targetHandler);
            targetHandler.addHiLiteListener(m_targetListener);
            targetHandler.addHiLiteTranslator(this);
            m_targetListener.hiLite(new KeyEvent(targetHandler,
                    targetHandler.getHiLitKeys()));
        }
    }

    /**
     * An unmodifiable set of target hilite handlers.
     *
     * @return the set of target hilite handlers
     */
    @Override
    public Set<HiLiteHandler> getToHiLiteHandlers() {
        return Collections.unmodifiableSet(m_targetHandlers);
    }

    /**
     * Removes all target hilite handlers from this translator. To be
     * used from the node that instantiates this instance when a new
     * connection is made.
     */
    @Override
    public void removeAllToHiliteHandlers() {
        Set<HiLiteHandler> copy = new HashSet<HiLiteHandler>(m_targetHandlers);
        for (HiLiteHandler hh : copy) {
            removeToHiLiteHandler(hh);
        }
        m_targetHandlers.clear();
        m_sourceHandler.removeHiLiteListener(m_sourceListener);
    }

    /**
     * The source hilite handler.
     *
     * @return source hilite handler
     */
    @Override
    public HiLiteHandler getFromHiLiteHandler() {
        return m_sourceHandler;
    }

    /**
     * Unregisters this {@link HiLiteTranslator} from all involved {@link HiLiteHandler}s, removes all target {@link HiLiteHandler}s and unsets the mapper.
     * This needs to be called before deletion, if this instance was created with providing a custom {@link HiLiteHandler} as the source.
     * @since 3.4
     */
    public void dispose() {
        if (m_sourceHandler != null) {
            m_sourceHandler.removeHiLiteTranslator(this);
        }
        removeAllToHiliteHandlers();
        setMapper(null);
    }

}
