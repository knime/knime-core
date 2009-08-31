/*
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 */
package org.knime.workbench.ui.database;

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.core.node.port.database.DatabaseDriverLoader;
import org.knime.workbench.core.KNIMECorePlugin;
import org.knime.workbench.core.preferences.HeadlessPreferencesConstants;

/**
 * Preference page used to load additional database drivers.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DatabasePreferencePage extends FieldEditorPreferencePage 
		implements IWorkbenchPreferencePage {

	/**
	 * 
	 */
	public DatabasePreferencePage() {
        super(GRID);
        setDescription("Load additional database driver files from Jar or Zip"
        		+ " archive.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void createFieldEditors() {
		final Shell shell = Display.getDefault().getActiveShell();
        addField(new ListEditor(HeadlessPreferencesConstants.P_DATABASE_DRIVERS,
        		"List of database driver files:", getFieldEditorParent()) {
 			@Override
			protected String[] parseString(final String string) {
				String[] strings = string.split(";");
				for (String str : strings) {
					try {
						DatabaseDriverLoader.loadDriver(new File(str));
					} catch (IOException ioe) {
					}
				}
				return strings;
			}
			
			@Override
			protected String getNewInputObject() {
				String fileName = new FileDialog(shell).open();
				try {
					DatabaseDriverLoader.loadDriver(new File(fileName));
					return fileName;
				} catch (IOException ioe) {
					return null;
				}
			}
			
			@Override
			protected String createList(final String[] string) {
				String res = "";
				for (int i = 0; i < string.length; i++) {
					if (i > 0) {
						res += ";";
					}
					res += string[i]; 
				}
				return res;
			}
		});
	}
	
    /**
     * {@inheritDoc}
     */
    public void init(final IWorkbench workbench) {
        IPreferenceStore corePrefStore =
            KNIMECorePlugin.getDefault().getPreferenceStore();
        setPreferenceStore(corePrefStore);
    }

}
