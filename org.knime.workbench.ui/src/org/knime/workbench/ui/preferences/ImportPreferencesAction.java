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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.IExportedPreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
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

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(ImportPreferencesAction.class);

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
    @Deprecated
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
           LOGGER.info("Importing preferences from file "
                            + inFile.getAbsolutePath() + " now ...");
            /* Importing preferences still causes problems with the network
             * preference page. After a restart of KNIME everything is back to
             * normal. 
             * (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=246754) */
            IExportedPreferences prefs = prefService.readPreferences(in);
            prefService.applyPreferences(prefs);
            LOGGER.info("Import of preferences successfully finished.");
            requestRestart();
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

    /**
     * Initializes the default preferences for all preference pages. This fixes
     * most of the issues that occur when new preferences are imported
     * without restarting KNIME.
     */
    private void initializeDefaultPreferences() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        for (IConfigurationElement element : registry
                .getConfigurationElementsFor("org.eclipse.core.runtime.preferences")) {
            try {
                final Object o = element.createExecutableExtension("class");
                @SuppressWarnings("unchecked")
                Class<? extends AbstractPreferenceInitializer> clazz =
                        (Class<? extends AbstractPreferenceInitializer>)o.getClass();
                if (o instanceof AbstractPreferenceInitializer) {
                    ((AbstractPreferenceInitializer)o).initializeDefaultPreferences();
                    LOGGER.debug("Found class: " + clazz.getCanonicalName());
                } else {
                    LOGGER.debug("Skipped class: " + clazz.getCanonicalName());
                }

            } catch (InvalidRegistryObjectException e) {
                throw new IllegalArgumentException(e);
            } catch (CoreException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    /**
     * Request a restart of the platform according to the specified
     * restart policy.
     *
     * @param restartPolicy
     */
    private void requestRestart() {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (PlatformUI.getWorkbench().isClosing())
                    return;
                int retCode = new MessageDialog(Display.getCurrent().getActiveShell(),
                        "Restart to complete settings import",
                        null,
                        "The settings have been imported. It is highly recommended to restart KNIME to complete the import of the preferences. Would you like to restart now?",
                        MessageDialog.QUESTION,
                        new String[] {"restart", "cancel"}, 0).open();
                if (retCode == 0) {
                    PlatformUI.getWorkbench().restart();
                } else {
                    initializeDefaultPreferences();
                }
            }
        });
    }
}
