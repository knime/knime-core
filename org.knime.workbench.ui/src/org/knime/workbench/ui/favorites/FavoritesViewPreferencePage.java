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
