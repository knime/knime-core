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
 */
package org.knime.base.node.preproc.split;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

import org.knime.base.node.util.FilterColumnPanel;

/**
 * Dialog with a column filter which is used to define the split of the columns.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class SplitNodeDialog extends NodeDialogPane {
    private final FilterColumnPanel m_filterPanel;

    /**
     * Creates new dialog.
     */
    public SplitNodeDialog() {
        m_filterPanel = new FilterColumnPanel();
        m_filterPanel.setIncludeTitle(" Top ");
        m_filterPanel.setExcludeTitle(" Bottom ");
        m_filterPanel.setRemoveAllButtonText(">>");
        m_filterPanel.setRemoveButtonText(">");
        m_filterPanel.setAddAllButtonText("<<");
        m_filterPanel.setAddButtonText("<");
        addTab("Settings", m_filterPanel);
    }

    /**
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        if (specs[0].getNumColumns() == 0) {
            throw new NotConfigurableException(
                    "No columns available for selection.");
        }
        String[] includes = null;
        includes = settings.getStringArray(SplitNodeModel.CFG_TOP,
                new String[0]);
        // we don't use the CFG_BOTTOM here as the remainder of the columns
        // must be the bottom table list.
        m_filterPanel.update(specs[0], false, includes);
    }

    /**
     * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        String[] top = m_filterPanel.getIncludedColumnList().toArray(
                new String[0]);
        String[] bottom = m_filterPanel.getExcludedColumnList().toArray(
                new String[0]);
        settings.addStringArray(SplitNodeModel.CFG_TOP, top);
        settings.addStringArray(SplitNodeModel.CFG_BOTTOM, bottom);
    }
}
