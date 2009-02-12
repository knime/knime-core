/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
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
 * -------------------------------------------------------------------
 * 
 * History
 *   11.03.2005 (georg): created
 */
package org.knime.workbench.ui.nature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

/**
 * Project nature for KNIME projects, not used by now.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class KNIMEProjectNature implements IProjectNature {
    
    public static final String ID = "org.knime.workbench.ui.KNIMEProjectNature";
    
    private IProject m_project;

    /**
     * {@inheritDoc}
     */
    public void configure() throws CoreException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    public void deconfigure() throws CoreException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    public IProject getProject() {
        return m_project;
    }

    /**
     * {@inheritDoc}
     */
    public void setProject(final IProject project) {
        m_project = project;

    }
}
