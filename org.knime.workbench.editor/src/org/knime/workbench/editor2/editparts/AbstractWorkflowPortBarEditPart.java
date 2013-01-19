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
 * ---------------------------------------------------------------------
 *
 * History
 *   20.02.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.editparts;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LayoutManager;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.workflow.NodePort;
import org.knime.core.node.workflow.NodePropertyChangedEvent;
import org.knime.core.node.workflow.NodePropertyChangedListener;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.WorkflowSelectionDragEditPartsTracker;
import org.knime.workbench.editor2.editparts.policy.PortGraphicalRoleEditPolicy;
import org.knime.workbench.editor2.figures.AbstractWorkflowPortBarFigure;
import org.knime.workbench.editor2.model.WorkflowPortBar;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public abstract class AbstractWorkflowPortBarEditPart
    extends AbstractWorkflowEditPart implements ConnectableEditPart, NodePropertyChangedListener {


    /**
     * {@inheritDoc}
     */
    @Override
    public void activate() {
        super.activate();
        // need to know about meta node port changes
        getNodeContainer().addNodePropertyChangedListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deactivate() {
        super.deactivate();
        getNodeContainer().removeNodePropertyChangedListener(this);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void refreshVisuals() {
        NodeUIInformation uiInfo = ((WorkflowPortBar)getModel())
            .getUIInfo();
        if (uiInfo != null && uiInfo.isFilledProperly()
                && !((AbstractWorkflowPortBarFigure)getFigure())
                .isInitialized()) {
            int[] bounds = uiInfo.getBounds();
            ((AbstractWorkflowPortBarFigure)getFigure()).setBounds(
                    new Rectangle(bounds[0], bounds[1], bounds[2], bounds[3]));
        }
        super.refreshVisuals();
    }

    /**
     * Updates the port index in all port editparts from the underlying port model.
     */
    private void updatePortIndex() {
        for (Object ep : getChildren()) {
            if (ep instanceof AbstractPortEditPart) {
                Object model = ((EditPart)ep).getModel();
                if (model instanceof NodePort) {
                    ((AbstractPortEditPart)ep).setIndex(((NodePort)model).getPortIndex());
                }
            }
        }
    }

    private void updateNumberOfPorts() {
        for (Object ep : getChildren()) {
            if (ep instanceof AbstractPortEditPart) {
                ((AbstractPortEditPart)ep).updateNumberOfPorts();
            }
        }
    }

    private void relayoutPorts() {
        IFigure nodeFig = getFigure();
        LayoutManager layoutManager = nodeFig.getLayoutManager();
        if (layoutManager != null) {
            layoutManager.invalidate();
            layoutManager.layout(figure);
        }
        nodeFig.repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createEditPolicies() {

        // This policy provides create/reconnect commands for connections that
        // are associated with ports of this node
        this.installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE,
                new PortGraphicalRoleEditPolicy());

    }

    /**
     * Overridden to return a custom <code>DragTracker</code> for
     * NodeContainerEditParts.
     *
     * @see org.eclipse.gef.EditPart#getDragTracker(Request)
     */
    @Override
    public DragTracker getDragTracker(final Request request) {
        return new WorkflowSelectionDragEditPartsTracker(this);
    }


    /** {@inheritDoc} */
    @Override
    public WorkflowManager getNodeContainer() {
        return ((WorkflowPortBar)getModel()).getWorkflowManager();
    }

    /** {@inheritDoc} */
    @Override
    public void nodePropertyChanged(final NodePropertyChangedEvent e) {
        Display.getDefault().asyncExec(new Runnable() {
            /** {@inheritDoc} */
            @Override
            public void run() {
                if (isActive()) {
                    switch (e.getProperty()) {
                    case JobManager:
                    case Name:
                    case TemplateConnection:
                    case LockStatus:
                        break;
                    case MetaNodePorts:
                        refreshChildren(); // account for new/removed ports
                        updatePortIndex(); // set the (possibly changed) index in all ports
                        updateNumberOfPorts();
                        relayoutPorts();   // in case an index has changed
                        break;
                    default:
                        // unknown, ignore
                    }
                }
            }
        });
    }

}
