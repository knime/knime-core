/*
 * ----------------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 * ----------------------------------------------------------------------------
 */
package org.knime.product.rcp;

import org.eclipse.core.runtime.IPlatformRunnable;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * This class controls all aspects of the application's execution.
 */
public class KNIMEApplication implements IPlatformRunnable {
    
    private static final String PROP_EXIT_CODE = "eclipse.exitcode";
    
    /**
     * @param args The args
     * @throws Exception on general application error
     * @return return code, interpreted by OS (e.g. "restart app")
     * @see org.eclipse.core.runtime.IPlatformRunnable#run(java.lang.Object)
     */
    public Object run(final Object args) throws Exception {
        Display display = PlatformUI.createDisplay();
        try {
            int returnCode = PlatformUI.createAndRunWorkbench(display,
                    new KNIMEApplicationWorkbenchAdvisor());
            
//          the workbench doesn't support relaunch yet (bug 61809) so
            // for now restart is used, and exit data properties are checked
            // here to substitute in the relaunch return code if needed
            if (returnCode != PlatformUI.RETURN_RESTART) {
                return EXIT_OK;
            }

            // if the exit code property has been set to the relaunch code, then
            // return that code now, otherwise this is a normal restart
            return EXIT_RELAUNCH.equals(Integer.getInteger(PROP_EXIT_CODE)) ? EXIT_RELAUNCH
                    : EXIT_RESTART;
            // if (returnCode == PlatformUI.RETURN_RESTART) {
            // return IPlatformRunnable.EXIT_RESTART;
            // }
            //            return IPlatformRunnable.EXIT_OK;
        } finally {
            display.dispose();
        }
    }
}
