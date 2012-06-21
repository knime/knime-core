/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
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
package org.knime.workbench.ui.navigator.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;

/**
 *
 * @author Fabian Dill, KNIME.com AG
 */
public class ResetWorkflowAction extends AbstractWorkflowAction {
    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Reset";
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor(SharedImages.Reset);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        if (super.isEnabled()) {
            return WorkflowManager.ROOT.canResetNode(getWorkflow().getID());
        }
        return false;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void run() {
        WorkflowManager workflow = getWorkflow();
        WorkflowManager.ROOT.resetAndConfigureNode(workflow.getID());
    }

}
