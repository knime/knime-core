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
 * -------------------------------------------------------------------
 *
 * History
 *   29.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.box;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.base.data.sort.SortedTable;
import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * The input data is sorted for each numeric column and the necessary
 * parameters are determined: minimum, lower whisker
 * (in case of outliers it is the first non-outlier), lower quartile, median,
 * upper quartile, upper whisker and maximum. Each column is then associated
 * with a double array of these parameters, which are passed to the
 * {@link org.knime.base.node.viz.plotter.box.BoxPlotter}.
 * To do so, the <code>BoxPlotNodeModel</code> implements a new interface, the
 * {@link org.knime.base.node.viz.plotter.box.BoxPlotDataProvider}, which
 * passes the statistical parameters and the mild and extreme outliers.
 *
 * @author Fabian Dill, University of Konstanz
 */
public class BoxPlotNodeModel extends NodeModel implements BoxPlotDataProvider {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            BoxPlotNodeModel.class);

    /** Constant for the minimum position in the statistics array. */
    public static final int MIN = 0;
    /** Constant for the lower whisker position in the statistics array. */
    public static final int LOWER_WHISKER = 1;
    /** Constant for the lower quartile position in the statistics array. */
    public static final int LOWER_QUARTILE = 2;
    /** Constant for the median position in the statistics array. */
    public static final int MEDIAN = 3;
    /** Constant for the upper quartile position in the statistics array. */
    public static final int UPPER_QUARTILE = 4;
    /** Constant for the upper whisker position in the statistics array. */
    public static final int UPPER_WHISKER = 5;
    /** Constant for the maximum position in the statistics array. */
    public static final int MAX = 6;
    /** Constant for the size of the statistics array. */
    public static final int SIZE = 7;

    private static final String FILE_NAME = "boxPlotInternals";
    private static final String ARRAY_FILE = "internalData";
    private static final String CFG_NR_COLS = "numberOf";
    private static final String CFG_COL = "colSpec";
    private static final String CFG_STATS = "stats";
    private static final String CFG_MILD = "mild";
    private static final String CFG_EXTREME = "extreme";
    private static final String CFG_ROW = "row";

    private Map<DataColumnSpec, double[]>m_statistics;
    private Map<String, Map<Double, Set<RowKey>>>m_mildOutliers;
    private Map<String, Map<Double, Set<RowKey>>>m_extremeOutliers;

    private DataArray m_array;

    private final HiLiteHandler m_hiliteHandler = new HiLiteHandler();


    /**
     * One input for the data one output for the parameters (median,
     * quartiles and inter-quartile range(IQR).
     *
     */
    public BoxPlotNodeModel() {
        super(1, 1);
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        List<DataColumnSpec>numericCols = new ArrayList<DataColumnSpec>();
        for (DataColumnSpec colSpec : inSpecs[0]) {
            if (colSpec.getType().isCompatible(DoubleValue.class)) {
                    numericCols.add(colSpec);
            }
        }
        if (numericCols.size() == 0) {
            throw new InvalidSettingsException(
                    "Only numeric columns can be displayed! "
                    + "Found no numeric column.");
        }
        return new DataTableSpec[]{createOutputSpec(numericCols)};
    }

    private int getNumNumericColumns(final DataTableSpec spec) {
        int nr = 0;
        for (DataColumnSpec colSpec : spec) {
            if (colSpec.getType().isCompatible(DoubleValue.class)) {
                nr++;
            }
        }
        return nr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        if (inData[0] == null) {
            return new BufferedDataTable[]{};
        }
        BufferedDataTable table = inData[0];
        m_statistics = new LinkedHashMap<DataColumnSpec, double[]>();
        m_mildOutliers
            = new LinkedHashMap<String, Map<Double, Set<RowKey>>>();
        m_extremeOutliers
            = new LinkedHashMap<String, Map<Double, Set<RowKey>>>();
        int colIdx = 0;
        List<DataColumnSpec> outputColSpecs = new ArrayList<DataColumnSpec>();
        double subProgress = 1.0 / getNumNumericColumns(
                table.getDataTableSpec());
        for (DataColumnSpec colSpec : table.getDataTableSpec()) {
            ExecutionContext colExec = exec.createSubExecutionContext(
                    subProgress);
            exec.checkCanceled();
            if (colSpec.getType().isCompatible(DoubleValue.class)) {
                double[] statistic = new double[SIZE];
                outputColSpecs.add(colSpec);
                List<String> col = new ArrayList<String>();
                col.add(colSpec.getName());
                ExecutionContext sortExec = colExec.createSubExecutionContext(
                        0.75);
                ExecutionContext findExec = colExec.createSubExecutionContext(
                        0.25);
                SortedTable sorted = new SortedTable(table,
                        col, new boolean[]{true},
                        sortExec);
                int currRowAbsolute = 0;
                int currCountingRow = 1;
                double lastValue = 1;
                int nrOfRows = table.getRowCount();
                boolean first = true;
                for (DataRow row : sorted) {
                    exec.checkCanceled();
                    double rowProgress = (double)currRowAbsolute
                    / (double)table.getRowCount();
                    findExec.setProgress(rowProgress,
                            "determining statistics for: "
                            + table.getDataTableSpec().getColumnSpec(colIdx)
                            .getName());
                    if (row.getCell(colIdx).isMissing()) {
                        // asserts that the missing values are sorted at
                        // the top of the table
                        currRowAbsolute++;
                        nrOfRows--;
                        continue;
                    }
                    // get the first value = actually observed minimum
                    if (first) {
                        statistic[MIN] = ((DoubleValue)row
                                .getCell(colIdx)).getDoubleValue();
                        // initialize the statistics with first value
                        // if the table is large enough it will be overriden
                        // this is just for the case of tables with < 5 rows
                        statistic[MEDIAN] = statistic[MIN];
                        statistic[LOWER_QUARTILE] = statistic[MIN];
                        statistic[UPPER_QUARTILE] = statistic[MIN];
                        first = false;
                    }
                    // get the last value = actually observed maximum
                    if (currRowAbsolute == table.getRowCount() - 1) {
                        statistic[MAX] = ((DoubleValue)row
                                .getCell(colIdx)).getDoubleValue();
                    }
                    float medianPos = nrOfRows * 0.5f;
                    float lowerQuartilePos = nrOfRows * 0.25f;
                    float upperQuartilePos = nrOfRows * 0.75f;
                    if (currCountingRow == (int)Math.floor(lowerQuartilePos) + 1) {
                        if (lowerQuartilePos % 1 != 0) {
                            // get the row's value
                            statistic[LOWER_QUARTILE] = ((DoubleValue)row
                                    .getCell(colIdx)).getDoubleValue();
                        } else {
                            // calculate the mean between row and last row
                            double value =
                                ((DoubleValue)row.getCell(colIdx))
                                .getDoubleValue();
                            statistic[LOWER_QUARTILE] = (value + lastValue) / 2;
                        }
                    }
                    if (currCountingRow == (int)Math.floor(medianPos) + 1) {
                        if (medianPos % 1 != 0) {
                            // get the row's value
                            statistic[MEDIAN] = ((DoubleValue)row
                                    .getCell(colIdx)).getDoubleValue();
                        } else {
                            // calculate the mean between row and last row
                            double value =
                                ((DoubleValue)row.getCell(colIdx))
                                .getDoubleValue();
                            statistic[MEDIAN] = (value + lastValue) / 2;
                        }
                    }
                    if (currCountingRow == (int)Math.floor(upperQuartilePos) + 1) {
                        if (upperQuartilePos % 1 != 0) {
                            // get the row's value
                            statistic[UPPER_QUARTILE] = ((DoubleValue)row
                                    .getCell(colIdx)).getDoubleValue();
                        } else {
                            // calculate the mean between row and last row
                            double value =
                                ((DoubleValue)row.getCell(colIdx))
                                .getDoubleValue();
                            statistic[UPPER_QUARTILE] = (value + lastValue) / 2;
                        }
                    }
                    lastValue = ((DoubleValue)row.getCell(colIdx))
                        .getDoubleValue();
                    currRowAbsolute++;
                    currCountingRow++;
                }
                double iqr = statistic[UPPER_QUARTILE]
                                       - statistic[LOWER_QUARTILE];
                Map<Double, Set<RowKey>> mild
                    = new LinkedHashMap<Double, Set<RowKey>>();
                Map<Double, Set<RowKey>>extreme
                    = new LinkedHashMap<Double, Set<RowKey>>();
                // per default the whiskers are at min and max
                double[] whiskers = new double[]{
                        statistic[MIN],
                        statistic[MAX]
                };
                if (statistic[MIN] < (statistic[LOWER_QUARTILE] - (1.5 * iqr))
                    ||
                    statistic[MAX] > statistic[UPPER_QUARTILE] + (1.5 * iqr)) {
                        detectOutliers(sorted, iqr,
                                new double[]{statistic[LOWER_QUARTILE],
                                statistic[UPPER_QUARTILE]},
                                mild, extreme, whiskers, colIdx);
                }
                statistic[LOWER_WHISKER] = whiskers[0];
                statistic[UPPER_WHISKER] = whiskers[1];
                m_mildOutliers.put(colSpec.getName(), mild);
                m_extremeOutliers.put(colSpec.getName(), extreme);
                m_statistics.put(colSpec, statistic);
            }
            colIdx++;
        }
        DataContainer container = createOutputTable(exec, outputColSpecs);
        // return a data array with just one row but with the data table spec
        // for the column selection panel
        m_array = new DefaultDataArray(table, 1, 2);
        return new BufferedDataTable[]{exec.createBufferedDataTable(
                container.getTable(), exec)};
    }



    private DataContainer createOutputTable(final ExecutionContext exec,
            final List<DataColumnSpec> outputColSpecs) {
        DataTableSpec outSpec = createOutputSpec(outputColSpecs);
        DataContainer container = exec.createDataContainer(outSpec);
        String[] rowKeys = new String[SIZE];
        rowKeys[MIN] = "Minimum";
        rowKeys[LOWER_WHISKER] = "Smallest";
        rowKeys[LOWER_QUARTILE] = "Lower Quartile";
        rowKeys[MEDIAN] = "Median";
        rowKeys[UPPER_QUARTILE] = "Upper Quartile";
        rowKeys[UPPER_WHISKER] = "Largest";
        rowKeys[MAX] = "Maximum";
        for (int i = 0; i < SIZE; i++) {
            DataCell[] cells = new DataCell[outputColSpecs.size()];
            for (int j = 0; j < cells.length; j++) {
                double[] stats = m_statistics.get(outputColSpecs.get(j));
                cells[j] = new DoubleCell(stats[i]);
            }
            DataRow row = new DefaultRow(rowKeys[i], cells);
            container.addRowToTable(row);
        }
        container.close();
        return container;
    }

    private DataTableSpec createOutputSpec(
            final List<DataColumnSpec>numericInputSpecs) {
        DataColumnSpec[] colSpecArray
            = new DataColumnSpec[numericInputSpecs.size()];
           DataColumnSpecCreator creator;
           int colSpecIdx = 0;
           for (DataColumnSpec spec : numericInputSpecs) {
               creator = new DataColumnSpecCreator(spec);
               creator.setType(DoubleCell.TYPE);
               colSpecArray[colSpecIdx++] = creator.createSpec();
           }
           return new DataTableSpec(colSpecArray);
    }

    /**
     * Detects mild (= < 3 * IQR) and extreme (= > 3 * IQR) outliers.
     * @param table the sorted! table containing the values.
     * @param iqr the interquartile range
     * @param mild list to store mild outliers
     * @param extreme list to store extreme outliers
     * @param colIdx the index for the column of interest
     * @param q quartiles the lower quartile at 0,upper quartile at 1.
     * @param whiskers array to store the lower and upper whisker bar
     */
    public void detectOutliers(final DataTable table,
            final double iqr, final double[] q,
            final Map<Double, Set<RowKey>>mild,
            final Map<Double, Set<RowKey>>extreme,
            final double[] whiskers,
            final int colIdx) {
        boolean searchLowerWhisker = true;
        boolean searchUpperWhisker = true;
        for (DataRow row : table) {
            DataCell cell = row.getCell(colIdx);
            if (cell.isMissing()) {
                continue;
            }
            double value = ((DoubleValue)cell).getDoubleValue();
            // lower outlier
            if (value < q[0] - (1.5 * iqr)) {
                // mild
                if (value > q[0] - (3.0 * iqr)) {
                    Set<RowKey>keys = mild.get(value);
                    if (keys == null) {
                        keys = new HashSet<RowKey>();
                    }
                    keys.add(row.getKey());
                    mild.put(value, keys);
                } else {
                    // extreme
                    Set<RowKey>keys = mild.get(value);
                    if (keys == null) {
                        keys = new HashSet<RowKey>();
                    }
                    keys.add(row.getKey());
                    extreme.put(value, keys);
                }
            } else if (value > q[1] + (1.5 * iqr)) {
                // upper outlier
                searchUpperWhisker = false;
                if (value < q[1] + (3.0 * iqr)) {
                    // mild
                    Set<RowKey>keys = mild.get(value);
                    if (keys == null) {
                        keys = new HashSet<RowKey>();
                    }
                    keys.add(row.getKey());
                    mild.put(value, keys);
                } else {
                    // extreme
                    Set<RowKey>keys = mild.get(value);
                    if (keys == null) {
                        keys = new HashSet<RowKey>();
                    }
                    keys.add(row.getKey());
                    extreme.put(value, keys);
                }
            } else if (searchLowerWhisker) {
                whiskers[0] = value;
                searchLowerWhisker = false;
            }
            if (searchUpperWhisker) {
                whiskers[1] = value;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Map<DataColumnSpec, double[]>getStatistics() {
        return m_statistics;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Map<Double, Set<RowKey>>> getMildOutliers() {
        return m_mildOutliers;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Map<Double, Set<RowKey>>> getExtremeOutliers() {
        return m_extremeOutliers;
    }


    /**
     * {@inheritDoc}
     */
    public DataArray getDataArray(final int index) {
        return m_array;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
            return m_hiliteHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // so far no settings
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_statistics = null;
        m_mildOutliers = null;
        m_extremeOutliers = null;
        m_array = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        try {
            File f = new File(nodeInternDir, FILE_NAME);
            FileInputStream fis = new FileInputStream(f);
            NodeSettingsRO settings = NodeSettings.loadFromXML(fis);
            m_statistics = new LinkedHashMap<DataColumnSpec, double[]>();
            m_mildOutliers = new LinkedHashMap<String, Map<Double,
                Set<RowKey>>>();
            m_extremeOutliers
                = new LinkedHashMap<String, Map<Double, Set<RowKey>>>();
            int nrOfCols = settings.getInt(CFG_NR_COLS);
            for (int i = 0; i < nrOfCols; i++) {
                NodeSettings subSetting = (NodeSettings)settings
                        .getConfig(CFG_COL + i);
                DataColumnSpec spec = DataColumnSpec.load(subSetting);
                double[] stats = settings.getDoubleArray(CFG_STATS
                        + spec.getName());
                m_statistics.put(spec, stats);
                loadOutliers(settings, spec);
            }
            File data = new File(nodeInternDir, ARRAY_FILE);
            ContainerTable table = DataContainer.readFromZip(data);
            m_array = new DefaultDataArray(table, 1, 2, exec);
        } catch (Exception e) {
            LOGGER.warn(e);
            throw new IOException(e.getMessage());
        }
    }

    private void loadOutliers(final NodeSettingsRO columnSubConfig,
            final DataColumnSpec spec) throws InvalidSettingsException {
        // try if the settings are new
        if (columnSubConfig.getDataCellArray(
                CFG_MILD + CFG_ROW + spec.getName()) == null) {
            loadOutliersNew(columnSubConfig, spec);
            return;
        }
        double[] mild = columnSubConfig.getDoubleArray(CFG_MILD
                + spec.getName());
        String[] mildKeys;
        try {
            // since this is the old loading method it is more probable that we
            // find DataCells, so we start by trying to get them
            DataCell[] mildKeysOld = columnSubConfig.getDataCellArray(
                    CFG_MILD + CFG_ROW + spec.getName());
            mildKeys = new String[mildKeysOld.length];
            for (int i = 0; i < mildKeysOld.length; i++) {
                mildKeys[i] = mildKeysOld[i].toString();
            }
        } catch (InvalidSettingsException ise) {
            // unlikely (impossible?) case that we have strings
            mildKeys = columnSubConfig.getStringArray(
                    CFG_MILD + CFG_ROW + spec.getName());
        }
        Map<Double, Set<RowKey>> mildOutliers
            = new LinkedHashMap<Double, Set<RowKey>>();
        for (int j = 0; j < mild.length; j++) {
            Set<RowKey> keys = new HashSet<RowKey>();
            keys.add(new RowKey(mildKeys[j]));
            mildOutliers.put(mild[j], keys);
        }
        m_mildOutliers.put(spec.getName(), mildOutliers);
        // extreme
        Map<Double, Set<RowKey>> extremeOutliers
            = new LinkedHashMap<Double, Set<RowKey>>();
        double[] extreme = columnSubConfig.getDoubleArray(CFG_EXTREME
                + spec.getName());
        String[] extremeKeys;
        // since this is the old loading method it is more probable that we
        // find DataCells, so we start by trying to get them
        try {
            DataCell[] extremeKeysOld = columnSubConfig.getDataCellArray(
                    CFG_EXTREME + CFG_ROW + spec.getName());
            extremeKeys = new String[extremeKeysOld.length];
            for (int i = 0; i < extremeKeysOld.length; i++) {
                extremeKeys[i] = extremeKeysOld[i].toString();
            }
        } catch (InvalidSettingsException ise) {
            // unlikely (impossible) case that we have strings
            extremeKeys = columnSubConfig.getStringArray(
                    CFG_EXTREME + CFG_ROW + spec.getName());
        }
        for (int j = 0; j < extreme.length; j++) {
            Set<RowKey>keys = new HashSet<RowKey>();
            keys.add(new RowKey(extremeKeys[j]));
            extremeOutliers.put(extreme[j], keys);
        }
        m_extremeOutliers.put(spec.getName(), extremeOutliers);
    }

    private void loadOutliersNew(final NodeSettingsRO columnSubConfig,
            final DataColumnSpec spec) throws InvalidSettingsException {
        double[] mild = columnSubConfig.getDoubleArray(CFG_MILD
                + spec.getName());
        Config mildOutlierSubConfig = columnSubConfig.getConfig(
                CFG_MILD + CFG_ROW + spec.getName());
        Map<Double, Set<RowKey>> mildOutliers
            = new LinkedHashMap<Double, Set<RowKey>>();
        for (int j = 0; j < mild.length; j++) {
            // this is for backward compatibility
            // good old times where RowID was a DataCell
            String[] mildKeys;
            try {
                mildKeys = mildOutlierSubConfig.getStringArray(
                        CFG_MILD + CFG_ROW + spec.getName() + j);
            } catch (InvalidSettingsException e) {
                // ok, try to load old data cells
                // if this is also not found the ISE is ok
                DataCell[] mildKeysOld = mildOutlierSubConfig.getDataCellArray(
                        CFG_MILD + CFG_ROW + spec.getName() + j);
                mildKeys = new String[mildKeysOld.length];
                for (int i = 0; i < mildKeysOld.length; i++) {
                    mildKeys[i] = mildKeysOld[i].toString();
                }
            }
            Set<RowKey> keys = new HashSet<RowKey>();
            for (int rk = 0; rk < mildKeys.length; rk++) {
                keys.add(new RowKey(mildKeys[rk]));
            }
            mildOutliers.put(mild[j], keys);
        }
        m_mildOutliers.put(spec.getName(), mildOutliers);
        // extreme
        Map<Double, Set<RowKey>> extremeOutliers
            = new LinkedHashMap<Double, Set<RowKey>>();
        double[] extreme = columnSubConfig.getDoubleArray(CFG_EXTREME
                + spec.getName());
        Config extrOutlierSubConfig = columnSubConfig.getConfig(
                CFG_EXTREME + CFG_ROW + spec.getName());
        for (int j = 0; j < extreme.length; j++) {
            // this is for backward compatibility
            // good old times where RowID was a DataCell
            String[] extremeKeys;
            try {
                extremeKeys = extrOutlierSubConfig.getStringArray(
                        CFG_EXTREME + CFG_ROW + spec.getName() + j);
            } catch (InvalidSettingsException ise) {
                // ok, old data cells
                DataCell[] extremeKeysOld = extrOutlierSubConfig
                    .getDataCellArray(
                        CFG_EXTREME + CFG_ROW + spec.getName() + j);
                extremeKeys = new String[extremeKeysOld.length];
                for (int i = 0; i < extremeKeysOld.length; i++) {
                    extremeKeys[i] = extremeKeysOld[i].toString();
                }
            }
            Set<RowKey>keys = new HashSet<RowKey>();
            for (int rk = 0; rk < extremeKeys.length; rk++) {
                keys.add(new RowKey(extremeKeys[rk]));
            }
            extremeOutliers.put(extreme[j], keys);
        }
        m_extremeOutliers.put(spec.getName(), extremeOutliers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        try {
        NodeSettings settings = new NodeSettings(FILE_NAME);
        settings.addInt(CFG_NR_COLS, m_statistics.size());
        int i = 0;
        for (DataColumnSpec spec : m_statistics.keySet()) {
            NodeSettings colSetting = (NodeSettings)settings.addConfig(
                    CFG_COL + i++);
            spec.save(colSetting);
        }
        for (Map.Entry<DataColumnSpec, double[]> entry : m_statistics
                .entrySet()) {
            String cfgName = entry.getKey().getName();
            settings.addDoubleArray(CFG_STATS + cfgName, entry.getValue());
            // mild outliers
            Map<Double, Set<RowKey>>mildOutliers =  m_mildOutliers.get(cfgName);
            double[] mild = new double[mildOutliers.size()];
            Config mildKeysSubConfig = settings.addConfig(CFG_MILD + CFG_ROW
                    + cfgName);
            int j = 0;
            for (Map.Entry<Double, Set<RowKey>> mildEntry : mildOutliers
                    .entrySet()) {
                mild[j] = mildEntry.getKey();
                // save method -> savely store string from now on
                String[] keys = new String[mildEntry.getValue().size()];
                int rk = 0;
                for (RowKey key : mildEntry.getValue()) {
                    keys[rk] = key.getString();
                    rk++;
                }
                mildKeysSubConfig.addStringArray(CFG_MILD + CFG_ROW
                    + cfgName + j, keys);
                j++;
            }
            settings.addDoubleArray(CFG_MILD + cfgName, mild);
//            settings.addDataCellArray(CFG_MILD + CFG_ROW + cfgName, mildKeys);
            Map<Double, Set<RowKey>>extremeOutliers =  m_extremeOutliers
                .get(cfgName);
            double[] extreme = new double[extremeOutliers.size()];
            int ext = 0;
            Config extKeysSubConfig = settings.addConfig(CFG_EXTREME + CFG_ROW
                    + cfgName);
            for (Map.Entry<Double, Set<RowKey>> extrEntry : extremeOutliers
                    .entrySet()) {
                extreme[ext] = extrEntry.getKey();
                // save method -> save store strings from now on
                String[] keys = new String[extrEntry.getValue().size()];
                int rk = 0;
                for (RowKey key : extrEntry.getValue()) {
                    keys[rk] = key.getString();
                    rk++;
                }
                extKeysSubConfig.addStringArray(
                        CFG_EXTREME + CFG_ROW + cfgName + ext, keys);
                ext++;
            }
            settings.addDoubleArray(CFG_EXTREME + cfgName, extreme);
//            settings.addDataCellArray(CFG_EXTREME + CFG_ROW + cfgName,
//                    extremeKeys);
        }
        File f = new File(nodeInternDir, FILE_NAME);
        FileOutputStream fos = new FileOutputStream(f);
        settings.saveToXML(fos);
        File dataFile = new File(nodeInternDir, ARRAY_FILE);
        DataContainer.writeToZip(m_array, dataFile, exec);
        } catch (IOException e) {
            LOGGER.warn(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // so far no settings

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // so far no settings
    }

}
