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
 */
package org.knime.base.node.preproc.columnTrans;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;

/**
 * This node converts one column to many columns, such that each possible value
 * becomes an extra column with the value 1 if the row contains this value in
 * the original column and 0 otherwise.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class One2ManyColNodeModel extends NodeModel {
    
    /** Config key for the columns 2 be transformed. */
    public static final String CFG_COLUMNS = "columns2Btransformed";
    
//    private String[] m_includedColumns = new String[0];
    
    private final SettingsModelFilterString m_includedColumns 
        = new SettingsModelFilterString(CFG_COLUMNS);
    
    
    // if several columns should be converted and the possible values
    // of them overlap the original column names is appended to 
    // distinguish them
    private boolean m_appendOrgColName = false;

    /**
     * 
     */
    public One2ManyColNodeModel() {
        super(1, 1);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_includedColumns.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_includedColumns.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {    
        m_includedColumns.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, 
            final ExecutionContext exec)
            throws Exception {
        checkColumnsSpecs(inData[0].getDataTableSpec());
        return new BufferedDataTable[]{
                exec.createColumnRearrangeTable(inData[0], 
                        createRearranger(inData[0].getDataTableSpec()), exec)};
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
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        m_appendOrgColName = false;
        if (m_includedColumns.getIncludeList() == null 
                || m_includedColumns.getIncludeList().size() <= 0) {
            setWarningMessage(
                    "No columns to transfrom selected. Will have no effect!");
        }
        // check if the values are present in the current spec
        if (m_includedColumns.getIncludeList() != null 
                && m_includedColumns.getIncludeList().size() > 0) {
            checkColumnsSpecs(inSpecs[0]);
        }
        ColumnRearranger rearranger = createRearranger(inSpecs[0]);
        return new DataTableSpec[]{rearranger.createSpec()};
    }
    
    private void checkColumnsSpecs(final DataTableSpec spec)
            throws InvalidSettingsException {
        Set<String> allPossibleValues = new HashSet<String>();
        for (final String colName : m_includedColumns.getIncludeList()) {
            if (spec.findColumnIndex(colName) < 0) {
                throw new InvalidSettingsException("Column " + colName
                        + " not found in input table");
            }
            if (!spec.getColumnSpec(colName).getDomain().hasValues()) {
                throw new InvalidSettingsException("column: " + colName
                        + " has no possible values");
            }
            Set<String> possibleValues = new HashSet<String>();
            for (DataCell dc : spec.getColumnSpec(colName).getDomain()
                    .getValues()) {
                possibleValues.add(dc.toString());
            }
            Set<String> duplicateTest = new HashSet<String>();
            duplicateTest.addAll(possibleValues);
            duplicateTest.retainAll(allPossibleValues);
            if (duplicateTest.size() > 0) {
                // there are elements in both
                setWarningMessage("Duplicate possible values found."
                        + " Original column name will be appended");
                m_appendOrgColName = true;
            }
            allPossibleValues.addAll(possibleValues);
        }
    }

    
    private ColumnRearranger createRearranger(final DataTableSpec spec) {
        CellFactory cellFactory = new One2ManyCellFactory(
                spec, m_includedColumns.getIncludeList(), 
                m_appendOrgColName);
        ColumnRearranger rearranger = new ColumnRearranger(spec);
        rearranger.append(cellFactory);        
        return rearranger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) {
        //nothing to do
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) {
        //nothing to do
    }

}
