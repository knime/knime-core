/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   29.09.2006 (Fabian Dill): created
 */
package org.knime.exp.node.view.plotter.box;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.knime.core.data.def.StringCell;
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
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
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
    private Map<String, Map<Double, RowKey>>m_mildOutliers;
    private Map<String, Map<Double, RowKey>>m_extremeOutliers;
    
    private DataArray m_array;
    
    private final BoxPlotter m_plotter;
    
    
    /**
     * One input for the data one output for the parameters (median, 
     * quartiles and inter-quartile range(IQR).
     *
     */
    public BoxPlotNodeModel() {
        super(1, 1);
        m_plotter = new BoxPlotter();
    }
    
    /**
     * 
     * @return the box plotter of this node (with the correct hilite handler).
     */
    public BoxPlotter getPlotter() {
        return m_plotter;
    }
    

    /**
     * @see org.knime.core.node.NodeModel#setInHiLiteHandler(int, 
     * org.knime.core.node.property.hilite.HiLiteHandler)
     */
    @Override
    protected void setInHiLiteHandler(final int inIndex, 
            final HiLiteHandler hiLiteHdl) {
        m_plotter.setHiLiteHandler(hiLiteHdl);
    }



    /**
     * @see org.knime.core.node.NodeModel#configure(
     * org.knime.core.data.DataTableSpec[])
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
            setWarningMessage("Only numeric columns are displayed!");
        } 
        return new DataTableSpec[]{createOutputSpec(numericCols)};
    }

    /**
     * @see org.knime.core.node.NodeModel#execute(
     * org.knime.core.node.BufferedDataTable[], 
     * org.knime.core.node.ExecutionContext)
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
            = new LinkedHashMap<String, Map<Double, RowKey>>();
        m_extremeOutliers 
            = new LinkedHashMap<String, Map<Double, RowKey>>();
        int colIdx = 0;
        int rowNr = table.getRowCount();
        float medianPos = rowNr / 2;
        float lowerQuartilePos = rowNr * 0.25f;
        float upperQuartilePos = rowNr * 0.75f;
        List<DataColumnSpec> outputColSpecs = new ArrayList<DataColumnSpec>();
        double subProgress = 1.0 / (double)table.getDataTableSpec()
            .getNumColumns();
        int currColumn = 0;
        for (DataColumnSpec colSpec : table.getDataTableSpec()) {
            exec.checkCanceled();
            if (colSpec.getType().isCompatible(DoubleValue.class)) {
                double[] statistic = new double[SIZE];
                statistic[MIN] = ((DoubleValue)colSpec.getDomain()
                        .getLowerBound()).getDoubleValue();
                statistic[MAX] = ((DoubleValue)colSpec.getDomain()
                        .getUpperBound()).getDoubleValue();
                outputColSpecs.add(colSpec);
                double progress = currColumn++ * subProgress;
                exec.setProgress(progress, "sorting: " + table
                        .getDataTableSpec().getColumnSpec(colIdx).getName());
//                System.out.println("progress: " + subProgress);
                List<String> col = new ArrayList<String>();
                col.add(colSpec.getName());
                SortedTable sorted = new SortedTable(table, 
                        col, new boolean[]{true}, 
                        exec);
                int currRow = 0;
                double lastValue = 1;
                for (DataRow row : sorted) {
                    if (row.getCell(colIdx).isMissing()) {
                        continue;
                    }
                    if (currRow == Math.ceil(lowerQuartilePos)) {
                        if (lowerQuartilePos % 1 == 0) {
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
                    if (currRow == Math.ceil(medianPos)) {
                        if (medianPos % 1 == 0) {
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
                    if (currRow == Math.ceil(upperQuartilePos)) {
                        if (upperQuartilePos % 1 == 0) {
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
                    currRow++;
                }
                double irq = statistic[UPPER_QUARTILE] 
                                       - statistic[LOWER_QUARTILE];
                Map<Double, RowKey> mild = new LinkedHashMap<Double, RowKey>();
                Map<Double, RowKey>extreme 
                    = new LinkedHashMap<Double, RowKey>();
                // per default the whiskers are at min and max
                double[] whiskers = new double[]{
                        statistic[MIN],
                        statistic[MAX]
                };
                if (statistic[MIN] < (statistic[LOWER_QUARTILE] 
                    - (1.5 * irq)) || statistic[MAX] 
                    > statistic[UPPER_QUARTILE] + (1.5 * irq)) {
                        detectOutliers(sorted, irq, 
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
        DataTableSpec outSpec = createOutputSpec(outputColSpecs);
        DataContainer container = exec.createDataContainer(outSpec);
        DataCell[] rowKeys = new DataCell[SIZE];
        rowKeys[MIN] = new StringCell("Minimum");
        rowKeys[LOWER_WHISKER] = new StringCell("Smallest");
        rowKeys[LOWER_QUARTILE] = new StringCell("Lower Quartile");
        rowKeys[MEDIAN] = new StringCell("Median");
        rowKeys[UPPER_QUARTILE] = new StringCell("Upper Quartile");
        rowKeys[UPPER_WHISKER] = new StringCell("Largest");
        rowKeys[MAX] = new StringCell("Maximum");
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
        // return a data array with just one row but with the data table spec 
        // for the column selection panel
        m_array = new DefaultDataArray(table, 1, 2);
        return new BufferedDataTable[]{exec.createBufferedDataTable(
                container.getTable(), exec)};
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
            final Map<Double, RowKey>mild, final Map<Double, RowKey>extreme, 
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
                    mild.put(value, row.getKey());
                } else {
                    // extreme
                    extreme.put(value, row.getKey());
                }
            } else if (value > q[1] + (1.5 * iqr)) {
                // upper outlier
                searchUpperWhisker = false;
                if (value < q[1] + (3.0 * iqr)) {
                    // mild 
                    mild.put(value, row.getKey());
                } else {
                    // extreme
                    extreme.put(value, row.getKey());
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
     * 
     * @see org.knime.exp.node.view.plotter.box.BoxPlotDataProvider#
     * getStatistics()
     */
    public Map<DataColumnSpec, double[]>getStatistics() {
        return m_statistics;
    }
    
    /**
     * 
     * @see org.knime.exp.node.view.plotter.box.BoxPlotDataProvider#
     * getMildOutliers()
     */
    public Map<String, Map<Double, RowKey>> getMildOutliers() {
        return m_mildOutliers;
    }
    
    /**
     * 
     * @see org.knime.exp.node.view.plotter.box.BoxPlotDataProvider#
     * getExtremeOutliers()
     */
    public Map<String, Map<Double, RowKey>> getExtremeOutliers() {
        return m_extremeOutliers;
    }
    

    /**
     * @see org.knime.exp.node.view.plotter.DataProvider#
     * getDataArray(int)
     */
    public DataArray getDataArray(final int index) {
        return m_array;
    }

    /**
     * @see org.knime.core.node.NodeModel#loadValidatedSettingsFrom(
     * org.knime.core.node.NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // so far no settings
    }

    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_statistics = null;
        m_array = null;
    }
    
    /**
     * @see org.knime.core.node.NodeModel#loadInternals(java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
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
            m_mildOutliers = new LinkedHashMap<String, Map<Double, RowKey>>();
            m_extremeOutliers 
                = new LinkedHashMap<String, Map<Double, RowKey>>();
            int nrOfCols = settings.getInt(CFG_NR_COLS);
            for (int i = 0; i < nrOfCols; i++) {
                NodeSettings subSetting = (NodeSettings)settings
                        .getConfig(CFG_COL + i);
                DataColumnSpec spec = DataColumnSpec.load(subSetting);
                double[] stats = settings.getDoubleArray(CFG_STATS 
                        + spec.getName());
                m_statistics.put(spec, stats);
                double[] mild = settings.getDoubleArray(CFG_MILD 
                        + spec.getName());
                DataCell[] mildKeys = settings.getDataCellArray(
                        CFG_MILD + CFG_ROW + spec.getName());
                Map<Double, RowKey> mildOutliers 
                    = new LinkedHashMap<Double, RowKey>();
                for (int j = 0; j < mild.length; j++) {
                    mildOutliers.put(mild[j], new RowKey(mildKeys[j]));
                }
                m_mildOutliers.put(spec.getName(), mildOutliers);
                // extreme 
                Map<Double, RowKey> extremeOutliers 
                = new LinkedHashMap<Double, RowKey>();
                double[] extreme = settings.getDoubleArray(CFG_EXTREME 
                        + spec.getName());
                DataCell[] extremeKeys = settings.getDataCellArray(
                        CFG_EXTREME + CFG_ROW + spec.getName());
                for (int j = 0; j < extreme.length; j++) {
                    extremeOutliers.put(extreme[j], new RowKey(extremeKeys[j]));
                }
                m_extremeOutliers.put(spec.getName(), extremeOutliers);
            }
            File data = new File(nodeInternDir, ARRAY_FILE);
            ContainerTable table = DataContainer.readFromZip(data);
            m_array = new DefaultDataArray(table, 1, 2, exec);
        } catch (Exception e) {
            LOGGER.warn(e);
            throw new IOException(e.getMessage());
        }
    }
    

    /**
     * @see org.knime.core.node.NodeModel#saveInternals(java.io.File, 
     * org.knime.core.node.ExecutionMonitor)
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
            Map<Double, RowKey>mildOutliers =  m_mildOutliers.get(cfgName);
            double[] mild = new double[mildOutliers.size()];
            DataCell[] mildKeys = new DataCell[mildOutliers.size()];
            int j = 0;
            for (Map.Entry<Double, RowKey> mildEntry : mildOutliers
                    .entrySet()) {
                mild[j] = mildEntry.getKey();
                mildKeys[j] = mildEntry.getValue().getId();
                j++;
            }
            settings.addDoubleArray(CFG_MILD + cfgName, mild);
            settings.addDataCellArray(CFG_MILD + CFG_ROW + cfgName, mildKeys);
            Map<Double, RowKey>extremeOutliers =  m_extremeOutliers
                .get(cfgName);
            double[] extreme = new double[extremeOutliers.size()];
            DataCell[] extremeKeys = new DataCell[extremeOutliers.size()];
            int ext = 0;
            for (Map.Entry<Double, RowKey> extrEntry : extremeOutliers
                    .entrySet()) {
                extreme[ext] = extrEntry.getKey();
                extremeKeys[ext] = extrEntry.getValue().getId();
                ext++;
            }
            settings.addDoubleArray(CFG_EXTREME + cfgName, extreme);
            settings.addDataCellArray(CFG_EXTREME + CFG_ROW + cfgName, 
                    extremeKeys);
        }
        File f = new File(nodeInternDir, FILE_NAME);
        FileOutputStream fos = new FileOutputStream(f);
        settings.saveToXML(fos);
        File dataFile = new File(nodeInternDir, ARRAY_FILE);
        DataContainer.writeToZip(m_array, dataFile, exec);
        } catch (Exception e) {
            LOGGER.warn(e);
        }
    }

    /**
     * @see org.knime.core.node.NodeModel#saveSettingsTo(
     * org.knime.core.node.NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // so far no settings

    }

    /**
     * @see org.knime.core.node.NodeModel#validateSettings(
     * org.knime.core.node.NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // so far no settings
    }

}
