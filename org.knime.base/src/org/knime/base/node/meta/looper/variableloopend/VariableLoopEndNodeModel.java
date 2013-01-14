/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 * 
 * History
 *   31.05.2012 (kilian): created
 */
package org.knime.base.node.meta.looper.variableloopend;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.LoopStartNodeTerminator;

/**
 * Node model of the variable loop end node. The node creates for each loop
 * iteration one row with one column for each selected variable. In each
 * iteration the variable values will be set to the according columns. It is
 * possible to select no variables at all, than a data table with only row
 * keys will be created.
 * 
 * @author Kilian Thiel, KNIME.com, Berlin, Germany
 */
class VariableLoopEndNodeModel extends NodeModel implements LoopEndNode {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(VariableLoopEndNodeModel.class);
    
    private long m_startTime;

    private BufferedDataContainer m_resultContainer;

    private int m_count;
    

    /**
     * Creates new instance of <code>VariableLoopEndNodeModel</code>.
     */
    protected VariableLoopEndNodeModel() {
        super(new PortType[]{FlowVariablePortObject.TYPE},
              new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{createDataTableSpec()};
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, 
            final ExecutionContext exec)
            throws Exception {
        
        // check for loop start node.
        if (!(this.getLoopStartNode() instanceof LoopStartNodeTerminator)) {
            throw new IllegalStateException("Loop End is not connected"
                    + " to matching/corresponding Loop Start node. You"
                    + " are trying to create an infinite loop!");
        }
        DataTableSpec amendedSpec = createDataTableSpec();
        if (m_resultContainer == null) {
            // first time we are getting to this: open container
            m_startTime = System.currentTimeMillis();
            m_resultContainer = exec.createDataContainer(amendedSpec);
            
        // if initially created data table spec and current spec differ, fail
        } else if (!amendedSpec
                .equalStructure(m_resultContainer.getTableSpec())) {
            DataTableSpec predSpec = m_resultContainer.getTableSpec();
            StringBuilder error =
                    new StringBuilder(
                            "Input table's structure differs from reference "
                                    + "(first iteration) table: ");
            if (amendedSpec.getNumColumns() != predSpec.getNumColumns()) {
                error.append("different column counts ");
                error.append(amendedSpec.getNumColumns());
                error.append(" vs. ").append(predSpec.getNumColumns());
            } else {
                for (int i = 0; i < amendedSpec.getNumColumns(); i++) {
                    DataColumnSpec inCol = amendedSpec.getColumnSpec(i);
                    DataColumnSpec predCol = predSpec.getColumnSpec(i);
                    if (!inCol.equalStructure(predCol)) {
                        error.append("Column ").append(i).append(" [");
                        error.append(inCol).append("] vs. [");
                        error.append(predCol).append("]");
                    }
                }
            }
            throw new IllegalArgumentException(error.toString());
        }
        
        // after all data table checks we are fine now and can add a single row
        // containing the values of the flow variables
        m_resultContainer.addRowToTable(createNewRow());

        boolean terminateLoop =
                ((LoopStartNodeTerminator)this.getLoopStartNode())
                        .terminateLoop();
        if (terminateLoop) {
            // this was the last iteration - close container and continue
            m_resultContainer.close();
            BufferedDataTable outTable = m_resultContainer.getTable();
            m_resultContainer.close();
            m_resultContainer = null;
            m_count = 0;
            LOGGER.debug("Total loop execution time: "
                    + (System.currentTimeMillis() - m_startTime) + "ms");
            m_startTime = 0;
            return new BufferedDataTable[]{outTable};
        } else {
            continueLoop();
            m_count++;
            return new BufferedDataTable[1];
        }
    }

    /**
     * Creates and returns new instance of <code>DataRow</code> with the 
     * iteration count as row key and the specified and available flow vars as
     * columns.
     * @return <code>DataRow</code> with the iteration count as row key and 
     * the specified and available flow vars as columns.
     * @throws InvalidSettingsException If flow variable is of any not 
     * compatible type.
     */
    private DataRow createNewRow() throws InvalidSettingsException {
        // get available flow vars and selected vars
        Map<String, FlowVariable> availableFlowVars = 
            getAvailableFlowVariables();
        // for now take all variable names
        String[] flowVarNames = new String[availableFlowVars.size()];
        int j = 0;
        for (String name : availableFlowVars.keySet()) {
            flowVarNames[j] = name;
            j++;
        }
        
        // create a cell for each selected and available flow var
        DataCell[] cells = new DataCell[flowVarNames.length];
        if (flowVarNames.length > 0) {
            for (int i = 0; i < flowVarNames.length; i++) {
                String varName = flowVarNames[i];
                FlowVariable var = availableFlowVars.get(varName);
                
                // if flow var is available use flow var value
                if (var != null) {
                    cells[i] = getCompatibleDataCell(var);
                    
                // if flow var is not available insert missing cell 
                } else {
                    cells[i] = DataType.getMissingCell();
                }
            }
        }
        return new DefaultRow(RowKey.createRowKey(m_count), cells);
    }
    
    /**
     * Creates and returns a new instance of <code>DataTableSpec</code>. The
     * spec consists of one column for each specified and available flow 
     * variable. If any specified variable is not available an exception will
     * be thrown.
     * @return A new instance of <code>DataTableSpec</code>. The spec consists 
     * of one column for each specified and available flow variable.
     * @throws InvalidSettingsException If any specified flow variable is not
     * available.
     */
    private DataTableSpec createDataTableSpec() 
    throws InvalidSettingsException {
        // get available flow vars and selected vars
        Map<String, FlowVariable> availableFlowVars = 
            getAvailableFlowVariables();        
        // for now take all variable names
        String[] flowVarNames = new String[availableFlowVars.size()];
        int j = 0;
        for (String name : availableFlowVars.keySet()) {
            flowVarNames[j] = name;
            j++;
        }
        
        DataColumnSpec[] colSpecs = new DataColumnSpec[0];
        
        // create for each  specified flow variable a column spec, if available
        if (flowVarNames != null && flowVarNames.length > 0) {
            colSpecs = new DataColumnSpec[flowVarNames.length];
            for (int i = 0; i < flowVarNames.length; i++) {
                String varName = flowVarNames[i];
                FlowVariable var = availableFlowVars.get(varName);
                
                // if flow var is available create col spec for var type
                if (var != null) {
                    colSpecs[i] = new DataColumnSpecCreator(varName,
                            getCompatibleDataType(var.getType())).createSpec();
                    
                // if specified flow var is not available fail 
                } else {
                    throw new InvalidSettingsException(
                            "Specified flow variable " + varName
                                    + " is not available!");
                }
            }
        }
        return new DataTableSpec(colSpecs);
    }
    
    /**
     * Returns the compatible data type according to the given flow variable
     * type.
     * @param flowType The type of the flow variable to get the compatible
     * data type for.
     * @return The compatible data type according to the given flow variable
     * type.
     * @throws InvalidSettingsException If type of flow variable is not 
     * supported.
     */
    private static DataType getCompatibleDataType(
            final FlowVariable.Type flowType) throws InvalidSettingsException {
        DataType type;
        switch (flowType) {
        case DOUBLE:
            type = DoubleCell.TYPE;
            break;
        case INTEGER:
            type = IntCell.TYPE;
            break;
        case STRING:
            type = StringCell.TYPE;
            break;
        default:
            throw new InvalidSettingsException("Unsupported variable type: "
                    + flowType);
        }
        return type;
    }
    
    /**
     * Creates and returns an instance of <code>DataCell</code>. The cell
     * is of a compatible type according to the type of the given flow var
     * and the value of the cell is equal to the value of the flow var. 
     * 
     * @param var The flow variable to create a data cell for (with compatible 
     * type and equal value).
     * @return an instance of <code>DataCell</code> with compatible type to the
     * given flow var and equal value.
     * @throws InvalidSettingsException If given flow var is of any 
     * unsupported type.
     */
    private static DataCell getCompatibleDataCell(final FlowVariable var) 
    throws InvalidSettingsException {
        DataCell cell;
        FlowVariable.Type flowType = var.getType();
        switch (flowType) {
        case DOUBLE:
            cell = new DoubleCell(var.getDoubleValue());
            break;
        case INTEGER:
            cell = new IntCell(var.getIntValue());
            break;
        case STRING:
            cell = new StringCell(var.getStringValue());
            break;
        default:
            throw new InvalidSettingsException("Unsupported variable type: "
                    + flowType);
        }
        return cell;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // Nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // Nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // Nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_resultContainer = null;
        m_count = 0;
        m_startTime = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // Nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // Nothing to do ...
    }
}
