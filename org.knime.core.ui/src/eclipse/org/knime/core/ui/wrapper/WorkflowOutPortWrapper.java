/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Oct 13, 2016 (hornm): created
 */
package org.knime.core.ui.wrapper;

import java.awt.Rectangle;

import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.node.workflow.NodeContainerState;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowOutPort;
import org.knime.core.ui.node.workflow.UIWorkflowOutPort;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class WorkflowOutPortWrapper extends AbstractWrapper<WorkflowOutPort> implements UIWorkflowOutPort {

    private WorkflowOutPort m_delegate;

    /**
     * @param delegate the implementation to delegate to
     */
    public WorkflowOutPortWrapper(final WorkflowOutPort delegate) {
        super(delegate);
        m_delegate = delegate;
    }

    public static final WorkflowOutPortWrapper wrap(final WorkflowOutPort wop) {
        return (WorkflowOutPortWrapper)Wrapper.wrapOrGet(wop, wop, o -> new WorkflowOutPortWrapper(o));
    }

    @Override
    public final int getPortIndex() {
        return m_delegate.getPortIndex();
    }

    public PortObject getPortObject() {
        return m_delegate.getPortObject();
    }

    @Override
    public final PortType getPortType() {
        return m_delegate.getPortType();
    }

    @Override
    public void setPortIndex(final int portIndex) {
        m_delegate.setPortIndex(portIndex);
    }

    @Override
    public final String getPortName() {
        return m_delegate.getPortName();
    }

    @Override
    public final void setPortName(final String portName) {
        m_delegate.setPortName(portName);
    }

    public HiLiteHandler getHiLiteHandler() {
        return m_delegate.getHiLiteHandler();
    }

    public FlowObjectStack getFlowObjectStack() {
        return m_delegate.getFlowObjectStack();
    }

    @Override
    public String getPortSummary() {
        return m_delegate.getPortSummary();
    }

    @Override
    public NodeContainerState getNodeState() {
        return m_delegate.getNodeState();
    }

    public SingleNodeContainer getConnectedNodeContainer() {
        return m_delegate.getConnectedNodeContainer();
    }

    public PortObjectSpec getPortObjectSpec() {
        return m_delegate.getPortObjectSpec();
    }

    @Override
    public boolean isInactive() {
        return m_delegate.isInactive();
    }

    public void openPortView(final String name) {
        m_delegate.openPortView(name);
    }

    public void openPortView(final String name, final Rectangle knimeWindowBounds) {
        m_delegate.openPortView(name, knimeWindowBounds);
    }

    public void disposePortView() {
        m_delegate.disposePortView();
    }

    @Override
    public boolean equals(final Object obj) {
        return m_delegate.equals(obj);
    }

    @Override
    public int hashCode() {
        return m_delegate.hashCode();
    }

    @Override
    public String toString() {
        return m_delegate.toString();
    }

    @Override
    public boolean addNodeStateChangeListener(final NodeStateChangeListener listener) {
        return m_delegate.addNodeStateChangeListener(listener);
    }

    @Override
    public void notifyNodeStateChangeListener(final NodeStateEvent e) {
        m_delegate.notifyNodeStateChangeListener(e);
    }

    @Override
    public boolean removeNodeStateChangeListener(final NodeStateChangeListener listener) {
        return m_delegate.removeNodeStateChangeListener(listener);
    }

    @Override
    public NodeContainerState getNodeContainerState() {
        return m_delegate.getNodeContainerState();
    }

    @Override
    public void stateChanged(final NodeStateEvent state) {
        m_delegate.stateChanged(state);
    }
}
