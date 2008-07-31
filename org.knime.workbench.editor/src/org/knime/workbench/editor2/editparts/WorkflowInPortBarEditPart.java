/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   20.02.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.editparts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.knime.core.node.workflow.NodePort;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.extrainfo.ModellingNodeExtraInfo;
import org.knime.workbench.editor2.figures.WorkflowInPortBarFigure;
import org.knime.workbench.editor2.model.WorkflowPortBar;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class WorkflowInPortBarEditPart extends AbstractWorkflowPortBarEditPart {

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected List<NodePort> getModelChildren() {
        WorkflowManager manager = ((WorkflowPortBar)getModel())
            .getWorkflowManager();
        List<NodePort> ports 
            = new ArrayList<NodePort>();
        for (int i = 0; i < manager.getNrWorkflowIncomingPorts(); i++) {
            ports.add(manager.getInPort(i));
        }
        return ports;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected IFigure createFigure() {
        WorkflowInPortBarFigure fig = new WorkflowInPortBarFigure();
        ModellingNodeExtraInfo uiInfo = ((WorkflowPortBar)getModel())
            .getUIInfo();
        if (uiInfo != null && uiInfo.isFilledProperly()) {
            int[] bounds = uiInfo.getBounds();
            Rectangle newBounds = new Rectangle(
                    bounds[0], bounds[1], bounds[2], bounds[3]);
            fig.setBounds(newBounds);
            // TODO: do we need this? or is it enought o set the bounds?
            fig.setInitialized(true);
        }
        return fig;
    }

}
