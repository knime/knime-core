/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * -------------------------------------------------------------------
 *
 * History
 *   16.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.actions;

import javax.swing.SwingUtilities;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 *
 *
 * @author Fabian Dill, University of Konstanz
 */
public class DefaultOpenViewAction extends AbstractNodeAction {

    private static final NodeLogger LOGGER = NodeLogger
    .getLogger(DefaultOpenViewAction.class);

    /** Unique id for this action. */
    public static final String ID = "knime.action.defaultOpen";

    /**
     *
     * @param editor current editor
     */
    public DefaultOpenViewAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     *
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
        return "Open first view";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/openView.gif");
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getDisabledImageDescriptor() {
        return ImageRepository.getImageDescriptor(
                "icons/openView_disabled.gif");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Opens the first view of the selected node(s)";
    }

    /**
     * @return true if at least one selected node is executing or queued
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean calculateEnabled() {

        NodeContainerEditPart[] parts =
            getSelectedParts(NodeContainerEditPart.class);

        // enable if we have at least one executing or queued node in our
        // selection
        boolean atLeastOneNodeIsExecuted = false;
        for (int i = 0; i < parts.length; i++) {
            NodeContainer nc = parts[i].getNodeContainer();
            atLeastOneNodeIsExecuted |= nc.getState().equals(
                    NodeContainer.State.EXECUTED)
            && nc.getNrViews() > 0;
        }
        return atLeastOneNodeIsExecuted;

    }

    /**
     * This opens the first view of all the selected nodes.
     *
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        LOGGER.debug("Creating open default view job for " + nodeParts.length
                + " node(s)...");
        NodeContainerEditPart[] parts =
            getSelectedParts(NodeContainerEditPart.class);
        for (NodeContainerEditPart p : parts) {
            final NodeContainer cont = p.getNodeContainer();
            if (cont.getState().equals(NodeContainer.State.EXECUTED)
                    && cont.getNrViews() > 0) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final String title = cont.getViewName(0) + " - "
                                + cont.getDisplayLabel();
                            Node.invokeOpenView(cont.getView(0), title);
                        } catch (Throwable t) {
                            MessageBox mb = new MessageBox(
                                    Display.getDefault().getActiveShell(),
                                    SWT.ICON_ERROR | SWT.OK);
                            mb.setText("View cannot be opened");
                            mb.setMessage("The view cannot be opened for the "
                                    + "following reason:\n" + t.getMessage());
                            mb.open();
                            LOGGER.error("The view for node '"
                                    + cont.getNameWithID() + "' has thrown a '"
                                    + t.getClass().getSimpleName()
                                    + "'. That is most likely an "
                                    + "implementation error.", t);
                        }
                    }
                });
            }
        }


        try {
            // Give focus to the editor again. Otherwise the actions (selection)
            // is not updated correctly.
            getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());
        } catch (Exception e) {
            // ignore
        }
    }

}
