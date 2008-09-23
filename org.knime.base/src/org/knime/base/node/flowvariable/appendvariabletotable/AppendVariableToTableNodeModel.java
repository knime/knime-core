/*
 * ------------------------------------------------------------------ *
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
 *   May 1, 2008 (wiswedel): created
 */
package org.knime.base.node.flowvariable.appendvariabletotable;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.workflow.ScopeVariable;
import org.knime.core.util.Pair;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class AppendVariableToTableNodeModel extends NodeModel {

    private final AppendVariableToTableSettings m_settings;
    
    /** One input, one output. */
    public AppendVariableToTableNodeModel() {
        super(new PortType[]{FlowVariablePortObject.TYPE,
                BufferedDataTable.TYPE},
              new PortType[]{BufferedDataTable.TYPE});
        m_settings = new AppendVariableToTableSettings();
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable t = (BufferedDataTable)inData[1];
        DataTableSpec ts = t.getSpec();
        ColumnRearranger ar = createColumnRearranger(ts);
        BufferedDataTable out = exec.createColumnRearrangeTable(t, ar, exec);
        return new BufferedDataTable[]{out};
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        ColumnRearranger ar = createColumnRearranger((DataTableSpec)inSpecs[1]);
        return new DataTableSpec[]{ar.createSpec()};
    }
    
    private ColumnRearranger createColumnRearranger(final DataTableSpec spec) 
        throws InvalidSettingsException {
        ColumnRearranger arranger = new ColumnRearranger(spec);
        Set<String> nameHash = new HashSet<String>();
        for (DataColumnSpec c : spec) {
            nameHash.add(c.getName());
        }
        List<Pair<String, ScopeVariable.Type>> vars = 
            m_settings.getVariablesOfInterest();
        if (vars.isEmpty()) {
            throw new InvalidSettingsException("No variables selected");
        }
        DataColumnSpec[] specs = new DataColumnSpec[vars.size()];
        final DataCell[] values = new DataCell[vars.size()];
        for (int i = 0; i < vars.size(); i++) {
            Pair<String, ScopeVariable.Type> c = vars.get(i);
            String name = c.getFirst();
            DataType type;
            switch (c.getSecond()) {
            case DOUBLE:
                type = DoubleCell.TYPE;
                try {
                    double dValue = peekScopeVariableDouble(name);
                    values[i] = new DoubleCell(dValue);
                } catch (NoSuchElementException e) {
                    throw new InvalidSettingsException(
                            "No such flow variable (of type double): " + name);
                }
                break;
            case INTEGER:
                type = IntCell.TYPE;
                try {
                    int iValue = peekScopeVariableInt(name);
                    values[i] = new IntCell(iValue);
                } catch (NoSuchElementException e) {
                    throw new InvalidSettingsException(
                            "No such flow variable (of type int): " + name);
                }
                break;
            case STRING:
                type = StringCell.TYPE;
                try {
                    String sValue = peekScopeVariableString(name);
                    sValue = sValue == null ? "" : sValue;
                    values[i] = new StringCell(sValue);
                } catch (NoSuchElementException e) {
                    throw new InvalidSettingsException(
                            "No such flow variable (of type String): " + name);
                }
                break;
            default:
                throw new InvalidSettingsException("Unsupported variable type: "
                        + c.getSecond());
            }
            if (nameHash.contains(name) 
                    && !name.toLowerCase().endsWith("(variable)")) {
                name = name.concat(" (variable)");
            }
            String newName = name;
            int uniquifier = 1; 
            while (!nameHash.add(newName)) {
                newName = name + " (#" + (uniquifier++) + ")"; 
            }
            specs[i] = new DataColumnSpecCreator(newName, type).createSpec();
        }
        arranger.append(new AbstractCellFactory(specs) {
            /** {@inheritDoc} */
            @Override
            public DataCell[] getCells(final DataRow row) {
                return values;
            }
        });
        return arranger;
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new AppendVariableToTableSettings().loadSettingsFrom(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

}
