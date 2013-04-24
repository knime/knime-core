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
 *   Apr 1, 2008 (mb/bw): created
 */
package org.knime.core.node.workflow;

import java.util.HashSet;
import java.util.Set;

import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.workflow.NodeContainer.State;

/**
 *
 * @author B. Wiswedel, M. Berthold, University of Konstanz
 */
public class NodeOutPortWrapper extends NodePortAdaptor implements NodeOutPort {

    private final Set<NodeStateChangeListener>m_listener;

    /** if connected, reference the outport this node is connected to.
     */
    private NodeOutPort m_underlyingPort;

    /**
     * Creates a new output port with a fixed type and index (should unique
     * to all other output ports of this node) for the given node.
     *
     * @param pType The port's type
     * @param portIndex This port index
     */
    NodeOutPortWrapper(final int portIndex, final PortType pType) {
        super(portIndex, pType);
        m_listener = new HashSet<NodeStateChangeListener>();
    }

    /**
     * Set a new underlying port - used when the connection inside this
     * workflow to this outgoing port changes. The argument is the new
     * NodeOutPort connected to the outgoing port of the WFM.
     *
     * @param p new port
     */
    void setUnderlyingPort(final NodeOutPort p) {
        if (m_underlyingPort != null) {
            m_underlyingPort.removeNodeStateChangeListener(this);
        }
        m_underlyingPort = p;
        if (m_underlyingPort != null) {
            // this fixes bug #1499
            // we use some prefix here and do not simply copy the name since
            // the default (null) name is "Outport 0", for instance. If we then
            // connect a WFM to an outer WFM, whereby the inner WFM is not
            // connected (i.e. its output would show "Output X"), we would end
            // up copying whatever the WFM decides (the port could show
            // "Outport 3" although the port is "0"
            // besides all that it's reasonable to show that the port only
            // forwards data, it does not hold it.
            String prefix = "Connected to: ";
            String name = m_underlyingPort.getPortName();
            if (name != null && !name.startsWith(prefix)) {
                name = prefix + name;
            }
            setPortName(name);
            m_underlyingPort.addNodeStateChangeListener(this);
            // if not null -> query state and throw event
            notifyNodeStateChangeListener(new NodeStateEvent(new NodeID(0),
                                                             m_underlyingPort.getNodeState().mapToOldStyleState()));
        } else {
            setPortName(null);
            // if null: disconnected set state -> idle
            notifyNodeStateChangeListener(new NodeStateEvent(new NodeID(0), State.IDLE));
        }
    }

    /**
     * @return underlying NodeOutPort
     */
    NodeOutPort getUnderlyingPort() {
        return m_underlyingPort;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HiLiteHandler getHiLiteHandler() {
        if (m_underlyingPort == null) {
            return null;
        }
        return m_underlyingPort.getHiLiteHandler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FlowObjectStack getFlowObjectStack() {
        if (m_underlyingPort == null) {
            return null;
        }
        FlowObjectStack sos = m_underlyingPort.getFlowObjectStack();
        return sos;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObject getPortObject() {
        if (m_underlyingPort == null) {
            return null;
        }
        return m_underlyingPort.getPortObject();
    }

    /** {@inheritDoc} */
    @Override
    public String getPortSummary() {
        if (m_underlyingPort == null) {
            return null;
        }
        return m_underlyingPort.getPortSummary();
    }

    /**
     * {@inheritDoc}
     * @since 2.8
     */
    @Override
    public InternalNodeContainerState getNodeState() {
        if (m_underlyingPort == null) {
            /*
             * Return the IDLE state if the underlying port is null.
             * This may happen during loading of meta nodes and
             * if the port is not connected. The port is anyway intersted in
             * the state and it is displayed as "no spec/node data".
             *
             * TODO: when necessary (e.g. if a "not connected" state for the
             * port should be displayed an additional event type has to be
             * implemented (NodeOutPort.State = {Not_CONNECTED, NO_SPEC_NO_DATA,
             * SPEC_AVAILABLE, DATA_AVAILABEL}. Then the SingleNodeContainer
             * has to cvonvert between the NodeContainer.State and the
             * NodeOutPort.State. The NodeContainer.State can then be moved to
             * the SingleNodeContainer.
             * Meanwhile return IDLE.
             */
            return InternalNodeContainerState.IDLE;
        }
        return m_underlyingPort.getNodeState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObjectSpec getPortObjectSpec() {
        if (m_underlyingPort == null) {
            return null;
        }
        return m_underlyingPort.getPortObjectSpec();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInactive() {
        return getPortObjectSpec() instanceof InactiveBranchPortObjectSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void openPortView(final String name) {
        if (m_underlyingPort == null) {
            return;
        }
        m_underlyingPort.openPortView(name);
    }

    /** {@inheritDoc} */
    @Override
    public void disposePortView() {
        // it's task of the underlying port to dispose views.
    }

    ///////////////////////////
    // Equals/HashCode/ToString
    ///////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (m_underlyingPort == null) {
            return this == obj;
        }
        return m_underlyingPort.equals(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        if (m_underlyingPort == null) {
            return System.identityHashCode(this);
        }
        return m_underlyingPort.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (m_underlyingPort == null) {
            return "<<not connected>>";
        }
        return m_underlyingPort.toString();
    }

    ///////////////////////////////////////////////
    ///         State Listener methods
    //////////////////////////////////////////////

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean addNodeStateChangeListener(
            final NodeStateChangeListener listener) {
        return m_listener.add(listener);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void notifyNodeStateChangeListener(final NodeStateEvent e) {
        for (NodeStateChangeListener l : m_listener) {
            l.stateChanged(e);
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean removeNodeStateChangeListener(
            final NodeStateChangeListener listener) {
        return m_listener.remove(listener);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void stateChanged(final NodeStateEvent state) {
        notifyNodeStateChangeListener(state);
    }

}
