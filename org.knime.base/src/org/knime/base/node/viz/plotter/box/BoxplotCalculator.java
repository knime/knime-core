/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   08.06.2015 (Alexander): created
 */
package org.knime.base.node.viz.plotter.box;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.knime.base.data.sort.SortedTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 *
 * @author Alexander Fillbrunn
 * @since 2.12
 */
public class BoxplotCalculator {

    private final static String MISSING_VALUES_CLASS = "Missing values";

    /**
     * List of excluded data cols (contain only missing values). For Box Plot.
     */
    private String[] m_excludedDataCols;

    /**
     * Map of "dataN -> K missing values". For Box Plot.
     */
    private LinkedHashMap<String, Long> m_numMissValPerCol;

    /**
     * List of excluded classes (contain only missing values). For Cond Box Plot.
     */
    private LinkedHashMap<String, String[]> excludedClasses;

    /**
     * Map of "classN -> K missing values" for each data column.
     */
    private LinkedHashMap<String, LinkedHashMap<String, Long>> m_ignoredMissVals;


    /**
     * @return the excludedDataCols
     */
    public String[] getExcludedDataCols() {
        return m_excludedDataCols;
    }

    /**
     * @return the numMissValPerCol
     */
    public LinkedHashMap<String, Long> getNumMissValPerCol() {
        return m_numMissValPerCol;
    }

    /**
     * @return the ignoredMissVals
     */
    public LinkedHashMap<String, LinkedHashMap<String, Long>> getIgnoredMissVals() {
        return m_ignoredMissVals;
    }

    /**
     * @return the excludedClasses
     */
    public LinkedHashMap<String, String[]> getExcludedClasses() {
        return excludedClasses;
    }

    /**
     * Creates a new default instance of the boxplot calculator.
     */
    public BoxplotCalculator() {

    }

    /**
     * Calculates statistics for a conditional box plot.
     * @param table the data table
     * @param catCol the column with the category values
     * @param numCol the numeric column
     * @param exec an execution context
     * @return A linked hash map with BoxplotStatistics for each category
     * @throws CanceledExecutionException when the user cancels the execution
     */
    public LinkedHashMap<String, LinkedHashMap<String, BoxplotStatistics>>
        calculateMultipleConditional(final BufferedDataTable table, final String catCol,
                                            final String[] numCol, final ExecutionContext exec)
                                                    throws CanceledExecutionException {
        DataTableSpec spec = table.getSpec();
        int catColIdx = spec.findColumnIndex(catCol);
        int[] numColIdxs = new int[numCol.length];
        for (int i = 0; i < numCol.length; i++) {
            numColIdxs[i] = spec.findColumnIndex(numCol[i]);
        }

        ArrayList<DataCell> vals = new ArrayList<>(spec.getColumnSpec(catColIdx).getDomain().getValues());
        Collections.sort(vals, new Comparator<DataCell>() {
            @Override
            public int compare(final DataCell o1, final DataCell o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });
        vals.add(new MissingCell(null));  // add Missing values class as it is never in specification

        // we need to have clear names, otherwise Missing values class will be taken as "?"
        ArrayList<String> catNames = new ArrayList<>(vals.size());
        for (DataCell cell : vals) {
            catNames.add(cell.isMissing() ? MISSING_VALUES_CLASS : cell.toString());
        }

        LinkedHashMap<String, LinkedHashMap<String, DataContainer>> containers = new LinkedHashMap<>();
        m_ignoredMissVals = new LinkedHashMap<>();

        for (int i = 0; i < numCol.length; i++) {
            LinkedHashMap<String, DataContainer> map = new LinkedHashMap<>();
            LinkedHashMap<String, Long> missValMap = new LinkedHashMap<>();
            for (DataCell c : vals) {
                String name = c.isMissing() ? MISSING_VALUES_CLASS : c.toString();
                map.put(name, exec.createDataContainer(new DataTableSpec(new String[] {"col"},
                    new DataType[] {DoubleCell.TYPE})));
                missValMap.put(name, 0L);
            }
            containers.put(numCol[i], map);
            m_ignoredMissVals.put(numCol[i], missValMap);
        }
        ExecutionContext subExec = exec.createSubExecutionContext(0.7);
        //long[][] ignoredMissVals = new long[numCol.length][vals.size()];  // count missing values per data col per class
        long count = 0;
        final long numOfRows = table.size();
        for (DataRow row : table) {
            exec.checkCanceled();
            subExec.setProgress(count++ / (double)numOfRows);
            DataCell catCell = row.getCell(catColIdx);
            String catName = catCell.isMissing() ? MISSING_VALUES_CLASS : catCell.toString();
            for (int i = 0; i < numCol.length; i++) {
                DataCell cell = row.getCell(numColIdxs[i]);
                if (!cell.isMissing()) {
                    containers.get(numCol[i]).get(catName).addRowToTable(
                        new DefaultRow(row.getKey(), cell));
                } else {
                    // increment missing values
                    LinkedHashMap<String, Long> missValMap = m_ignoredMissVals.get(numCol[i]);
                    missValMap.replace(catName, missValMap.get(catName) + 1);
                }
            }
        }

        LinkedHashMap<String, LinkedHashMap<String, BoxplotStatistics>> statsMap
        = new LinkedHashMap<>();
        excludedClasses = new LinkedHashMap<>();
        List<String> colList = Arrays.asList(numCol);

        ExecutionContext subExec2 = exec.createSubExecutionContext(1.0);
        int count2 = 0;
        for (Entry<String, LinkedHashMap<String, DataContainer>> entry : containers.entrySet()) {
            exec.checkCanceled();
            subExec2.setProgress(count2++ / (double)containers.size());
            LinkedHashMap<String, DataContainer> containers2 = entry.getValue();
            LinkedHashMap<String, BoxplotStatistics> colStats = new LinkedHashMap<String, BoxplotStatistics>();
            String colName = entry.getKey();

            List<String> excludedColClassesList = new ArrayList<>();
            LinkedHashMap<String, Long> ignoredColMissVals = new LinkedHashMap<>();

            for (Entry<String, DataContainer> entry2 : containers2.entrySet()) {
                Set<Outlier> extremeOutliers = new HashSet<Outlier>();
                Set<Outlier> mildOutliers = new HashSet<Outlier>();
                entry2.getValue().close();
                String catName = entry2.getKey();

                BufferedDataTable catTable = (BufferedDataTable)entry2.getValue().getTable();
                LinkedHashMap<String, Long> missValMap = m_ignoredMissVals.get(colName);
                if (catTable.size() == 0) {
                    if (!(catName.equals(MISSING_VALUES_CLASS) && missValMap.get(catName) == 0)) {  // we should add missing values to this list, only if they were there
                        excludedColClassesList.add(catName);
                    }
                    missValMap.remove(catName);
                    continue;
                } else {
                    if (missValMap.get(catName) == 0) {
                        missValMap.remove(catName);
                    }
                }

                SortedTable st = new SortedTable(catTable, new Comparator<DataRow>() {
                    @Override
                    public int compare(final DataRow o1, final DataRow o2) {
                        double d1 = ((DoubleValue)o1.getCell(0)).getDoubleValue();
                        double d2 = ((DoubleValue)o2.getCell(0)).getDoubleValue();
                        if (d1 == d2) {
                            return 0;
                        } else {
                            return d1 < d2 ? -1 : 1;
                        }
                    }
                }, false, exec);

                double min = 0, max = 0, q1 = 0, q3 = 0, median = 0;
                long q1Idx = catTable.size() / 4;
                long q3Idx = (long)Math.ceil((double)catTable.size() * 3 / 4);
                boolean dMedian = catTable.size() % 2 == 0;
                long medianIdx = catTable.size() / 2;
                int counter = 0;
                for (DataRow row : st) {
                    double val = ((DoubleValue)row.getCell(0)).getDoubleValue();
                    if (counter == 0) {
                        min = val;
                    }
                    if (counter == catTable.size() - 1) {
                        max = val;
                    }
                    if (counter == q1Idx || (counter == 0 && st.size() <= 3)) {
                        q1 = val;
                    }
                    if (counter == q3Idx || (counter == st.size() - 1 && st.size() <= 3)) {
                        q3 = val;
                    }
                    if (counter == medianIdx && dMedian) {
                        median = val;
                    } else if (counter == medianIdx) {
                        median = val;
                    } else if (counter == medianIdx + 1 && dMedian) {
                        median = (median + val) / 2;
                    }
                    counter++;
                }

                double iqr = q3 - q1;
                double lowerWhisker = min;
                double upperWhisker = max;
                double upperWhiskerFence = q3 + (1.5 * iqr);
                double lowerWhiskerFence = q1 - (1.5 * iqr);
                double lowerFence = q1 - (3 * iqr);
                double upperFence = q3 + (3 * iqr);

                for (DataRow row : st) {
                    double value = ((DoubleValue)row.getCell(0)).getDoubleValue();
                    String rowKey = row.getKey().getString();
                    if (value < lowerFence) {
                        extremeOutliers.add(new Outlier(value, rowKey));
                    } else if (value < lowerWhiskerFence) {
                        mildOutliers.add(new Outlier(value, rowKey));
                    } else if (lowerWhisker < lowerWhiskerFence
                            && value >= lowerWhiskerFence) {
                        lowerWhisker = value;
                    } else if (value <= upperWhiskerFence) {
                        upperWhisker = value;
                    } else if (value > upperFence) {
                        extremeOutliers.add(new Outlier(value, rowKey));
                    } else if (value > upperWhiskerFence) {
                        mildOutliers.add(new Outlier(value, rowKey));
                    }
                }

                colStats.put(catName, new BoxplotStatistics(mildOutliers, extremeOutliers,
                    min, max, lowerWhisker, q1, median, q3, upperWhisker));
            }
            statsMap.put(colName, colStats);

            // missing values part
            String[] excludedColClasses = excludedColClassesList.toArray(new String[excludedColClassesList.size()]);
            excludedClasses.put(colName, excludedColClasses);
        }

        return statsMap;
    }

    /**
     * Calculates the necessary statistics for a non-conditional boxplot.
     * @param table the input data
     * @param numCol array of names of numeric columns to plot
     * @param exec Execution context to report progress to
     * @return LinkedHashMap with the column name as key and statistics as value
     * @throws CanceledExecutionException when the user cancels the execution
     */
    public LinkedHashMap<String, BoxplotStatistics>
    calculateMultiple(final BufferedDataTable table, final String[] numCol, final ExecutionContext exec)
                                                throws CanceledExecutionException {
        DataTableSpec spec = table.getSpec();
        int[] numColIdxs = new int[numCol.length];
        for (int i = 0; i < numCol.length; i++) {
            numColIdxs[i] = spec.findColumnIndex(numCol[i]);
        }

        LinkedHashMap<String, DataContainer> containers = new LinkedHashMap<String, DataContainer>();
        for (int i = 0; i < numCol.length; i++) {
            containers.put(numCol[i], exec.createDataContainer(new DataTableSpec(new String[] {"col"},
                new DataType[] {DoubleCell.TYPE})));
        }

        ExecutionContext subExec = exec.createSilentSubExecutionContext(0.7);
        long[] numMissValPerCol = new long[numCol.length];
        int count = 0;
        for (DataRow row : table) {
            exec.checkCanceled();
            subExec.setProgress((double)count++ / table.size());
            for (int i = 0; i < numCol.length; i++) {
                DataCell cell = row.getCell(numColIdxs[i]);
                if (!cell.isMissing()) {
                    containers.get(numCol[i]).addRowToTable(
                        new DefaultRow(row.getKey(), cell));
                } else {
                    numMissValPerCol[i]++;
                }
            }
        }

        LinkedHashMap<String, BoxplotStatistics> statsMap
        = new LinkedHashMap<>();

        ExecutionContext subExec2 = exec.createSilentSubExecutionContext(1.0);
        count = 0;

        List<String> excludedDataColList = new ArrayList<String>();
        for (Entry<String, DataContainer> entry : containers.entrySet()) {
            exec.checkCanceled();
            subExec2.setProgress((double)count++ / containers.size());
            Set<Outlier> extremeOutliers = new HashSet<Outlier>();
            Set<Outlier> mildOutliers = new HashSet<Outlier>();
            entry.getValue().close();

            BufferedDataTable catTable = (BufferedDataTable)entry.getValue().getTable();
            if (catTable.size() == 0) {
                excludedDataColList.add(entry.getKey());
                continue;
            }

            SortedTable st = new SortedTable(catTable, new Comparator<DataRow>() {
                @Override
                public int compare(final DataRow o1, final DataRow o2) {
                    DataCell c1 = o1.getCell(0);
                    DataCell c2 = o2.getCell(0);
                    double d1 = ((DoubleValue)c1).getDoubleValue();
                    double d2 = ((DoubleValue)c2).getDoubleValue();
                    if (d1 == d2) {
                        return 0;
                    } else {
                        return d1 < d2 ? -1 : 1;
                    }
                }
            }, false, exec);

            double min = 0, max = 0, q1 = 0, q3 = 0, median = 0;
            long q1Idx = catTable.size() / 4;
            long q3Idx = (long)Math.ceil((double)catTable.size() * 3 / 4);
            boolean dMedian = catTable.size() % 2 == 0;
            long medianIdx = catTable.size() / 2;
            int counter = 0;
            for (DataRow row : st) {
                double val = ((DoubleValue)row.getCell(0)).getDoubleValue();
                if (counter == 0) {
                    min = val;
                }
                if (counter == catTable.size() - 1) {
                    max = val;
                }
                if (counter == q1Idx || (counter == 0 && st.size() <= 3)) {
                    q1 = val;
                }
                if (counter == q3Idx || (counter == st.size() - 1 && st.size() <= 3)) {
                    q3 = val;
                }
                if (counter == medianIdx && dMedian) {
                    median = val;
                } else if (counter == medianIdx) {
                    median = val;
                } else if (counter == medianIdx + 1 && dMedian) {
                    median = (median + val) / 2;
                }
                counter++;
            }

            double iqr = q3 - q1;
            double lowerWhisker = min;
            double upperWhisker = max;
            double upperWhiskerFence = q3 + (1.5 * iqr);
            double lowerWhiskerFence = q1 - (1.5 * iqr);
            double lowerFence = q1 - (3 * iqr);
            double upperFence = q3 + (3 * iqr);

            for (DataRow row : st) {
                double value = ((DoubleValue)row.getCell(0)).getDoubleValue();
                String rowKey = row.getKey().getString();

                if (value < lowerFence) {
                    extremeOutliers.add(new Outlier(value, rowKey));
                } else if (value < lowerWhiskerFence) {
                    mildOutliers.add(new Outlier(value, rowKey));
                } else if (lowerWhisker < lowerWhiskerFence
                        && value >= lowerWhiskerFence) {
                    lowerWhisker = value;
                } else if (value <= upperWhiskerFence) {
                    upperWhisker = value;
                } else if (value > upperFence) {
                    extremeOutliers.add(new Outlier(value, rowKey));
                } else if (value > upperWhiskerFence) {
                    mildOutliers.add(new Outlier(value, rowKey));
                }
            }

            statsMap.put(entry.getKey(), new BoxplotStatistics(mildOutliers, extremeOutliers,
                min, max, lowerWhisker, q1, median, q3, upperWhisker));
        }

        // missing values part
        m_excludedDataCols = excludedDataColList.toArray(new String[excludedDataColList.size()]);
        m_numMissValPerCol = new LinkedHashMap<String, Long>();
        for (int i = 0; i < numCol.length; i++) {
            if (numMissValPerCol[i] > 0 && !excludedDataColList.contains(numCol[i])) {
                m_numMissValPerCol.put(numCol[i], numMissValPerCol[i]);
            }
        }

        return statsMap;
    }
}
