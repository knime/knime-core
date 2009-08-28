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
 *   26.08.2009 (ohl): created
 */
package org.knime.workbench.ui.preferences;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * Action to import current preferences to a file.
 *
 * @author Peter Ohl, KNIME.com, Switzerland
 */
public class ImportPreferencesAction extends Action {

    private static final ImageDescriptor ICON =
            KNIMEUIPlugin.imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                    "icons/prefs_import.png");

    /**
     * The id for this action.
     */
    public static final String ID = "KNIMEPreferencesImport";

    /**
     * The workbench window; or <code>null</code> if this action has been
     * <code>dispose</code>d.
     */

    /**
     * Create a new instance of this class.
     *
     * @param window the window
     */
    public ImportPreferencesAction(final IWorkbenchWindow window) {
        super("Import Preferences...");
        if (window == null) {
            throw new IllegalArgumentException();
        }
        setToolTipText("Sets the preferences to values imported from a file");
        setId(ID); //$NON-NLS-1$
        // window.getWorkbench().getHelpSystem().setHelp(this,
        // IWorkbenchHelpContextIds.IMPORT_ACTION);
        // self-register selection listener (new for 3.0)

    }

    /**
     * Create a new instance of this class.
     *
     * @param workbench the workbench
     * @deprecated use the constructor
     *             <code>ExportPreferencesAction(IWorkbenchWindow)</code>
     */
    public ImportPreferencesAction(final IWorkbench workbench) {
        this(workbench.getActiveWorkbenchWindow());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ICON;
    }

    /**
     * Invoke the Import wizards selection Wizard.
     */
    @Override
    public void run() {
        IWorkbenchWindow workbenchWindow =
                PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (workbenchWindow == null) {
            // action has been disposed
            return;
        }

        FileDialog fileDialog =
                new FileDialog(workbenchWindow.getShell(), SWT.OPEN);
        fileDialog.setFilterExtensions(new String[]{"*.epf", "*.*"});
        fileDialog.setText("Specify the preferences file to import.");
        String filePath = fileDialog.open();
        if (filePath == null || filePath.trim().length() == 0) {
            return;
        }

        File inFile = new File(filePath);
        if (!inFile.isFile() || !inFile.canRead()) {
            MessageDialog.openError(workbenchWindow.getShell(),
                    "File Selection Error",
                    "Unable to read from specified location.");
            return;
        }

        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(inFile));
            IPreferencesService prefService = Platform.getPreferencesService();
            NodeLogger.getLogger(ImportPreferencesAction.class).info(
                    "Importing preferences from file "
                            + inFile.getAbsolutePath() + " now ...");
            prefService.importPreferences(in);
        } catch (Throwable t) {
            String msg = "Unable to read preferences from selected file";
            if (t.getMessage() != null && !t.getMessage().isEmpty()) {
                msg = t.getMessage();
            }
            MessageDialog.openError(workbenchWindow.getShell(),
                    "Error Importing Preferences", msg);
            return;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

    }
}
