/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.workbench.editor2.actions;

import java.util.List;

import org.knime.core.node.NodeLogger;
import org.knime.workbench.editor2.EditorModeParticipant;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.WorkflowEditorMode;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * This is the action which toggles between Annotation Edit Mode and Node Edit Mode.
 *
 * @author loki der quaeler
 */
public class ToggleEditorModeAction extends AbstractNodeAction {
    /** The ID returned by getId() and used in the plugin.xml **/
    public static final String ID = "knime.action.editor.toggleEditorMode";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ToggleEditorModeAction.class);


    private final boolean m_clearSelectionOnToggle;

    /**
     * This calls <code>this(editor, true)</code>
     *
     * @param editor The workflow editor
     */
    public ToggleEditorModeAction(final WorkflowEditor editor) {
        this(editor, true);
    }

    /**
     * @param editor The workflow editor
     * @param deselectAllOnToggle is set to true, the elements on the canvas will be deselected as part of the action
     */
    public ToggleEditorModeAction(final WorkflowEditor editor, final boolean deselectAllOnToggle) {
        super(editor);

        m_clearSelectionOnToggle = deselectAllOnToggle;
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
        return "Toggle Editor Mode...";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Toggles the edit mode between Annotation Edit and Node Edit";
    }

    /**
     * @return <code>true</code> - we are always enabled
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean internalCalculateEnabled() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runInSWT() {
        final WorkflowEditor we = (WorkflowEditor)getWorkbenchPart();

        if (we != null) {
            final WorkflowEditorMode currentMode = we.getEditorMode();
            final WorkflowEditorMode newMode = currentMode.equals(WorkflowEditorMode.NODE_EDIT)
                ? WorkflowEditorMode.ANNOTATION_EDIT : WorkflowEditorMode.NODE_EDIT;
            final List<?> canvasObjects = getAllObjects();

            we.setEditorMode(newMode);

            if (m_clearSelectionOnToggle) {
                getSelectionManager().deselectAll();
            }

            if (canvasObjects != null) {
                canvasObjects.stream().forEach((o) -> {
                    if (o instanceof EditorModeParticipant) {
                        ((EditorModeParticipant)o).workflowEditorModeWasSet(newMode);
                    }
                });
            }
        } else {
            LOGGER.error("Somehow we failed to get the workflow editor instance.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        throw new IllegalStateException("This method should not be called.");
    }
}
