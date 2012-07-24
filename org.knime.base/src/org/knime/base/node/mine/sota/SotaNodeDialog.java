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
 *   Nov 16, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import org.knime.base.node.mine.sota.logic.SotaUtil;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;


/**
 *
 * @author Kilian Thiel, University of Konstanz
 */
public class SotaNodeDialog extends NodeDialogPane {
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(SotaNodeDialog.class);

    private SotaSettingsPanel m_settings;
    private SotaHierarchicalFuzzySettings m_hierarchicalFuzzyDataSettings;

    /**
     * Constructor of SotaNodedialog.
     * Creates new instance of SotaNodeDialog.
     */
    public SotaNodeDialog() {
        super();

        m_settings = new SotaSettingsPanel(LOGGER);
        JPanel outerSettingsPanel = new JPanel();
        outerSettingsPanel.add(m_settings);
        outerSettingsPanel.setBorder(new EtchedBorder());

        m_hierarchicalFuzzyDataSettings =
            new SotaHierarchicalFuzzySettings(LOGGER);
        JPanel outerFuzzyPanel = new JPanel();
        outerFuzzyPanel.add(m_hierarchicalFuzzyDataSettings);
        outerFuzzyPanel.setBorder(new EtchedBorder());

        JPanel jp = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 10;
        c.weighty = 10;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(10, 10, 10, 10);
        jp.add(outerSettingsPanel, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 10;
        c.weighty = 10;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(10, 10, 10, 10);
        jp.add(outerFuzzyPanel, c);

        addTab("Settings", jp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        assert (settings != null && specs != null);

        int numberCells = 0;
        int fuzzyCells = 0;
        for (int i = 0; i < specs[SotaNodeModel.INPORT].getNumColumns();
            i++) {
                DataType type = specs[SotaNodeModel.INPORT].getColumnSpec(i)
                        .getType();

                if (SotaUtil.isNumberType(type)) {
                    numberCells++;
                } else if (SotaUtil.isFuzzyIntervalType(type)) {
                    fuzzyCells++;
                }
        }

        StringBuffer buffer = new StringBuffer();
        if (numberCells <= 0 && fuzzyCells <= 0) {
            buffer.append("No FuzzyIntervalCells or NumberCells found !"
                    + " Is the node fully connected ?");
        }

        // if buffer throw exception
        if (buffer.length() > 0) {
            throw new NotConfigurableException(buffer.toString());
        }

        m_settings.loadSettingsFrom(settings, specs);
        m_hierarchicalFuzzyDataSettings.loadSettingsFrom(settings, specs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        assert (settings != null);
        m_settings.saveSettingsTo(settings);
        m_hierarchicalFuzzyDataSettings.saveSettingsTo(settings);
    }
}
