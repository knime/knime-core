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


    /** @return true if the contained spec is not null and instance of
     * {@link org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec}
     */
    public boolean isInactive();

    /**
     * @return the state of the node owning this port.
     */
    public InternalNodeContainerState getNodeState();


    /**
     * Returns the hilite handler for this port as set by the node this port is
     * output for.
     *
     * @return The HiLiteHandler for this port or null.
     */
    public HiLiteHandler getHiLiteHandler();

    /**
     * Returns the {@link FlowObjectStack} of the underlying node.
     *
     * @return the flow obj stack container
     */
    public FlowObjectStack getFlowObjectStack();

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
