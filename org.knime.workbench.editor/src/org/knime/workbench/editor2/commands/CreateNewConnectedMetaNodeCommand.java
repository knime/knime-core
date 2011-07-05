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
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowCopyContent;
import org.knime.core.node.workflow.WorkflowManager;

/**
 *
 * @author ohl, University of Konstanz
 */
public class CreateNewConnectedMetaNodeCommand
    extends CreateNewConnectedNodeCommand {

    private final WorkflowManager m_source;

    private final NodeID m_sourceID;

    private final Point m_location;

    /**
     * @param viewer
     * @param manager
     * @param factory
     * @param location absolute coordinates of the new node
     * @param connectTo
     */
    public CreateNewConnectedMetaNodeCommand(final EditPartViewer viewer,
            final WorkflowManager hostWFM, final WorkflowManager source,
            final NodeID sourceID, final Point location, final NodeID connectTo) {
        super(viewer, hostWFM, null, location, connectTo);
        m_source = source;
        m_sourceID = sourceID;
        m_location = location;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        return super.canExecute() && m_source != null && m_sourceID != null
                && m_location != null;

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
                hostWFM.copyFromAndPasteHere(m_source, content)
                        .getNodeIDs();
        assert copied.length == 1;
        // create UI info
        NodeContainer newNode = hostWFM.getNodeContainer(copied[0]);
        NodeUIInformation uiInfo =
                newNode.getUIInformation();
        // create extra info and set it
        if (uiInfo == null) {
            uiInfo =
                    new NodeUIInformation(m_location.x, m_location.y, -1, -1,
                            true);
        } else {
            uiInfo.setNodeLocation(m_location.x, m_location.y,
                    uiInfo.getBounds()[2], uiInfo.getBounds()[3]);
        }
        newNode.setUIInformation(uiInfo);
        return copied[0];

    }

}
