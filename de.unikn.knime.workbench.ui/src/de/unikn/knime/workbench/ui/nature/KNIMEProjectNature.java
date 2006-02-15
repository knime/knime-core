/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   11.03.2005 (georg): created
 */
package de.unikn.knime.workbench.ui.nature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

/**
 * Project nature for KNIME projects, not used by now.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class KNIMEProjectNature implements IProjectNature {

    private IProject m_project;

    /**
     * @see org.eclipse.core.resources.IProjectNature#configure()
     */
    public void configure() throws CoreException {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.eclipse.core.resources.IProjectNature#deconfigure()
     */
    public void deconfigure() throws CoreException {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.eclipse.core.resources.IProjectNature #getProject()
     */
    public IProject getProject() {
        return m_project;
    }

    /**
     * @see org.eclipse.core.resources.IProjectNature
     *      #setProject(org.eclipse.core.resources.IProject)
     */
    public void setProject(final IProject project) {
        m_project = project;

    }

}
