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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * Action to export current preferences to a file.
 *
 * @author Peter Ohl, KNIME.com, Switzerland
 */
public class ExportPreferencesAction extends Action {

    private static final ImageDescriptor ICON =
            KNIMEUIPlugin.imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                    "icons/prefs_export.png");

    /**
     * The id for this action.
     */
    public static final String ID = "KNIMEPreferencesExport";

    /**
     * The workbench window; or <code>null</code> if this action has been
     * <code>dispose</code>d.
     */

    /**
     * Create a new instance of this class.
     *
     * @param window the window
     */
    public ExportPreferencesAction(final IWorkbenchWindow window) {
        super("Export Preferences...");
        if (window == null) {
            throw new IllegalArgumentException();
        }
        setToolTipText("Exports the current preferences to a file");
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
    public ExportPreferencesAction(final IWorkbench workbench) {
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

        ExportPreferencesDialog dlg =
                new ExportPreferencesDialog(workbenchWindow.getShell());
        dlg.setBlockOnOpen(true);
        int dlgResult = dlg.open();
        if (dlgResult != IDialogConstants.OK_ID) {
            return;
        }

        File outFile = new File(dlg.fileName());
        if (!outFile.exists()) {
            File p = outFile.getParentFile();
            if (!p.exists() && !outFile.getParentFile().mkdirs()) {
                MessageDialog.openError(workbenchWindow.getShell(),
                        "File Creation Error",
                        "Unable to create parent directory for output file");
                return;
            }
        }

        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(outFile));
            IPreferencesService prefService = Platform.getPreferencesService();
            NodeLogger.getLogger(ExportPreferencesAction.class).info(
                    "Exporting preferences to file "
                            + outFile.getAbsolutePath());
            prefService.exportPreferences(prefService.getRootNode(), out,
                    new String[0]);
        } catch (Throwable t) {
            String msg = "Unable to write preferences to output file";
            if (t.getMessage() != null && !t.getMessage().isEmpty()) {
                msg = t.getMessage();
            }
            MessageDialog.openError(workbenchWindow.getShell(),
                    "Error Writing File", msg);
            return;

        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

    }
}
