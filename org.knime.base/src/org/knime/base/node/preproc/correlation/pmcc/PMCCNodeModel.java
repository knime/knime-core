/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Feb 17, 2007 (wiswedel): created
 */
package org.knime.base.node.preproc.correlation.pmcc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.base.data.normalize.Normalizer;
import org.knime.base.data.statistics.StatisticsTable;
import org.knime.base.util.HalfDoubleMatrix;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class PMCCNodeModel extends NodeModel 
    implements BufferedDataTableHolder {
    
    private final SettingsModelFilterString m_columnIncludesList;
    private final SettingsModelIntegerBounded m_maxPossValueCountModel;
    
    private BufferedDataTable m_correlationTable;

    /** One input, one output.
     */
    public PMCCNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE},
                new PortType[]{BufferedDataTable.TYPE, 
                PMCCPortObjectAndSpec.TYPE});
        m_columnIncludesList = createNewSettingsObject();
        m_maxPossValueCountModel = createNewPossValueCounterModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        final BufferedDataTable in = (BufferedDataTable)inData[0];
        final double rC = in.getRowCount(); // floating point operation
        int[] includes = getIncludes(in.getDataTableSpec());
        String[] includeNames = m_columnIncludesList.getIncludeList().toArray(
                new String[0]);
        double progNormalize = 0.3;
        double progDetermine = 0.65;
        double progFinish = 1.0 - progNormalize - progDetermine;
        exec.setMessage("Normalizing data");
        final ExecutionMonitor normProg = exec.createSubProgress(progNormalize);
        FilterColumnTable filterTable = new FilterColumnTable(in, includes);
        final int l = includes.length;
        int nomCount = (l - 1) * l / 2;
        final HalfDoubleMatrix nominatorMatrix = 
            new HalfDoubleMatrix(includes.length, /*withDiagonal*/false);
        nominatorMatrix.fill(Double.NaN);
        @SuppressWarnings("unchecked")
        final LinkedHashMap<DataCell, Integer>[] possibleValues = 
            new LinkedHashMap[l];
        DataTableSpec filterTableSpec = filterTable.getDataTableSpec();
        for (int i = 0; i < l; i++) {
            DataColumnSpec cs = filterTableSpec.getColumnSpec(i);
            if (cs.getType().isCompatible(NominalValue.class)) {
                possibleValues[i] = new LinkedHashMap<DataCell, Integer>();
            }
        }
        final int possValueUpperBound = m_maxPossValueCountModel.getIntValue();
        // determines possible values. We can't use those from the domain
        // as the domain can also contain values not present in the data 
        // but in the contingency table we need rows/columns to have at least
        // one cell with a value >= 1
        StatisticsTable statTable = new StatisticsTable(filterTable) {
            // that is sort of the constructor in this derived class
            { calculateAllMoments(in.getRowCount(), normProg); }
            @Override
            protected void calculateMomentInSubClass(final DataRow row) {
                for (int i = 0; i < l; i++) {
                    if (possibleValues[i] != null) {
                        DataCell c = row.getCell(i);
                        // note: also take missing value as possible value
                        possibleValues[i].put(c, null);
                        if (possibleValues[i].size() > possValueUpperBound) {
                            possibleValues[i] = null;
                        }
                    }
                }
            }
        };
        for (LinkedHashMap<DataCell, Integer> map : possibleValues) {
            if (map != null) {
                int index = 0;
                for (Map.Entry<DataCell, Integer> entry : map.entrySet()) {
                    entry.setValue(index++);
                }
            }
        }
        // stores all pair-wise contingency tables, 
        // contingencyTables[i] == null <--> either column of the corresponding
        // pair is non-categorical.
        // What is a contingency table?
        // http://en.wikipedia.org/wiki/Contingency_table
        int[][][] contingencyTables = new int[nomCount][][];
        // column which only contain one value - no correlation available
        LinkedHashSet<String> constantColumns = new LinkedHashSet<String>();
        int valIndex = 0;
        for (int i = 0; i < l; i++) {
            for (int j = i + 1; j < l; j++) {
                if (possibleValues[i] != null && possibleValues[j] != null) {
                    int iSize = possibleValues[i].size();
                    int jSize = possibleValues[j].size();
                    contingencyTables[valIndex] = new int[iSize][jSize];
                }
                DataColumnSpec colSpecI = filterTableSpec.getColumnSpec(i);
                DataColumnSpec colSpecJ = filterTableSpec.getColumnSpec(j);
                DataType ti = colSpecI.getType();
                DataType tj = colSpecJ.getType();
                if (ti.isCompatible(DoubleValue.class) 
                        && tj.isCompatible(DoubleValue.class)) {
                    // one of the two columns contains only one value
                    if (statTable.getVariance(i) 
                            < PMCCPortObjectAndSpec.ROUND_ERROR_OK) {
                        constantColumns.add(colSpecI.getName());
                        nominatorMatrix.set(i, j, Double.NaN);
                    } else if (statTable.getVariance(j) 
                            < PMCCPortObjectAndSpec.ROUND_ERROR_OK) {
                        constantColumns.add(colSpecJ.getName());
                        nominatorMatrix.set(i, j, Double.NaN);
                    } else {
                        nominatorMatrix.set(i, j, 0.0);
                    }
                }
                valIndex++;
            }
        }
        // column containing only one numeric value can't have a correlation
        // to other column (will be a missing value)
        if (!constantColumns.isEmpty()) {
            String[] constantColumnNames = constantColumns.toArray(
                    new String[constantColumns.size()]);
            NodeLogger.getLogger(getClass()).info("The following numeric " 
                    + "columns contain only one distinct value or have " 
                    + "otherwise a low standard deviation: " 
                    + Arrays.toString(constantColumnNames));
            int maxLength = 4;
            if (constantColumns.size() > maxLength) {
                constantColumnNames = 
                    Arrays.copyOf(constantColumnNames, maxLength);
                constantColumnNames[maxLength - 1] = "...";
            }
            setWarningMessage("Some columns contain only one distinct value: "
                    + Arrays.toString(constantColumnNames));
        }
        
        DataTable att;
        if (statTable.getNrRows() > 0) {
            att = new Normalizer(statTable, includeNames).doZScoreNorm(
                    exec.createSubProgress(0.0)); // no iteration needed
        } else {
            att = statTable;
        }
        normProg.setProgress(1.0);
        exec.setMessage("Calculating correlation measure");
        ExecutionMonitor detProg = exec.createSubProgress(progDetermine);
        int rowIndex = 0;
        double[] buf = new double[l];
        DataCell[] catBuf = new DataCell[l];
        boolean containsMissing = false;
        for (DataRow r : att) {
            detProg.checkCanceled();
            for (int i = 0; i < l; i++) {
                catBuf[i] = null;
                buf[i] = Double.NaN;
                DataCell c = r.getCell(i);
                // missing value is also a possible value here
                if (possibleValues[i] != null) {
                    catBuf[i] = c;
                } else if (c.isMissing()) {
                    containsMissing = true;
                } else if (filterTableSpec.getColumnSpec(i).getType()
                        .isCompatible(DoubleValue.class)) {
                    buf[i] = ((DoubleValue)c).getDoubleValue();
                }
            }
            valIndex = 0;
            for (int i = 0; i < l; i++) {
                for (int j = i + 1; j < l; j++) {
                    double b1 = buf[i];
                    double b2 = buf[j];
                    if (!Double.isNaN(b1) && !Double.isNaN(b2)) {
                        double old = nominatorMatrix.get(i, j);
                        nominatorMatrix.set(i, j, old + b1 * b2);
                    } else if (catBuf[i] != null && catBuf[j] != null) {
                        int iIndex = possibleValues[i].get(catBuf[i]);
                        assert iIndex >= 0 : "Value unknown in value list " 
                            + "of column " + includeNames[i] + ": " + catBuf[i];
                        int jIndex = possibleValues[j].get(catBuf[j]);
                        assert jIndex >= 0 : "Value unknown in value list " 
                            + "of column " + includeNames[j] + ": " + catBuf[j];
                        contingencyTables[valIndex][iIndex][jIndex]++;
                    }
                    valIndex++;
                }
            }
            rowIndex++;
            detProg.setProgress(rowIndex / rC, "Processing row " + rowIndex 
                    + " (\"" + r.getKey() + "\")");
        }
        if (containsMissing) {
            setWarningMessage("Some row(s) contained missing values.");
        }
        detProg.setProgress(1.0);
        double normalizer = 1.0 / (rC - 1.0);
        valIndex = 0;
        for (int i = 0; i < l; i++) {
            for (int j = i + 1; j < l; j++) {
                if (contingencyTables[valIndex] != null) {
                    nominatorMatrix.set(i, j, 
                            computeCramersV(contingencyTables[valIndex]));
                } else if (!Double.isNaN(nominatorMatrix.get(i, j))) {
                    double old = nominatorMatrix.get(i, j);
                    nominatorMatrix.set(i, j, old * normalizer);
                } // else pair of columns is double - string (for instance)
                valIndex++;
            }
        }
        normProg.setProgress(progDetermine);
        PMCCPortObjectAndSpec pmccModel = 
            new PMCCPortObjectAndSpec(includeNames, nominatorMatrix);
        ExecutionContext subExec = exec.createSubExecutionContext(progFinish);
        BufferedDataTable out = pmccModel.createCorrelationMatrix(subExec);
        m_correlationTable = out;
        return new PortObject[]{out, pmccModel};
    }
    
    /** Get for each included column the corresponding index. */
    private int[] getIncludes(final DataTableSpec spec) 
        throws InvalidSettingsException {
        List<String> includes = m_columnIncludesList.getIncludeList();
        int[] result = new int[includes.size()];
        for (int i = 0; i < result.length; i++) {
            int index = spec.findColumnIndex(includes.get(i));
            if (index < 0) {
                throw new InvalidSettingsException(
                        "Invalid column: " + includes.get(i));
            }
            result[i] = index;
        }
        return result;
    }
    
    /** Calculates G formula, for details see
     * http://en.wikipedia.org/wiki/Chi-square_test and 
     * http://planetmath.org/encyclopedia/CramersV.html.
     */
    private static double computeCramersV(final int[][] contingency) {
        if (contingency.length <= 1 || contingency[0].length <= 1) {
            return 0.0;
        }
        double[] rowSums = new double[contingency.length];
        double[] colSums = new double[contingency[0].length];
        double totalSum = 0.0;
        for (int i = 0; i < contingency.length; i++) {
            for (int j = 0; j < contingency[0].length; j++) {
                rowSums[i] += contingency[i][j];
                colSums[j] += contingency[i][j];
                totalSum += contingency[i][j];
            }
        }
        double chisquare = 0.0;
        for (int i = 0; i < contingency.length; i++) {
            for (int j = 0; j < contingency[0].length; j++) {
                double expected = rowSums[i] * colSums[j] / totalSum;
                // this is asserted as each row/column must contain at least
                // one value >= 1
                assert expected > 0.0 : "value should be > 0 " + expected;
                double diff = contingency[i][j] - expected;
                chisquare += diff * diff / expected; 
            }
        }
        int minValueCount = Math.min(rowSums.length , colSums.length) - 1;
        return Math.sqrt(chisquare / (totalSum * minValueCount));
    }
    
    /** Calculates G formula, for details see
     * http://en.wikipedia.org/wiki/G-test and 
     * http://planetmath.org/encyclopedia/CramersV.html.
     */
//    private static double computeGTestCramersV(final int[][] contingency) {
//        if (contingency.length <= 1 || contingency[0].length <= 1) {
//            return 0.0;
//        }
//        double[] rowSums = new double[contingency.length];
//        double[] colSums = new double[contingency[0].length];
//        double totalSum = 0.0;
//        for (int i = 0; i < contingency.length; i++) {
//            for (int j = 0; j < contingency[0].length; j++) {
//                rowSums[i] += contingency[i][j];
//                colSums[j] += contingency[i][j];
//                totalSum += contingency[i][j];
//            }
//        }
//        double g = 0.0;
//        for (int i = 0; i < contingency.length; i++) {
//            for (int j = 0; j < contingency[0].length; j++) {
//                double expected = rowSums[i] * colSums[j] / totalSum;
//                if (contingency[i][j] > 0) {
//                    g += contingency[i][j] 
//                        * Math.log(contingency[i][j] / expected);
//                }
//            }
//        }
//        g = 2.0 * g;
//        // FIXME: Use appropriate normalization here. Cramer's V doesn't work
//        // here.
//        int minValueCount = Math.min(rowSums.length , colSums.length) - 1;
//        return 
//          g / (minValueCount * Math.log(rowSums.length * colSums.length)); 
//    }

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
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec in = (DataTableSpec)inSpecs[0];
        if (!in.containsCompatibleType(DoubleValue.class) 
                && !in.containsCompatibleType(NominalValue.class)) {
            throw new InvalidSettingsException(
                    "Neither double nor nominal compatible columns in input");
        }
        List<String> includes = m_columnIncludesList.getIncludeList();
        List<String> excludes = m_columnIncludesList.getExcludeList();
        // not configured yet?
        if (includes.isEmpty() && excludes.isEmpty()) {
            ArrayList<String> includeDefault = new ArrayList<String>();
            for (DataColumnSpec s : in) {
                if (s.getType().isCompatible(DoubleValue.class)
                        || s.getType().isCompatible(NominalValue.class)) {
                    includeDefault.add(s.getName());
                }
            }
            m_columnIncludesList.setIncludeList(includeDefault);
            includes = m_columnIncludesList.getIncludeList();
            setWarningMessage("Auto configuration: Using all suitable "
                    + "columns (in total " + includes.size() + ")");
        }
        for (String s : includes) {
            DataColumnSpec sp = in.getColumnSpec(s);
            if (sp == null) {
                throw new InvalidSettingsException(
                        "No such column: " + s);
            }
            if (!sp.getType().isCompatible(DoubleValue.class)
                    && !sp.getType().isCompatible(NominalValue.class)) {
                throw new InvalidSettingsException("Column is neither double " 
                        + "nor nominal compatible: " + s);
            }
        }
        String[] toArray = includes.toArray(new String[includes.size()]);
        return new PortObjectSpec[]{PMCCPortObjectAndSpec.createOutSpec(
                toArray), new PMCCPortObjectAndSpec(toArray)};
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_columnIncludesList.saveSettingsTo(settings);
        m_maxPossValueCountModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        SettingsModelFilterString clone = 
            m_columnIncludesList.createCloneWithValidatedValue(settings);
        if (clone.getIncludeList().isEmpty()) {
            throw new InvalidSettingsException("No column selected");
        }
        m_maxPossValueCountModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_columnIncludesList.loadSettingsFrom(settings);
        m_maxPossValueCountModel.loadSettingsFrom(settings);
    }
    
    /**
     * Getter for correlation table to display. <code>null</code> if not
     * executed.
     * @return the correlationTable
     */
    public DataTable getCorrelationTable() {
        return m_correlationTable;
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
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }
    
    /** Factory method to instantiate a default settings object, used
     * in constructor and in dialog.
     * @return A new default settings object.
     */
    @SuppressWarnings("unchecked")
    static SettingsModelFilterString createNewSettingsObject() {
        return new SettingsModelFilterString(
            "includeList", Collections.EMPTY_LIST,  Collections.EMPTY_LIST);
    }
    
    /** Factory method to create the bounded range model for the 
     * possible values count.
     * @return A new model.
     */
    static SettingsModelIntegerBounded createNewPossValueCounterModel() {
        return new SettingsModelIntegerBounded(
                "possibleValuesCount", 50, 2, Integer.MAX_VALUE);
    }

    /** {@inheritDoc} */
    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[]{m_correlationTable};
    }

    /** {@inheritDoc} */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        m_correlationTable = tables[0];
    }

}
