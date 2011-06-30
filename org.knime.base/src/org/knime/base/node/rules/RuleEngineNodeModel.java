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
 *   11.04.2008 (thor): created
 */
package org.knime.base.node.rules;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.knime.base.node.rules.Rule.ColumnReference;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
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
            final List<Rule> rules) throws InvalidSettingsException {
        ColumnRearranger crea = new ColumnRearranger(inSpec);

        String newColName =
                DataTableSpec.getUniqueColumnName(inSpec,
                        m_settings.getNewColName());
        m_settings.setNewcolName(newColName);

        final int defaultLabelColumnIndex;
        if (m_settings.getDefaultLabelIsColumn()) {
            if (m_settings.getDefaultLabel().length() < 3) {
                throw new InvalidSettingsException(
                        "Default label is not a column reference");
            }

            if (!m_settings.getDefaultLabel().startsWith("$")
                    || !m_settings.getDefaultLabel().endsWith("$")) {
                throw new InvalidSettingsException(
                        "Column references in default label must be enclosed in $");
            }
            String colRef =
                    m_settings.getDefaultLabel().substring(1,
                            m_settings.getDefaultLabel().length() - 1);
            defaultLabelColumnIndex = inSpec.findColumnIndex(colRef);
            if (defaultLabelColumnIndex == -1) {
                throw new InvalidSettingsException("Column '"
                        + m_settings.getDefaultLabel()
                        + "' for default label does not exist in input table");
            }
        } else {
            defaultLabelColumnIndex = -1;
        }

        // determine output type
        List<DataType> types = new ArrayList<DataType>();
        // add outcome column types
        for (Rule r : rules) {
            if (r.getOutcome() instanceof ColumnReference) {
                types.add(((ColumnReference)r.getOutcome()).spec.getType());
            } else if (r.getOutcome() instanceof Double) {
                types.add(DoubleCell.TYPE);
            } else if (r.getOutcome() instanceof Integer) {
                types.add(IntCell.TYPE);
            } else if (r.getOutcome().toString().length() > 0) {
                types.add(StringCell.TYPE);
            }
        }

        if (defaultLabelColumnIndex >= 0) {
            types.add(inSpec.getColumnSpec(defaultLabelColumnIndex).getType());
        } else if (m_settings.getDefaultLabel().length() > 0) {
            try {
                Integer.parseInt(m_settings.getDefaultLabel());
                types.add(IntCell.TYPE);
            } catch (NumberFormatException ex) {
                try {
                    Double.parseDouble(m_settings.getDefaultLabel());
                    types.add(DoubleCell.TYPE);
                } catch (NumberFormatException ex1) {
                    types.add(StringCell.TYPE);
                }
            }
        }
        final DataType outType;
        if (types.size() > 0) {
            DataType temp = types.get(0);
            for (int i = 1; i < types.size(); i++) {
                temp = DataType.getCommonSuperType(temp, types.get(i));
            }
            if ((temp.getValueClasses().size() == 1)
                    && temp.getValueClasses().contains(DataValue.class)) {
                // a non-native type, we replace it with string
                temp = StringCell.TYPE;
            }
            outType = temp;
        } else {
            outType = StringCell.TYPE;
        }

        DataColumnSpec cs =
                new DataColumnSpecCreator(newColName, outType).createSpec();

        crea.append(new SingleCellFactory(cs) {
            @Override
            public DataCell getCell(final DataRow row) {
                for (Rule r : rules) {
                    if (r.matches(row)) {
                        Object outcome = r.getOutcome();
                        if (outcome instanceof ColumnReference) {
                            DataCell cell =
                                    row.getCell(((ColumnReference)outcome).index);
                            if (outType.equals(StringCell.TYPE)
                                    && !cell.getType().equals(StringCell.TYPE)) {
                                return new StringCell(cell.toString());
                            } else {
                                return cell;
                            }
                        } else if (outType.equals(IntCell.TYPE)) {
                            return new IntCell((Integer)outcome);
                        } else if (outType.equals(DoubleCell.TYPE)) {
                            return new DoubleCell((Double)outcome);
                        } else {
                            return new StringCell(outcome.toString());
                        }
                    }
                }

                if (defaultLabelColumnIndex >= 0) {
                    DataCell cell = row.getCell(defaultLabelColumnIndex);
                    if (outType.equals(StringCell.TYPE)
                            && !cell.getType().equals(StringCell.TYPE)) {
                        return new StringCell(cell.toString());
                    } else {
                        return cell;
                    }
                } else if (m_settings.getDefaultLabel().length() > 0) {
                    String l = m_settings.getDefaultLabel();
                    if (outType.equals(StringCell.TYPE)) {
                        return new StringCell(l);
                    }

                    try {
                        int i = Integer.parseInt(l);
                        return new IntCell(i);
                    } catch (NumberFormatException ex) {
                        try {
                            double d = Double.parseDouble(l);
                            return new DoubleCell(d);
                        } catch (NumberFormatException ex1) {
                            return new StringCell(l);
                        }
                    }
                } else {
                    return DataType.getMissingCell();
                }
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
            ColumnRearranger crea =
                    createRearranger(inSpecs[0], parseRules(inSpecs[0]));
            return new DataTableSpec[]{crea.createSpec()};
        } catch (ParseException ex) {
            throw new InvalidSettingsException(ex);
        }
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
