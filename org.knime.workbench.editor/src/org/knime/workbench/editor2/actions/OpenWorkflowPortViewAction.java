/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 *
 * History
 *   05.02.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.actions;


import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeOutPort;
import org.knime.core.node.workflow.WorkflowInPort;
import org.knime.workbench.KNIMEEditorPlugin;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class OpenWorkflowPortViewAction extends OpenPortViewAction {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            OpenWorkflowPortViewAction.class);

    private final NodeOutPort m_port;

    /**
     * @param nodeContainer
     * @param portIndex
     */
    public OpenWorkflowPortViewAction(final NodeContainer nodeContainer,
            final int portIndex) {
        super(nodeContainer, portIndex);
        m_port = ((WorkflowInPort)getNodeContainer().getInPort(getPortIndex()))
            .getUnderlyingPort();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {

        // the name is constructed like "Model outport <specificIndex>"
        String name;

        if (m_port.getPortType().equals(
                BufferedDataTable.TYPE)) {
            name = "Workflow Data Inport " + getPortIndex();
        } else if (KNIMEEditorPlugin.PMML_PORT_TYPE.isSuperTypeOf(
                m_port.getPortType())) {
            name = "Workflow Model Inport " + getPortIndex();
        } else {
            name = "Unknown Outport " + getPortIndex();
        }

        // the text will be displayed in the context menu
        // it consists of the specific port type and index and its description
        String description = m_port.getPortName();

        return name + ": " + description;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        LOGGER.debug("Open Workflow Port View " + getNodeContainer().getName()
                + " (#" + getPortIndex()+ ")");
        m_port.openPortView(m_port.getPortName());
    }

}
