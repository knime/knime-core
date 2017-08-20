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

import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArraySet;

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
import org.knime.core.clientproxy.util.ClientProxyUtil;
import org.knime.core.clientproxy.util.ObjectCache;
import org.knime.core.gateway.services.ServerServiceConfig;
import org.knime.core.gateway.v0.workflow.entity.BoundsEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeMessageEnt;
import org.knime.core.gateway.v0.workflow.service.NodeService;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.config.base.JSONConfig;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.util.JobManagerUtil;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public abstract class ClientProxyNodeContainer implements INodeContainer {

    private final NodeEnt m_node;

    protected ObjectCache m_objCache;

    /*--------- listener administration------------*/

    private final CopyOnWriteArraySet<NodeStateChangeListener> m_stateChangeListeners =
        new CopyOnWriteArraySet<NodeStateChangeListener>();

    private final CopyOnWriteArraySet<NodeMessageListener> m_messageListeners =
        new CopyOnWriteArraySet<NodeMessageListener>();

    private final CopyOnWriteArraySet<NodeProgressListener> m_progressListeners =
        new CopyOnWriteArraySet<NodeProgressListener>();

    private final CopyOnWriteArraySet<NodeUIInformationListener> m_uiListeners =
        new CopyOnWriteArraySet<NodeUIInformationListener>();

    private final CopyOnWriteArraySet<NodePropertyChangedListener> m_nodePropertyChangedListeners =
        new CopyOnWriteArraySet<NodePropertyChangedListener>();

    protected ServerServiceConfig m_serviceConfig;


    /**
     * If the underlying entity is a node.
     *
     * @param node
     * @param objCache
     * @param serviceConfig
     */
    public ClientProxyNodeContainer(final NodeEnt node, final ObjectCache objCache, final ServerServiceConfig serviceConfig) {
        m_node = node;
        m_objCache = objCache;
        m_serviceConfig = serviceConfig;
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
        return m_node.getParentNodeID().map(s -> {
            //get parent wf
            String parentNodeID;
            if (NodeID.fromString(s).getPrefix() == NodeID.ROOTID) {
                //parent is the highest level workflow
                //the node id has then no meaning here and need to be empty
                parentNodeID = null;
            } else {
                parentNodeID = s;
            }
            return ClientProxyUtil.getWorkflowManager(m_node.getRootWorkflowID(), Optional.ofNullable(parentNodeID),
                m_objCache, m_serviceConfig);
        }).orElse(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<JobManagerUID> getJobManagerUID() {
        if (m_node.getJobManager().isPresent()) {
            return m_node.getJobManager()
                .map(jm -> JobManagerUID.builder(jm.getJobManagerID()).setName(jm.getName()).build());
        } else if (getParent() == null) {
            //if it's the root workflow and no job manager set, return the default one
            return Optional
                .of(JobManagerUtil.getJobManagerUID(NodeExecutionJobManagerPool.getDefaultJobManagerFactory()));
        } else {
            //if there is no job manager set nor it's the root workflow
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JobManagerUID findJobManagerUID() {
        //optionally derive the job manager uid from the parent
        return m_node.getJobManager()
            .map(jm -> JobManagerUID.builder(jm.getJobManagerID()).setName(jm.getName()).build())
            .orElseGet(() -> {
                if(getParent() == null) {
                    //if it's the root workflow, there must be a job manager set
                    return getJobManagerUID().get();
                } else {
                    return getParent().findJobManagerUID();
                }

            });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addNodePropertyChangedListener(final NodePropertyChangedListener l) {
        return m_nodePropertyChangedListeners.add(l);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeNodePropertyChangedListener(final NodePropertyChangedListener l) {
        return m_nodePropertyChangedListeners.remove(l);
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
        if (listener == null) {
            throw new NullPointerException("Node progress listener must not be null");
        }
        return m_progressListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeNodeProgressListener(final NodeProgressListener listener) {
        return m_progressListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addNodeMessageListener(final NodeMessageListener listener) {
        if (listener == null) {
            throw new NullPointerException("Node message listner must not be null!");
        }
        return m_messageListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeNodeMessageListener(final NodeMessageListener listener) {
        return m_messageListeners.remove(listener);
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
        if (l == null) {
            throw new NullPointerException("NodeUIInformationListener must not be null!");
        }
        m_uiListeners.add(l);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeUIInformationListener(final NodeUIInformationListener l) {
        m_uiListeners.remove(l);

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
        //        service(NodeService.class).updateNode(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addNodeStateChangeListener(final NodeStateChangeListener listener) {
        if (listener == null) {
            throw new NullPointerException("Node state change listener must not be null!");
        }
        return m_stateChangeListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeNodeStateChangeListener(final NodeStateChangeListener listener) {
        return m_stateChangeListeners.remove(listener);
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
                return state.equals("QUEUED");
            }

            @Override
            public boolean isIdle() {
                return state.equals("IDLE");
            }

            @Override
            public boolean isHalted() {
                return state.equals("HALTED");
            }

            @Override
            public boolean isExecutionInProgress() {
                return state.equals("EXECUTING");
            }

            @Override
            public boolean isExecutingRemotely() {
                return state.equals("EXECUTING_REMOTELY");
            }

            @Override
            public boolean isExecuted() {
                return state.equals("EXECUTED");
            }

            @Override
            public boolean isConfigured() {
                return state.equals("CONFIGURED");
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return state;
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public ConfigBaseRO getNodeSettings() {
        String json = service(NodeService.class, m_serviceConfig).getNodeSettingsJSON(m_node.getRootWorkflowID(), m_node.getNodeID());
        try {
            return JSONConfig.readJSON(new NodeSettings("settings"), new StringReader(json));
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read NodeSettings from XML String", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDataAwareDialogPane() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAllInputDataAvailable() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canExecuteUpToHere() {
        throw new UnsupportedOperationException();
        //        return service(ExecutionService.class).getCanExecuteUpToHere(null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applySettingsFromDialog() throws InvalidSettingsException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean areDialogSettingsValid() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return m_node.getHasDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean areDialogAndNodeSettingsEqual() {
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
        return ClientProxyUtil.getNodeInPort(m_node.getInPorts().get(index), m_objCache);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public INodeOutPort getOutPort(final int index) {
        return ClientProxyUtil.getNodeOutPort(m_node.getOutPorts().get(index), m_objCache);
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
        //TODO
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getViewName(final int i) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNodeViewName(final int i) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasInteractiveView() {
        //TODO
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInteractiveViewName() {
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
        return getName() + " " + getID().toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayLabel() {
        //copied from NodeContainer
        String label = getID().toString() + " - " + getName();
        // if this node has an annotation add the first line to the label - TODO
        //        String customLabel = getDisplayCustomLine();
        //        if (!customLabel.isEmpty()) {
        //            label += " (" + customLabel + ")";
        //        }
        return label;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCustomName() {
        //TODO
        return "TODO custom name";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public INodeAnnotation getNodeAnnotation() {
        //TODO return the same node annotation instance in multiple calls
        return new ClientProxyNodeAnnotation(m_node.getNodeAnnotation(), this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCustomDescription() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCustomDescription(final String customDescription) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDeletable(final boolean value) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDeletable() {
        return m_node.getIsDeletable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirty() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDirty() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeNodeLocks(final boolean setLock, final NodeLock... locks) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeLocks getNodeLocks() {
        //TODO
        return new NodeLocks(false, false, false);
    }

}
