/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
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
 *   16.07.2013 (Peter Ohl): created
 */
package org.knime.workbench.editor2.commands;

import java.net.URI;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.UseImplUtil;
import org.knime.core.node.workflow.MetaNodeTemplateInformation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * GEF Command for changing the link (back to its template) of a sub node.
 *
 * @author Peter Ohl, KNIME.com AG, Zurich, Switzerland
 */
public class ChangeSubNodeLinkCommand extends AbstractKNIMECommand {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ChangeSubNodeLinkCommand.class);
    private URI m_oldLink;
    private URI m_newLink;

    /* must not keep NodeContainer here to enable undo/redo (the node container
     * instance may change if deleted and the delete is undone. */
    private final NodeID m_subNodeID;

    /**
     * @param flow the manager (project) that contains the sub node
     * @param subNode whose link to its template is changed
     * @param oldLink the old link for undo
     * @param newLink the new link to set
     *
     */
    public ChangeSubNodeLinkCommand(final WorkflowManager flow, final SubNodeContainer subNode, final URI oldLink,
        final URI newLink) {
        super(flow);
        m_subNodeID = subNode.getID();
        m_oldLink = oldLink;
        m_newLink = newLink;
    }

    /**
     * Sets the new bounds.
     *
     * @see org.eclipse.gef.commands.Command#execute()
     */
    @Override
    public void execute() {
        if (!setLink(m_newLink)) {
            m_oldLink = null; // disable undo
        }
    }

    private boolean setLink(final URI link) {
        NodeContainer subNode = UseImplUtil.getWFMImplOf(getHostWFM()).getNodeContainer(m_subNodeID);
        if (!(subNode instanceof SubNodeContainer)) {
            LOGGER.error("Command failed: Specified node is not a Wrapped Metanode");
            return false;
        }
        MetaNodeTemplateInformation templateInfo = ((SubNodeContainer)subNode).getTemplateInformation();
        MetaNodeTemplateInformation newInfo;
        try {
            newInfo = templateInfo.createLinkWithUpdatedSource(m_newLink);
        } catch (InvalidSettingsException e1) {
            // will not happen.
            LOGGER.error("Command failed: Specified node is not a Wrapped Metanode with a link." + e1.getMessage(), e1);
            return false;
        }
        UseImplUtil.getWFMImplOf(getHostWFM()).setTemplateInformation(m_subNodeID, newInfo);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canUndo() {
        return m_oldLink != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        return m_newLink != null && m_subNodeID != null;
    }

    /**
     * Sets the old bounds.
     *
     * @see org.eclipse.gef.commands.Command#execute()
     */
    @Override
    public void undo() {
        if (!setLink(m_oldLink)) {
            m_newLink = null;
        }
    }
}
