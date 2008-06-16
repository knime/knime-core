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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   21.12.2006 (gabriel): created
 */
package org.knime.base.node.mine.mds;

import java.util.List;
import java.util.Random;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class MDSPivotDataNodeModel extends MDSPivotNodeModel {
    
    /**
     * New MDS data node model with the given number of in- and outputs.
     * @param nrDataIns number of data ins
     * @param nrDataOuts number of data outs
     */
    public MDSPivotDataNodeModel(final int nrDataIns, final int nrDataOuts) {
        super(nrDataIns, nrDataOuts);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        List<String> incl = m_numericFilter.getIncludeList();
        for (String s : incl) {
            if (!inSpecs[0].containsName(s)) {
                throw new InvalidSettingsException("Column \"" + s 
                        + "\" not in input spec at port 0.");
            }
        }
        for (String s : incl) {
            if (!inSpecs[1].containsName(s)) {
                throw new InvalidSettingsException("Column \"" + s 
                        + "\" not in input spec at port 1.");
            } else {
                if (!inSpecs[1].getColumnSpec(s).getType().isCompatible(
                        DoubleValue.class)) {
                    throw new InvalidSettingsException("Column \"" + s 
                            + "\" in input spec at port 1 is not "
                            + "double-value compatible.");
                }
            }
        }
        DataTableSpec[] resultSpec = super.configure(
                new DataTableSpec[]{inSpecs[0]});
        return new DataTableSpec[]{resultSpec[0], createSpec(inSpecs[1])};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final int nrRows = inData[0].getRowCount();
        final DataTableSpec ospec = inData[0].getDataTableSpec();
        ColumnRearranger colRe = createColumnRearranger(ospec);
        final List<String> incl = m_numericFilter.getIncludeList();
        colRe.keepOnly(incl.toArray(new String[0]));
        BufferedDataTable data = exec.createColumnRearrangeTable(
                inData[0], colRe, exec.createSilentSubProgress(0.0));
        final int colCnt = data.getDataTableSpec().getNumColumns();
        double[][] dataArray = new double[colCnt][nrRows];
        int rowCnt = 0;
        for (DataRow row : data) {
            exec.checkCanceled();
            for (int i = 0; i < colCnt; i++) {
                DataCell cell = row.getCell(i);
                if (cell.isMissing()) {
                    break;
                } else {
                    dataArray[i][rowCnt] = 
                        ((DoubleValue) cell).getDoubleValue();
                }
            }
            rowCnt++;
        }
        final int lowDim = m_lowerDim.getIntValue();
        double[][] result = new double[lowDim][nrRows];
        if (rowCnt > 0) {
            // initialize with random nonzero stuff
            Random random = new Random(0L);
            for (int i = 0; i < result.length; i++) {
                for (int j = 0; j < result[0].length; j++) {
                    result[i][j] = random.nextDouble();
                }
            }
            super.executeMDS(dataArray, rowCnt, result, exec);
        }
        DataTableSpec spec0 = createSpec(lowDim, ospec);
        BufferedDataContainer buf1 = exec.createDataContainer(spec0);
        rowCnt = 0;
        for (DataRow row : inData[0]) {
            exec.checkCanceled();
            DataCell[] doubles;
            boolean isMissing = false;
            if (m_appendColumns.getBooleanValue()) {
                doubles = new DataCell[ospec.getNumColumns() 
                                       - incl.size() + lowDim];
                int idx = 0;
                for (int j = 0; j < ospec.getNumColumns(); j++) {
                    if (!incl.contains(ospec.getColumnSpec(j).getName())) {
                        DataCell cell = row.getCell(j);
                        doubles[(idx++) + lowDim] = cell;
                    } else {
                        isMissing |= row.getCell(j).isMissing();
                    }
                }
            } else {
                doubles = new DoubleCell[result.length];
            }
            for (int j = 0; j < result.length; j++) {
                if (isMissing) {
                    doubles[j] = DataType.getMissingCell();
                } else {
                    doubles[j] = new DoubleCell(result[j][rowCnt]);
                }
            }
            DataRow newRow = new DefaultRow(row.getKey(), doubles);
            buf1.addRowToTable(newRow);
            rowCnt++;
        }
        buf1.close();
        colRe = createColumnRearranger(inData[1].getDataTableSpec());
        colRe.keepOnly(incl.toArray(new String[0]));
        data = exec.createColumnRearrangeTable(inData[1], colRe, 
                exec.createSilentSubProgress(0.0));
        rowCnt = 0;
        final DataTableSpec spec1 = inData[1].getDataTableSpec();
        final int nrCells1 = spec1.getNumColumns() + lowDim - incl.size();
        BufferedDataContainer buf2 = 
            exec.createDataContainer(createSpec(spec1));
        for (DataRow row : inData[1]) {
            exec.checkCanceled();
            DataCell[] doubles;
            boolean isMissing = false;
            if (m_appendColumns.getBooleanValue()) {
                doubles = new DataCell[nrCells1];
                int idx = 0;
                for (int j = 0; j < spec1.getNumColumns(); j++) {
                    if (!incl.contains(spec1.getColumnSpec(j).getName())) {
                        DataCell cell = row.getCell(j);
                        doubles[(idx++) + lowDim] = cell;
                    } else {
                        isMissing |= row.getCell(j).isMissing();
                    }
                }
            } else {
                doubles = new DoubleCell[result.length];
            }
            if (isMissing) {
                for (int i = 0; i < lowDim; i++) {
                    doubles[i] = DataType.getMissingCell();
                }
            } else {
                double[] scale = new double[incl.size()];
                for (int j = 0; j < incl.size(); j++) {
                    int idx = spec1.findColumnIndex(incl.get(j));
                    DataCell cell = row.getCell(idx);
                    scale[j] = ((DoubleValue) cell).getDoubleValue();
                }
                double[] place = ClassicalMDS.place(result, dataArray, scale);
                for (int i = 0; i < place.length; i++) {
                    doubles[i] = new DoubleCell(place[i]);
                }
            }
            DataRow newRow = new DefaultRow(row.getKey(), doubles);
            buf2.addRowToTable(newRow);
            rowCnt++;
        }
        buf2.close();
        return new BufferedDataTable[]{buf1.getTable(), buf2.getTable()};
    }
    
    private DataTableSpec createSpec(final DataTableSpec spec) {
        final int lowDim = m_lowerDim.getIntValue();
        final List<String> incl = m_numericFilter.getIncludeList();
        final int nrCells1 = spec.getNumColumns() + lowDim - incl.size();
        DataColumnSpec[] cspecs;
        if (m_appendColumns.getBooleanValue()) {
            cspecs = new DataColumnSpec[nrCells1];
            int idx = 0;
            for (int j = 0; j < spec.getNumColumns(); j++) {
                if (!incl.contains(spec.getColumnSpec(j).getName())) {
                    cspecs[(idx++) + lowDim] = spec.getColumnSpec(j);
                }
            }
        } else {
            cspecs = new DataColumnSpec[lowDim];
        }
        for (int i = 0; i < lowDim; i++) {
            String name = new String("X" + (i + 1));
            DataType type = DoubleCell.TYPE;
            cspecs[i] = new DataColumnSpecCreator(name, type).createSpec();
        }
        return new DataTableSpec(cspecs);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_lowerDim.loadSettingsFrom(settings);
        m_numericFilter.loadSettingsFrom(settings);
        m_appendColumns.loadSettingsFrom(settings);
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
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_lowerDim.saveSettingsTo(settings);
        m_numericFilter.saveSettingsTo(settings);
        m_appendColumns.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_lowerDim.validateSettings(settings);
        m_numericFilter.validateSettings(settings);
        m_appendColumns.validateSettings(settings);
    }
    
    /**
     * Forwards the corresponding input hilite handler from input 0 to output 0,
     * and input 1 to output 1.
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        return getInHiLiteHandler(outIndex);
    }

}

