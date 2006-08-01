/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
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
     * @see org.eclipse.jface.viewers.ISelectionProvider
     *      #addSelectionChangedListener
     *      (org.eclipse.jface.viewers.ISelectionChangedListener)
     */
    public void addSelectionChangedListener(
            final ISelectionChangedListener listener) {
    }

    /**
     * @see org.eclipse.ui.part.IPage
     *      #createControl(org.eclipse.swt.widgets.Composite)
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
     * @see org.eclipse.ui.part.IPage#dispose()
     */
    @Override
    public void dispose() {
        if (null != m_thumbnail) {
            m_thumbnail.deactivate();
        }

        super.dispose();
    }

    /**
     * @see org.eclipse.ui.part.IPage#getControl()
     */
    @Override
    public Control getControl() {
        return m_overview;
    }

    /**
     * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
     */
    public ISelection getSelection() {
        return StructuredSelection.EMPTY;
    }

    /**
     * @see org.eclipse.jface.viewers.ISelectionProvider#
     *      removeSelectionChangedListener
     *      (org.eclipse.jface.viewers.ISelectionChangedListener)
     */
    public void removeSelectionChangedListener(
            final ISelectionChangedListener listener) {
        // nothing
    }

    /**
     * @see org.eclipse.ui.part.IPage#setFocus()
     */
    @Override
    public void setFocus() {
        if (getControl() != null) {
            getControl().setFocus();
        }
    }

    /**
     * @see org.eclipse.jface.viewers.ISelectionProvider
     *      #setSelection(org.eclipse.jface.viewers.ISelection)
     */
    public void setSelection(final ISelection selection) {
        // nothing
    }
}
