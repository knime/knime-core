/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * --------------------------------------------------------------------- *
 * 
 */
package org.knime.base.node.mine.bfn;

import java.util.Set;

import javax.swing.BorderFactory;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnFilterPanel;

/**
 * Abstract dialog pane used showing a column filter panel for class column
 * selected and a panel for general learner options.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class BasisFunctionLearnerNodeDialogPane 
        extends NodeDialogPane {

    /** Contains the basic settings for this learner. */
    private final BasisFunctionLearnerNodeDialogPanel m_basicsPanel;

    /** Select target column with class-label. */
    private final ColumnFilterPanel m_targetColumns;

    /**
     * Creates a new pane with basics and column filter panel.
     */
    @SuppressWarnings("unchecked")
    protected BasisFunctionLearnerNodeDialogPane() {
        // panel with model specific settings
        m_basicsPanel = new BasisFunctionLearnerNodeDialogPanel();
        super.addTab(m_basicsPanel.getName(), m_basicsPanel);
       
        // target columns
        m_targetColumns = new ColumnFilterPanel();
        m_targetColumns.setBorder(BorderFactory
                .createTitledBorder(" Target columns "));
        super.addTab("Target Columns", m_targetColumns);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        DataTableSpec[] inSpecs = new DataTableSpec[specs.length];
        for (int i = 0; i < inSpecs.length; i++) {
            inSpecs[i] = (DataTableSpec) specs[i];
        }
        // update settings of basic tab
        m_basicsPanel.loadSettingsFrom(settings, inSpecs);
        // update target columns
        setTargetColumns(inSpecs[0], settings.getStringArray(
                BasisFunctionLearnerNodeModel.TARGET_COLUMNS, new String[0]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        // save settings from basic tab
        m_basicsPanel.saveSettingsTo(settings);
        String[] targetColumns = getTargetColumns();
        if (targetColumns == null || targetColumns.length == 0) {
            throw new InvalidSettingsException("No target column specified.");
        }
        // set target columns
        settings.addStringArray(BasisFunctionLearnerNodeModel.TARGET_COLUMNS,
                targetColumns);

    }

    private String[] getTargetColumns() {
        Set<String> set = m_targetColumns.getIncludedColumnSet();
        return set.toArray(new String[0]);
    }

    /**
     * Sets a new list of target column name using the input spec.
     * 
     * @param target the target column to select
     * @param spec the spec to retrieve column names from
     * @throws NotConfigurableException if the spec is <code>null</code> or
     *             contains no columns
     */
    private void setTargetColumns(final DataTableSpec spec,
            final String... target) throws NotConfigurableException {
        if (spec == null || spec.getNumColumns() == 0) {
            throw new NotConfigurableException("No data spec found");
        }
        if (target == null || target.length == 0) {
            m_targetColumns.update(spec, false, spec.getColumnSpec(
                    spec.getNumColumns() - 1).getName());
        } else {
            m_targetColumns.update(spec, false, target);
        }
    }

}
