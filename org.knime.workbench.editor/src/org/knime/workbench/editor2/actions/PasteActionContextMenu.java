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
 *   20.02.2006 (sieb): created
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.editparts.ZoomManager;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.ClipboardObject;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.PasteFromWorkflowPersistorCommand.ShiftCalculator;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Implements the clipboard paste action to paste nodes and connections from the
 * clipboard into the editor. This sub class is used for context invoked pastes
 * only and pastes the nodes to the location of the current cursor.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class PasteActionContextMenu extends PasteAction {
//     private static final NodeLogger LOGGER =
//         NodeLogger.getLogger(PasteActionContextMenu.class);

    /** ID for this action. */
    public static final String ID = "PasteActionContext";

    /**
     * Constructs a new clipboard paste action.
     *
     * @param editor the workflow editor this action is intended for
     */
    public PasteActionContextMenu(final WorkflowEditor editor) {
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
     *
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        super.runOnNodes(nodeParts);
    }

    /** {@inheritDoc} */
    @Override
    protected ShiftCalculator newShiftCalculator() {
        return new ShiftCalculator() {
            /** {@inheritDoc} */
            @Override
            public int[] calculateShift(final Iterable<int[]> boundsList,
                    final WorkflowManager manager,
                    final ClipboardObject clipObject) {
                int x = getEditor().getSelectionTool().getXLocation();
                int y = getEditor().getSelectionTool().getYLocation();
                int smallestX = Integer.MAX_VALUE;
                int smallestY = Integer.MAX_VALUE;
                for (int[] bounds : boundsList) {
                    int currentX = bounds[0];
                    int currentY = bounds[1];
                    if (currentX < smallestX) {
                        smallestX = currentX;
                    }
                    if (currentY < smallestY) {
                        smallestY = currentY;
                    }
                }
                ZoomManager zoomManager =
                        (ZoomManager)getEditor().getViewer().getProperty(
                                ZoomManager.class.toString());

                Point viewPortLocation =
                    zoomManager.getViewport().getViewLocation();
                x += viewPortLocation.x;
                y += viewPortLocation.y;
                double zoom = zoomManager.getZoom();
                x /= zoom;
                y /= zoom;

                int shiftx = x - smallestX;
                int shifty = y - smallestY;
                if (getEditor().getEditorSnapToGrid()) {
                    shiftx = getEditor().getEditorGridXOffset(shiftx);
                    shifty = getEditor().getEditorGridYOffset(shifty);
                }
                return new int[]{shiftx, shifty};
            }
        };
    }

}
