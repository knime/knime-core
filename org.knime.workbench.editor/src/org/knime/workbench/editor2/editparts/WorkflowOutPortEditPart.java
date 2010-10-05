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
 *   22.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.editparts;

import java.util.Collections;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalEditPart;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowOutPort;
import org.knime.workbench.editor2.figures.WorkflowOutPortFigure;
import org.knime.workbench.editor2.model.WorkflowPortBar;

/**
 * Edit part for the {@link WorkflowOutPort}.
 * Model: {@link WorkflowOutPort}
 * View: {@link WorkflowOutPortFigure}
 * Controller: {@link WorkflowOutPortEditPart}
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class WorkflowOutPortEditPart extends AbstractPortEditPart {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            WorkflowOutPortEditPart.class);


    
    /**
     * @param type port type
     * @param portID port id
     */
    public WorkflowOutPortEditPart(final PortType type, final int portID) {
        super(type, portID, false);
    }


    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void refreshVisuals() {
        IFigure fig = getFigure();
        fig.setBounds(new Rectangle(new Point(0, 50), new Dimension(30, 30)));
        super.refreshVisuals();
    }


    /**
     * Convenience, returns the hosting container.
     *
     * {@inheritDoc}
     */
    @Override
    protected final NodeContainer getNodeContainer() {
        if (getParent() == null) {
            return null;
        }
        // if the referring WorkflowManager is displayed as a meta node, then  
        // the parent is a NodeContainerEditPart
        if (getParent() instanceof NodeContainerEditPart) {
            return (NodeContainer) getParent().getModel();
        }
        // if the referring WorkflowManager is the "root" workflow manager of 
        // the open editor then the parent is a WorkflowRootEditPart
        return ((WorkflowPortBar)getParent().getModel()).getWorkflowManager();
    }
    
    /**
     * We must register *every* port as a listener on the workflow, as we have
     * not real objects for it.
     *
     * @see org.eclipse.gef.EditPart#activate()
     */
    @Override
    public void activate() {
        super.activate();
    }

    /**
     * Convenience, returns the WFM.
     *
     * {@inheritDoc}
     */
    @Override
    protected final WorkflowManager getManager() {
        return (WorkflowManager)getNodeContainer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IFigure createFigure() {
        LOGGER.debug("create figure. Parent's figure: " 
                + ((GraphicalEditPart)getParent()).getFigure());
        return new WorkflowOutPortFigure(getType(),
                getManager().getNrOutPorts(), getIndex(), 
                getManager().getDisplayLabel());
    }


    /**
     * This returns the (single !) connection that has this workflow out port 
     * as a target.
     *
     * @return singleton list containing the connection, or an empty list. Never
     *         <code>null</code>
     *
     * @see org.eclipse.gef.GraphicalEditPart#getTargetConnections()
     */
    @Override
    public List<ConnectionContainer> getModelTargetConnections() {
        if (getManager() == null) {
            return EMPTY_LIST;
        }
        ConnectionContainer container = getManager().getIncomingConnectionFor(
                getNodeContainer().getID(), getIndex());

        if (container != null) {
            return Collections.singletonList(container);
        }

        return EMPTY_LIST;
    }

    /**
     * @return empty list, as workflow out ports are never source for 
     * connections
     *
     * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart
     *      #getModelSourceConnections()
     */
    @Override
    protected List<ConnectionContainer> getModelSourceConnections() {
        return EMPTY_LIST;
    }


    @Override
    public void zoomChanged(double zoom) {
        // TODO store the old size in order to restore it on 
        // zoom out
        // on zoom out restore size and on zoom in restore it
        LOGGER.debug("zoom changed: " + zoom);
        LOGGER.debug(getFigure().getBounds());
        super.zoomChanged(zoom);
    }

}
