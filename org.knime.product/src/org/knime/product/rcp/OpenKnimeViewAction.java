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
 *   Sep 23, 2009 (ohl): created
 */
package org.knime.product.rcp;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;

/**
 * Used to open views that are registered with the multInstView extension point
 * (in workbench.ui). The KNIME single-instance views are usually registered
 * with the viewShortcut extension point (as part of the perspective ext point).
 * View shortcuts are handled by Eclipse.
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class OpenKnimeViewAction extends Action {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(OpenKnimeViewAction.class);

    /**
     * An action for the View menu in the product. Opens the view with the
     * specified id, when run.
     *
     * @param viewID the id of the view to open
     */
    public OpenKnimeViewAction(final String viewID) {
        LOGGER.debug("Registering view " + viewID
                + " (multInstView) with the view menu");
        setId(viewID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        try {
            PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                    .getActivePage().showView(getId(),
                            Long.toString(System.currentTimeMillis()),
                            IWorkbenchPage.VIEW_ACTIVATE);
        } catch (PartInitException e) {
            LOGGER.coding("Unable to open view id=" + getId(), e);
        }
    }

}
