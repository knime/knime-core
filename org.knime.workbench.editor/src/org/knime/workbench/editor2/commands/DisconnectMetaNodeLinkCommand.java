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
 * History
 *   25.05.2011 (Bernd Wiswedel): created
 */
package org.knime.workbench.editor2.commands;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.MetaNodeTemplateInformation;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * GEF command for disconnecting meta node links.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich
 */
public class DisconnectMetaNodeLinkCommand extends AbstractKNIMECommand {

    public static final WeakHashMap<NodeID, URI> RECENTLY_USED_URIS =
        new WeakHashMap<NodeID, URI>();


    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(DisconnectMetaNodeLinkCommand.class);

    private final NodeID[] m_ids;
    private List<NodeID> m_changedIDs; // for undo
    private List<MetaNodeTemplateInformation> m_oldTemplInfos; // for undo

    /**
     * Creates a new command.
     *
     * @param manager The workflow manager containing the links to change
     * @param ids The ids of the link nodes.
     */
    public DisconnectMetaNodeLinkCommand(final WorkflowManager manager,
            final NodeID[] ids) {
        super(manager);
        m_ids = ids;
    }

    /** We can execute, if all components were 'non-null' in the constructor.
     * {@inheritDoc} */
    @Override
    public boolean canExecute() {
        if (!super.canExecute()) {
            return false;
        }
        if (m_ids == null) {
            return false;
        }
        for (NodeID id : m_ids) {
            NodeContainer nc = getHostWFM().getNodeContainer(id);
            if (nc instanceof WorkflowManager) {
                WorkflowManager wm = (WorkflowManager)nc;
                MetaNodeTemplateInformation lI = wm.getTemplateInformation();
                if (Role.Link.equals(lI.getRole())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void execute() {
        m_changedIDs = new ArrayList<NodeID>();
        m_oldTemplInfos = new ArrayList<MetaNodeTemplateInformation>();
        WorkflowManager hostWFM = getHostWFM();
        for (NodeID id : m_ids) {
            NodeContainer nc = hostWFM.getNodeContainer(id);
            if (nc instanceof WorkflowManager) {
                WorkflowManager wm = (WorkflowManager)nc;
                MetaNodeTemplateInformation lI = wm.getTemplateInformation();
                if (Role.Link.equals(lI.getRole())) {
                    MetaNodeTemplateInformation old =
                            hostWFM.setTemplateInformation(id,
                                    MetaNodeTemplateInformation.NONE);
                    RECENTLY_USED_URIS.put(wm.getID(), old.getSourceURI());
                    m_changedIDs.add(id);
                    m_oldTemplInfos.add(old);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean canUndo() {
        if (m_ids == null || m_ids.length == 0) {
            return false;
        }
        if (m_changedIDs == null || m_oldTemplInfos == null) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        LOGGER.debug("Undo: Reconnecting meta node links ("
                + m_changedIDs.size() + " meta node(s))");
        for (int i = 0; i < m_changedIDs.size(); i++) {
            NodeID id = m_changedIDs.get(i);
            MetaNodeTemplateInformation old = m_oldTemplInfos.get(i);
            getHostWFM().setTemplateInformation(id, old);
        }
        m_changedIDs = null;
        m_oldTemplInfos = null;
    }

}
