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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Apr 28, 2008 (wiswedel): created
 */
package org.knime.base.node.flowvariable.tabletovariable;

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
import org.knime.core.node.GenericNodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class TableToVariableNodeModel extends GenericNodeModel {

    /** Two inputs, one output..  */
    protected TableToVariableNodeModel() {
        super(new PortType[]{
                new PortType(PortObject.class), BufferedDataTable.TYPE},
                new PortType[]{new PortType(PortObject.class)});
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        pushVariables((DataTableSpec)inSpecs[1], null);
        return new PortObjectSpec[]{inSpecs[0]};
    }
    
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
                cell = defaultRow.getCell(i);
            }
            if (type.isCompatible(IntValue.class)) {
                pushScopeVariableInt(name, ((IntValue)cell).getIntValue());
            } else if (type.isCompatible(DoubleValue.class)) {
                pushScopeVariableDouble(
                        name, ((DoubleValue)cell).getDoubleValue());
            } else if (type.isCompatible(StringValue.class)) {
                pushScopeVariableString(
                        name, ((StringValue)cell).getStringValue());
            }
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable variables = (BufferedDataTable)inData[1];
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
        return new PortObject[]{inData[0]};
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
