/* This source code, its documentation and all appendant files
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
 */
package org.knime.workbench.ui.metanodes;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;

/**
 * Sets a {@link MetaNodeTemplateRepositoryItem} to 
 * the {@link LocalSelectionTransfer}, that it is available for the 
 * WorkflowEditor and the 
 * org.knime.workbench.editor.editor2.MetaNodeTemplateDropTargetListener, 
 * which retrieves it from the {@link LocalSelectionTransfer} and drops it 
 * onto the workflow editor.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class MetaNodeTemplateRepositoryDragSource 
    implements DragSourceListener {
    
    private MetaNodeTemplateRepositoryView m_view;
    
    /**
     * 
     * @param view meta node template repository
     */
    MetaNodeTemplateRepositoryDragSource(
            final MetaNodeTemplateRepositoryView view) {
        m_view = view;
    }
    
    /**
     * {@inheritDoc}
     */
    public void dragFinished(final DragSourceEvent event) {
    }

    /**
     * {@inheritDoc}
     */
    public void dragSetData(final DragSourceEvent event) {
        IStructuredSelection sel = (IStructuredSelection)m_view.getSelection();
        LocalSelectionTransfer.getTransfer().setSelection(sel);
    }

    /**
     * {@inheritDoc}
     */
    public void dragStart(final DragSourceEvent event) {
        IStructuredSelection sel = (IStructuredSelection)m_view.getSelection();
        LocalSelectionTransfer.getTransfer().setSelection(sel);
        event.detail = DND.DROP_COPY;
        // cancel event, if not an NodeTemplate, or not exactly one element
        // selected
        if (!(sel.getFirstElement() instanceof MetaNodeTemplateRepositoryItem)
                || (sel.size() != 1)) {
            event.doit = false;
        } else {
            event.doit = true;
        }
    }

}
