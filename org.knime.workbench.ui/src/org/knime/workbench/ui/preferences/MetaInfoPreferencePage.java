/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
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
 */
package org.knime.workbench.ui.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.metainfo.model.MetaInfoFile;

/**
 * 
 * @author Fabian Dill, KNIME.com GmbH
 */
public class MetaInfoPreferencePage extends FieldEditorPreferencePage implements
        IWorkbenchPreferencePage {
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void createFieldEditors() {
        addField(new FileFieldEditor(
                MetaInfoFile.PREF_KEY_META_INFO_TEMPLATE_WF,
                "Meta Info Template for workflows:", true, 
                getFieldEditorParent()));
        
        addField(new FileFieldEditor(
                MetaInfoFile.PREF_KEY_META_INFO_TEMPLATE_WFS,
                "Meta Info Template for workflow sets:", true, 
                getFieldEditorParent()));        
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IWorkbench workbench) {
        IPreferenceStore prefStore = KNIMEUIPlugin.getDefault()
            .getPreferenceStore();
        prefStore.setDefault(MetaInfoFile.PREF_KEY_META_INFO_TEMPLATE_WF, "");
        prefStore.setDefault(MetaInfoFile.PREF_KEY_META_INFO_TEMPLATE_WFS, "");
        setPreferenceStore(prefStore);
    }

}
