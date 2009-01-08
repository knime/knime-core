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
package org.knime.workbench.ui.nature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

/**
 * 
 * @author Fabian Dill, KNIME.com GmbH
 */
public class KNIMEWorkflowSetProjectNature implements IProjectNature {
    
    /**
     * ID as defined in plugin.xml.
     */
    public static final String ID 
        = "com.knime.workflowset.KNIMEWorkflowsetProject";
    
    private IProject m_project;

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure() throws CoreException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deconfigure() throws CoreException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IProject getProject() {
        return m_project;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProject(final IProject project) {
        m_project = project;
    }

}
