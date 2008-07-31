/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.node.preproc.split;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilterPanel;


/**
 * Dialog with a column filter which is used to define the split of the columns.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class SplitNodeDialog extends NodeDialogPane {
    private final ColumnFilterPanel m_filterPanel;

    /**
     * Creates new dialog.
     */
    public SplitNodeDialog() {
        m_filterPanel = new ColumnFilterPanel();
        m_filterPanel.setIncludeTitle(" Bottom ");
        m_filterPanel.setExcludeTitle(" Top ");
        m_filterPanel.setRemoveAllButtonText("<<");
        m_filterPanel.setRemoveButtonText("<");
        m_filterPanel.setAddAllButtonText(">>");
        m_filterPanel.setAddButtonText(">");
        addTab("Settings", m_filterPanel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        if (specs[0].getNumColumns() == 0) {
            throw new NotConfigurableException(
                    "No columns available for selection.");
        }
        String[] includes = 
            settings.getStringArray(SplitNodeModel.CFG_TOP, new String[0]);
        
        // we don't use the CFG_BOTTOM here as the remainder of the columns
        // must be the bottom table list.
        m_filterPanel.update(specs[0], true, includes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        String[] bottom = m_filterPanel.getIncludedColumnSet().toArray(
                new String[0]);
        String[] top = m_filterPanel.getExcludedColumnSet().toArray(
                new String[0]);
        settings.addStringArray(SplitNodeModel.CFG_TOP, top);
        settings.addStringArray(SplitNodeModel.CFG_BOTTOM, bottom);
    }
}
