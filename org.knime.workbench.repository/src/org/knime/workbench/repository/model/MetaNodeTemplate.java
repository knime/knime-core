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
package org.knime.workbench.repository.model;

import org.knime.core.node.workflow.WorkflowManager;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class MetaNodeTemplate extends AbstractSimpleObject {

    private final WorkflowManager m_manager;
    /**
     * 
     * @param workflowDir the directory containing the workflow files
     */
    public MetaNodeTemplate(final String id, final String name, 
            final WorkflowManager manager) {
        super();
        m_manager = manager;
        setID(id);
        setName(name);
        setAfterID("");
    }

    public WorkflowManager getManager() {
        return m_manager;
    }
    
    
    public String getCategoryPath() {
        return "/meta";
    }
    
    public String getDescription() {
        return m_manager.getName() + ": " + m_manager.getCustomDescription();
    }

    
}
