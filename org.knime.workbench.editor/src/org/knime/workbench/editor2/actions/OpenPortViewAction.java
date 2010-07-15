/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   Aug 5, 2005 (georg): created
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodePort;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.ImageRepository;

/**
 * Action to open a port view on a specific out-port.
 *
 * @author Florian Georg, University of Konstanz
 */
public class OpenPortViewAction extends Action {
    private final NodeContainer m_nodeContainer;

    private final int m_index;

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(OpenPortViewAction.class);

    /**
     * New action to opne view on a port.
     *
     * @param nodeContainer The node
     * @param portIndex The index of the out-port
     */
    public OpenPortViewAction(final NodeContainer nodeContainer,
            final int portIndex) {
        m_nodeContainer = nodeContainer;
        // the port index specified is the index including the implicit
        // flow var port. In this class we need the index used within the node,
        // that minus one - unless it is a meta node, (they don't have implicit
        // ports).
        if (!(m_nodeContainer instanceof WorkflowManager)) {
            assert portIndex > 0;
            if (portIndex > 0) {
                m_index = portIndex - 1;
            } else {
                m_index = portIndex;
            }
        } else {
            m_index = portIndex;
        }
    }

    protected int getPortIndex() {
        return m_index;
    }

    protected NodeContainer getNodeContainer() {
        return m_nodeContainer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/openView.gif");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Opens a view on outport #" + m_index;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return m_index + " "
                + m_nodeContainer.getOutPort(m_index).getPortName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        LOGGER.debug("Open Port View " + m_nodeContainer.getName() + " (#"
                + m_index + ")");
        NodePort port = m_nodeContainer.getOutPort(m_index);
        m_nodeContainer.getOutPort(m_index).openPortView(port.getPortName());
    }
}
