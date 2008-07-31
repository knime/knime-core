/* 
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 * 
 */
package org.knime.base.node.mine.bfn;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.GenericNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.PortObjectSpec;
import org.knime.core.node.util.ColumnFilterPanel;

/**
 * Abstract dialog pane used showing a column filter panel for class column
 * selected and a panel for general learner options.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class BasisFunctionLearnerNodeDialogPane 
        extends GenericNodeDialogPane {

    /** Contains the basic settings for this learner. */
    private final BasisFunctionLearnerNodeDialogPanel m_basicsPanel;

    /** Select target column with class-label. */
    private final ColumnFilterPanel m_dataColumns;

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

        // data columns
        m_dataColumns = new ColumnFilterPanel(DoubleValue.class);
        m_dataColumns.setBorder(BorderFactory
                .createTitledBorder(" Data columns "));
        super.addTab("Data Columns", m_dataColumns);

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
        // update data columns
        setDataColumns(inSpecs[0], settings.getStringArray(
                BasisFunctionLearnerNodeModel.DATA_COLUMNS, new String[0]));
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
        // check data vs. target columns
        String[] dataColumns = getDataColumns();
        if (dataColumns == null || dataColumns.length == 0) {
            throw new InvalidSettingsException("No data column specified.");
        }
        HashSet<String> hash = new HashSet<String>(Arrays.asList(dataColumns));
        String[] targetColumns = getTargetColumns();
        if (targetColumns == null || targetColumns.length == 0) {
            throw new InvalidSettingsException("No target column specified.");
        }
        for (String target : targetColumns) {
            if (hash.contains(target)) {
                throw new InvalidSettingsException(
                        "Target and data columns overlap in: " 
                        + Arrays.toString(targetColumns));
            }
        }
        // set data columns
        settings.addStringArray(BasisFunctionLearnerNodeModel.DATA_COLUMNS,
                dataColumns);
        // set target columns
        settings.addStringArray(BasisFunctionLearnerNodeModel.TARGET_COLUMNS,
                targetColumns);

    }

    private String[] getDataColumns() {
        Set<String> set = m_dataColumns.getIncludedColumnSet();
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
    private void setDataColumns(final DataTableSpec spec, final String... data)
            throws NotConfigurableException {
        if (spec == null || spec.getNumColumns() == 0) {
            throw new NotConfigurableException("No data spec found");
        }
        if (data == null || data.length == 0) {
            m_dataColumns.update(spec, true); // all in incl
        } else { 
            m_dataColumns.update(spec, false, data); // data in incl
        }
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
