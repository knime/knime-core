/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 * 
 * History
 *   09.09.2009 (Fabian Dill): created
 */
package org.knime.workbench.ui.wizards.workflowgroup;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.swt.graphics.Image;
import org.knime.workbench.ui.navigator.KnimeResourceLabelProvider;

/**
 * A special label provider which also returns a label for the 
 * {@link IWorkspaceRoot}.
 * 
 * @see KnimeResourceContentProviderWithRoot
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class KnimeResourceLabelProviderWithRoot extends
        KnimeResourceLabelProvider {
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String getText(final Object element) {
        if (element instanceof IWorkspaceRoot) {
            return "/";
        }
        return super.getText(element);
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public Image getImage(final Object element) {
        if (element instanceof IWorkspaceRoot) {
            return KnimeResourceLabelProvider.WORKFLOW_GROUP;
        }
        return super.getImage(element);
    }

}
