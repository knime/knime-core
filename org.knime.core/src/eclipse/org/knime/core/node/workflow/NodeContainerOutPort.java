/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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

import java.awt.Rectangle;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.util.ThreadPool;

/**
 * The implementation of an OutPort of a SingleNodeContainer - e.g. a native node.
 *
 * @author M. Berthold, University of Konstanz
 */
public class NodeContainerOutPort extends NodePortAdaptor implements NodeOutPort {

    private final Set<NodeStateChangeListener>m_listener;

    /**
     * The underlying Node.
     */
    private final SingleNodeContainer m_snc;

    /**
     * The inspector view of this port. Could be null if not opened yet.
     */
    private OutPortView m_portView;

    /** Create output for {@link NativeNodeContainer}.
     * @param nnc the underlying NativeNodeContainer.
     * @param portIndex the (output) port index.
     * @noreference This constructor is not intended to be referenced by clients.
     */
    public NodeContainerOutPort(final NativeNodeContainer nnc, final int portIndex) {
        this(nnc, nnc.getOutputType(portIndex), portIndex);
    }

    /** Create output for {@link SubNodeContainer}.
     * @param snc the underlying SubNodeContainer.
     * @param type the port type of the output.
     * @param portIndex the (output) port index.
     * @noreference This constructor is not intended to be referenced by clients.
     */
    public NodeContainerOutPort(final SubNodeContainer snc, final PortType type, final int portIndex) {
        this((SingleNodeContainer)snc, type, portIndex);
    }

    private NodeContainerOutPort(final SingleNodeContainer snc, final PortType type, final int portIndex) {
        super(portIndex, type);
        m_snc = CheckUtils.checkArgumentNotNull(snc);
        m_portView = null;
        // TODO register this object as listener to spec/object... changes with Node!!
        m_snc.addNodeStateChangeListener(this);
        m_listener = new LinkedHashSet<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObjectSpec getPortObjectSpec() {
        return m_snc.getOutputSpec(getPortIndex());
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
                      ? m_snc.getOutputObject(getPortIndex()) : null;
    }

    /** {@inheritDoc} */
    @Override
    public String getPortSummary() {
        return m_snc.getInternalState().equals(InternalNodeContainerState.EXECUTED)
            ? m_snc.getOutputObjectSummary(getPortIndex()) : null;
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
    public HiLiteHandler getHiLiteHandler() {
        return m_snc.getOutputHiLiteHandler(getPortIndex());
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
    @Override
    public void openPortView(final String name) {
        openPortView(name, null);
    }

    /**
     * {@inheritDoc}
     * @since 2.12
     */
    @Override
    public void openPortView(final String name, final Rectangle knimeWindowBounds) {
        NodeContext.pushContext(m_snc);
        try {
            ViewUtils.invokeLaterInEDT(() -> {
                openPortViewInEDT(knimeWindowBounds);
            });
        } finally {
            NodeContext.removeLastContext();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createPortView(final Consumer<OutPortView> consumer) {
        NodeContext.pushContext(m_snc);
        try {
            ViewUtils.invokeLaterInEDT(() -> {
                final OutPortView view = constructPortViewInstance();
                final ThreadPool pool = KNIMEConstants.GLOBAL_THREAD_POOL;

                pool.enqueue(() -> {
                    consumer.accept(view);
                });
            });
        } finally {
            NodeContext.removeLastContext();
        }
    }

    private OutPortView constructPortViewInstance() {
        assert SwingUtilities.isEventDispatchThread();

        final OutPortView view = new OutPortView(m_snc.getDisplayLabel(), getPortName());
        view.update(getPortObject(), getPortObjectSpec(), getFlowObjectStack(), m_snc.getCredentialsProvider(),
            getHiLiteHandler());

        return view;
    }

    private void openPortViewInEDT(final Rectangle knimeWindowBounds) {
        assert SwingUtilities.isEventDispatchThread();
        if (m_portView == null) {
            m_portView = constructPortViewInstance();
        }
        // the custom name might have changed meanwhile
        m_portView.setTitle(getPortName() + " - " + m_snc.getDisplayLabel());
        m_portView.openView(knimeWindowBounds);
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
                NodeContext.pushContext(m_snc);
                try {
                    if (m_portView != null) {
                        m_portView.update(null, null, new FlowObjectStack(NodeID.ROOTID), null, null);
                        m_portView.setVisible(false);
                        m_portView.dispose();
                        m_portView = null;
                    }
                } finally {
                    NodeContext.removeLastContext();
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

    /** {@inheritDoc}
     * @since 2.8 */
    @Override
    public InternalNodeContainerState getNodeState() {
        return m_snc.getInternalState();
    }

    /** {@inheritDoc}
     * @since 3.2 */
    @Override
    public SingleNodeContainer getConnectedNodeContainer() {
        return m_snc;
    }

    /** {@inheritDoc}
     * @since 2.8 */
    @Override
    public NodeContainerState getNodeContainerState() {
        return getNodeState();
    }

    /** {@inheritDoc} */
    @Override
    public boolean addNodeStateChangeListener(
            final NodeStateChangeListener listener) {
        return m_listener.add(listener);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyNodeStateChangeListener(final NodeStateEvent e) {
        NodeContext.pushContext(m_snc);
        try {
            for (NodeStateChangeListener l : m_listener) {
                l.stateChanged(e);
            }
        } finally {
            NodeContext.removeLastContext();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeNodeStateChangeListener(
            final NodeStateChangeListener listener) {
        return m_listener.remove(listener);
    }

    /** {@inheritDoc} */
    @Override
    public void stateChanged(final NodeStateEvent state) {
        if (!m_snc.getNodeContainerState().isExecutionInProgress()) {
            NodeContext.pushContext(m_snc);
            try {
                notifyNodeStateChangeListener(state);
                if (m_portView != null) {
                    try {
                        m_portView.update(getPortObject(), getPortObjectSpec(), getFlowObjectStack(),
                                m_snc.getCredentialsProvider(), getHiLiteHandler());
                    } catch (Exception e) {
                        NodeLogger.getLogger(getClass()).error("Failed to update port view.", e);
                    }
                }
            } finally {
                NodeContext.removeLastContext();
            }
        }

    }

}
