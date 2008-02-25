/*
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 *
 * History
 *   04.05.2006(sieb, ohl): reviewed
 */
package org.knime.core.node;

import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.workflow.ScopeObjectStack;


/**
 * Class implements a node's output port. A variable number of input ports can
 * be connected to it (which are part of the next nodes in the workflow).
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public class NodeOutPort extends NodePort {

    /**
     * The inspector view of this port. Could be null if not opened yet.
     */
    private NodeOutPortView m_portView;

    /**
     * The data table for this port - if any.
     */
    private PortObject m_portObject;
    
    /** the following flag allows SingleNodeContainers/WFMs to hide
     * the PortObjects after a Node.execute() until the state of the
     * SNC/WFM has been adjusted to "EXECUTED".
     */
    private boolean m_showPortObject = true;

    /**
     * The table spec for this port.
     */
    private PortObjectSpec m_portObjectSpec;

    /**
     * The hilite handler of this port.
     */
    private HiLiteHandler m_hiliteHdl;

    /**
     * The ScopeContextObject stack of the underlying node.
     */
    private ScopeObjectStack m_scopeObjectStack;

    /**
     * Creates a new output port with a fixed and ID (should unique to all other
     * output ports of this node) for the given node.
     *
     * @param portID This port ID.
     */
    public NodeOutPort(final int portID, final PortType pType) {
        super(portID, pType);
        m_portView = null;
        m_portObjectSpec = null;
        m_portObject = null;
        m_hiliteHdl = null;
    }

    /**
     * Sets a new data table spec in this port. The port will notify port views.
     *
     * @param tSpec The new data table spec for this port or null.
     */
    void setPortObjectSpec(final PortObjectSpec tSpec) {
        m_portObjectSpec = tSpec;
        if (m_portView != null) {
            m_portView.update(m_portObject, m_portObjectSpec);
        }
    }

    /**
     * Returns the <code>DataTableSpec</code> or null if not available.
     *
     * @return The <code>DataTableSpec</code> for this port.
     */
    public PortObjectSpec getPortObjectSpec() {
        return m_portObjectSpec;
    }

    /**
     * Sets a new data table in this port. The port will notify all connected
     * input ports and its view.
     *
     * @param portObj The new port content for this port or null.
     * @param node the node the underlying node of the port.
     */
    void setPortObject(final PortObject portObj, final Node node) {
        if (m_portObject == portObj) {
            return;
        }
        // clean up old objects:
        if (m_portObject instanceof BufferedDataTable && node != null) {
                ((BufferedDataTable)m_portObject).clear(node);
        } else {
            // TODO cleanup operations of other PortObjects required?
        }
        // and remember the new one
        m_portObject = portObj;
        if (m_portView != null) {
            m_portView.update(m_portObject, m_portObjectSpec);
        }
    }

    /**
     * Returns the DataTable for this port, as set by the node this port is
     * output for.
     *
     * @return PortObject the object for this port. Can be null.
     */
    public PortObject getPortObject() {
        // the following test allows SingleNodeContainers/WFMs to hide
        // the PortObjects after a Node.execute() until the state of the
        // SNC/WFM has been adjusted to "EXECUTED"
        return isPortObjectVisible() ? m_portObject : null;
    }
    
    /**
     * Disable/Enable port content - used to make sure port stays in sync
     * with EXECUTED-flag and does not provide content to the outside while
     * the node is officially not (yet) executed.
     * 
     * @param flag true if content is to be seen
     */
    public void showPortObject(final boolean flag) {
        m_showPortObject = flag;
    }
    
    /**
     * @return true if content is to be seen
     */
    public boolean isPortObjectVisible() {
        return m_showPortObject;
    }

    /**
     * Returns the hilite handler for this port as set by the node this port is
     * output for.
     *
     * @return The HiLiteHandler for this port or null.
     */
    public HiLiteHandler getHiLiteHandler() {
        return m_hiliteHdl;
    }

    /**
     * Sets a new hilite handler in the port. The port will notify all
     * connected input ports of a really new (i.e. not equal) hightlight handler
     * available.
     *
     * @param hiLiteHdl The new HiLiteHandler for this port.
     */
    void setHiLiteHandler(final HiLiteHandler hiLiteHdl) {
        m_hiliteHdl = hiLiteHdl;
    }

    /**
     * Returns the scope object stack of the underlying node.
     *
     * @return the scope obj stack container
     */
    public ScopeObjectStack getScopeContextStackContainer() {
        return m_scopeObjectStack;
    }

    /**
     * Sets a new scope object stack.
     * @param sos scope object stack
     */
    void setScopeContextStackContainer(final ScopeObjectStack sos) {
        m_scopeObjectStack = sos;
    }


    /**
     * Sets a port view for this port. The port view can only be set once.
     *
     * @param portView The port view to set.
     * @throws NullPointerException If the port view is null.
     * @throws IllegalStateException If the port view was already set.
     * @see #getPortView()
     */
    private final void setPortView(final NodeOutPortView portView) {
        if (portView == null) {
            throw new NullPointerException("Can't set port view to null");
        }
        if (m_portView != null) {
            throw new IllegalStateException("Port View can only set once.");
        }
        m_portView = portView;
    }

    /**
     * Returns the port view for this output port which can be null.
     *
     * @return The port view or null.
     */
    protected final NodeOutPortView getPortView() {
        return m_portView;
    }

    /**
     * Opens the port view for this port with the given name.
     *
     * @param name The name of the port view.
     */
    // TODO: return component with convenience method for Frame construction.
    public void openPortView(final String name) {
        if (m_portView == null) {
            setPortView(NodeOutPortView.createOutPortView(getPortType(),
                    name, getPortName()));
        }
        m_portView.update(m_portObject, m_portObjectSpec);
        m_portView.openView();
    }

    /**
     * Call this when the port is not used anymore, certainly when you've opened
     * a view before. All port views will be closed and disposed.
     */
    protected void disposePortView(final Node node) {
        if (m_portView != null) {
            m_portView.setVisible(false);
            setPortObject(null, node);
            m_portView.dispose();
            m_portView = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable {
        // TODO (tg) This method is not guaranteed to be called!
        // make sure to blow away the port view
        disposePortView(null);
        super.finalize();
    }

}
