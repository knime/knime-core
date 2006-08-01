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
 *   12.01.2005 (Flo): created
 */
package org.knime.workbench.repository.view;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;

import org.knime.workbench.repository.model.NodeTemplate;

/**
 * Drag listener for dragging NodeTemplates out of the Repository tree viewer.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NodeTemplateDragListener implements DragSourceListener {
    private TreeViewer m_viewer;

    /**
     * @param viewer The viewer to add drag support to
     */
    public NodeTemplateDragListener(final TreeViewer viewer) {
        m_viewer = viewer;
    }

    /**
     * Only start the drag, if there's exactly one NodeTemplate selected in the
     * viewer.
     * 
     * @see org.eclipse.swt.dnd.DragSourceListener
     *      #dragStart(org.eclipse.swt.dnd.DragSourceEvent)
     */
    public void dragStart(final DragSourceEvent event) {
        IStructuredSelection sel = (IStructuredSelection)m_viewer
                .getSelection();

        // cancel event, if not an NodeTemplate, or not exactly one element
        // selected
        if (!(sel.getFirstElement() instanceof NodeTemplate)
                || (sel.size() != 1)) {
            event.doit = false;
        }
    }

    /**
     * @see org.eclipse.swt.dnd.DragSourceListener
     *      #dragSetData(org.eclipse.swt.dnd.DragSourceEvent)
     */
    public void dragSetData(final DragSourceEvent event) {
        IStructuredSelection sel = (IStructuredSelection)m_viewer
                .getSelection();

        event.data = sel.getFirstElement();
    }

    /**
     * @see org.eclipse.swt.dnd.DragSourceListener
     *      #dragFinished(org.eclipse.swt.dnd.DragSourceEvent)
     */
    public void dragFinished(final DragSourceEvent event) {
        // TODO Auto-generated method stub
    }
}
