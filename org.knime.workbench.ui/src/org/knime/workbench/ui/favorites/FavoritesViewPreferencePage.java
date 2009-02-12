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
 * ---------------------------------------------------------------------
 * 
 * History
 *   18.03.2008 (Fabian Dill): created
 */
package org.knime.workbench.ui.favorites;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.workbench.repository.NodeUsageRegistry;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class FavoritesViewPreferencePage extends FieldEditorPreferencePage
        implements IWorkbenchPreferencePage {

    private IntegerFieldEditor m_freqHistorySizeEditor;
    private IntegerFieldEditor m_usedHistorySizeEditor; 

    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void createFieldEditors() {
        Composite parent = getFieldEditorParent();
        m_freqHistorySizeEditor = new IntegerFieldEditor(
                PreferenceConstants.P_FAV_FREQUENCY_HISTORY_SIZE,
                "Maximal size for most frequently used nodes", parent, 3);
        m_freqHistorySizeEditor.setValidRange(1, 50);
        m_freqHistorySizeEditor.setTextLimit(3);
        m_freqHistorySizeEditor.load();
        
        m_usedHistorySizeEditor = new IntegerFieldEditor(
                PreferenceConstants.P_FAV_LAST_USED_SIZE,
                "Maximal size for last used nodes", parent, 3);
        m_usedHistorySizeEditor.setValidRange(1, 50);
        m_usedHistorySizeEditor.setTextLimit(3);
        m_usedHistorySizeEditor.load();
        
        addField(m_usedHistorySizeEditor);
        addField(m_freqHistorySizeEditor);

    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean performOk() {
        getPreferenceStore().setValue(
                PreferenceConstants.P_FAV_FREQUENCY_HISTORY_SIZE, 
                m_freqHistorySizeEditor.getIntValue());
        getPreferenceStore().setValue(
                PreferenceConstants.P_FAV_LAST_USED_SIZE, 
                m_usedHistorySizeEditor.getIntValue());
        
        NodeUsageRegistry.setMaxFrequentSize(
                m_freqHistorySizeEditor.getIntValue());
        
        NodeUsageRegistry.setMaxLastUsedSize(
                m_usedHistorySizeEditor.getIntValue());
        return true;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected void performApply() {
        performOk();
    }

    /**
     * {@inheritDoc}
     */
    public void init(final IWorkbench workbench) {
        setPreferenceStore(KNIMEUIPlugin.getDefault().getPreferenceStore());
    }

}
