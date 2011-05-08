/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 * 
 * History
 *   07.05.2011 (mb): created
 */
package org.knime.workbench.editor2.commands;

import java.util.Collection;

import org.eclipse.gef.commands.Command;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * 
 * @author M. Berthold, University of Konstanz
 */
public class ExpandMetaNodeCommand extends Command {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            ExpandMetaNodeCommand.class);

    private WorkflowManager m_manager;
    private String m_name;
    private NodeID[] m_nodes;

    /**
     * @param wfm the workflow manager holding the new metanode
     * @param id of node to be expanded.
     */
    public ExpandMetaNodeCommand(final WorkflowManager wfm,
            final NodeID id) {
        m_manager = wfm;
        NodeContainer nc = wfm.getNodeContainer(id);
        m_name = nc.getName();
        WorkflowManager wm = (WorkflowManager)nc;
        Collection<NodeContainer> ncs = wm.getNodeContainers();
        m_nodes = new NodeID[ncs.size()];
        int i = 0;
        for (NodeContainer n : ncs) {
            m_nodes[i] = n.getID();
            i++;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        // do nothing - all done in corresponding Action.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canUndo() {
        if (m_manager != null && m_nodes != null) {
            return null == m_manager.canCollapseNodesIntoMetaNode(m_nodes);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        m_manager.collapseNodesIntoMetaNode(m_nodes, m_name);
        m_manager = null;
        m_name = null;
        m_nodes = null;
    }

}
