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
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class TableToVariableNodeModel extends NodeModel {

    /** One in, one output. */
    protected TableToVariableNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, 
                new PortType[]{FlowVariablePortObject.TYPE});
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        pushVariables((DataTableSpec)inSpecs[0], null);
        return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
    }
    
    /** Pushes the variable as given by the row argument onto the stack. 
     * @param variablesSpec The spec (for names and types)
     * @param currentVariables The values of the variables.
     */
    protected void pushVariables(final DataTableSpec variablesSpec,
            final DataRow currentVariables) {
        int colCount = variablesSpec.getNumColumns();
        DataCell[] cells = new DataCell[variablesSpec.getNumColumns()];
        for (int i = 0; i < cells.length; i++) {
            DataColumnSpec c = variablesSpec.getColumnSpec(i);
            if (c.getType().isCompatible(IntValue.class)) {
                cells[i] = new IntCell(0);
            } else if (c.getType().isCompatible(DoubleValue.class)) {
                cells[i] = new DoubleCell(0);
            } else {
                cells[i] = new StringCell("");
            }
        }
        DataRow defaultRow = new DefaultRow("Default Vars", cells);
        // column names starting with "knime." are uniquified as they represent
        // global constants. 
        HashSet<String> variableNames = new HashSet<String>();
        for (int i = 0; i < colCount; i++) {
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
            DataCell cell = currentVariables == null 
                ? defaultRow.getCell(i) : currentVariables.getCell(i);
            if (cell.isMissing()) {
                throw new IllegalArgumentException("Missing Values not"
                    + " allowed for Variable Values!");
            }
            if (type.isCompatible(IntValue.class)) {
                pushFlowVariableInt(name, ((IntValue)cell).getIntValue());
            } else if (type.isCompatible(DoubleValue.class)) {
                pushFlowVariableDouble(
                        name, ((DoubleValue)cell).getDoubleValue());
            } else if (type.isCompatible(StringValue.class)) {
                pushFlowVariableString(
                        name, ((StringValue)cell).getStringValue());
            }
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable variables = (BufferedDataTable)inData[0];
        DataRow row = null;
        for (DataRow r : variables) {
            row = r;
            break;
        }
        if (row == null) {
            throw new Exception("No rows in input table");
        }
        int rowCount = variables.getRowCount();
        if (rowCount > 1) {
            setWarningMessage("Table has " + rowCount + " rows, ignored all " 
                    + "rows except the first one");
        }
        pushVariables(variables.getDataTableSpec(), row);
        return new PortObject[]{new FlowVariablePortObject()};
    }
    
    /** {@inheritDoc} */
    @Override
    protected void reset() {
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

}
