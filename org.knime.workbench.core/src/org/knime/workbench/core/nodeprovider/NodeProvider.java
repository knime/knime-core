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
 * Created: Mar 30, 2011
 * Author: ohl
 */
package org.knime.workbench.core.nodeprovider;

import java.util.concurrent.CopyOnWriteArrayList;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Used by views to initiate a node addition in a workflow editor. The workflow
 * editors listen to the provider(s) and will be notified when
 * {@link #addNode(NodeFactory)}) is called. Editors will then - if
 * circumstances permit - add the specified node. <br>
 * This would be used e.g. on a double-click or similar actions. Drag/drop
 * events should go through the appropriate mechanisms as they also provide
 * location.
 *
 * @author ohl, University of Konstanz
 */
public final class NodeProvider {

    /** the only and single instance of this class. */
    public static final NodeProvider INSTANCE = new NodeProvider();

    private NodeProvider() {
        // there is only one instance of this.
    }

    private final CopyOnWriteArrayList<EventListener> m_listeners =
            new CopyOnWriteArrayList<EventListener>();

    /**
     * For example workflow editors will register as listeners.
     *
     * @param listener to be notified about add events
     */
    public void addListener(final EventListener listener) {
        if (!m_listeners.contains(listener)) {
            m_listeners.add(listener);
        }
    }

    /**
     * Remove the listener.
     *
     * @param listener to be removed.
     */
    public void removeListener(final EventListener listener) {
        m_listeners.remove(listener);
    }

    /**
     * Triggers an event at all registered listeners.
     *
     * @param nodeFactory the node factory to create the new node from
     * @return true, if at least one listener actually added the node.
     */
    public boolean addNode(final NodeFactory<? extends NodeModel> nodeFactory) {
        boolean added = false;
        for (EventListener l : m_listeners) {
            added |= l.addNode(nodeFactory);
        }
        return added;
    }

    /**
     * Triggers an event at all registered listeners.
     *
     * @param sourceMgr the manager to copy the meta node from
     * @param sourceID the id of the node in the source mgr
     * @return true, if at least one listener actually added the node.
     */
    public boolean addMetaNode(final WorkflowManager sourceMgr,
            final NodeID sourceID) {
        boolean added = false;
        for (EventListener l : m_listeners) {
            added |= l.addMetaNode(sourceMgr, sourceID);
        }
        return added;

    }

    /**
     * Interface for interested listeners.
     *
     * @author ohl, University of Konstanz
     */
    public interface EventListener {
        /**
         * Called when a node should be added to the workflow editor. Only the
         * active editor should respond to the request.
         *
         * @param nodeFactory to create the new node from
         * @return true if this listener actually added the node
         */
        public boolean addNode(
                final NodeFactory<? extends NodeModel> nodeFactory);

        /**
         * Called when a meta node should be added to the workflow editor. Only
         * the active editor should respond to the request.
         *
         * @param sourceMgr the mgr to copy the meta node from
         * @param sourceID the id of the meta node in the source mgr
         * @return true, if this listener actually added the node
         */
        public boolean addMetaNode(final WorkflowManager sourceMgr,
                final NodeID sourceID);
    }
}
