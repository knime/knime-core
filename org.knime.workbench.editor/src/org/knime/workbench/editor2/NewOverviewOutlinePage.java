/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
