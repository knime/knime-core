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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   16.07.2007 (gabriel): created
 */
package org.knime.base.node.preproc.colcompare;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.preproc.colcompare.ColumnComparatorNodeDialogPane.ComparatorMethod;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
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
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import static org.knime.base.node.preproc.colcompare.ColumnComparatorNodeDialogPane.*;

/**
 * The comparator node model which compares two columns by it values within one
 * row, and appends a new column with the value of the first column if both are
 * equals, or a replacement value if not.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class ColumnComparatorNodeModel extends NodeModel {
    
    private final SettingsModelString m_operator = createComparatorMethod();
    private final SettingsModelString m_firstColumn = createFirstColumnModel();
    private final SettingsModelString m_secondColumn = 
        createSecondColumnModel();
    private final SettingsModelString m_matchOption = createMatchOption();
    private final SettingsModelString m_mismatchOption = createMismatchOption();
    private final SettingsModelString m_matchValue = createMatchValue();
    private final SettingsModelString m_mismatchValue = createMismatchValue();
    private final SettingsModelString m_newColumn = createNewColumnName();
    
    /**
     * Creates a new node model with one in- and outport.
     */
    public ColumnComparatorNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_firstColumn.getStringValue() == null 
                || m_secondColumn.getStringValue() == null) {
            throw new InvalidSettingsException(
                    "No column is selected for comparison.");
        }
        if (!inSpecs[0].containsName(m_firstColumn.getStringValue())) {
            throw new InvalidSettingsException("Column left not in spec: "
                    + m_firstColumn.getStringValue());
        }
        if (!inSpecs[0].containsName(m_secondColumn.getStringValue())) {
            throw new InvalidSettingsException("Column right not in spec: "
                    + m_secondColumn.getStringValue());
        }
        if (m_newColumn.getStringValue().trim().length() == 0) {
            throw new InvalidSettingsException("New column name must not be "
                    + "empty.");
        }
        if (inSpecs[0].containsName(m_newColumn.getStringValue())) {
            throw new InvalidSettingsException("New column name '"
                    + m_newColumn.getStringValue() + "' already exists "
                    + "in input data.");
        }
        DataColumnSpec leftSpec = inSpecs[0].getColumnSpec(
                m_firstColumn.getStringValue());
        DataColumnSpec rightSpec = inSpecs[0].getColumnSpec(
                m_secondColumn.getStringValue());
        ColumnRearranger colRe = new ColumnRearranger(inSpecs[0]);
        colRe.append(new SingleCellFactory(createSpec(leftSpec, rightSpec)) {
            @Override
            public DataCell getCell(final DataRow row) {
                return null;
            }
            
        });
        return new DataTableSpec[]{colRe.createSpec()};
    }

    private DataColumnSpec createSpec(final DataColumnSpec leftSpec,
            final DataColumnSpec rightSpec) {
        DataColumnSpecCreator newCSpec;
        String matchOp = m_matchOption.getStringValue();
        String mismatchOp = m_mismatchOption.getStringValue();
        if (matchOp.equals(REPL_OPTIONS[3]) // user defined
                || mismatchOp.equals(REPL_OPTIONS[3])) { // user defined
            newCSpec = new DataColumnSpecCreator(m_newColumn.getStringValue(),
                      StringCell.TYPE);
        } else {
            if (matchOp.equals(REPL_OPTIONS[2]) // missing
                    || mismatchOp.equals(REPL_OPTIONS[2])) { // missing
                if (matchOp.equals(REPL_OPTIONS[0]) // left value
                        || mismatchOp.equals(REPL_OPTIONS[0])) { // left value
                    newCSpec = new DataColumnSpecCreator(leftSpec);
                } else {
                    newCSpec = new DataColumnSpecCreator(rightSpec);
                }
                newCSpec.setName(m_newColumn.getStringValue());
            } else {
                DataType commonType = DataType.getCommonSuperType(
                        leftSpec.getType(), rightSpec.getType());
                newCSpec = new DataColumnSpecCreator(
                        m_newColumn.getStringValue(), commonType);
            }
        }
        return newCSpec.createSpec();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final ComparatorMethod method = ComparatorMethod.getMethod(
                m_operator.getStringValue());
        DataTableSpec spec = inData[0].getDataTableSpec();
        final int idx1 = spec.findColumnIndex(m_firstColumn.getStringValue());
        final int idx2 = spec.findColumnIndex(m_secondColumn.getStringValue());
        DataColumnSpec leftSpec = spec.getColumnSpec(idx1);
        DataColumnSpec rightSpec = spec.getColumnSpec(idx2);
        ColumnRearranger colRe = new ColumnRearranger(spec);
        colRe.append(new SingleCellFactory(createSpec(leftSpec, rightSpec)) {
            private final StringCell m_matchRepl = new StringCell(
                    m_matchValue.getStringValue());
            private final StringCell m_mismatchRepl = new StringCell(
                    m_mismatchValue.getStringValue());
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell1 = row.getCell(idx1);
                DataCell cell2 = row.getCell(idx2);
                if (method.compare(cell1, cell2)) {
                    String strMatch = m_matchOption.getStringValue();
                    if (strMatch.equals(REPL_OPTIONS[0])) {
                        return covertMatch(cell1);
                    } else if (strMatch.equals(REPL_OPTIONS[1])) {
                        return covertMatch(cell2);
                    } else if (strMatch.equals(REPL_OPTIONS[2])) {
                        return DataType.getMissingCell();
                    } else {
                        return m_matchRepl;
                    }
                } else {
                    String strMismatch = m_mismatchOption.getStringValue();
                    if (strMismatch.equals(REPL_OPTIONS[0])) {
                        return covertMismatch(cell1);
                    } else if (strMismatch.equals(REPL_OPTIONS[1])) {
                        return covertMismatch(cell2);
                    } else if (strMismatch.equals(REPL_OPTIONS[2])) {
                        return DataType.getMissingCell();
                    } else {
                        return m_mismatchRepl;
                    }
                }
            }
        });
        BufferedDataTable outData =
            exec.createColumnRearrangeTable(inData[0], colRe, exec);
        return new BufferedDataTable[]{outData};
    }
    
    private DataCell covertMatch(final DataCell cell) { 
        String strMismatch = m_mismatchOption.getStringValue();
        if (strMismatch.equals(REPL_OPTIONS[3])) {
            return new StringCell(cell.toString());
        } else {
            return cell;
        }
    }
    
    private DataCell covertMismatch(final DataCell cell) { 
        String strMatch = m_matchOption.getStringValue();
        if (strMatch.equals(REPL_OPTIONS[3])) {
            return new StringCell(cell.toString());
        } else {
            return cell;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {

    }

    /**
     * @see org.knime.core.node.NodeModel#loadValidatedSettingsFrom(org.knime.core.node.NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_operator.loadSettingsFrom(settings);
        m_firstColumn.loadSettingsFrom(settings);
        m_secondColumn.loadSettingsFrom(settings);
        m_matchOption.loadSettingsFrom(settings);
        m_mismatchOption.loadSettingsFrom(settings);
        m_matchValue.loadSettingsFrom(settings);
        m_mismatchValue.loadSettingsFrom(settings);
        m_newColumn.loadSettingsFrom(settings);
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
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(NodeSettingsWO settings) {
        m_operator.saveSettingsTo(settings);
        m_firstColumn.saveSettingsTo(settings);
        m_secondColumn.saveSettingsTo(settings);
        m_matchOption.saveSettingsTo(settings);
        m_mismatchOption.saveSettingsTo(settings);
        m_matchValue.saveSettingsTo(settings);
        m_mismatchValue.saveSettingsTo(settings);
        m_newColumn.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(NodeSettingsRO settings)
            throws InvalidSettingsException {
        SettingsModelString first = 
            m_firstColumn.createCloneWithValidatedValue(settings);
        SettingsModelString second = 
            m_secondColumn.createCloneWithValidatedValue(settings);
        if (first.getStringValue() == null || second.getStringValue() == null) {
            throw new InvalidSettingsException(
                    "No column is selected for comparison.");
        }
        if (first.getStringValue().equals(second.getStringValue())) {
            throw new InvalidSettingsException(
                    "Left and right column are the same: " 
                    + first.getStringValue());
        }
        SettingsModelString matchOption = 
            m_matchOption.createCloneWithValidatedValue(settings);
        SettingsModelString mismatchOption = 
            m_mismatchOption.createCloneWithValidatedValue(settings);
        if (!matchOption.getStringValue().equals(REPL_OPTIONS[3]) 
           && !mismatchOption.getStringValue().equals(REPL_OPTIONS[3])) {
            if (matchOption.getStringValue().equals(
                    mismatchOption.getStringValue())) {
                throw new InvalidSettingsException(
                        "Replacement options are the same for true/false!");
            }
        }
        m_firstColumn.validateSettings(settings);
        m_secondColumn.validateSettings(settings);
        m_matchOption.validateSettings(settings);
        m_mismatchOption.validateSettings(settings);
        m_operator.validateSettings(settings);
        m_matchValue.validateSettings(settings);
        m_mismatchValue.validateSettings(settings);
        m_newColumn.validateSettings(settings);
    }

}
