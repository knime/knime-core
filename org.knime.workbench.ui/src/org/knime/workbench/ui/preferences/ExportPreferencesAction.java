/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
    @Deprecated
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
            /* Do not export the default values. */
            prefService.exportPreferences(prefService.getRootNode(), out,
                    new String[]{
                    "bundle_defaults"}); //, "configuration", "profile"});
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
