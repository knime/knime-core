/*
 * ------------------------------------------------------------------------
 *
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
 *   Oct 13, 2016 (hornm): created
 */
package org.knime.core.ui.wrapper;

import java.io.InputStream;
import java.net.URL;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeAnnotation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainer.NodeLock;
import org.knime.core.node.workflow.NodeContainer.NodeLocks;
import org.knime.core.node.workflow.NodeContainerState;
import org.knime.core.node.workflow.NodeExecutionJobManager;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.NodeMessageListener;
import org.knime.core.node.workflow.NodeProgressEvent;
import org.knime.core.node.workflow.NodeProgressListener;
import org.knime.core.node.workflow.NodePropertyChangedListener;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.NodeUIInformationListener;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.InteractiveWebViewsResultUI;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.NodeInPortUI;
import org.knime.core.ui.node.workflow.NodeOutPortUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;

/**
 * UI-interface implementation that wraps a {@link NodeContainer}.
 *
 * @author Martin Horn, University of Konstanz
 */
public abstract class NodeContainerWrapper<W extends NodeContainer> extends AbstractWrapper<W>
    implements NodeContainerUI {


    /**
     * @param delegate the implementation to delegate to
     *
     */
    protected NodeContainerWrapper(final W delegate) {
        super(delegate);
    }

    /**
     * Wraps the object. It also checks for sub-types of the node container and uses the more specific wrappers.
     *
     * @param nc the object to be wrapped
     * @return a new wrapper or a already existing one or <code>null</code> if nc is null
     */
    @SuppressWarnings("rawtypes")
    public static final NodeContainerWrapper wrap(final NodeContainer nc) {
        if (nc == null) {
            return null;
        }
        //order of checking important
        if (nc instanceof SubNodeContainer) {
            return SubNodeContainerWrapper.wrap((SubNodeContainer)nc);
        } else if (nc instanceof NativeNodeContainer) {
            return NativeNodeContainerWrapper.wrap((NativeNodeContainer)nc);
        } else if (nc instanceof WorkflowManager) {
            return WorkflowManagerWrapper.wrap((WorkflowManager)nc);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * {@inheritDoc}
     * @since 3.6
     */
    @Override
    public NodeDialogPane getDialogPaneWithSettings() throws NotConfigurableException {
        return unwrap().getDialogPaneWithSettings();
    }

    @Override
    public void progressChanged(final NodeProgressEvent pe) {
        unwrap().progressChanged(pe);
    }

    @Override
    public WorkflowManagerUI getParent() {
        return WorkflowManagerWrapper.wrap(unwrap().getParent());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeExecutionJobManager getJobManager() {
        return unwrap().getJobManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeExecutionJobManager findJobManager() {
        return unwrap().findJobManager();
    }

    @Override
    public boolean addNodePropertyChangedListener(final NodePropertyChangedListener l) {
        return unwrap().addNodePropertyChangedListener(l);
    }

    @Override
    public boolean removeNodePropertyChangedListener(final NodePropertyChangedListener l) {
        return unwrap().removeNodePropertyChangedListener(l);
    }

    @Override
    public void clearWaitingLoopList() {
        unwrap().clearWaitingLoopList();
    }

    @Override
    public boolean addProgressListener(final NodeProgressListener listener) {
        return unwrap().addProgressListener(listener);
    }

    @Override
    public boolean removeNodeProgressListener(final NodeProgressListener listener) {
        return unwrap().removeNodeProgressListener(listener);
    }

    @Override
    public boolean addNodeMessageListener(final NodeMessageListener listener) {
        return unwrap().addNodeMessageListener(listener);
    }

    @Override
    public boolean removeNodeMessageListener(final NodeMessageListener listener) {
        return unwrap().removeNodeMessageListener(listener);
    }

    @Override
    public NodeMessage getNodeMessage() {
        return unwrap().getNodeMessage();
    }

    @Override
    public void setNodeMessage(final NodeMessage newMessage) {
        unwrap().setNodeMessage(newMessage);
    }

    @Override
    public void addUIInformationListener(final NodeUIInformationListener l) {
        unwrap().addUIInformationListener(l);
    }

    @Override
    public void removeUIInformationListener(final NodeUIInformationListener l) {
        unwrap().removeUIInformationListener(l);
    }

    @Override
    public NodeUIInformation getUIInformation() {
        return unwrap().getUIInformation();
    }

    @Override
    public void setUIInformation(final NodeUIInformation uiInformation) {
        unwrap().setUIInformation(uiInformation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUIInformationForCorrection(final NodeUIInformation uiInfo) {
        unwrap().setUIInformation(uiInfo);
    }

    @Override
    public boolean addNodeStateChangeListener(final NodeStateChangeListener listener) {
        return unwrap().addNodeStateChangeListener(listener);
    }

    @Override
    public boolean removeNodeStateChangeListener(final NodeStateChangeListener listener) {
        return unwrap().removeNodeStateChangeListener(listener);
    }

    @Override
    public NodeContainerState getNodeContainerState() {
        return unwrap().getNodeContainerState();
    }

    @Override
    public boolean hasDataAwareDialogPane() {
        return unwrap().hasDataAwareDialogPane();
    }

    @Override
    public boolean isAllInputDataAvailable() {
        return unwrap().isAllInputDataAvailable();
    }

    @Override
    public boolean canExecuteUpToHere() {
        return unwrap().canExecuteUpToHere();
    }

    @Override
    public void applySettingsFromDialog() throws InvalidSettingsException {
        unwrap().applySettingsFromDialog();
    }

    /** {@inheritDoc} */
    @Override
    public ConfigBaseRO getNodeSettings() {
        return unwrap().getNodeSettings();
    }

    @Override
    public boolean areDialogSettingsValid() {
        return unwrap().areDialogSettingsValid();
    }

    @Override
    public boolean hasDialog() {
        return unwrap().hasDialog();
    }

    @Override
    public boolean areDialogAndNodeSettingsEqual() {
        return unwrap().areDialogAndNodeSettingsEqual();
    }

    @Override
    public int getNrInPorts() {
        return unwrap().getNrInPorts();
    }

    @Override
    public NodeInPortUI getInPort(final int index) {
        return new NodeInPortWrapper(unwrap().getInPort(index));
    }

    @Override
    public NodeOutPortUI getOutPort(final int index) {
        return new NodeOutPortWrapper(unwrap().getOutPort(index));
    }

    @Override
    public int getNrOutPorts() {
        return unwrap().getNrOutPorts();
    }

    @Override
    public int getNrViews() {
        return unwrap().getNrViews();
    }

    @Override
    public int getNrNodeViews() {
        return unwrap().getNrNodeViews();
    }

    @Override
    public String getViewName(final int i) {
        return unwrap().getViewName(i);
    }

    @Override
    public String getNodeViewName(final int i) {
        return unwrap().getNodeViewName(i);
    }

    @Override
    public boolean hasInteractiveView() {
        return unwrap().hasInteractiveView();
    }

    @Override
    public InteractiveWebViewsResultUI getInteractiveWebViews() {
        return InteractiveWebViewsResultWrapper.wrap(unwrap().getInteractiveWebViews());
    }

    @Override
    public String getInteractiveViewName() {
        return unwrap().getInteractiveViewName();
    }

    @Override
    public URL getIcon() {
        return unwrap().getIcon();
    }

    @Override
    public InputStream getIconAsStream() {
        return unwrap().getIconAsStream();
    }

    @Override
    public NodeType getType() {
        return unwrap().getType();
    }

    @Override
    public NodeID getID() {
        return unwrap().getID();
    }

    @Override
    public String getName() {
        return unwrap().getName();
    }

    @Override
    public String getNameWithID() {
        return unwrap().getNameWithID();
    }

    @Override
    public String getDisplayLabel() {
        return unwrap().getDisplayLabel();
    }

    @Override
    public String getCustomName() {
        return unwrap().getCustomName();
    }

    @Override
    public NodeAnnotation getNodeAnnotation() {
        return unwrap().getNodeAnnotation();
    }

    @Override
    public String getCustomDescription() {
        return unwrap().getCustomDescription();
    }

    @Override
    public void setCustomDescription(final String customDescription) {
        unwrap().setCustomDescription(customDescription);
    }

    @Override
    public void setDeletable(final boolean value) {
        unwrap().setDeletable(value);
    }

    @Override
    public boolean isDeletable() {
        return unwrap().isDeletable();
    }

    @Override
    public boolean isDirty() {
        return unwrap().isDirty();
    }

    @Override
    public void setDirty() {
        unwrap().setDirty();
    }

    @Override
    public void changeNodeLocks(final boolean setLock, final NodeLock... locks) {
        unwrap().changeNodeLocks(setLock, locks);
    }

    @Override
    public NodeLocks getNodeLocks() {
        return unwrap().getNodeLocks();
    }

    @Override
    public String doRpc(final String remoteProcedureCall) {
        throw new UnsupportedOperationException();
    }

}
