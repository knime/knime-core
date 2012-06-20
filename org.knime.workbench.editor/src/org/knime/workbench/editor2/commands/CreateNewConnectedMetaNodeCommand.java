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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * Created: Mar 31, 2011
 * Author: ohl
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPartViewer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowCopyContent;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Creates a new meta node - and may auto connect it to another one.
 *
 * @author ohl, University of Konstanz
 */
public class CreateNewConnectedMetaNodeCommand extends
        AbstractCreateNewConnectedNodeCommand {

    private final WorkflowManager m_source;

    private final NodeID m_sourceID;

    /**
     * Creates a new command to insert a new metanode.
     *
     * @param viewer the current viewer (usually a workflow editor)
     * @param hostWfm the workflow manager in which the node should be inserted
     * @param sourceWfm the workflow manager that contains the meta node
     *            template
     * @param sourceID the id of the meta node in the source workflow manager
     * @param location absolute coordinates of the new node
     * @param connectTo the node to which the new node should be connected
     */
    public CreateNewConnectedMetaNodeCommand(final EditPartViewer viewer,
            final WorkflowManager hostWfm, final WorkflowManager sourceWfm,
            final NodeID sourceID, final Point location, final NodeID connectTo) {
        super(viewer, hostWfm, location, connectTo);
        m_source = sourceWfm;
        m_sourceID = sourceID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        return super.canExecute() && m_source != null && m_sourceID != null;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeID createNewNode() {
        WorkflowCopyContent content = new WorkflowCopyContent();
        content.setNodeIDs(m_sourceID);
        WorkflowManager hostWFM = getHostWFM();
        NodeID[] copied =
                hostWFM.copyFromAndPasteHere(m_source, content).getNodeIDs();
        assert copied.length == 1;
        return copied[0];
    }
}
