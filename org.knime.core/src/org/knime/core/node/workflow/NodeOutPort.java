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
 *   21.03.2008(mb): changed to an interface to accomodate wfm 2.0
 */
package org.knime.core.node.workflow;

import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.property.hilite.HiLiteHandler;


/**
 * Interface for a node's output port. A variable number of input ports can
 * be connected to it (which are part of the next nodes in the workflow).
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public interface NodeOutPort extends NodePort, NodeStateChangeListener {

    /**
     * Returns the <code>DataTableSpec</code> or null if not available.
     *
     * @return The <code>DataTableSpec</code> for this port.
     */
    public PortObjectSpec getPortObjectSpec();

    /**
     * Returns the DataTable for this port, as set by the node this port is
     * output for.
     *
     * @return PortObject the object for this port. Can be null.
     */
    public PortObject getPortObject();
    
    /** Get summary of the underlying port object as provided by 
     * {@link PortObject#getSummary()}. It's a separate method since calling
     * getPortObject().getSummary() may force the underlying table (if it is
     * a table) to restore its content from disc. Summaries are saved in the 
     * workflow file (or the node's corresponding sub directory).
     * @return The port object's summary. 
     */ 
    public String getPortSummary();

    /**
     * @return the state of the node owning this port.
     */
    public NodeContainer.State getNodeState();
    
    
    /**
     * Returns the hilite handler for this port as set by the node this port is
     * output for.
     *
     * @return The HiLiteHandler for this port or null.
     */
    public HiLiteHandler getHiLiteHandler();

    /**
     * Returns the scope object stack of the underlying node.
     *
     * @return the scope obj stack container
     */
    public ScopeObjectStack getScopeContextStackContainer();

    /**
     * Opens the port view for this port with the given name.
     *
     * @param name The name of the port view.
     */
    // TODO: return component with convenience method for Frame construction.
    public void openPortView(final String name);
    
    /** Dispose the view (if any) associated with this port. */
    public void disposePortView();
    
    /**
     * 
     * @param listener a listener to the state of the port, that is the state
     *  of the predecessor node
     * @return true if the listener was added, false if it was already 
     *  registered
     */
    public boolean addNodeStateChangeListener(
            final NodeStateChangeListener listener);
    
    /**
     * 
     * @param listener the listener to be de-registered
     * @return true if it was successfully removed, false if it was not 
     *  registered
     */
    public boolean removeNodeStateChangeListener(
            final NodeStateChangeListener listener);
    
    /**
     * 
     * @param e the event which should be forwarded to all regsitered 
     * {@link NodeStateChangeListener}s
     */
    public void notifyNodeStateChangeListener(final NodeStateEvent e);
    

}
