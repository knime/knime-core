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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.NotConfigurableException;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.wrapper.WrappedNodeDialog;

/**
 * 
 * @author Fabian Dill, KNIME.com GmbH
 */
public class ConfigureWorkflowAction extends AbstractWorkflowAction {
    
    private static final ImageDescriptor IMG 
        = KNIMEUIPlugin.imageDescriptorFromPlugin(
                KNIMEUIPlugin.PLUGIN_ID, 
                "icons/actions/configure.gif");
    
    @Override
    public String getText() {
        return "Configure...";
    }
    
    @Override
    public String getDescription() {
        return "Opens a configuration dialog for this workflow";
    }
    
    @Override
    public ImageDescriptor getImageDescriptor() {
        return IMG;
    }
    
    @Override
    public void run() {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    WrappedNodeDialog dialog = new WrappedNodeDialog(
                            Display.getDefault().getActiveShell(), 
                            getWorkflow());
                    dialog.setBlockOnOpen(true);
                    dialog.open();
                } catch (final NotConfigurableException nce) {
                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            MessageDialog.openError(
                                    Display.getDefault().getActiveShell(), 
                                    "Workflow Not Configurable", 
                                    "This workflow can not be configured: "
                                    + nce.getMessage());
                        }
                    });
                }
                
            }
        });
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        if (super.isEnabled()) {
            return getWorkflow().hasDialog();
        }
        return false;
    }

}
