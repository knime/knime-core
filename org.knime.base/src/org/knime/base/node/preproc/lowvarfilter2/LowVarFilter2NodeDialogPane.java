/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   25.02.2007 (wiswedel): created
 */
package org.knime.base.node.preproc.lowvarfilter2;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilterPanel;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;
import org.knime.core.node.util.filter.column.DataTypeColumnFilter;

/**
 * Dialog for low variance filter node. Shows a double value chooser and a 
 * filter panel.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class LowVarFilter2NodeDialogPane extends NodeDialogPane {
    
    private final JSpinner m_varianceSpinner;
    
    /**
     * Using {@link DataColumnSpecFilterPanel} instead of deprecated
     * {@link ColumnFilterPanel}.
     */
    private final DataColumnSpecFilterPanel m_colFilterPanel;

    /** Inits GUI. */
    @SuppressWarnings("unchecked")
    public LowVarFilter2NodeDialogPane() {
        m_varianceSpinner = new JSpinner(new SpinnerNumberModel(
                0.0, 0.0, Double.POSITIVE_INFINITY, 0.1));
        m_varianceSpinner.setEditor(new JSpinner.NumberEditor(m_varianceSpinner,
                "0.0#########"));        
        JSpinner.DefaultEditor editor =
            (JSpinner.DefaultEditor)m_varianceSpinner.getEditor();
        editor.getTextField().setColumns(10);
        m_colFilterPanel = new DataColumnSpecFilterPanel(DoubleValue.class);
        JPanel p = new JPanel(new BorderLayout());
        JPanel northPanel = new JPanel(new FlowLayout());
        northPanel.add(new JLabel("Variance Upper Bound"));
        northPanel.add(m_varianceSpinner);
        p.add(northPanel, BorderLayout.NORTH);
        p.add(m_colFilterPanel, BorderLayout.CENTER);
        addTab("Options", p);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, 
            final DataTableSpec[] specs) throws NotConfigurableException {
        final DataTableSpec spec = specs[0];
        if (spec == null || spec.getNumColumns() == 0) {
            throw new NotConfigurableException("No columns available for "
                    + "selection.");
        }
        
        List<String> defIncludes = new ArrayList<String>();
        for (DataColumnSpec s : specs[0]) {
            if (s.getType().isCompatible(DoubleValue.class)) {
                defIncludes.add(s.getName());
            }
        }
        double threshold = settings.getDouble(
                LowVarFilter2NodeModel.CFG_KEY_MAX_VARIANCE, 0.0);
        m_varianceSpinner.setValue(threshold);

        DataColumnSpecFilterConfiguration config = createColFilterConf();
        config.loadConfigurationInDialog(settings, spec);
        m_colFilterPanel.loadConfiguration(config, spec);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) 
        throws InvalidSettingsException {
        double threshold = ((Number)m_varianceSpinner.getValue()).doubleValue();
        settings.addDouble(
                LowVarFilter2NodeModel.CFG_KEY_MAX_VARIANCE, threshold);
        
        DataColumnSpecFilterConfiguration config = createColFilterConf();
        m_colFilterPanel.saveConfiguration(config);
        config.saveConfiguration(settings);
    }

    /**
     * @return creates and returns configuration instance for column filter 
     * panel.
     */
    @SuppressWarnings("unchecked")
    private DataColumnSpecFilterConfiguration createColFilterConf() {
        return new DataColumnSpecFilterConfiguration(
                LowVarFilter2NodeModel.CFG_KEY_COL_FILTER,
                new DataTypeColumnFilter(DoubleValue.class));
    }
}
