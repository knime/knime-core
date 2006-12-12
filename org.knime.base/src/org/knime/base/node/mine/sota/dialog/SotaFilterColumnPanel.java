/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Nov 24, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota.dialog;

import java.util.Set;

import javax.swing.JPanel;

import org.knime.base.node.mine.sota.SotaConfigKeys;
import org.knime.base.node.mine.sota.SotaNodeModel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.FuzzyIntervalValue;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.ColumnFilterPanel;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class SotaFilterColumnPanel extends JPanel {
    private static final long serialVersionUID = 4053372436104609508L;

    private final ColumnFilterPanel m_filterPanel;

    /**
     * Constructor.
     */
    @SuppressWarnings("unchecked")
    public SotaFilterColumnPanel() {
        super();
        m_filterPanel = new ColumnFilterPanel(DoubleValue.class,
                FuzzyIntervalValue.class);
        this.add(m_filterPanel);
    }

    /**
     * Method loadSettingsFrom.
     * 
     * @param settings the NodeSettings object of the containing NodeDialogPane
     * @param specs the DataTableSpec[] of the containing NodeDialogPane
     */
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) {
        assert (settings != null && specs != null);

        String[] columns = settings.getStringArray(
                SotaConfigKeys.CFGKEY_EXCLUDE, new String[0]);

        m_filterPanel.update(specs[SotaNodeModel.INPORT], true, columns);
    }

    /**
     * Method saveSettingsTo.
     * 
     * @param settings the settings object where to save to
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        assert (settings != null);

        Set<String> listIn = m_filterPanel.getIncludedColumnSet();
        Set<String> listEx = m_filterPanel.getExcludedColumnSet();
        settings.addStringArray(SotaConfigKeys.CFGKEY_INCLUDE, listIn
                .toArray(new String[listIn.size()]));
        settings.addStringArray(SotaConfigKeys.CFGKEY_EXCLUDE, listEx
                .toArray(new String[listEx.size()]));
    }
}
