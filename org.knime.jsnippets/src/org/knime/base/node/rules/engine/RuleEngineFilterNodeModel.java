/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
package org.knime.base.node.rules.engine;

import java.text.ParseException;
import java.util.List;

import org.knime.base.node.rules.engine.Condition.MatchOutcome.MatchState;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.container.RowAppender;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;

/**
 * This is the model for the business rule node. It takes the user-defined rules and assigns the row to the first or the
 * second outport (the second can be dummy).
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.8
 */
public class RuleEngineFilterNodeModel extends RuleEngineNodeModel {
    /** Configuration key for include on match parameter. */
    static final String CFGKEY_INCLUDE_ON_MATCH = "include";

    /** Default value for the include on match parameter. */
    static final boolean DEFAULT_INCLUDE_ON_MATCH = true;

    private SettingsModelBoolean m_includeOnMatch = new SettingsModelBoolean(CFGKEY_INCLUDE_ON_MATCH,
            DEFAULT_INCLUDE_ON_MATCH);

    /**
     * Creates a new model.
     *
     * @param filter Filter or splitter? {@code true} -> filter, {@code false} -> splitter.
     */
    public RuleEngineFilterNodeModel(final boolean filter) {
        super(1, filter ? 1 : 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        try {
            parseRules(inSpecs[0], RuleNodeSettings.RuleFilter);
        } catch (ParseException ex) {
            throw new InvalidSettingsException(ex.getMessage(), ex);
        }
        final DataTableSpec[] ret = new DataTableSpec[getNrOutPorts()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = inSpecs[0];
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        final List<Rule> rules = parseRules(inData[0].getDataTableSpec(), RuleNodeSettings.RuleFilter);
        final BufferedDataContainer first = exec.createDataContainer(inData[0].getDataTableSpec(), true);
        final int nrOutPorts = getNrOutPorts();
        final RowAppender second =
                nrOutPorts > 1 ? exec.createDataContainer(inData[0].getDataTableSpec(), true) : new RowAppender() {
                    @Override
                    public void addRowToTable(final DataRow row) {
                        //do nothing
                    }
                };
        final RowAppender[] containers = new RowAppender[]{first, second};
        final int matchIndex = m_includeOnMatch.getBooleanValue() ? 0 : 1;
        final int otherIndex = 1 - matchIndex;

        final BufferedDataTable[] ret = new BufferedDataTable[nrOutPorts];
        try {
            final int[] rowIdx = new int[]{0};
            final int rows = inData[0].getRowCount();
            final VariableProvider provider = new VariableProvider() {
                @Override
                public Object readVariable(final String name, final Class<?> type) {
                    return RuleEngineFilterNodeModel.this.readVariable(name, type);
                }

                @Override
                public int getRowCount() {
                    return rows;
                }

                @Override
                public int getRowIndex() {
                    return rowIdx[0];
                }
            };
            for (DataRow row : inData[0]) {
                rowIdx[0]++;
                exec.setProgress(rowIdx[0] / (double)rows, "Adding row " + rowIdx[0] + " of " + rows);
                exec.checkCanceled();
                boolean wasMatch = false;
                for (Rule r : rules) {
                    if (r.getCondition().matches(row, provider).getOutcome() == MatchState.matchedAndStop) {
                        //                        r.getSideEffect().perform(row, provider);
                        DataValue value = r.getOutcome().getComputedResult(row, provider);
                        if (value instanceof BooleanValue) {
                            final BooleanValue bv = (BooleanValue)value;
                            containers[bv.getBooleanValue() ? matchIndex : otherIndex].addRowToTable(row);
                        } else {
                            containers[matchIndex].addRowToTable(row);
                        }
                        wasMatch = true;
                        break;
                    }
                }
                if (!wasMatch) {
                    containers[otherIndex].addRowToTable(row);
                }
            }
        } finally {
            first.close();
            ret[0] = first.getTable();
            if (second instanceof BufferedDataContainer) {
                BufferedDataContainer container = (BufferedDataContainer)second;
                container.close();
                ret[1] = container.getTable();
            }
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_includeOnMatch.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_includeOnMatch.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_includeOnMatch.validateSettings(settings);
    }
}
