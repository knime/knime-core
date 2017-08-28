/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   31.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.editparts;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.draw2d.IFigure;
import org.eclipse.swt.widgets.Display;
import org.knime.core.def.node.workflow.IConnectionContainer;
import org.knime.core.def.node.workflow.INodeContainer;
import org.knime.core.def.node.workflow.INodeOutPort;
import org.knime.core.def.node.workflow.ISingleNodeContainer;
import org.knime.core.def.node.workflow.NodeStateChangeListener;
import org.knime.core.def.node.workflow.NodeStateEvent;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NodeOutPort;
import org.knime.workbench.editor2.figures.NodeOutPortFigure;

/**
 * Edit part for a {@link NodeOutPort}. Model: {@link NodeOutPort} View:
 * {@link NodeOutPortFigure} Controller: {@link NodeOutPortEditPart}
 *
 * @author Florian Georg, University of Konstanz
 */
public class NodeOutPortEditPart extends AbstractPortEditPart implements
        NodeStateChangeListener {

    /**
     * @param type the port type
     * @param portIndex the port index
     */
    public NodeOutPortEditPart(final PortType type, final int portIndex) {
        super(type, portIndex, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IFigure createFigure() {
        // Create the figure, we need the number of ports from the parent
        // container
        INodeContainer container = getNodeContainer();
        INodeOutPort port = container.getOutPort(getIndex());
        String tooltip = getTooltipText(port.getPortName(), port);
        boolean isMetaNode = !(container instanceof ISingleNodeContainer);
        NodeOutPortFigure portFigure =
                new NodeOutPortFigure(getType(), getIndex(),
                        container.getNrOutPorts(), isMetaNode, tooltip);
        portFigure.setInactive(port.isInactive());
        portFigure.setIsConnected(isConnected());
        return portFigure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void activate() {
        super.activate();
        INodeOutPort outPort = (INodeOutPort)getModel();
        outPort.addNodeStateChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deactivate() {
        INodeOutPort outPort = (INodeOutPort)getModel();
        outPort.removeNodeStateChangeListener(this);
        super.deactivate();
    }

    /**
     * This returns the (single !) connection that has this in-port as a target.
     *
     * @return singleton list containing the connection, or an empty list. Never
     *         <code>null</code>
     *
     *         {@inheritDoc}
     */
    @Override
    public List<IConnectionContainer> getModelSourceConnections() {
        if (getManager() == null) {
            return EMPTY_LIST;
        }
        Set<IConnectionContainer> containers =
                getManager().getOutgoingConnectionsFor(
                        getNodeContainer().getID(), getIndex());
        List<IConnectionContainer> conns = new ArrayList<IConnectionContainer>();
        if (containers != null) {
            conns.addAll(containers);
        }
        return conns;
    }

    /**
     *
     * @return empty list, as out-ports are never target for connections
     *
     *         {@inheritDoc}
     */
    @Override
    protected List<IConnectionContainer> getModelTargetConnections() {
        return EMPTY_LIST;
    }

    private final AtomicBoolean m_updateInProgressFlag = new AtomicBoolean(false);

    /**
     * {@inheritDoc}
     */
    @Override
    public void stateChanged(final NodeStateEvent state) {
        if (m_updateInProgressFlag.compareAndSet(false, true)) {
            Display display = Display.getDefault();
            if (display.isDisposed()) {
                return;
            }
            display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    if (!isActive()) {
                        return;
                    }
                    m_updateInProgressFlag.set(false);
                    INodeOutPort outPort = (INodeOutPort)getModel();
                    NodeOutPortFigure fig = (NodeOutPortFigure)getFigure();
                    rebuildTooltip();
                    fig.setInactive(outPort.isInactive());
                    fig.repaint();
                }
            });
        }
    }

}
