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
 * -------------------------------------------------------------------
 *
 * History
 *   22.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.editparts;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeOutPort;
import org.knime.core.node.workflow.WorkflowInPort;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.WorkflowContextMenuProvider;
import org.knime.workbench.editor2.figures.NewToolTipFigure;
import org.knime.workbench.editor2.figures.WorkflowInPortFigure;
import org.knime.workbench.editor2.model.WorkflowPortBar;

/**
 * Edit part representing a {@link WorkflowInPort}.
 * Model: {@link WorkflowInPort}
 * View: {@link WorkflowInPortFigure}
 * Controller: {@link WorkflowInPortEditPart} 
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class WorkflowInPortEditPart extends AbstractPortEditPart {

//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            WorkflowInPortEditPart.class);

    private static final String PORT_NAME = "Workflow In Port";

    private boolean m_isSelected = false;
    
    /**
     *
     * @param type port type
     * @param portID port id
     */
    public WorkflowInPortEditPart(final PortType type, final int portID) {
        super(type, portID, true);
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
            return (NodeContainer)getParent().getModel();
        }
        // if the referring WorkflowManager is the "root" workflow manager of 
        // the open editor then the parent is a WorkflowRootEditPart
        return ((WorkflowPortBar)getParent().getModel()).getWorkflowManager();
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
     * Creates {@link WorkflowInPortFigure}, sets the tooltip and adds a 
     * {@link MouseListener} to the figure in order to detect if the figure was
     * clicked and a context menu entry should be provided to open the port 
     * view.
     * 
     * @see WorkflowContextMenuProvider#buildContextMenu(
     * org.eclipse.jface.action.IMenuManager)
     * @see WorkflowInPortFigure
     *  
     * {@inheritDoc}
     */
    @Override
    protected IFigure createFigure() {
        NodeOutPort port = getManager().getInPort(getIndex()).getUnderlyingPort();
        String tooltip = getTooltipText(PORT_NAME + ": "  + getIndex(), port);
        WorkflowInPortFigure f = new WorkflowInPortFigure(getType(),
                getManager().getNrInPorts(), getIndex(), tooltip);
        f.addMouseListener(new MouseListener() {

            public void mouseDoubleClicked(final MouseEvent me) { }

            /**
             * Set the selection state of the figure to true. This is 
             * evaluated in the context menu. If it is selected a context menu 
             * entry is provided to open the port view. 
             * 
             * @see WorkflowContextMenuProvider#buildContextMenu(
             *  org.eclipse.jface.action.IMenuManager)
             *  
             * {@inheritDoc}
             */
            public void mousePressed(final MouseEvent me) {
                setSelected(true);
            }
            
            /**
             * Set the selection state of the figure to true. This is 
             * evaluated in the context menu. If it is selected a context menu 
             * entry is provided to open the port view. 
             * 
             * @see WorkflowContextMenuProvider#buildContextMenu(
             *  org.eclipse.jface.action.IMenuManager)
             *  
             * {@inheritDoc}
             */
            public void mouseReleased(final MouseEvent me) {
                setSelected(false);
            }

        });
        return f;
    }

    /**
     * The context menu ({@link WorkflowContextMenuProvider#buildContextMenu(
     * org.eclipse.jface.action.IMenuManager)}) reads and resets the selection 
     * state. This state is read by the mouse listener added in 
     * {@link #createFigure()}.
     * 
     * @return true if the underlying workflow in port figure was clicked, false
     *  otherwise
     * @see WorkflowContextMenuProvider#buildContextMenu(
     *  org.eclipse.jface.action.IMenuManager)
     *  
     */
    public boolean isSelected() {
        return m_isSelected;
    }

    /**
     * The context menu ({@link WorkflowContextMenuProvider#buildContextMenu(
     * org.eclipse.jface.action.IMenuManager)}) reads and resets the selection 
     * state. This state is set by the mouse listener added in 
     * {@link #createFigure()}.
     * 
     * @param isSelected true if the figure was clicked, false otherwise.
     * 
     * @see WorkflowContextMenuProvider#buildContextMenu(
     *  org.eclipse.jface.action.IMenuManager)
     */
    public void setSelected(final boolean isSelected) {
        m_isSelected = isSelected;
    }

    /**
     * Returns the connections that has this workflow in-port as a source.
     *
     * @return list containing the connections, or an empty list. Never
     *         <code>null</code>
     *
     * @see org.eclipse.gef.GraphicalEditPart#getTargetConnections()
     */
    @Override
    public List<ConnectionContainer> getModelSourceConnections() {

        Set<ConnectionContainer> containers =
                getManager().getOutgoingConnectionsFor(
                        getNodeContainer().getID(),
                        getIndex());
        List<ConnectionContainer>conns = new ArrayList<ConnectionContainer>();
        if (containers != null) {
            conns.addAll(containers);
        }
        return conns;
    }

    /**
     *
     * @return empty list, as workflow in ports are never target for connections
     *
     * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart
     *      #getModelSourceConnections()
     */
    @Override
    protected List<ConnectionContainer> getModelTargetConnections() {
        return EMPTY_LIST;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rebuildTooltip() {
        NodeOutPort port = ((WorkflowInPort)getNodeContainer().getInPort(
                getIndex())).getUnderlyingPort();
        String tooltip = getTooltipText(PORT_NAME + ": " + getIndex(), port);
        ((NewToolTipFigure)getFigure().getToolTip()).setText(tooltip);
    }



}
