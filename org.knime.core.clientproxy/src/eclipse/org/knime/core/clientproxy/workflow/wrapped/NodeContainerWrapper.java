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
package org.knime.core.clientproxy.workflow.wrapped;

import java.net.URL;
import java.util.Optional;

import org.knime.core.api.node.NodeType;
import org.knime.core.api.node.workflow.INodeAnnotation;
import org.knime.core.api.node.workflow.INodeContainer;
import org.knime.core.api.node.workflow.INodeInPort;
import org.knime.core.api.node.workflow.INodeOutPort;
import org.knime.core.api.node.workflow.ISingleNodeContainer;
import org.knime.core.api.node.workflow.IWorkflowManager;
import org.knime.core.api.node.workflow.JobManagerUID;
import org.knime.core.api.node.workflow.NodeContainerState;
import org.knime.core.api.node.workflow.NodeMessageListener;
import org.knime.core.api.node.workflow.NodeProgressEvent;
import org.knime.core.api.node.workflow.NodeProgressListener;
import org.knime.core.api.node.workflow.NodePropertyChangedListener;
import org.knime.core.api.node.workflow.NodeStateChangeListener;
import org.knime.core.api.node.workflow.NodeUIInformation;
import org.knime.core.api.node.workflow.NodeUIInformationListener;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeMessage;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public abstract class NodeContainerWrapper implements INodeContainer {

    private final INodeContainer m_delegate;

    /**
     * @param delegate the implementation to delegate to
     *
     */
    protected NodeContainerWrapper(final INodeContainer delegate) {
        m_delegate = delegate;
    }

    public static final NodeContainerWrapper wrap(final INodeContainer nc) {
        if(nc instanceof ISingleNodeContainer) {
            return SingleNodeContainerWrapper.wrap((ISingleNodeContainer)nc);
        } else if(nc instanceof IWorkflowManager) {
            return WorkflowManagerWrapper.wrap((IWorkflowManager) nc);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_delegate.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        return m_delegate.equals(obj);
    }

    @Override
    public void progressChanged(final NodeProgressEvent pe) {
        m_delegate.progressChanged(pe);
    }

    @Override
    public IWorkflowManager getParent() {
        return WorkflowManagerWrapper.wrap(m_delegate.getParent());
    }

    @Override
    public Optional<JobManagerUID> getJobManagerUID() {
        return m_delegate.getJobManagerUID();
    }

    @Override
    public JobManagerUID findJobManagerUID() {
        return m_delegate.findJobManagerUID();
    }

    @Override
    public boolean addNodePropertyChangedListener(final NodePropertyChangedListener l) {
        return m_delegate.addNodePropertyChangedListener(l);
    }

    @Override
    public boolean removeNodePropertyChangedListener(final NodePropertyChangedListener l) {
        return m_delegate.removeNodePropertyChangedListener(l);
    }

    @Override
    public void clearWaitingLoopList() {
        m_delegate.clearWaitingLoopList();
    }

    @Override
    public boolean addProgressListener(final NodeProgressListener listener) {
        return m_delegate.addProgressListener(listener);
    }

    @Override
    public boolean removeNodeProgressListener(final NodeProgressListener listener) {
        return m_delegate.removeNodeProgressListener(listener);
    }

    @Override
    public boolean addNodeMessageListener(final NodeMessageListener listener) {
        return m_delegate.addNodeMessageListener(listener);
    }

    @Override
    public boolean removeNodeMessageListener(final NodeMessageListener listener) {
        return m_delegate.removeNodeMessageListener(listener);
    }

    @Override
    public NodeMessage getNodeMessage() {
        return m_delegate.getNodeMessage();
    }

    @Override
    public void setNodeMessage(final NodeMessage newMessage) {
        m_delegate.setNodeMessage(newMessage);
    }

    @Override
    public void addUIInformationListener(final NodeUIInformationListener l) {
        m_delegate.addUIInformationListener(l);
    }

    @Override
    public void removeUIInformationListener(final NodeUIInformationListener l) {
        m_delegate.removeUIInformationListener(l);
    }

    @Override
    public NodeUIInformation getUIInformation() {
        return m_delegate.getUIInformation();
    }

    @Override
    public void setUIInformation(final NodeUIInformation uiInformation) {
        m_delegate.setUIInformation(uiInformation);
    }

    @Override
    public boolean addNodeStateChangeListener(final NodeStateChangeListener listener) {
        return m_delegate.addNodeStateChangeListener(listener);
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
    public boolean hasDataAwareDialogPane() {
        return m_delegate.hasDataAwareDialogPane();
    }

    @Override
    public boolean isAllInputDataAvailable() {
        return m_delegate.isAllInputDataAvailable();
    }

    @Override
    public boolean canExecuteUpToHere() {
        return m_delegate.canExecuteUpToHere();
    }

    @Override
    public void applySettingsFromDialog() throws InvalidSettingsException {
        m_delegate.applySettingsFromDialog();
    }

    /** {@inheritDoc} */
    @Override
    public ConfigBaseRO getNodeSettings() {
        return m_delegate.getNodeSettings();
    }

    @Override
    public boolean areDialogSettingsValid() {
        return m_delegate.areDialogSettingsValid();
    }

    @Override
    public boolean hasDialog() {
        return m_delegate.hasDialog();
    }

    @Override
    public boolean areDialogAndNodeSettingsEqual() {
        return m_delegate.areDialogAndNodeSettingsEqual();
    }

    @Override
    public int getNrInPorts() {
        return m_delegate.getNrInPorts();
    }

    @Override
    public INodeInPort getInPort(final int index) {
        return new NodeInPortWrapper(m_delegate.getInPort(index));
    }

    @Override
    public INodeOutPort getOutPort(final int index) {
        return new NodeOutPortWrapper(m_delegate.getOutPort(index));
    }

    @Override
    public int getNrOutPorts() {
        return m_delegate.getNrOutPorts();
    }

    @Override
    public int getNrViews() {
        return m_delegate.getNrViews();
    }

    @Override
    public int getNrNodeViews() {
        return m_delegate.getNrNodeViews();
    }

    @Override
    public String getViewName(final int i) {
        return m_delegate.getViewName(i);
    }

    @Override
    public String getNodeViewName(final int i) {
        return m_delegate.getNodeViewName(i);
    }

    @Override
    public boolean hasInteractiveView() {
        return m_delegate.hasInteractiveView();
    }

    @Override
    public String getInteractiveViewName() {
        return m_delegate.getInteractiveViewName();
    }

    @Override
    public URL getIcon() {
        return m_delegate.getIcon();
    }

    @Override
    public NodeType getType() {
        return m_delegate.getType();
    }

    @Override
    public NodeID getID() {
        return m_delegate.getID();
    }

    @Override
    public String getName() {
        return m_delegate.getName();
    }

    @Override
    public String getNameWithID() {
        return m_delegate.getNameWithID();
    }

    @Override
    public String toString() {
        return m_delegate.toString();
    }

    @Override
    public String getDisplayLabel() {
        return m_delegate.getDisplayLabel();
    }

    @Override
    public String getCustomName() {
        return m_delegate.getCustomName();
    }

    @Override
    public INodeAnnotation getNodeAnnotation() {
        return NodeAnnotationWrapper.wrap(m_delegate.getNodeAnnotation());
    }

    @Override
    public String getCustomDescription() {
        return m_delegate.getCustomDescription();
    }

    @Override
    public void setCustomDescription(final String customDescription) {
        m_delegate.setCustomDescription(customDescription);
    }

    @Override
    public void setDeletable(final boolean value) {
        m_delegate.setDeletable(value);
    }

    @Override
    public boolean isDeletable() {
        return m_delegate.isDeletable();
    }

    @Override
    public boolean isDirty() {
        return m_delegate.isDirty();
    }

    @Override
    public void setDirty() {
        m_delegate.setDirty();
    }

    @Override
    public void changeNodeLocks(final boolean setLock, final NodeLock... locks) {
        m_delegate.changeNodeLocks(setLock, locks);
    }

    @Override
    public NodeLocks getNodeLocks() {
        return m_delegate.getNodeLocks();
    }

}
