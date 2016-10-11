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
 *   Oct 11, 2016 (hornm): created
 */
package org.knime.core.api.node.workflow;

/**
 * TODO
 *
 * @author Martin Horn, University of Konstanz
 */
public interface ISubNodeContainer extends ISingleNodeContainer{

//    /** Called from virtual input node when executed - in possibly executes nodes in the parent wfm and then
//     * fetches the data from it.
//     * @return the subnode data input (incl. mandatory flow var port object).
//     * @throws ExecutionException any exception thrown while waiting for upstream nodes to finish execution. */
//    PortObject[] fetchInputDataFromParent() throws ExecutionException;
//
//    /** Fetches input specs of subnode, including mandatory flow var port. Used by virtual sub node input during
//     * configuration.
//     * @return input specs from subnode (as available from connected outports).
//     */
//    PortObjectSpec[] fetchInputSpecFromParent();
//
//    /** @return the inNodeID */
//    NodeID getVirtualInNodeID();
//
//    /** @return the outNodeID */
//    NodeID getVirtualOutNodeID();
//
//    /**
//     * {@inheritDoc}
//     */
//    URL getIcon();
//
//    /**
//     * {@inheritDoc}
//     */
//    NodeType getType();
//
//    /**
//     * {@inheritDoc}
//     */
//    String getName();
//
//    /**
//     * @param name The new name
//     * @since 2.10
//     */
//    void setName(String name);

    /**
     * @return underlying workflow.
     */
    IWorkflowManager getWorkflowManager();

//    /**
//     * {@inheritDoc}
//     */
//    boolean hasDialog();
//
//    // TODO: enable view handling!
//    /**
//     * {@inheritDoc}
//     */
//    int getNrNodeViews();
//
//    /**
//     * {@inheritDoc}
//     */
//    String getNodeViewName(int i);
//
//    /**
//     * {@inheritDoc}
//     */
//    AbstractNodeView<NodeModel> getNodeView(int i);
//
//    /**
//     * {@inheritDoc}
//     */
//    boolean hasInteractiveView();
//
//    /**
//     * {@inheritDoc}
//     */
//    boolean hasInteractiveWebView();
//
//    /**
//     * {@inheritDoc}
//     */
//    String getInteractiveViewName();
//
//    /**
//     * {@inheritDoc}
//     */
//    <V extends AbstractNodeView<?> & InteractiveView<?, ? extends ViewContent, ? extends ViewContent>> V
//        getInteractiveView();
//
//    /**
//     * {@inheritDoc}
//     */
//    NodeContainerExecutionStatus performExecuteNode(PortObject[] rawInObjects);
//
//    /**
//     * {@inheritDoc}
//     * @since 2.12
//     */
//    SubnodeContainerExecutionResult createExecutionResult(ExecutionMonitor exec) throws CanceledExecutionException;
//
//    /** {@inheritDoc} */
//    void loadExecutionResult(NodeContainerExecutionResult result, ExecutionMonitor exec, LoadResult loadResult);
//
//    /**
//     * {@inheritDoc}
//     */
//    int getNrInPorts();
//
//    /**
//     * {@inheritDoc}
//     */
//    NodeInPort getInPort(int index);
//
//    /**
//     * @param portTypes Types of the new ports
//     * @since 2.10
//     */
//    void setInPorts(PortType[] portTypes);
//
//    /**
//     * {@inheritDoc}
//     */
//    int getNrOutPorts();
//
//    /**
//     * {@inheritDoc}
//     */
//    NodeOutPort getOutPort(int index);
//
//    /**
//     * @param portTypes Types of the new ports
//     * @since 2.10
//     */
//    void setOutPorts(PortType[] portTypes);
//
//    /**
//     * {@inheritDoc}
//     */
//    FlowObjectStack getFlowObjectStack();
//
//    /**
//     * @return the layoutInfo
//     * @since 2.10
//     * @deprecated use {@link #getLayoutJSONString()} instead
//     */
//    @Deprecated
//    Map<Integer, WizardNodeLayoutInfo> getLayoutInfo();
//
//    /**
//     * @param layoutInfo the layoutInfo to set
//     * @since 2.10
//     * @deprecated use {@link #setLayoutJSONString(String)} instead
//     */
//    @Deprecated
//    void setLayoutInfo(Map<Integer, WizardNodeLayoutInfo> layoutInfo);
//
//    /**
//     * @return the layoutJSONString
//     * @since 3.1
//     */
//    String getLayoutJSONString();
//
//    /**
//     * @param layoutJSONString the layoutJSONString to set
//     * @since 3.1
//     */
//    void setLayoutJSONString(String layoutJSONString);
//
//    /** {@inheritDoc} */
//    WorkflowLock lock();
//
//    /** {@inheritDoc} */
//    ReentrantLock getReentrantLockInstance();
//
//    /** {@inheritDoc} */
//    boolean isLockedByCurrentThread();
//
//    /** {@inheritDoc} */
//    boolean canConfigureNodes();
//
//    /** {@inheritDoc} */
//    boolean canResetContainedNodes();
//
//    /** {@inheritDoc} */
//    boolean isWriteProtected();
//
//    /** {@inheritDoc} */
//    void pushWorkflowVariablesOnStack(FlowObjectStack sos);
//
//    /** {@inheritDoc} */
//    String getCipherFileName(String fileName);
//
//    /** {@inheritDoc} */
//    WorkflowCipher getWorkflowCipher();
//
//    /** {@inheritDoc} */
//    OutputStream cipherOutput(OutputStream out) throws IOException;
//
//    /**
//     * {@inheritDoc}
//     * @since 2.10
//     */
//    WorkflowManager getProjectWFM();
//
//    /** @return the templateInformation
//     * @since 2.10*/
//    MetaNodeTemplateInformation getTemplateInformation();
//
//    /** {@inheritDoc} */
//    void setTemplateInformation(MetaNodeTemplateInformation tI);
//
//    /** {@inheritDoc} */
//    MetaNodeTemplateInformation saveAsTemplate(File directory, ExecutionMonitor exec)
//        throws IOException, CanceledExecutionException, LockFailedException;
//
//    /**
//     * {@inheritDoc}
//     * @since 2.10
//     */
//    void notifyTemplateConnectionChangedListener();
//
//    /**
//     * {@inheritDoc}
//     */
//    void updateMetaNodeLinkInternalRecursively(ExecutionMonitor exec, WorkflowLoadHelper loadHelper,
//        Map<URI, NodeContainerTemplate> visitedTemplateMap, NodeContainerTemplateLinkUpdateResult loadRes)
//        throws Exception;
//
//    /** {@inheritDoc} */
//    Map<NodeID, NodeContainerTemplate> fillLinkedTemplateNodesList(Map<NodeID, NodeContainerTemplate> mapToFill,
//        boolean recurse, boolean stopRecursionAtLinkedMetaNodes);
//
//    /** {@inheritDoc} */
//    boolean containsExecutedNode();
//
//    /** {@inheritDoc} */
//    Collection<NodeContainer> getNodeContainers();

}