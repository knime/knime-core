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
 *   Nov 26, 2021 (hornm): created
 */
package org.knime.gateway.impl.service.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.RowKey;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.view.NodeView;

import com.google.common.collect.MapMaker;

/**
 * Registry for {@link HiLiteListener}s registered with nodes that have a {@link NodeView}.
 *
 * After registering a hilite-listener (via {@link #registerHiLiteListener(NodeID, HiLiteListener)} associated with a
 * particular node, the listener will only start listening for hilite-events once
 * {@link #getHiliteStateAndActivateListener(NativeNodeContainer)} for the same node has been called. I.e. it is recommended to always
 * call {@link #registerHiLiteListener(NodeID, HiLiteListener)} <b>before</b>
 * {@link #getHiliteStateAndActivateListener(NativeNodeContainer)} for a particular node. This guarantees that no hilite-events get lost
 * between retrieving the current hilite-state and registering a hilite listener.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class HiLiteListenerRegistry {

    // a registered hilite-listener does not receive hilite-events, yet
    private final Map<NodeID, HiLiteListener> m_registeredHiLiteListeners = new HashMap<>();

    // an 'active' hilite-listener is receiving events (i.e. added to a hilite-handler)
    private final Map<NodeID, Pair<HiLiteHandler, HiLiteListener>> m_activeHiLiteListeners =
        new MapMaker().weakValues().makeMap();

    /**
     * Returns the currently hilit keys and activates the hilite-listener registered with the given node via
     * {@link #registerHiLiteListener(NodeID, HiLiteListener)}. Once a hilite-lister is 'active' it will finally listen
     * to hilite-events.
     *
     * @param nnc the node to get the current hilite state for and activate the associated hilite-listener for
     * @throws IllegalStateException if there is no inactive (and to be activated) hilite-listener registered for the
     *             given node
     * @return the currently hilit keys
     */
    public Set<RowKey> getHiliteStateAndActivateListener(final NativeNodeContainer nnc) {
        var handler = nnc.getNodeModel().getInHiLiteHandler(0);
        Set<RowKey> hiLitKeys;
        var nodeId = nnc.getID();
        synchronized (handler) {
            hiLitKeys = handler.getHiLitKeys();
            activateHiLiteListener(nodeId, handler);
        }
        return hiLitKeys;
    }

    /**
     * Registers a {@link HiLiteListener} for a node (represented by its {@link NodeID}.
     *
     * <br>
     * IMPORTANT NOTE: the hilite-listener will NOT receive hilite-events until
     * {@link #getHiliteStateAndActivateListener(NativeNodeContainer)} has been called afterwards for the same node.
     *
     * @param nodeId the node to register the hilite-listener for
     * @param listener
     */
    public void registerHiLiteListener(final NodeID nodeId, final HiLiteListener listener) {
        m_registeredHiLiteListeners.put(nodeId, listener);
    }

    /**
     * Removes the (active or inactive) hilite-listener for the given node.
     *
     * @param nodeId
     */
    public void unregisterHiLiteListener(final NodeID nodeId) {
        m_registeredHiLiteListeners.remove(nodeId);
        if (m_activeHiLiteListeners.containsKey(nodeId)) {
            var value = m_activeHiLiteListeners.remove(nodeId);
            deactivateHiLiteListener(value);
        }
    }

    private void activateHiLiteListener(final NodeID nodeId, final HiLiteHandler handler) {
        var listener = m_registeredHiLiteListeners.get(nodeId);
        if (listener == null) {
            throw new IllegalStateException("Can't activate the hilite-listener. None registered for node " + nodeId);
        }
        handler.addHiLiteListener(listener);
        m_activeHiLiteListeners.put(nodeId, Pair.create(handler, listener));
        m_registeredHiLiteListeners.remove(nodeId);
    }

    private static void deactivateHiLiteListener(final Pair<HiLiteHandler, HiLiteListener> p) {
        if (p != null) {
            p.getFirst().removeHiLiteListener(p.getSecond());
        }
    }

}
