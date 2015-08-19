/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainerState;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.ImageRepository;

/**
 * Action to execute all selected nodes and open their interactive view.
 *
 * @author Christian Albrecht, KNIME.com AG, Zurich
 */
public class ExecuteAndOpenInteractiveViewAction extends Action {
    private final NodeContainer m_nodeContainer;

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(ExecuteAndOpenInteractiveViewAction.class);

    /**
     * unique ID for this action.
     */
    public static final String ID = "knime.action.executeandopeninteractiveview";

    /**
     * @param container The node container
     */
    public ExecuteAndOpenInteractiveViewAction(final NodeContainer container) {
        m_nodeContainer = container;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Execute and Open Interactive View\t";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/executeAndView.GIF");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getDisabledImageDescriptor() {
        return ImageRepository
                .getImageDescriptor("icons/executeAndView_diabled.PNG");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Execute the selected nodes and opens the interactive view.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        WorkflowManager wm = m_nodeContainer.getParent();
        if (wm.canExecuteNode(m_nodeContainer.getID())
                && (m_nodeContainer.hasInteractiveView() || m_nodeContainer.hasInteractiveWebView())) {
                return true;
        }
        return false;
    }

    private void executeAndOpen() {
        if (m_nodeContainer.hasInteractiveView() || m_nodeContainer.hasInteractiveWebView()) {
            // another listener must be registered at the workflow manager to
            // receive also those events from nodes that have just been queued
            m_nodeContainer.addNodeStateChangeListener(new NodeStateChangeListener() {
                @Override
                public void stateChanged(final NodeStateEvent state) {
                    NodeContainerState ncState = m_nodeContainer.getNodeContainerState();
                    // check if the node has finished (either executed or
                    // removed from the queue)
                    if ((state.getSource() == m_nodeContainer.getID()) && ncState.isExecuted()) {
                        // if the node was successfully executed
                        // start the view
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                // run open view action
                                IAction viewAction = new OpenInteractiveViewAction(m_nodeContainer);
                                viewAction.run();
                            }
                        });
                    }
                    if (!ncState.isExecutionInProgress()) {
                        // in those cases remove the listener
                        m_nodeContainer.removeNodeStateChangeListener(this);
                    }
                }

            });
        }
        m_nodeContainer.getParent().executeUpToHere(m_nodeContainer.getID());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        LOGGER.debug("Creating 'Execute and Open Interactive Views' job for "
                + m_nodeContainer.getNameWithID());
        executeAndOpen();
    }
}
