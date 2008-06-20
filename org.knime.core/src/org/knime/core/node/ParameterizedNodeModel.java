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
 *   Jun 19, 2008 (wiswedel): created
 */
package org.knime.core.node;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.StringValue;
import org.knime.core.node.workflow.LoopStartNode;
import org.knime.core.node.workflow.ScopeObjectStack;
import org.knime.core.node.workflow.ScopeVariable;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
class ParameterizedNodeModel<T extends GenericNodeModel> 
    extends GenericNodeModel implements LoopStartNode {

    static final String SCOPEVARIABLE_NAME = "isLastIteration"; 
    
    private final T m_delegate;

    private DataTableSpec m_variablesSpec;
    private RowIterator m_variablesIterator;
    private DataRow m_currentVariables;

    /**
     * 
     */
    public ParameterizedNodeModel(final T delegate) {
        super(getInPortTypes(delegate), getOutPortTypes(delegate));
        m_delegate = delegate;
    }
    
    public T getDelegate() {
        return m_delegate;
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        PortObjectSpec[] sub = Arrays.copyOfRange(inSpecs, 1, inSpecs.length);
        resetIfNecessary((DataTableSpec)inSpecs[0]);
        pushVariables(getScopeContextStackContainer());
        return m_delegate.configureModel(sub);
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable variables = (BufferedDataTable)inData[0];
        resetIfNecessary(variables.getDataTableSpec());
        if (m_variablesIterator == null) {
            m_variablesIterator = variables.iterator();
        }
        if (!m_variablesIterator.hasNext()) {
            throw new Exception("No more iterations (variables table has "
                    + variables.getRowCount() + " variable sets, i.e. rows)");
        }
        m_currentVariables = m_variablesIterator.next();
        pushVariables(getScopeContextStackContainer());
        PortObject[] sub = Arrays.copyOfRange(inData, 1, inData.length);
        return m_delegate.executeModel(sub, exec);
    }

    private void pushVariables(final ScopeObjectStack stack) {
        if (m_variablesSpec == null) {
            return;
        }
        boolean isLastIteration;
        if (m_currentVariables == null) {
            isLastIteration = false;
        } else {
            isLastIteration = m_variablesIterator != null 
            && m_variablesIterator.hasNext();
        }
        for (int i = 0; i < m_variablesSpec.getNumColumns(); i++) {
            DataColumnSpec spec = m_variablesSpec.getColumnSpec(i);
            DataType type = spec.getType();
            String name = spec.getName();
            DataCell cell = m_currentVariables == null 
                ? null : m_currentVariables.getCell(i);
            if (type.isCompatible(IntValue.class)) {
                if (cell == null) {
                    stack.push(new ScopeVariable(name, 0));
                } else if (!cell.isMissing()) {
                    stack.push(new ScopeVariable(
                            name, ((IntValue)cell).getIntValue()));
                }
            } else if (type.isCompatible(DoubleValue.class)) {
                if (cell == null) {
                    stack.push(new ScopeVariable(name, 0.0));
                } else if (!cell.isMissing()) {
                    stack.push(new ScopeVariable(
                            name, ((DoubleValue)cell).getDoubleValue()));
                }
            } else if (type.isCompatible(StringValue.class)) {
                if (cell == null) {
                    stack.push(new ScopeVariable(name, ""));
                } else if (!cell.isMissing()) {
                    stack.push(new ScopeVariable(
                            name, ((StringValue)cell).getStringValue()));
                }
            }
        }
        stack.push(new ScopeVariable(SCOPEVARIABLE_NAME, 
                Boolean.toString(isLastIteration)));
    }

    private void resetIfNecessary(final DataTableSpec variablesSpec) {
        if (!variablesSpec.equalStructure(m_variablesSpec)) {
            m_variablesSpec = variablesSpec;
            m_variablesIterator = null;
            m_currentVariables = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        m_delegate.resetModel();
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_delegate.validateSettings(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_delegate.loadValidatedSettingsFrom(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_delegate.saveSettingsTo(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        m_delegate.loadInternals(nodeInternDir, exec);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        m_delegate.saveInternals(nodeInternDir, exec);
    }

    /** {@inheritDoc} */
    @Override
    public void addWarningListener(final NodeModelWarningListener listener) {
        m_delegate.addWarningListener(listener);
    }

    /** {@inheritDoc} */
    @Override
    public void removeWarningListener(final NodeModelWarningListener listener) {
        m_delegate.removeWarningListener(listener);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyWarningListeners(final String warning) {
        m_delegate.notifyWarningListeners(warning);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        return m_delegate.equals(obj);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_delegate.hashCode();
    }

    private static final PortType[] getInPortTypes(final GenericNodeModel d) {
        PortType[] result = new PortType[d.getNrInPorts() + 1];
        result[0] = BufferedDataTable.TYPE;
        for (int i = 0; i < result.length - 1; i++) {
            result[i + 1] = d.getInPortType(i);
        }
        return result;
    }

    private static final PortType[] getOutPortTypes(final GenericNodeModel d) {
        PortType[] result = new PortType[d.getNrOutPorts()];
        for (int i = 0; i < result.length; i++) {
            result[i] = d.getOutPortType(i);
        }
        return result;
    }
    
    /** {@inheritDoc} */
    @Override
    void setScopeContextStackContainer(final ScopeObjectStack scsc) {
        pushVariables(scsc);
        m_delegate.setScopeContextStackContainer(scsc);
    }
    
    /** {@inheritDoc} */
    @Override
    ScopeObjectStack getScopeContextStackContainer() {
        return m_delegate.getScopeContextStackContainer();
    }

}
