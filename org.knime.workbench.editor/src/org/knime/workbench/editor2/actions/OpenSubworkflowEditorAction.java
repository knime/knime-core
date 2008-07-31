/*
 * ------------------------------------------------------------------ *
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   10.06.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class OpenSubworkflowEditorAction extends Action {
    
    private static final String ID = "knime.open.subworkflow.editor";

    
    private final NodeContainerEditPart m_nodeContainer;
    
    /**
     * @param editor the workflow editor
     */
    public OpenSubworkflowEditorAction(final NodeContainerEditPart node) {
        m_nodeContainer = node;
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
    public String getText() {
        return "Open Subworkflow Editor";
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Opens editor for this subworkflow";
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/meta/meta.png");
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    protected boolean calculateEnabled() {
        if (m_nodeContainer.getModel() instanceof WorkflowManager) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        m_nodeContainer.openSubWorkflowEditor();
    }

}
