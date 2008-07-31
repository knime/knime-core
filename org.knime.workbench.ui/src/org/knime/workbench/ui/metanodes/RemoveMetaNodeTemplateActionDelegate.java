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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class RemoveMetaNodeTemplateActionDelegate implements
        IViewActionDelegate {
    
    private MetaNodeTemplateRepositoryView m_view;
    private MetaNodeTemplateRepositoryItem m_item;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IViewPart view) {
        m_view = (MetaNodeTemplateRepositoryView)view;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final IAction action) {
        if (m_view != null && m_item != null) {
            if (MessageDialog.openConfirm(
                    PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getShell(), 
                    "Delete Meta Node Template",
                    "Do really want to delete meta node template " 
                    + m_item.getName() + " ?")) {
                m_view.deleteTemplate(m_item);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectionChanged(final IAction action, 
            final ISelection selection) {
        action.setEnabled(false);
        Object o = ((IStructuredSelection)selection).getFirstElement(); 
        if (o != null && o instanceof MetaNodeTemplateRepositoryItem) {
            m_item = (MetaNodeTemplateRepositoryItem)o;
            action.setEnabled(true);
        }
    }

}
