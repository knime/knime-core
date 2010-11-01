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
 * ---------------------------------------------------------------------
 *
 * History
 *   28.04.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.editparts;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.draw2d.IFigure;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeOutPort;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.WorkflowOutPort;
import org.knime.workbench.editor2.figures.MetaNodeOutPortFigure;
import org.knime.workbench.ui.SyncExecQueueDispatcher;

/**
 * Represent edit part for meta node outport (attached to a node icon, decorated
 * with a bubble indicating the node state).
 * @author Fabian Dill, University of Konstanz
 */
public class MetaNodeOutPortEditPart extends AbstractPortEditPart
    implements NodeStateChangeListener {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            MetaNodeOutPortEditPart.class);

    /**
     * @param type type of port (data, db, model)
     * @param portIndex index of the port
     */
    public MetaNodeOutPortEditPart(final PortType type,
            final int portIndex) {
        super(type, portIndex, false);
        LOGGER.debug("created sub workflow out port edit part with type "
                + type + " and index " + portIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IFigure createFigure() {
        LOGGER.debug("returning new sub meta node out port figure "
                + " with type " + getType() + " index " + getIndex()
                + " nr outports " + getNodeContainer().getNrOutPorts()
                + " and tooltip " + getNodeContainer().getOutPort(getIndex())
                .getPortName());
        WorkflowOutPort model = (WorkflowOutPort)getModel();
        LOGGER.debug("model: " + getModel()
                + " state: " + model.getNodeState());

        NodeOutPort port = getNodeContainer().getOutPort(getIndex());
        String tooltip = getTooltipText(port.getPortName(), port);

        MetaNodeOutPortFigure f = new MetaNodeOutPortFigure(
                getType(), getIndex(),
                getNodeContainer().getNrOutPorts(),
                tooltip, model.getNodeState());
        model.addNodeStateChangeListener(this);
        f.setIsConnected(isConnected());
        return f;
    }

    /**
     * This returns the (single !) connection that has this in-port as a target.
     *
     * @return singleton list containing the connection, or an empty list. Never
     *         <code>null</code>
     *
     * {@inheritDoc}
     */
    @Override
    public List<ConnectionContainer> getModelSourceConnections() {
        if (getManager() == null) {
            return EMPTY_LIST;
        }
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
     * @return empty list, as out-ports are never target for connections
     *
     * {@inheritDoc}
     */
    @Override
    protected List<ConnectionContainer> getModelTargetConnections() {
        return EMPTY_LIST;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        super.removeNotify();
        // TODO remove the figure from the model
        ((WorkflowOutPort)getModel()).removeNodeStateChangeListener(this);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void stateChanged(final NodeStateEvent state) {
        SyncExecQueueDispatcher.asyncExec(new Runnable() {
            @Override
            public void run() {
                MetaNodeOutPortFigure fig = (MetaNodeOutPortFigure)getFigure();
                fig.setState(state.getState());
                rebuildTooltip();
                WorkflowOutPort outPort = (WorkflowOutPort)getModel();
                boolean isInactive =
                        outPort.getPortObjectSpec() instanceof InactiveBranchPortObjectSpec;
                fig.setInactive(isInactive);
                fig.repaint();
            }
        });
    }

}
