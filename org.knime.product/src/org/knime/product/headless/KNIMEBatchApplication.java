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
package org.knime.product.headless;

import org.eclipse.core.runtime.IPlatformRunnable;
import org.knime.core.node.workflow.BatchExecutor;
import org.knime.workbench.repository.RepositoryManager;

/**
 * The run method of this class is executed when KNIME is run headless, 
 * that is in batch mode. 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class KNIMEBatchApplication implements IPlatformRunnable {

    /**
     * @see org.eclipse.core.runtime.IPlatformRunnable#run(java.lang.Object)
     */
    public Object run(final Object args) throws Exception {
        // unless the user specified this property, we set it to true here
        // (true means no icons etc will be loaded, if it is false, the
        // loading of the repository manager is likely to print many errors 
        // - though it will still function)
        if (System.getProperty("java.awt.headless") == null) {
            System.setProperty("java.awt.headless", "true");
        }
        // this is just to load the repository plugin
        RepositoryManager.INSTANCE.toString();
        String[] stringArgs;
        if (args instanceof String[]) {
            stringArgs = (String[])args;
            if (stringArgs.length > 0 && stringArgs[0].equals("-pdelaunch")) {
                String[] copy = new String[stringArgs.length - 1];
                System.arraycopy(stringArgs, 1, copy, 0, copy.length);
                stringArgs = copy;
            }
        } else if (args != null) {
            System.err.println("Unable to cast class " 
                    + args.getClass().getName() 
                    + " to string array, toString() returns "
                    + args.toString());
            stringArgs = new String[0];
        } else {
            stringArgs = new String[0];
        }
        // this actually returns with a non-0 value when failed, 
        // we ignore it here
        BatchExecutor.mainRun(stringArgs);
        return EXIT_OK;
    }
}
