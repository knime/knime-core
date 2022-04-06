/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Nov 25, 2021 (hornm): created
 */
package org.knime.gateway.impl.service.events;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.knime.core.data.RowKey;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.property.hilite.KeyEvent;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.util.Pair;
import org.knime.gateway.api.entity.NodeIDEnt;

/**
 * An event source that emits selection events (i.e. hiliting events) to the given event consumer.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @since 4.5
 */
public class SelectionEventSource extends EventSource<NativeNodeContainer, SelectionEvent> {

    /**
     * The mode of selection event.
     */
    @SuppressWarnings("javadoc")
    public enum SelectionEventMode {
            ADD, REMOVE, REPLACE
    }

    private final Map<NodeID, Pair<HiLiteHandler, HiLiteListener>> m_hiLiteListeners = new HashMap<>();

    /**
     * @param eventConsumer selection events will be forwarded to this consumer
     */
    public SelectionEventSource(final BiConsumer<String, Object> eventConsumer) {
        super(eventConsumer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getName() {
        return "SelectionEvent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<SelectionEvent> addEventListenerAndGetInitialEventFor(final NativeNodeContainer nnc) {
        // TODO see UIEXT-51
        var handler = nnc.getNodeModel().getInHiLiteHandler(0);
        synchronized (handler) {
            var hiLitKeys = handler.getHiLitKeys();
            var listener = new PerNodeHiliteListener(this::sendEvent, nnc);
            var selectionEvent = listener.createSelectionEvent(SelectionEventMode.ADD, hiLitKeys);
            var nodeID = nnc.getID();
            if (!m_hiLiteListeners.containsKey(nodeID)) {
                handler.addHiLiteListener(listener);
                m_hiLiteListeners.put(nnc.getID(), Pair.create(handler, listener));
            }
            return Optional.ofNullable(selectionEvent);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAllEventListeners() {
        m_hiLiteListeners.values().forEach(p -> p.getFirst().removeHiLiteListener(p.getSecond()));
        m_hiLiteListeners.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeEventListener(final NativeNodeContainer nnc) {
        var nodeId = nnc.getID();
        var p = m_hiLiteListeners.get(nodeId);
        p.getFirst().removeHiLiteListener(p.getSecond());
        m_hiLiteListeners.remove(nodeId);
    }

    /**
     * Forwards selection events to the hilite-handler associated with the given node.
     *
     * @param nc the node to use the hilite handler for
     * @param selectionEventMode the selection event mode
     * @param async if {@code true}, it will return immediately; if {@code false} it will return once the selection has
     *            been processed completely (i.e. once all associated nodes have received the selection change, too).
     * @param rowKeys the keys to be (un-)selected
     */
    public static void processSelectionEvent(final NativeNodeContainer nc, final SelectionEventMode selectionEventMode,
        final boolean async, final List<String> rowKeys) {
        final var keyEvent = new KeyEvent(nc.getID(), rowKeys.stream().map(RowKey::new).toArray(RowKey[]::new));
        // TODO see UIEXT-51
        var hiLiteHandler = nc.getNodeModel().getInHiLiteHandler(0);
        switch (selectionEventMode) {
            case ADD:
                hiLiteHandler.fireHiLiteEvent(keyEvent, async);
                break;
            case REMOVE:
                hiLiteHandler.fireUnHiLiteEvent(keyEvent, async);
                break;
            case REPLACE:
                hiLiteHandler.fireReplaceHiLiteEvent(keyEvent, async);
                break;
            default:
        }
    }

    private static class PerNodeHiliteListener implements HiLiteListener {

        private final Consumer<SelectionEvent> m_eventConsumer;

        private final NodeID m_nodeId;

        private final String m_projectId;

        private final String m_workflowId;

        private final String m_nodeIdString;

        PerNodeHiliteListener(final Consumer<SelectionEvent> eventConsumer, final NativeNodeContainer nnc) {
            m_eventConsumer = eventConsumer;
            var parent = nnc.getParent();
            var projectWfm = parent.getProjectWFM();
            m_projectId = projectWfm.getNameWithID();
            NodeID ncParentId = parent.getDirectNCParent() instanceof SubNodeContainer
                ? ((SubNodeContainer)parent.getDirectNCParent()).getID() : parent.getID();
            m_workflowId = new NodeIDEnt(ncParentId).toString();
            m_nodeId = nnc.getID();
            m_nodeIdString = new NodeIDEnt(m_nodeId).toString();
        }

        @Override
        public void hiLite(final KeyEvent event) {
            consumeSelectionEvent(event, SelectionEventMode.ADD);
        }

        @Override
        public void unHiLite(final KeyEvent event) {
            consumeSelectionEvent(event, SelectionEventMode.REMOVE);
        }

        @Override
        public void unHiLiteAll(final KeyEvent event) {
            consumeSelectionEvent(new KeyEvent(event.getSource()), SelectionEventMode.REPLACE);
        }

        @Override
        public void replaceHiLite(final KeyEvent event) {
            consumeSelectionEvent(event, SelectionEventMode.REPLACE);
        }

        private void consumeSelectionEvent(final KeyEvent event, final SelectionEventMode mode) {
            // do not consume selection events that have been fired by the node this listener is registered on
            if (!m_nodeId.equals(event.getSource())) {
                m_eventConsumer.accept(createSelectionEvent(mode, event.keys()));
            }
        }

        private SelectionEvent createSelectionEvent(final SelectionEventMode mode, final Set<RowKey> keys) {
            return new SelectionEvent() { // NOSONAR

                @Override
                public SelectionEventMode getMode() {
                    return mode;
                }

                @Override
                public List<String> getKeys() {
                    return keys.stream().map(RowKey::getString).collect(Collectors.toUnmodifiableList());
                }

                @Override
                public String getProjectId() {
                    return m_projectId;
                }

                @Override
                public String getWorkflowId() {
                    return m_workflowId;
                }

                @Override
                public String getNodeId() {
                    return m_nodeIdString;
                }

            };
        }
    }



}
