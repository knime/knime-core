/*
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   27.06.2006 (sieb): created
 */
package org.knime.workbench.ui.navigator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.views.navigator.ResourcePatternFilter;
import org.knime.core.node.workflow.WorkflowPersistor;
/**
 * Implements the knime resource filter for the knime resource navigator. Only
 * the project has to be shown.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class KnimeResourcePatternFilter extends ResourcePatternFilter {
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean select(final Viewer viewer, final Object parentElement,
            final Object element) {
        if (element instanceof IResource) {
            IResource resource = (IResource)element;
            if (resource.getType() == IResource.PROJECT) {

                IProject project = (IProject)resource;
                /*
                try {
                    project.refreshLocal(IResource.DEPTH_ZERO, null);
                } catch (Exception e) {
                    // if crashes for some reason do not display it
                    return false;
                }
               */
                return project.exists(new Path("/" 
                        + WorkflowPersistor.WORKFLOW_FILE));

            } else if (resource.getType() == IResource.FOLDER) {
                return true; 
            }
        }

        return false;
    }
}
