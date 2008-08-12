/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   Feb 1, 2008 (wiswedel): created
 */
package org.knime.base.collection.list.create;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class CollectionCreateNodeModel extends NodeModel {
    
    private SettingsModelFilterString m_includeModel;

    /**
     * 
     */
    public CollectionCreateNodeModel() {
        super(1, 1);
        m_includeModel = createSettingsModel();
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        ColumnRearranger rearranger = createColumnRearranger(inSpecs[0]);
        return new DataTableSpec[]{rearranger.createSpec()};
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        ColumnRearranger rearranger = 
            createColumnRearranger(inData[0].getDataTableSpec());
        BufferedDataTable out = 
            exec.createColumnRearrangeTable(inData[0], rearranger, exec);
        return new BufferedDataTable[]{out};
    }
    
    private ColumnRearranger createColumnRearranger(final DataTableSpec in)
        throws InvalidSettingsException {
        List<String> includes = m_includeModel.getIncludeList();
        if (includes == null || includes.isEmpty()) {
            throw new InvalidSettingsException("Not configured");
        }
        String[] names = includes.toArray(new String[includes.size()]);
        final int[] colIndices = new int[names.length];
        DataType comType = null;
        for (int i = 0; i < names.length; i++) {
            int index = in.findColumnIndex(names[i]);
            if (index < 0) {
                throw new InvalidSettingsException(
                        "No column \"" + names[i] + "\" in input table");
            }
            DataColumnSpec colSpec = in.getColumnSpec(index);
            comType = comType == null ? colSpec.getType() 
                    : DataType.getCommonSuperType(comType, colSpec.getType());
            colIndices[i] = index;
        }
        assert comType != null;
        String newColName = DataTableSpec.getUniqueColumnName(in, "List");
        DataType type = ListCell.getCollectionType(comType);
        DataColumnSpecCreator newColSpecC = 
            new DataColumnSpecCreator(newColName, type);
        newColSpecC.setElementNames(names);
        DataColumnSpec newColSpec = newColSpecC.createSpec();
        CellFactory appendFactory = new SingleCellFactory(newColSpec) {
            /** {@inheritDoc} */
            @Override
            public DataCell getCell(final DataRow row) {
                return ListCell.create(row, colIndices);
            }  
        };
        ColumnRearranger rearranger = new ColumnRearranger(in);
        rearranger.append(appendFactory);
        return rearranger;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_includeModel.loadSettingsFrom(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_includeModel.saveSettingsTo(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_includeModel.validateSettings(settings);
    }
    
    /** Create settings model collection create node.
     * @return a new settings object. */
    static SettingsModelFilterString createSettingsModel() {
        return new SettingsModelFilterString("includes");
    }

}
