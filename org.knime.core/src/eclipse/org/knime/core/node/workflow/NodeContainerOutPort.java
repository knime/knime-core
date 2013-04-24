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
 *   Mar 20, 2008 (mb): created from NodeOutPort (now an interface)
 */
package org.knime.core.node.workflow;

import java.util.HashSet;
import java.util.Set;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.util.ViewUtils;

/**
 * The implementation of an OutPort of a SingleNodeContainer - e.g. a "real"
 * node.
 *
 * @author M. Berthold, University of Konstanz
 */
public class NodeContainerOutPort extends NodePortAdaptor
    implements NodeOutPort {

    private final Set<NodeStateChangeListener>m_listener;

    /**
     * The underlying Node.
     */
    private final SingleNodeContainer m_snc;

    /**
     * The inspector view of this port. Could be null if not opened yet.
     */
    private OutPortView m_portView;

    /**
     * Creates a new output port with a fixed and ID (should unique to all other
     * output ports of this node) for the given node.
     *
     * @param snc the underlying SingleNodeContainer.
     * @param portIndex the (output) port index.
     */
    public NodeContainerOutPort(final SingleNodeContainer snc,
            final int portIndex) {
        super(portIndex, snc.getNode().getOutputType(portIndex));
        m_snc = snc;
        m_portView = null;
        // TODO register this object as listener to spec/object... changes
        //   with Node!!
        m_snc.addNodeStateChangeListener(this);
        m_listener = new HashSet<NodeStateChangeListener>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObjectSpec getPortObjectSpec() {
        return m_snc.getNode().getOutputSpec(getPortIndex());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObject getPortObject() {
        // the following test allows SingleNodeContainers/WFMs to hide
        // the PortObjects after a Node.execute() until the state of the
        // SNC/WFM has been adjusted to "EXECUTED"
        return m_snc.getInternalState().equals(InternalNodeContainerState.EXECUTED)
                      ? m_snc.getNode().getOutputObject(getPortIndex()) : null;
    }

    /** {@inheritDoc} */
    @Override
    public String getPortSummary() {
        return m_snc.getInternalState().equals(InternalNodeContainerState.EXECUTED)
            ? m_snc.getNode().getOutputObjectSummary(getPortIndex()) : null;
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
    public InternalNodeContainerState getNodeState() {
        return m_snc.getInternalState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HiLiteHandler getHiLiteHandler() {
        return m_snc.getNode().getOutputHiLiteHandler(getPortIndex());
    }

    /** Lets the underlying {@link SingleNodeContainer} create an outgoing
     * flow object stack and returns it.
     * @return a new flow stack containing the incoming variables and the
     * variables added in the node (whereby a loop end node will have all
     * variables added in the loop body removed).
     * @see SingleNodeContainer#createOutFlowObjectStack() */
    @Override
    public FlowObjectStack getFlowObjectStack() {
        return m_snc.createOutFlowObjectStack();
    }

    /**
     * Sets a port view for this port. The port view can only be set once.
     *
     * @param portView The port view to set.
     * @throws NullPointerException If the port view is null.
     * @throws IllegalStateException If the port view was already set.
     * @see #getPortView()
     */
    private final void setPortView(final OutPortView portView) {
        if (portView == null) {
            throw new NullPointerException("Can't set port view to null");
        }
        if (m_portView != null) {
            throw new IllegalStateException("Port View can only be set once.");
        }
        m_portView = portView;
    }

    /**
     * Returns the port view for this output port which can be null.
     *
     * @return The port view or null.
     */
    protected final OutPortView getPortView() {
        return m_portView;
    }

    /**
     * {@inheritDoc}
     */
    // TODO: return component with convenience method for Frame construction.
    @Override
    public void openPortView(final String name) {
        if (m_portView == null) {
            setPortView(new OutPortView(m_snc.getDisplayLabel(),
                    getPortName()));
        } else {
            // the custom name might have changed meanwhile
            m_portView.setTitle(getPortName() + " - "
                    + m_snc.getDisplayLabel());
        }
        m_portView.update(getPortObject(), getPortObjectSpec(),
                getFlowObjectStack(),
                m_snc.getNode().getCredentialsProvider());
        m_portView.openView();
    }

    /** {@inheritDoc} */
    @Override
    public void disposePortView() {
        if (m_portView == null) {
            return;
        }
        Runnable run = new Runnable() {
            /** {@inheritDoc} */
            @Override
            public void run() {
                if (m_portView != null) {
                    m_portView.update(null, null,
                            new FlowObjectStack(NodeID.ROOTID), null);
                    m_portView.setVisible(false);
                    m_portView.dispose();
                    m_portView = null;
                }
            }
        };
        ViewUtils.runOrInvokeLaterInEDT(run);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable {
        disposePortView();
        super.finalize();
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
        if (!m_snc.getNodeContainerState().isExecutionInProgress()) {
            notifyNodeStateChangeListener(state);
            if (m_portView != null) {
                try {
                    m_portView.update(getPortObject(), getPortObjectSpec(), getFlowObjectStack(),
                            m_snc.getNode().getCredentialsProvider());
                } catch (Exception e) {
                    NodeLogger.getLogger(getClass()).error("Failed to update port view.", e);
                }
            }
        }

    }

}
