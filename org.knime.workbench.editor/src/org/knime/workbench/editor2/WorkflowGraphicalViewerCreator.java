/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   25.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorSite;

/**
 * Helper class that creates the <code>GraphicalViewer</code> that is hosted
 * inside the editor. This creates the root edit part and the
 * <code>NodeTemplateDropTargetListener</code> that is responsible for
 * dropping <code>NodeTemplates</code> into the viewer. (which get converted
 * into <code>NodeContainer</code> objects.)
 *
 *
 * @author Florian Georg, University of Konstanz
 */
public class WorkflowGraphicalViewerCreator {
    /** the viewer. * */
    private GraphicalViewer m_viewer;

    /** the editor's action registry. */
    private final ActionRegistry m_actionRegistry;

    /**
     *
     * @param editorSite Current editor site
     * @param actionRegistry The action registry to use
     */
    public WorkflowGraphicalViewerCreator(final IEditorSite editorSite,
            final ActionRegistry actionRegistry) {

        assert editorSite != null;

        this.m_actionRegistry = actionRegistry;
    }

    /**
     * Creates a new <code>Viewer</code>, configures, registers and
     * initializes it.
     *
     * @param parent the parent composite
     */
    public void createGraphicalViewer(final Composite parent) {
        m_viewer = createViewer(parent);
    }

    /**
     * Creates the viewer control, and connect it to a root edit part
     * Additionally the viewer gets the edit part factory and a drop-listener.
     *
     * @param parent Parent composite
     * @return The viewer
     */
    protected GraphicalViewer createViewer(final Composite parent) {

        // StatusLineValidationMessageHandler validationMessageHandler = new
        // StatusLineValidationMessageHandler(editorSite);
        ScrollingGraphicalViewer viewer = new ScrollingGraphicalViewer();
        viewer.createControl(parent);

        // configure the m_viewer
        viewer.getControl().setBackground(ColorConstants.white);
        ScalableFreeformRootEditPart part = new ConnectionSelectingScalableFreeformRootEditPart();
        viewer.setRootEditPart(part);
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));

        // Add a drop listener
        NodeTemplateDropTargetListener2 listener
            = new NodeTemplateDropTargetListener2(viewer);
        viewer.addDropTargetListener(listener);
        // configure context menu
        viewer.setContextMenu(new WorkflowContextMenuProvider(m_actionRegistry,
                viewer));

        // set the factory that is able to create the edit parts to be
        // used in the viewer
        viewer.setEditPartFactory(new WorkflowEditPartFactory());


        
        return viewer;
    }

    /**
     * @return Returns the m_viewer.
     */
    public GraphicalViewer getViewer() {
        return m_viewer;
    }
}
