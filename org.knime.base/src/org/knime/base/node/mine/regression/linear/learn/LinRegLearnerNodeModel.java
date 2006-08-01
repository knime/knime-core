/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   Feb 22, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.regression.linear.learn;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.base.node.mine.regression.linear.LinearRegressionParams;
import org.knime.base.node.mine.regression.linear.view.LinRegDataProvider;
import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * NodeModel to the linear regression learner node. It performs the calculation.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class LinRegLearnerNodeModel extends NodeModel implements
        LinRegDataProvider {

    /** Logger to print debug info to. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(LinRegLearnerNodeModel.class);

    /** Settings object key for the starting row. */
    static final String CFG_FROMROW = "FromRow";

    /** Settings object key for the number of row to display. */
    static final String CFG_ROWCNT = "RowCount";

    /** Key for the included columns, used for dialog settings. */
    static final String CFG_VARIATES = "included_columns";

    /** Key for the target column, used for dialog settings. */
    static final String CFG_TARGET = "target";

    /** Key for flag if to compute error on training data. */
    static final String CFG_CALC_ERROR = "calc_error";

    /** The column names to include. */
    private String[] m_includes;

    /** The response column. */
    private String m_target;

    /** If to calculate the error on the training data. */
    private boolean m_isCalcError;

    /** The first row to paint. */
    private int m_firstRowPaint = 1;

    /** The row count to paint. */
    private int m_rowCountPaint = 20000;

    /** The row container for the line view. */
    private DataArray m_rowContainer;

    /* Some statics for the view */
    private int m_nrRows;

    private int m_nrRowsSkipped;

    private double m_error;

    /** The learned values and also the means for each input variable. */
    private LinearRegressionParams m_params;

    /** Inits a new node model, it will have 1 data input and 1 model output. */
    public LinRegLearnerNodeModel() {
        super(1, 0, 0, 1);
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_includes != null) {
            settings.addStringArray(CFG_VARIATES, m_includes);
            settings.addString(CFG_TARGET, m_target);
            settings.addBoolean(CFG_CALC_ERROR, m_isCalcError);
            settings.addInt(CFG_FROMROW, m_firstRowPaint);
            settings.addInt(CFG_ROWCNT, m_rowCountPaint);
        }
    }

    /**
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String[] includes = settings.getStringArray(CFG_VARIATES);
        if (includes == null || includes.length == 0) {
            throw new InvalidSettingsException(
                    "No columns for regression have been set.");
        }
        String target = settings.getString(CFG_TARGET);
        if (target == null) {
            throw new InvalidSettingsException("No target set.");
        }
        List<String> asList = Arrays.asList(includes);
        if (asList.contains(null)) {
            throw new InvalidSettingsException("Included columns "
                    + "must not contain null values");
        }
        if (asList.contains(target)) {
            throw new InvalidSettingsException("Included columns "
                    + "must not contain target value: " + target);
        }
        settings.getBoolean(CFG_CALC_ERROR);

        // the row indices to paint
        int first = settings.getInt(CFG_FROMROW);
        int count = settings.getInt(CFG_ROWCNT);
        if (first < 0) {
            throw new InvalidSettingsException("Invalid start row: " + first);
        }
        if (count < 0) {
            throw new InvalidSettingsException("Invalid row count: " + count);
        }
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_includes = settings.getStringArray(CFG_VARIATES);
        m_target = settings.getString(CFG_TARGET);
        m_isCalcError = settings.getBoolean(CFG_CALC_ERROR);
        m_firstRowPaint = settings.getInt(CFG_FROMROW);
        m_rowCountPaint = settings.getInt(CFG_ROWCNT);
    }

    /**
     * @see NodeModel#execute(BufferedDataTable[], ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        /*
         * What comes next is the matrix calculation, solving A \times w = b
         * where A is the matrix having the training data (as many rows as there
         * are rows in inData[0], w is the vector of weights to learn (number of
         * variables) and b is the target output
         */
        // reset was called, must be cleared
        final BufferedDataTable data = inData[0];
        final DataTableSpec spec = data.getDataTableSpec();
        final int nrUnknown = m_includes.length + 1;
        double[] means = new double[m_includes.length];
        // indizes of the columns in m_includes
        final int[] colIndizes = new int[m_includes.length];
        for (int i = 0; i < m_includes.length; i++) {
            colIndizes[i] = spec.findColumnIndex(m_includes[i]);
        }
        // index of m_target
        final int target = spec.findColumnIndex(m_target);
        // this is the matrix (A^T x A) where A is the training data including
        // one column fixed to one.
        // (we do it here manually in order to avoid to get all the data in
        // double[][])
        double[][] ata = new double[nrUnknown][nrUnknown];
        double[] buffer = new double[nrUnknown];
        // we memorize for each row if it contains missing values.
        BitSet missingSet = new BitSet();
        m_nrRows = data.getRowCount();
        int myProgress = 0;
        // we need 2 or 3 scans on the data (first run was done already)
        final double totalProgress = (2 + (m_isCalcError ? 1 : 0)) * m_nrRows;
        int rowCount = 0;
        boolean hasPrintedWarning = false;
        for (RowIterator it = data.iterator(); it.hasNext(); rowCount++) {
            DataRow row = it.next();
            myProgress++;
            exec.setProgress(myProgress / totalProgress, "Calculating matrix "
                    + (rowCount + 1) + " (\"" + row.getKey().getId() + "\")");
            exec.checkCanceled();
            DataCell targetValue = row.getCell(target);
            // read data from row into buffer, skip missing value rows
            boolean containsMissing = targetValue.isMissing()
                    || readIntoBuffer(row, buffer, colIndizes);
            missingSet.set(rowCount, containsMissing);
            if (containsMissing) {
                String errorMessage = "Row \"" + row.getKey().getId()
                        + "\" contains missing values, skipping it.";
                if (!hasPrintedWarning) {
                    LOGGER.warn(errorMessage + " Suppress further warnings.");
                    hasPrintedWarning = true;
                } else {
                    LOGGER.debug(errorMessage);
                }
                m_nrRowsSkipped++;
                continue; // with next row
            }
            updateMean(buffer, means);
            // the matrix is symmetric
            for (int i = 0; i < nrUnknown; i++) {
                for (int j = 0; j < nrUnknown; j++) {
                    ata[i][j] += buffer[i] * buffer[j];
                }
            }
        }
        assert (m_nrRows == rowCount);
        normalizeMean(means);
        // no unique solution when there are less rows than unknown variables
        if (rowCount <= nrUnknown) {
            throw new Exception("Too few rows to perform regression ("
                    + rowCount + " rows, but degree of freedom of " + nrUnknown
                    + ")");
        }
        exec.setMessage("Calculating pseudo inverse...");
        double[][] ataInverse = MatrixOperation.inverse(ata);
        checkForNaN(ataInverse);
        // multiply with A^T and b, i.e. (A^T x A)^-1 x A^T x b
        double[] outcome = new double[nrUnknown];
        rowCount = 0;
        for (RowIterator it = data.iterator(); it.hasNext(); rowCount++) {
            DataRow row = it.next();
            exec.setMessage("Determining output " + (rowCount + 1) + " (\""
                    + row.getKey().getId() + "\")");
            myProgress++;
            exec.setProgress(myProgress / totalProgress);
            exec.checkCanceled();
            // does row containg missing values?
            if (missingSet.get(rowCount)) {
                // error has printed above, silently ignore here.
                continue;
            }
            boolean containsMissing = readIntoBuffer(row, buffer, colIndizes);
            assert !containsMissing;
            DataCell targetValue = row.getCell(target);
            double b = ((DoubleValue)targetValue).getDoubleValue();
            for (int i = 0; i < nrUnknown; i++) {
                double buf = 0.0;
                for (int j = 0; j < nrUnknown; j++) {
                    buf += ataInverse[i][j] * buffer[j];
                }
                outcome[i] += buf * b;
            }
        }

        if (m_isCalcError) {
            assert m_error == 0.0;
            rowCount = 0;
            for (RowIterator it = data.iterator(); it.hasNext(); rowCount++) {
                DataRow row = it.next();
                exec.setMessage("Calculating error " + (rowCount + 1) + " (\""
                        + row.getKey().getId() + "\")");
                myProgress++;
                exec.setProgress(myProgress / totalProgress);
                exec.checkCanceled();
                // does row containg missing values?
                if (missingSet.get(rowCount)) {
                    // error has printed above, silently ignore here.
                    continue;
                }
                boolean hasMissing = readIntoBuffer(row, buffer, colIndizes);
                assert !hasMissing;
                DataCell targetValue = row.getCell(target);
                double b = ((DoubleValue)targetValue).getDoubleValue();
                double out = 0.0;
                for (int i = 0; i < nrUnknown; i++) {
                    out += outcome[i] * buffer[i];
                }
                m_error += (b - out) * (b - out);
            }
        }
        m_params = createParams(outcome, means);
        // cache the entire table as otherwise the color information
        // may be lost (filtering out the "colored" column)
        m_rowContainer = new DefaultDataArray(data, m_firstRowPaint,
                m_rowCountPaint);
        return new BufferedDataTable[0];
    }

    /**
     * Checks if the array contains {@link Double#NaN} and throws an
     * {@link ArithmeticException} if it does.
     * 
     * @param d the array to check
     */
    private static void checkForNaN(final double[][] d) {
        for (int i = 0; i < d.length; i++) {
            for (int j = 0; j < d[i].length; j++) {
                if (Double.isNaN(d[i][j])) {
                    StringBuffer sbuffer = new StringBuffer();
                    sbuffer.append("Could not calculate inverse matrix, ");
                    sbuffer.append("got NaN.\n");
                    sbuffer.append("Possible fixes:\n");
                    sbuffer.append("  - Remove outliers?");
                    sbuffer.append("  - Select less columns?\n");
                    sbuffer.append("  - Normalize the data?\n");
                    throw new ArithmeticException(sbuffer.toString());
                }
            }
        }
    }

    /**
     * Reads from row <code>row</code> the values into the buffer
     * <code>buffer</code>. The first element will be fixed to 1.0, the
     * remaining values are filled with the (double) values from row according
     * to the order defined by colIndizes. This method is used in the execute
     * method.
     * 
     * @param row the row to read from
     * @param buffer to read into, one more element than colIndizes
     * @param colIndizes the column indizes to use
     * @return if the row contains a missing value in one of the cells of
     *         interest
     */
    private static boolean readIntoBuffer(final DataRow row,
            final double[] buffer, final int[] colIndizes) {
        boolean containsMissing = false;
        for (int i = 0; i < buffer.length && !containsMissing; i++) {
            if (i == 0) {
                buffer[i] = 1.0;
            } else {
                DataCell cell = row.getCell(colIndizes[i - 1]);
                if (cell.isMissing()) {
                    containsMissing = true;
                } else {
                    buffer[i] = ((DoubleValue)cell).getDoubleValue();
                }
            }
        }
        return containsMissing;
    }

    /*
     * Adds the values in buffer to the m_mean array, needs to be normalized
     * over the number of rows, later on.
     */
    private void updateMean(final double[] buffer, final double[] means) {
        for (int i = 1; i < buffer.length; i++) {
            means[i - 1] += buffer[i];
        }
    }

    /* Devides all values in mean by the number of valid rows. */
    private void normalizeMean(final double[] means) {
        final double rowCount = getNrRows() - getNrRowsSkipped();
        for (int i = 0; i < means.length; i++) {
            means[i] /= rowCount;
        }
    }

    /*
     * Create the parameters object for the current execution, called at the
     * very end of execute().
     */
    private LinearRegressionParams createParams(final double[] outcome,
            final double[] means) {
        Map<String, Double> parMap = new LinkedHashMap<String, Double>();
        Map<String, Double> meansMap = new LinkedHashMap<String, Double>();
        // update the map
        for (int i = 0; i < outcome.length; i++) {
            // the offset will have the same name as the target column,
            // this allows the predictor node to assign the "correct" name
            // to the prediction column
            String name;
            double val;
            if (i == 0) {
                name = m_target;
            } else {
                name = m_includes[i - 1];
                double mean = means[i - 1];
                meansMap.put(name, mean);
            }
            val = outcome[i];
            parMap.put(name, val);
        }
        return new LinearRegressionParams(parMap, meansMap);
    }

    /**
     * @see NodeModel#saveModelContent(int, ModelContentWO)
     */
    @Override
    protected void saveModelContent(final int index,
            final ModelContentWO predParams) throws InvalidSettingsException {
        if (index != 0) {
            throw new IndexOutOfBoundsException("Invalid model index: " + index);
        }
        m_params.saveParams(predParams);
    }

    /**
     * @see NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_nrRowsSkipped = 0;
        m_nrRows = 0;
        m_error = 0.0;
        m_params = null;
    }

    /**
     * @see NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_includes == null) {
            throw new InvalidSettingsException("No settings available");
        }
        assert m_target != null;
        DataTableSpec spec = inSpecs[0];
        // array containing element from m_include and also m_target
        String[] mustHaveColumns = new String[m_includes.length + 1];
        System.arraycopy(m_includes, 0, mustHaveColumns, 0, m_includes.length);
        mustHaveColumns[m_includes.length] = m_target;
        // check if contained in data and if DoubleValue-compatible.
        for (String include : mustHaveColumns) {
            DataColumnSpec colSpec = spec.getColumnSpec(include);
            if (colSpec == null) {
                throw new InvalidSettingsException("No such column in data: "
                        + include);
            }
            if (!colSpec.getType().isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("Type of column \""
                        + include + "\" is not numeric: " + colSpec.getType());
            }
        }
        return new DataTableSpec[0];
    }

    /**
     * @return the nrRowsProcessed.
     */
    protected int getNrRows() {
        return m_nrRows;
    }

    /**
     * @return the nrRowsSkipped.
     */
    protected int getNrRowsSkipped() {
        return m_nrRowsSkipped;
    }

    /**
     * Returns <code>true</code> if model is avaiable, i.e. node has been
     * executed.
     * 
     * @return if model has been executed
     */
    protected boolean isDataAvailable() {
        return m_params != null;
    }

    /**
     * @return the parameters
     */
    protected Map<String, Double> getParametersMap() {
        if (m_params == null) {
            throw new IllegalStateException("Not executed.");
        }
        return m_params.getMap();
    }

    /**
     * Get all parameters to the currently learned model.
     * 
     * @return a reference to the current values
     * @see LinRegDataProvider#getParams()
     */
    public LinearRegressionParams getParams() {
        return m_params;
    }

    /**
     * @see LinRegDataProvider#getRowContainer()
     */
    public DataArray getRowContainer() {
        return m_rowContainer;
    }

    /**
     * @return the error
     */
    protected double getError() {
        return m_error;
    }

    /**
     * @return the isCalcError
     */
    protected boolean isCalcError() {
        return m_isCalcError;
    }

    private static final String FILE_DATA = "rowcontainer.zip";

    private static final String FILE_SAVE = "model.xml.gz";

    private static final String CFG_SETTINGS = "settings";

    private static final String CFG_NR_ROWS = "nr_rows";

    private static final String CFG_NR_ROWS_SKIPPED = "nr_rows_skipped";

    private static final String CFG_ERROR = "error";

    private static final String CFG_PARAMS = "params";

    /**
     * @see org.knime.core.node.NodeModel
     *      #loadInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        File inFile = new File(internDir, FILE_SAVE);
        ModelContentRO c = ModelContent.loadFromXML(new BufferedInputStream(
                new GZIPInputStream(new FileInputStream(inFile))));
        try {
            m_nrRows = c.getInt(CFG_NR_ROWS);
            m_nrRowsSkipped = c.getInt(CFG_NR_ROWS_SKIPPED);
            m_error = c.getDouble(CFG_ERROR);
            ModelContentRO parContent = c.getModelContent(CFG_PARAMS);
            m_params = LinearRegressionParams.loadParams(parContent);
        } catch (InvalidSettingsException ise) {
            IOException ioe = new IOException("Unable to restore state: "
                    + ise.getMessage());
            ioe.initCause(ise);
            throw ioe;
        }
        File dataFile = new File(internDir, FILE_DATA);
        ContainerTable t = DataContainer.readFromZip(dataFile);
        int rowCount = t.getRowCount();
        m_rowContainer = new DefaultDataArray(t, 1, rowCount, exec);
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #saveInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        ModelContent content = new ModelContent(CFG_SETTINGS);
        content.addInt(CFG_NR_ROWS, m_nrRows);
        content.addInt(CFG_NR_ROWS_SKIPPED, m_nrRowsSkipped);
        content.addDouble(CFG_ERROR, m_error);
        ModelContentWO parContent = content.addModelContent(CFG_PARAMS);
        m_params.saveParams(parContent);
        File outFile = new File(internDir, FILE_SAVE);
        content.saveToXML(new BufferedOutputStream(new GZIPOutputStream(
                new FileOutputStream(outFile))));
        File dataFile = new File(internDir, FILE_DATA);
        DataContainer.writeToZip(m_rowContainer, dataFile, exec);
    }
}
