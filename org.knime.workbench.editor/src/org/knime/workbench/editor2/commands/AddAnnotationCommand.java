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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   2010 10 26 (ohl): created
 */
package org.knime.workbench.editor2.commands;

import java.util.Collections;

import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.swt.graphics.Point;
import org.knime.core.node.workflow.WorkflowAnnotation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.directannotationedit.StyledTextEditor;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;

/**
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class AddAnnotationCommand extends AbstractKNIMECommand {

    private final GraphicalViewer m_viewer;

    private final Point m_location;

    private static final int DEFAULT_HEIGHT;

    private static final int DEFAULT_WIDTH;

    static {
        DEFAULT_WIDTH = Math.max(175, StyledTextEditor.TOOLBAR_MIN_WIDTH);
        DEFAULT_HEIGHT = (int)Math.round(DEFAULT_WIDTH * 0.38);
    }
    private static final int INITIAL_COLOR = AnnotationEditPart
            .colorToRGBint(AnnotationEditPart
                    .getAnnotationDefaultBackgroundColor());

    public static final String INITIAL_TEXT = "Double-click to edit.";

    // remember the new annotation for undo
    private WorkflowAnnotation m_anno;

    /**
     * @param wfm
     * @param viewer
     * @param location
     */
    public AddAnnotationCommand(final WorkflowManager wfm,
            final GraphicalViewer viewer, final Point location) {
        super(wfm);
        m_location = location;
        m_viewer = viewer;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canExecute() {
        return super.canExecute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        // adapt location to the viewport location and the zoom factor
        // this seems to be a workaround for a bug in the framework
        ZoomManager zoomManager =
                (ZoomManager)m_viewer.getProperty(ZoomManager.class.toString());

        // adjust the location according to the viewport position
        // seems to be a workaround for a bug in the framework
        // (should imediately deliver the correct view position and not
        // the position of the viewport)
        PrecisionPoint location =
                new PrecisionPoint(m_location.x, m_location.y);
        WorkflowEditor.adaptZoom(zoomManager, location, true);

        m_anno = new WorkflowAnnotation();
        m_anno.setText(INITIAL_TEXT);
        m_anno.setBgColor(INITIAL_COLOR);
        m_anno.setStyleRanges(new WorkflowAnnotation.StyleRange[0]);
        m_anno.setDimension((int)location.preciseX, (int)location.preciseY,
                DEFAULT_WIDTH, DEFAULT_HEIGHT);
        WorkflowManager hostWFM = getHostWFM();
        hostWFM.addWorkflowAnnotation(m_anno);
        m_viewer.deselectAll();
        // select the new ones....
        if (m_viewer.getRootEditPart().getContents() != null
                && m_viewer.getRootEditPart().getContents() instanceof WorkflowRootEditPart) {
            ((WorkflowRootEditPart)m_viewer.getRootEditPart().getContents())
                    .setFutureAnnotationSelection(Collections.singleton(m_anno));
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canUndo() {
        return m_anno != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        getHostWFM().removeAnnotation(m_anno);
        m_anno = null;
    }
}
