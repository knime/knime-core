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
 * Created on Oct 1, 2013 by Berthold
 */
package org.knime.core.node.workflow;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.filestore.internal.FileStoreHandlerRepository;
import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeConfigureHelper;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.interactive.InteractiveView;
import org.knime.core.node.interactive.ViewContent;
import org.knime.core.node.interactive.WebViewTemplate;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.execresult.NodeContainerExecutionResult;
import org.knime.core.node.workflow.execresult.NodeContainerExecutionStatus;
import org.w3c.dom.Element;

/** Implementation of a {@link NodeContainer} holding a set of nodes via a {@link WorkflowManager}
 * and acting like a {@link NativeNodeContainer} to the outside.
 *
 * @author M. Berthold & B. Wiswedel
 * @since 2.9
 */
public class SubNodeContainer extends SingleNodeContainer {

    /**
     * @param parent
     * @param id
     */
    public SubNodeContainer(final WorkflowManager parent, final NodeID id) {
        super(parent, id);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param parent
     * @param id
     * @param persistor
     */
    public SubNodeContainer(final WorkflowManager parent, final NodeID id, final NodeContainerMetaPersistor persistor) {
        super(parent, id, persistor);
        // TODO Auto-generated constructor stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void setInHiLiteHandler(final int index, final HiLiteHandler hdl) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExecutionContext createExecutionContext() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean performConfigure(final PortObjectSpec[] inSpecs, final NodeConfigureHelper nch) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void performLoadModelSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    void performReset() {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    void cleanOutPorts(final boolean isLoopRestart) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeContainerExecutionStatus performExecuteNode(final PortObject[] inObjects) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    WorkflowCopyContent performLoadContent(final SingleNodeContainerPersistor nodePersistor,
        final Map<Integer, BufferedDataTable> tblRep, final FlowObjectStack inStack, final ExecutionMonitor exec, final LoadResult loadResult,
        final boolean preserveNodeMessage) throws CanceledExecutionException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void performSaveModelSettingsTo(final NodeSettings modelSettings) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean performAreModelSettingsValid(final NodeSettingsRO modelSettings) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void performSetCredentialsProvider(final CredentialsProvider cp) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    CredentialsProvider getCredentialsProvider() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void setFlowObjectStack(final FlowObjectStack st, final FlowObjectStack outgoingStack) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FlowObjectStack getFlowObjectStack() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FlowObjectStack getOutgoingFlowObjectStack() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isModelCompatibleTo(final Class<?> nodeModelClass) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInactive() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInactiveBranchConsumer() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element getXMLDescription() {
        // TODO Auto-generated method stub
        return null;
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
    NodeDialogPane getDialogPaneWithSettings(final PortObjectSpec[] inSpecs, final PortObject[] inData)
        throws NotConfigurableException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    NodeDialogPane getDialogPane() {
        // TODO Auto-generated method stub
        return null;
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
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeInPort getInPort(final int index) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeOutPort getOutPort(final int index) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrOutPorts() {
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
    public String getNodeViewName(final int i) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractNodeView<NodeModel> getNodeView(final int i) {
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
    public <V extends AbstractNodeView<?> & InteractiveView<?, ? extends ViewContent>> V getInteractiveView() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WebViewTemplate getInteractiveWebViewTemplate() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL getIcon() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeType getType() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeContainerPersistor getCopyPersistor(final HashMap<Integer, ContainerTable> tableRep,
        final FileStoreHandlerRepository fileStoreHandlerRepository, final boolean preserveDeletableFlags,
        final boolean isUndoableDeleteCommand) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeContainerExecutionResult createExecutionResult(final ExecutionMonitor exec) throws CanceledExecutionException {
        // TODO Auto-generated method stub
        return null;
    }

    /* ---------- Leftovers --------------- */

    public WorkflowManager getWorkflowManager() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortType getOutputType(final int portIndex) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObjectSpec getOutputSpec(final int portIndex) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObject getOutputObject(final int portIndex) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOutputObjectSummary(final int portIndex) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HiLiteHandler getOutputHiLiteHandler(final int portIndex) {
        // TODO Auto-generated method stub
        return null;
    }
}
