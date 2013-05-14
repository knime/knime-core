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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.timeseries.node.movavg;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.timeseries.node.movavg.maversions.MovingAverage;
import org.knime.timeseries.util.SlidingWindow;

/**
 *
 * @author Rosaria Silipo
 */
public class MovingAverageNodeModel extends NodeModel {

    private MovingAverage[] m_mas;

    private final SettingsModelIntegerBounded m_winLength
        = MovingAverageDialog.createWindowLengthModel();

    private final SettingsModelFilterString m_columnNames
        = MovingAverageDialog.createColumnNamesModel();

    private final SettingsModelString m_kindOfMAModel
        = MovingAverageDialog.createWeightModel();

    private final SettingsModelBoolean m_replace
        = MovingAverageDialog.createReplaceColumnModel();

     /** Init node, 1 input, 1 output. */
    public MovingAverageNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec inSpec = inSpecs[0];
        // define column name on which to apply MA
        if ((m_columnNames.getIncludeList().size() == 0)
                && (m_columnNames.getExcludeList().size() == 0)) {
            // auto-configure
            List<String>autoConfiguredInclList = new ArrayList<String>();
            for (DataColumnSpec colSpec : inSpec) {
                if (colSpec.getType().isCompatible(DoubleValue.class)) {
                    autoConfiguredInclList.add(colSpec.getName());
                }
            }
            m_columnNames.setIncludeList(autoConfiguredInclList);
            setWarningMessage("Auto-configure: selected all double columns!");
        }
        if (m_columnNames.getIncludeList().isEmpty()) {
            setWarningMessage("No double columns selected: input will be same as output!");
        }
        // check for the existence of the selected columns
        for (String colName : m_columnNames.getIncludeList()) {
            if (!inSpecs[0].containsName(colName)) {
                throw new InvalidSettingsException(
                        "Column \"" + colName + "\" not found in input data!");
            }
        }

        // define moving average window length
        int winLength = m_winLength.getIntValue();
        if (winLength == -1) {
            throw new InvalidSettingsException("Window length is not selected.");
        }

        // define weight function
        if (m_kindOfMAModel.getStringValue() == null) {
            throw new InvalidSettingsException("No weight function selected.");
        } else {
           // create one MA-compute engine per column (overkill, I know
           // but much easier to reference later on in our DataCellFactory)

            MA_METHODS method = MA_METHODS.getPolicy4Label(m_kindOfMAModel.getStringValue());
            // if the center method is selected, the window size
            // has to be uneven

            if (MA_METHODS.getCenteredMethods().contains(method)) {
                if (winLength % 2 == 0) {
                    throw new InvalidSettingsException(
                            "For centered methods, the window size has to be uneven");
                }
            }

           m_mas = new MovingAverage[inSpecs[0].getNumColumns()];
           for (int i = 0; i < inSpecs[0].getNumColumns(); i++) {
               m_mas[i] = method.getMAObject(winLength);
           }
        }

        ColumnRearranger c = createColRearranger(inSpecs[0]);
        return new DataTableSpec[]{c.createSpec()};
    }


    private ColumnRearranger createColRearranger(final DataTableSpec spec) {

        ColumnRearranger result = new ColumnRearranger(spec);

        for (String thisCol : m_columnNames.getIncludeList()) {
            final int colIndex = spec.findColumnIndex(thisCol);
            DataColumnSpec newColSpec =
                    new DataColumnSpecCreator(DataTableSpec
                            .getUniqueColumnName(spec, "MA(" + thisCol + ")"),
                            DoubleCell.TYPE).createSpec();
            SingleCellFactory c = new SingleCellFactory(newColSpec) {
                @Override
                public DataCell getCell(final DataRow row) {
                    DataCell cell = row.getCell(colIndex);
                    if (cell.isMissing() || !(cell instanceof DoubleValue)) {
                        return DataType.getMissingCell();
                    }
                    return m_mas[colIndex].getMeanandUpdate(
                            ((DoubleValue)cell).getDoubleValue());
                }
            };
            if (m_replace.getBooleanValue()) {
                result.replace(c, colIndex);
            } else {
                result.append(c);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        if (m_winLength.getIntValue() > inData[0].getRowCount()) {
            throw new Exception(
                    "Number of total samples in time series smaller than "
                    + "moving average window length!");
        }
        MA_METHODS method = MA_METHODS.getPolicy4Label(
                               m_kindOfMAModel.getStringValue());
        if (!MA_METHODS.getBackwardCompMethods().contains(method)) {

            // in the first option everything which is looking to future rows
            // must be handeled

            int position = 0;
            if (MA_METHODS.getCenteredMethods().contains(method)) {
                position = (m_winLength.getIntValue() - 1) / 2;
            }

            // idea... we save the rows to add them in the sliding window
            // but still, use the MovingAverage class to  calculate everything.
            return new BufferedDataTable[] {getSlidingNotBackward(
                    inData[0].getSpec(),
                    inData[0],
                    position,
                    exec)};

        } else { // default we use the old behaviour

            // the column rearranger can currently be used for backward
            // and cumulative moving average.
            ColumnRearranger c = createColRearranger(
                    inData[0].getDataTableSpec());
            return new BufferedDataTable[]{exec.createColumnRearrangeTable(
                inData[0], c, exec)};
        }
    }

    private BufferedDataTable getSlidingNotBackward(final DataTableSpec spec,
            final BufferedDataTable inData, final int refNR,
            final ExecutionContext exec)
                throws CanceledExecutionException {

        // for speed, at first create an array with the int of the columns.
        int[] colindexex = new int[m_columnNames.getIncludeList().size()];
        int counter = 0;
        for (String thisCol : m_columnNames.getIncludeList()) {
            colindexex[counter] = spec.findColumnIndex(thisCol);
            counter++;
        }

        // the sliding window over data rows is used to save the already filled
        // data rows
        SlidingWindow<DataRow> window
                    = new SlidingWindow<DataRow>(m_winLength.getIntValue());
        // the output container.
        BufferedDataContainer out
                = exec.createDataContainer(
                        createColRearranger(spec).createSpec());

        int nrNewColumns = 0;
        if (!m_replace.getBooleanValue()) {
            // we add missing value cells to ensure the rowlength
            nrNewColumns = colindexex.length;
        }

        // create a map, which moves from the old if necessary to the new
        // index.
        int[] gotoMap = new int[spec.getNumColumns()];
        for (int i = 0; i < gotoMap.length; i++) {
            gotoMap[i]  = i;
        }
        if (!m_replace.getBooleanValue()) {
            int currentend = spec.getNumColumns();
            for (int colIndex : colindexex) {
                gotoMap[colIndex] = currentend;
                currentend++;
            }
        }
        double rowCount = 0;
        double rowNrs = inData.getRowCount();
        exec.setMessage("Calculating Means");
        for (DataRow row : inData) {
            exec.checkCanceled();
            exec.setProgress(rowCount++ / rowNrs);

            // first add old / e.g. rows, no longer used for the sliding window
            // to the data table.

            // the donerow is no longer necessary in the windows
            // and can therefore be written to the data table
            DataRow donerow = window.addandget(addMissingCells(nrNewColumns,
                    colindexex,
                    row));
            if (donerow != null) {
                out.addRowToTable(donerow);
            }

            // all moving values, of the currently examined window are
            // calculated
            DataCell[] cells = new DataCell[colindexex.length];
            counter = 0;
            for (int colIndex : colindexex) {
                DataCell cell = row.getCell(colIndex);
                cells[counter] = DataType.getMissingCell();
                if (!cell.isMissing() && (cell instanceof DoubleValue)) {
                    double value = ((DoubleValue)cell).getDoubleValue();
                    cells[counter] = m_mas[colIndex].getMeanandUpdate(value);
                }
                counter++;
            }
            // now replace the cells in the sliding window with the newly
            // calculated means.

            // first check if the row exists.
            DataRow oldrow = window.get(refNR);
            if (oldrow != null) {
                DataCell[] oldcells = new DataCell[oldrow.getNumCells()];
                counter = 0;
                for (DataCell c : oldrow) {
                    oldcells[counter] =  c;
                    counter++;
                }

                // second replace the cells, therefore go through the colindexex
                for (int i = 0; i < colindexex.length; i++) {
                    oldcells[gotoMap[colindexex[i]]] = cells[i];
                }
                window.replace(refNR,
                        new DefaultRow(oldrow.getKey(), oldcells));
            }
        }

        // finally clear the sliding window
        for (DataRow row : window.getList()) {
            out.addRowToTable(row);
        }

        out.close();
        return out.getTable();
    }

    /**
     * adds a fixed number of missing cells to the given row.
     * or replaces the  original ones with missings.
     * @param colindexex
     */
    private DataRow addMissingCells(final int nrMiss,
            final int[] colindexex,
            final DataRow oldrow) {
        DataCell[] cell = new DataCell[oldrow.getNumCells() + nrMiss];
        int counter = 0;
        for (DataCell c : oldrow) {
            cell[counter] = c;
            counter++;
        }
        if (nrMiss <= 0) {
            // we replace the colindexex with missings.
            for (int i : colindexex) {
                cell[i] = DataType.getMissingCell();
            }
            return oldrow;
        } else { // we add missings to the end.

            for (; counter < cell.length; counter++) {
                cell[counter] = DataType.getMissingCell();
            }
        }
        return new DefaultRow(oldrow.getKey(), cell);

    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_columnNames.validateSettings(settings);
        m_replace.validateSettings(settings);
        m_kindOfMAModel.validateSettings(settings);
        m_winLength.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_columnNames.loadSettingsFrom(settings);
        m_kindOfMAModel.loadSettingsFrom(settings);
        m_winLength.loadSettingsFrom(settings);
        m_replace.loadSettingsFrom(settings);
   }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_columnNames.saveSettingsTo(settings);
        m_winLength.saveSettingsTo(settings);
        m_kindOfMAModel.saveSettingsTo(settings);
        m_replace.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }
}
