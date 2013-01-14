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
 * -------------------------------------------------------------------
 *
 * History
 *   13.04.2011 (Bernd Wiswedel): created
 */
package org.knime.workbench.editor2.commands;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.UnsupportedWorkflowVersionException;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.workbench.editor2.LoadMetaNodeTemplateRunnable;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;

/**
 * GEF command for adding a MetaNode from a file location to the workflow.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich
 */
public class CreateMetaNodeTemplateCommand extends AbstractKNIMECommand {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(CreateMetaNodeTemplateCommand.class);

    private final AbstractExplorerFileStore m_templateKNIMEFolder;

    private final Point m_location;

    private final boolean m_snapToGrid;

    private NodeContainer m_container;


    /**
     * Creates a new command.
     *
     * @param manager The workflow manager that should host the new node
     * @param templateFolder the directory underlying the template
     * @param location Initial visual location in the
     * @param snapToGrid if node should be placed on closest grid location
     */
    public CreateMetaNodeTemplateCommand(final WorkflowManager manager,
            final AbstractExplorerFileStore templateFolder,
            final Point location, final boolean snapToGrid) {
        super(manager);
        m_templateKNIMEFolder = templateFolder;
        m_location = location;
        m_snapToGrid = snapToGrid;
    }

    /** We can execute, if all components were 'non-null' in the constructor.
     * {@inheritDoc} */
    @Override
    public boolean canExecute() {
        if (!super.canExecute()) {
            return false;
        }
        if (m_location == null || m_templateKNIMEFolder == null) {
            return false;
        }
        return AbstractExplorerFileStore.isWorkflowTemplate(
                m_templateKNIMEFolder);
    }

    /** {@inheritDoc} */
    @Override
    public void execute() {
        // Add node to workflow and get the container
        LoadMetaNodeTemplateRunnable loadRunnable = null;
        try {
            IWorkbench wb = PlatformUI.getWorkbench();
            IProgressService ps = wb.getProgressService();
            // this one sets the workflow manager in the editor
            loadRunnable = new LoadMetaNodeTemplateRunnable(getHostWFM(),
                    m_templateKNIMEFolder);
            ps.busyCursorWhile(loadRunnable);
            WorkflowLoadResult result = loadRunnable.getWorkflowLoadResult();
            m_container = result.getWorkflowManager();
            if (m_container == null) {
                throw new RuntimeException("No template returned by load "
                        + "routine, see log for details");
            }
            // create extra info and set it
            NodeUIInformation info = new NodeUIInformation(
                    m_location.x, m_location.y, -1, -1, true);
            info.setSnapToGrid(m_snapToGrid);
            info.setIsDropLocation(true);
            m_container.setUIInformation(info);
        } catch (Throwable ex) {
            while ((ex.getCause() != null) && (ex.getCause() != ex)) {
                ex = ex.getCause();
            }

            String error = "The selected node could not be created";
            if (ex instanceof FileNotFoundException) {
                error += " because a file could not be found: " + ex.getMessage();
                MessageDialog.openError(Display.getDefault().
                        getActiveShell(), "Node cannot be created.", error);
            } else if (ex instanceof IOException) {
                error += " because of an I/O error: " + ex.getMessage();
                MessageDialog.openError(Display.getDefault().
                        getActiveShell(), "Node cannot be created.", error);
            } else if (ex instanceof InvalidSettingsException) {
                error += " because the metanode contains invalid settings: " + ex.getMessage();
                MessageDialog.openError(Display.getDefault().
                        getActiveShell(), "Node cannot be created.", error);
            } else if (ex instanceof UnsupportedWorkflowVersionException) {
                error += " because the metanode version is incompatible: " + ex.getMessage();
                MessageDialog.openError(Display.getDefault().
                        getActiveShell(), "Node cannot be created.", error);
            } else if ((ex instanceof CanceledExecutionException) || (ex instanceof InterruptedException)) {
                LOGGER.info("Metanode loading was canceled by the user", ex);
            } else {
                error += ": " + ex.getMessage();
                MessageDialog.openError(Display.getDefault().
                        getActiveShell(), "Node cannot be created.", error);
            }
        }
    }



    /** {@inheritDoc} */
    @Override
    public boolean canUndo() {
        return m_container != null
            && getHostWFM().canRemoveNode(m_container.getID());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        LOGGER.debug("Undo: Removing node #" + m_container.getID());
        if (canUndo()) {
            getHostWFM().removeNode(m_container.getID());
        } else {
            MessageDialog.openInformation(Display.getDefault().getActiveShell(),
                    "Operation no allowed", "The node "
                    + m_container.getNameWithID()
                    + " can currently not be removed");
        }
    }

}
