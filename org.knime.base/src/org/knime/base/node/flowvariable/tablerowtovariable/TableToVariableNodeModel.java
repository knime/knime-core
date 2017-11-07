/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *   Apr 28, 2008 (wiswedel): created
 */
package org.knime.base.node.flowvariable.tablerowtovariable;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DefaultRow;
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
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;

/** The node model for the table row to variable node.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
public class TableToVariableNodeModel extends NodeModel {

    private final SettingsModelString m_onMV;

    private final SettingsModelInteger m_int;

    private final SettingsModelDouble m_double;

    private final SettingsModelString m_string;

    /** One in, one output. */
    protected TableToVariableNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{FlowVariablePortObject.TYPE});
        m_onMV = TableToVariableNodeDialog.getOnMissing();
        m_int = TableToVariableNodeDialog.getReplaceInteger(m_onMV);
        m_double = TableToVariableNodeDialog.getReplaceDouble(m_onMV);
        m_string = TableToVariableNodeDialog.getReplaceString(m_onMV);
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        pushVariables((DataTableSpec) inSpecs[0]);
        return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
    }

    private DataCell[] createDefaultCells(final DataTableSpec spec) {
        final DataCell[] cells = new DataCell[spec.getNumColumns()];
        for (int i = cells.length; --i >= 0;) {
            final DataColumnSpec c = spec.getColumnSpec(i);
            if (c.getType().isCompatible(IntValue.class)) {
                cells[i] = new IntCell(m_int.getIntValue());
            } else if (c.getType().isCompatible(DoubleValue.class)) {
                cells[i] = new DoubleCell(m_double.getDoubleValue());
            } else {
                cells[i] = new StringCell(m_string.getStringValue());
            }
        }
        return cells;
    }

    /** Pushes the default variables onto the stack; only used during configure.
     * @param variablesSpec The spec (for names and types)
     * @since 2.9
     */
    protected void pushVariables(final DataTableSpec variablesSpec) {
        if (m_onMV.getStringValue().equals(MissingValuePolicy.OMIT.getName())) {
            return;
        }
        final DefaultRow row = new DefaultRow(/* default value for RowID*/ "", createDefaultCells(variablesSpec));
        try {
            this.pushVariables(variablesSpec, row);
        } catch (Exception e) {
            // ignored
        }
    }


    /** Pushes the variable as given by the row argument onto the stack.
     * @param variablesSpec The spec (for names and types)
     * @param currentVariables The values of the variables.
     * @throws Exception if the node is supposed to fail on missing values or empty table
     */
    protected void pushVariables(final DataTableSpec variablesSpec, final DataRow currentVariables) throws Exception {
        // push also the rowID onto the stack
        final String rowIDVarName = "RowID";
        final boolean fail = m_onMV.getStringValue().equals(MissingValuePolicy.FAIL.getName());
        final boolean defaults = m_onMV.getStringValue().equals(MissingValuePolicy.DEFAULT.getName());
        pushFlowVariableString(rowIDVarName, currentVariables == null ? "" : currentVariables.getKey().getString());
        final DataCell[] defaultCells = createDefaultCells(variablesSpec);
        // column names starting with "knime." are uniquified as they represent global constants
        final HashSet<String> variableNames = new HashSet<String>();
        variableNames.add(rowIDVarName);
        final int colCount = variablesSpec.getNumColumns();
        for (int i = colCount; --i >= 0;) {
            DataColumnSpec spec = variablesSpec.getColumnSpec(i);
            DataType type = spec.getType();
            String name = spec.getName();
            if (name.equals("knime.")) {
                name = "column_" + i;
            } else if (name.startsWith("knime.")) {
                name = name.substring("knime.".length());
            }
            int uniquifier = 1;
            String basename = name;
            while (!variableNames.add(name)) {
                name = basename + "(#" + (uniquifier++) + ")";
            }
            final DataCell cell;
            if (currentVariables == null) {
                if (fail) {
                    throw new Exception("No rows in input table");
                } else if (defaults) {
                    cell = defaultCells[i];
                } else {
                    // omit
                    cell = null;
                }
            } else if (currentVariables.getCell(i).isMissing()) {
                if (fail) {
                    throw new Exception(String.format("Missing Values not allowed as variable values -- "
                            + "in row with ID \"%s\", column \"%s\" (index %d)",
                            currentVariables.getKey(), variablesSpec.getColumnSpec(i).getName(), i));
                } else if (defaults) {
                    cell = defaultCells[i];
                } else {
                    // omit
                    cell = null;
                }
            } else {
                // take the value from the input table row
                cell = currentVariables.getCell(i);
            }
            if (cell != null) {
                if (type.isCompatible(IntValue.class)) {
                    pushFlowVariableInt(name, ((IntValue) cell).getIntValue());
                } else if (type.isCompatible(DoubleValue.class)) {
                    pushFlowVariableDouble(name, ((DoubleValue) cell).getDoubleValue());
                } else if (type.isCompatible(StringValue.class)) {
                    pushFlowVariableString(name, ((StringValue) cell).getStringValue());
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final BufferedDataTable variables = (BufferedDataTable) inData[0];
        DataRow row = null;
        for (DataRow r : variables) {
            // take the first row
            row = r;
            break;
        }
        if (variables.size() > 1) {
            setWarningMessage("Table has " + variables.size() + " rows, ignored all rows except the first one");
        }
        pushVariables(variables.getDataTableSpec(), row);
        return new PortObject[]{FlowVariablePortObject.INSTANCE};
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        // no op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_onMV.saveSettingsTo(settings);
        m_int.saveSettingsTo(settings);
        m_double.saveSettingsTo(settings);
        m_string.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // new since 2.9
        if (settings.containsKey(m_string.getKey())) {
            m_onMV.loadSettingsFrom(settings);
            m_int.loadSettingsFrom(settings);
            m_double.loadSettingsFrom(settings);
            m_string.loadSettingsFrom(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // new since 2.9
        if (settings.containsKey(m_string.getKey())) {
            m_onMV.validateSettings(settings);
            m_int.validateSettings(settings);
            m_double.validateSettings(settings);
            m_string.validateSettings(settings);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
    }

}
