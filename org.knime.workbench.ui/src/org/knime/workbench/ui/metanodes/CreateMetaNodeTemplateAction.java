/* This source code, its documentation and all appendant files
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
 */
package org.knime.workbench.ui.metanodes;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class CreateMetaNodeTemplateAction extends Action {

    private WorkflowManager m_manager;
    
    private NodeContainer m_metaNode;
    
    private final IInputValidator m_validator 
        = new MetaNodeTemplateNameValidator();
    
    private static final String ID 
        = "org.knime.workbench.ui.metanodetemplates.create";
    
    private static final ImageDescriptor ICON 
        = KNIMEUIPlugin.imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                "icons/meta/metanode_template.png");
    
    /**
     * 
     * @param parent WorkflowManager of the WorkflowEditor from which the nodes
     *  are copied
     * @param metaNode the meta node that should be exported as a template
     */
    public CreateMetaNodeTemplateAction(final WorkflowManager parent, 
            final NodeContainer metaNode) {
        m_manager = parent;
        m_metaNode = metaNode;
    }
    

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return getToolTipText();
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Add to MetaNode templates";
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Adds this meta node to the meta node template repository";
    }
    
    /**
     * 
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
    public ImageDescriptor getImageDescriptor() {
        return ICON;
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        if (m_metaNode != null) {
            InputDialog nameDialog = new InputDialog(
                    PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow().getShell(),
                    "MetaNode Template", 
                    "Enter name for the meta node template:",
                    null, m_validator);
            
            int returnCode = nameDialog.open(); 
            if (returnCode == Window.CANCEL) {
                return;
            } else {
                String name = nameDialog.getValue();
                MetaNodeTemplateRepositoryView.getInstance()
                    .createMetaNodeTemplate(name, 
                        m_manager, 
                        new NodeID[] {m_metaNode.getID()});
            }
        }
    }
    
    private class MetaNodeTemplateNameValidator implements IInputValidator {

        /**
         * 
         * {@inheritDoc}
         */
        @Override
        public String isValid(final String newText) {
            if (!MetaNodeTemplateRepositoryView.getInstance()
                    .isNameUnique(newText)) {
                return "Name " + newText + " is already in repository.";
            }
            return null;
        }
        
    }


}
