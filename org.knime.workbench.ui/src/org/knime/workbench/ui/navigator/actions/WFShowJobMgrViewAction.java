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

import javax.swing.SwingUtilities;

import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.Node;
import org.knime.core.node.workflow.NodeExecutionJobManager;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 *
 * @author Fabian Dill, KNIME.com GmbH
 */
public class WFShowJobMgrViewAction extends AbstractWorkflowAction {

    private static final ImageDescriptor IMG
        = KNIMEUIPlugin.imageDescriptorFromPlugin(
                KNIMEUIPlugin.PLUGIN_ID,
                "icons/actions/openView.gif");

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Open JobMgr View";
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return IMG;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        if (super.isEnabled()) {
            NodeExecutionJobManager jobMgr = getWorkflow().getJobManager();
            if (jobMgr == null) {
                return false;
            }
            return jobMgr.hasView();
        }
        return false;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void run() {
        final WorkflowManager workflow = getWorkflow();
        Runnable runner = new Runnable() {
            @Override
            public void run() {
                Node.invokeOpenView(workflow.getJobManager().getView(workflow),
                        workflow.getNameWithID());
            }
        };
        SwingUtilities.invokeLater(runner);

    }

}
