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
 *   ${date} (${user}): created
 */
package org.knime.workbench.editor2;

import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.draw2d.parts.Thumbnail;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * Overview outline page that shows a scaled thumbnail picture of the editor's
 * content.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NewOverviewOutlinePage extends Page 
        implements IContentOutlinePage {
    /** the control of the overview. */
    private Canvas m_overview;

    /** the root edit part. */
    private ScalableFreeformRootEditPart m_rootEditPart;

    /** the thumbnail. */
    private Thumbnail m_thumbnail;

    /**
     * Creates a new NewOverviewOutlinePage instance.
     * 
     * @param rootPart the root edit part to show the overview from
     */
    public NewOverviewOutlinePage(final ScalableFreeformRootEditPart rootPart) {
        super();
        m_rootEditPart = rootPart;
    }

    /**
     * {@inheritDoc}
     */
    public void addSelectionChangedListener(
            final ISelectionChangedListener listener) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createControl(final Composite parent) {
        // create canvas and lws
        m_overview = new Canvas(parent, SWT.NONE);
        LightweightSystem lws = new LightweightSystem(m_overview);

        // create thumbnail
        m_thumbnail = new ScrollableThumbnail((Viewport) m_rootEditPart
                .getFigure());
        m_thumbnail.setBorder(new MarginBorder(3));
        m_thumbnail.setSource(m_rootEditPart
                .getLayer(LayerConstants.PRINTABLE_LAYERS));
        lws.setContents(m_thumbnail);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        if (null != m_thumbnail) {
            m_thumbnail.deactivate();
        }

        super.dispose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Control getControl() {
        return m_overview;
    }

    /**
     * {@inheritDoc}
     */
    public ISelection getSelection() {
        return StructuredSelection.EMPTY;
    }

    /**
     * {@inheritDoc}
     */
    public void removeSelectionChangedListener(
            final ISelectionChangedListener listener) {
        // nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFocus() {
        if (getControl() != null) {
            getControl().setFocus();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setSelection(final ISelection selection) {
        // nothing
    }
}
