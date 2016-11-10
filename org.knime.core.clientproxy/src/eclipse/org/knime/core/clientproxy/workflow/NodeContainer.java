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
 *   Nov 8, 2016 (hornm): created
 */
package org.knime.core.clientproxy.workflow;

import static org.knime.core.gateway.services.ServiceManager.service;

import java.net.URL;
import java.util.Optional;

import org.knime.core.api.node.NodeType;
import org.knime.core.api.node.workflow.INodeAnnotation;
import org.knime.core.api.node.workflow.INodeContainer;
import org.knime.core.api.node.workflow.INodeInPort;
import org.knime.core.api.node.workflow.INodeOutPort;
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
import org.knime.core.gateway.v0.workflow.entity.BoundsEnt;
import org.knime.core.gateway.v0.workflow.entity.JobManagerEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeMessageEnt;
import org.knime.core.gateway.v0.workflow.entity.WorkflowEntID;
import org.knime.core.gateway.v0.workflow.service.ExecutionService;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeMessage;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class NodeContainer implements INodeContainer {

    private final NodeEnt m_node;

    /**
     * @param node
     */
    public NodeContainer(final NodeEnt node) {
        m_node = node;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void progressChanged(final NodeProgressEvent pe) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWorkflowManager getParent() {
        WorkflowEntID parent = m_node.getParent();
        //TODO return same instance if the instance for this ID has already been created
        return new WorkflowManager(parent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<JobManagerUID> getJobManagerUID() {
        JobManagerEnt jobManager = m_node.getJobManager();
        JobManagerUID jobManagerUID = null;
        if(jobManager != null) {
            jobManagerUID = JobManagerUID.builder(jobManager.getID()).setName(jobManager.getName()).build();
        }
        return Optional.ofNullable(jobManagerUID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JobManagerUID findJobManagerUID() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addNodePropertyChangedListener(final NodePropertyChangedListener l) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeNodePropertyChangedListener(final NodePropertyChangedListener l) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearWaitingLoopList() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addProgressListener(final NodeProgressListener listener) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeNodeProgressListener(final NodeProgressListener listener) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addNodeMessageListener(final NodeMessageListener listener) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeNodeMessageListener(final NodeMessageListener listener) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeMessage getNodeMessage() {
        NodeMessageEnt nme = m_node.getNodeMessage();
        return new NodeMessage(NodeMessage.Type.valueOf(nme.getType()), nme.getMessage());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNodeMessage(final NodeMessage newMessage) {
        throw new UnsupportedOperationException();
//        service(NodeService.class).updateNode(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addUIInformationListener(final NodeUIInformationListener l) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeUIInformationListener(final NodeUIInformationListener l) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeUIInformation getUIInformation() {
        BoundsEnt bounds = m_node.getBounds();
        return NodeUIInformation.builder()
            .setNodeLocation(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight()).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUIInformation(final NodeUIInformation uiInformation) {
        throw new UnsupportedOperationException();
        //        service(NodeService.class).updateNode(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addNodeStateChangeListener(final NodeStateChangeListener listener) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeNodeStateChangeListener(final NodeStateChangeListener listener) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeContainerState getNodeContainerState() {
        String state = m_node.getNodeState();
        //TODO more rigid mapping here
        return new NodeContainerState() {

            @Override
            public boolean isWaitingToBeExecuted() {
                return state.equals("queued");
            }

            @Override
            public boolean isIdle() {
                return state.equals("idle");
            }

            @Override
            public boolean isHalted() {
                return state.equals("halted");
            }

            @Override
            public boolean isExecutionInProgress() {
                return state.equals("executing");
            }

            @Override
            public boolean isExecutingRemotely() {
                return state.equals("executing_remotely");
            }

            @Override
            public boolean isExecuted() {
                return state.equals("executed");
            }

            @Override
            public boolean isConfigured() {
                return state.equals("configured");
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDataAwareDialogPane() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAllInputDataAvailable() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canExecuteUpToHere() {
        return service(ExecutionService.class).canExecuteUpToHere(m_node.getID());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applySettingsFromDialog() throws InvalidSettingsException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean areDialogSettingsValid() {
        // TODO
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean areDialogAndNodeSettingsEqual() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrInPorts() {
        return m_node.getInPorts().size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public INodeInPort getInPort(final int index) {
        //TODO possibly return the same node in port instance for the same index
        return new NodeInPort(m_node.getInPorts().get(index));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public INodeOutPort getOutPort(final int index) {
      //TODO possibly return the same node in port instance for the same index
        return new NodeOutPort(m_node.getOutPorts().get(index));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrOutPorts() {
        return m_node.getOutPorts().size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrViews() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getViewName(final int i) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNodeViewName(final int i) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasInteractiveView() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasInteractiveWebView() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInteractiveViewName() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL getIcon() {
        // TODO mapping from the node factory to the respective icon
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeType getType() {
        return Enum.valueOf(NodeType.class, m_node.getNodeType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeID getID() {
        return NodeID.fromString(m_node.getNodeID());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return m_node.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNameWithID() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayLabel() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCustomName() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public INodeAnnotation getNodeAnnotation() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCustomDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCustomDescription(final String customDescription) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDeletable(final boolean value) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDeletable() {
        return m_node.isDeletable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirty() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDirty() {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeNodeLocks(final boolean setLock, final NodeLock... locks) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeLocks getNodeLocks() {
        // TODO Auto-generated method stub
        return null;
    }

}
