/*
 * ----------------------------------------------------------------------------
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
 * ----------------------------------------------------------------------------
 */
package org.knime.product.rcp;

import java.io.File;
import java.net.URL;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.ide.ChooseWorkspaceData;
import org.eclipse.ui.internal.ide.ChooseWorkspaceDialog;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.knime.core.util.FileLocker;

/**
 * This class controls all aspects of the application's execution.
 */
public class KNIMEApplication implements IApplication {

    private static final String PROP_EXIT_CODE = "eclipse.exitcode";

    private static final String LOCK_FOLDERNAME = ".metadata";

    private static final String LOCK_FILENAME = ".lock";

    /**
     * {@inheritDoc}
     */
    @Override
    public Object start(final IApplicationContext context) throws Exception {
        Display display = PlatformUI.createDisplay();
        // check if the workspace is already in use
        // at this point its valid, so try to lock it and update the
        // metadata version information if successful
        try {
            Shell shell = new Shell(display, SWT.ON_TOP);
            if (!lockWorkspace()) {
                Platform.endSplash();
                MessageDialog
                        .openError(
                                shell,
                                IDEWorkbenchMessages.IDEApplication_workspaceCannotLockTitle,
                                IDEWorkbenchMessages.IDEApplication_workspaceCannotLockMessage);
                return IApplication.EXIT_OK;
            }
        } catch (Exception e) {
            // do nothing if locking could not be performed
        }

        try {
            Location workspace = Platform.getInstanceLocation();
            if (!workspace.isSet()) {
                boolean forcePrompt = false;
                do {
                    ChooseWorkspaceData data =
                            new ChooseWorkspaceData(workspace.getDefault());
                    ChooseWorkspaceDialog dialog =
                            new ChooseWorkspaceDialog(Display.getDefault()
                                    .getActiveShell(), data, false, true);
                    dialog.prompt(forcePrompt);
                    String loc = data.getSelection();
                    if (loc == null) {
                        return EXIT_OK;
                    }
                    if (loc.length() < 1) {
                        forcePrompt = true;
                        continue;
                    }
                    File f = new File(loc);
                    URL wsUrl = f.toURI().toURL();
                    workspace.setURL(wsUrl, true);
                    break;
                } while (true);
            }

            int returnCode =
                    PlatformUI.createAndRunWorkbench(display,
                            new KNIMEApplicationWorkbenchAdvisor());

            // the workbench doesn't support relaunch yet (bug 61809) so
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
            // return IPlatformRunnable.EXIT_OK;
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            display.dispose();
        }
        return EXIT_OK;
    }

    private boolean lockWorkspace() throws Exception {

        Location instanceLoc = Platform.getInstanceLocation();

        File lockFileFolder =
                new File(instanceLoc.getURL().getFile(), LOCK_FOLDERNAME);
        if (!lockFileFolder.exists()) {
            lockFileFolder.mkdirs();
        }
        File lockFile = new File(lockFileFolder, LOCK_FILENAME);
        if (!lockFile.exists()) {
            lockFile.createNewFile();
        }

        FileLocker locker = new FileLocker(lockFile);
        return locker.lock();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
    }
}
