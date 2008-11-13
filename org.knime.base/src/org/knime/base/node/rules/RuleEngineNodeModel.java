/* ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 *
 * History
 *   11.04.2008 (thor): created
 */
package org.knime.base.node.rules;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This is the model for the business rule node. It takes the user-defined rules
 * and assigns the outcome of the first matching rule to the new cell.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class RuleEngineNodeModel extends NodeModel {
    private final RuleEngineSettings m_settings = new RuleEngineSettings();

    /**
     * Creates a new model.
     */
    public RuleEngineNodeModel() {
        super(1, 1);
    }

    /**
     * Parses all rules in the settings object.
     *
     * @param spec the spec of the table on which the rules are applied.
     * @return a list of parsed rules
     * @throws ParseException if a rule cannot be parsed
     */
    private List<Rule> parseRules(final DataTableSpec spec)
            throws ParseException {
        ArrayList<Rule> rules = new ArrayList<Rule>();

        for (String s : m_settings.rules()) {
            rules.add(new Rule(s, spec));
        }

        return rules;
    }

    private ColumnRearranger createRearranger(final DataTableSpec inSpec,
            final List<Rule> rules) {
        ColumnRearranger crea = new ColumnRearranger(inSpec);

        String newColName =
                DataTableSpec.getUniqueColumnName(inSpec, m_settings
                        .getNewColName());
        m_settings.setNewcolName(newColName);
        DataColumnSpec cs =
                new DataColumnSpecCreator(newColName, StringCell.TYPE)
                        .createSpec();

        crea.append(new SingleCellFactory(cs) {
            @Override
            public DataCell getCell(final DataRow row) {
                for (Rule r : rules) {
                    if (r.matches(row)) {
                        return new StringCell(r.getOutcome());
                    }
                }

                return new StringCell(m_settings.getDefaultLabel());
            }
        });

        return crea;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        try {
            parseRules(inSpecs[0]);
        } catch (ParseException ex) {
            throw new InvalidSettingsException(ex);
        }

        ColumnRearranger crea = createRearranger(inSpecs[0], null);

        return new DataTableSpec[]{crea.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        List<Rule> rules = parseRules(inData[0].getDataTableSpec());
        ColumnRearranger crea =
                createRearranger(inData[0].getDataTableSpec(), rules);

        return new BufferedDataTable[]{exec.createColumnRearrangeTable(
                inData[0], crea, exec)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        RuleEngineSettings s = new RuleEngineSettings();
        s.loadSettings(settings);
    }
}
