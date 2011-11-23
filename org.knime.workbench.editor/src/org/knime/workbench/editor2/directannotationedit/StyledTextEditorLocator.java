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
 *   2010 10 25 (ohl): created
 */
package org.knime.workbench.editor2.directannotationedit;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.tools.CellEditorLocator;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;
import org.knime.workbench.editor2.figures.AnnotationFigure3;

/**
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class StyledTextEditorLocator implements CellEditorLocator {

    private final AnnotationFigure3 m_figure;

    private AnnotationEditPart m_editPart = null;

    public StyledTextEditorLocator(final AnnotationFigure3 figure) {
        figure.getClass(); // must not be null
        m_figure = figure;
    }

    public void setEditPart(final GraphicalEditPart editpart) {
        if (editpart instanceof AnnotationEditPart) {
            m_editPart = (AnnotationEditPart)editpart;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void relocate(final CellEditor celleditor) {
        Composite edit = (Composite)celleditor.getControl();
        Rectangle figBounds = m_figure.getBounds().getCopy();
        ZoomManager zoomManager = null;
        if (m_editPart != null) {
            zoomManager = (ZoomManager)(m_editPart.getRoot().getViewer()
                    .getProperty(ZoomManager.class.toString()));
        }
        double zooFactor = 1;
        if (zoomManager != null) {
            if (zoomManager.getZoom() < 1.0) {
                zooFactor = zoomManager.getZoom();
            }
        }
        // adapt to zoom level and viewport
        m_figure.translateToAbsolute(figBounds);
        org.eclipse.swt.graphics.Rectangle trim = edit.computeTrim(0, 0, 0, 0);
        figBounds.translate(trim.x, trim.y);
        figBounds.width = (int)((figBounds.width + trim.width) / zooFactor);
        figBounds.height = (int)((figBounds.height + trim.height) / zooFactor);

        // grow the height with the text entered
        StyledTextEditor stEditor = ((StyledTextEditor)celleditor);
        org.eclipse.swt.graphics.Rectangle textBounds = stEditor.getTextBounds();
        int th = textBounds.height;
        // + stEditor.getLineHeight();
        if (th > figBounds.height) {
            figBounds.height = th;
        }
        edit.setBounds(new org.eclipse.swt.graphics.Rectangle(figBounds.x,
                figBounds.y, figBounds.width, figBounds.height));
    }

}
