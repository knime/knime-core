/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   25.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;

import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Action to set the custom name and description of a node.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class SetNameAndDescriptionAction extends AbstractNodeAction {
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(SetNameAndDescriptionAction.class);

    /**
     * unique ID for this action.
     */
    public static final String ID = "knime.action.setnameanddescription";

    /**
     * @param editor The workflow editor
     */
    public SetNameAndDescriptionAction(final WorkflowEditor editor) {
        super(editor);
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
        return "Node name and description";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository
                .getImageDescriptor("icons/setNameDescription.PNG");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Set/view the user specified node name and a context dependent description.";
    }

    /**
     * @return <code>true</code>, if just one node part is selected.
     * 
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean calculateEnabled() {

        NodeContainerEditPart[] parts = getSelectedNodeParts();

        // only if just one node part is selected
        if (parts.length != 1) {
            return false;
        }

        return true;
    }

    /**
     * Opens a dialog and collects the user name and description. After the
     * dialog is closed the new name and description are set to the node
     * container if applicable.
     * 
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        // if more than one node part is selected
        if (nodeParts.length != 1) {
            LOGGER.debug("Execution denied as more than one node is "
                    + "selected. Not allowed in 'Set name and "
                    + "description' action.");
            return;
        }

        final NodeContainer container = nodeParts[0].getNodeContainer();

        LOGGER.debug("Opening 'Set name and description' dialog"
                + " for one node ...");

        // open name and description dialog
        try {
            Shell editorShell = PlatformUI.getWorkbench()
                .getDisplay().getActiveShell();

        Shell parent = new Shell(editorShell, SWT.BORDER
                | SWT.TITLE | SWT.NO_TRIM | SWT.APPLICATION_MODAL);

        String initialName = "Node " + container.getID().getIndex();
        if (container.getCustomName() != null) {
            initialName = container.getCustomName();
        }
        String initialDescr = "";
        if (container.getCustomDescription() != null) {
            initialDescr = container.getCustomDescription();
        }
        String dialogTitle = container.getNameWithID();
        if (container.getCustomName() != null) {
            dialogTitle += " - " + container.getCustomName();
        }
        NameDescriptionDialog dialog =
            new NameDescriptionDialog(parent, dialogTitle, 
                    initialName, initialDescr,
                    // (bugfix 1402)
                    container.getID());

        Point relativeLocation =  new Point(
                nodeParts[0].getFigure().getBounds().x,
                nodeParts[0].getFigure().getBounds().y);

        relativeLocation = editorShell.toDisplay(relativeLocation);
        parent.setLocation(relativeLocation);

        LOGGER.debug("custom description location: "
                + parent.getLocation().x
                + ", " + parent.getLocation().y);

        int result = dialog.open();

        // check if ok was pressed
        if (result == Window.OK) {
            // if the name or description have been changed
            // the editor must be set dirty
            String description = dialog.getDescription();
            String customName = dialog.getName();
            if (customName.trim().equals("")) {

                if (container.getCustomName() != null
                        || container.getCustomDescription() != null) {

                    // mark editor as dirty
                    getEditor().markDirty();
                }
                container.setCustomName(null);
                container.setCustomDescription(null);
            } else {
                // if name or description is different mark editor dirty
                if (!customName.equals(container.getCustomName())
                        || !description.equals(container
                                .getCustomDescription())) {
                    getEditor().markDirty();
                }

                container.setCustomName(customName);
                container.setCustomDescription(description);
            }
            }
        } catch (Throwable t) {
            LOGGER.error("trying to open description editor: ", t);
        }
        // else do nothing
    }
}
