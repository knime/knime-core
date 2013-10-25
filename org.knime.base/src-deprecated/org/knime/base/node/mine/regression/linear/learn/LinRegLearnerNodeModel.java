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
 *   Feb 22, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.regression.linear.learn;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.base.node.mine.regression.linear.LinearRegressionContent;
import org.knime.base.node.mine.regression.linear.view.LinRegDataProvider;
import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.base.util.math.MathUtils;
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
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;

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

    /** Key for whether to include all appropriate columns by default. */
    static final String CFG_VARIATES_USE_ALL = "included_columns_use_all";

    /** Key for the target column, used for dialog settings. */
    static final String CFG_TARGET = "target";

    /** Key for flag if to compute error on training data. */
    static final String CFG_CALC_ERROR = "calc_error";

    /** Key for flag which columns are included in the linear regression. */
    static final String  CFG_USED_COLUMNS = "used_columns";

    /** The column names to include. */
    private String[] m_includes;

    /** Whether to include all appropriate columns by default, overwrites
     * {@link #m_includes}. (added in v2.1) */
    private boolean m_includeAll;

    /** Names of the columns that were actually used. If {@link #m_includeAll}
     * is false, this is identical to {@link #m_includes}. */
    private String[] m_actualUsedColumns;

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
    private LinearRegressionContent m_params;

    /** Inits a new node model, it will have 1 data input, 1 optional
     * model input and 1 model output. */
    public LinRegLearnerNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE,
                new PortType(PMMLPortObject.class, true)},
                new PortType[]{PMMLPortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_target != null) {
            if (m_includeAll) {
                settings.addBoolean(CFG_VARIATES_USE_ALL, true);
            } else {
                settings.addStringArray(CFG_VARIATES, m_includes);
            }
            settings.addString(CFG_TARGET, m_target);
            settings.addBoolean(CFG_CALC_ERROR, m_isCalcError);
            settings.addInt(CFG_FROMROW, m_firstRowPaint);
            settings.addInt(CFG_ROWCNT, m_rowCountPaint);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String target = settings.getString(CFG_TARGET);
        if (target == null) {
            throw new InvalidSettingsException("No target set.");
        }
        // we check for null in the line below, improved error message
        String[] includes =
            settings.getStringArray(CFG_VARIATES, (String[])null);
        // added in v2.1
        boolean includeAll = settings.getBoolean(CFG_VARIATES_USE_ALL, false);
        if (!includeAll) {
            if (includes == null || includes.length == 0) {
                throw new InvalidSettingsException(
                        "No columns for regression have been set.");
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
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // field added in v2.1
        m_includeAll = settings.getBoolean(CFG_VARIATES_USE_ALL, false);
        if (m_includeAll) {
            m_includes = new String[0];
        } else {
            m_includes = settings.getStringArray(CFG_VARIATES);
        }
        m_target = settings.getString(CFG_TARGET);
        m_isCalcError = settings.getBoolean(CFG_CALC_ERROR);
        m_firstRowPaint = settings.getInt(CFG_FROMROW);
        m_rowCountPaint = settings.getInt(CFG_ROWCNT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        /*
         * What comes next is the matrix calculation, solving A \times w = b
         * where A is the matrix having the training data (as many rows as there
         * are rows in inData[0], w is the vector of weights to learn (number of
         * variables) and b is the target output
         */
        // reset was called, must be cleared
        final BufferedDataTable data = (BufferedDataTable)inData[0];
        final DataTableSpec spec = data.getDataTableSpec();
        String[] includes = computeIncludes(spec);
        m_actualUsedColumns = includes;
        final int nrUnknown = includes.length + 1;
        double[] means = new double[includes.length];
        // indices of the columns in m_includes
        final int[] colIndizes = new int[includes.length];
        for (int i = 0; i < includes.length; i++) {
            colIndizes[i] = spec.findColumnIndex(includes[i]);
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
                    + (rowCount + 1) + " (\""
                    + row.getKey().getString() + "\")");
            exec.checkCanceled();
            DataCell targetValue = row.getCell(target);
            // read data from row into buffer, skip missing value rows
            boolean containsMissing = targetValue.isMissing()
                    || readIntoBuffer(row, buffer, colIndizes);
            missingSet.set(rowCount, containsMissing);
            if (containsMissing) {
                String errorMessage = "Row \"" + row.getKey().getString()
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
        double[][] ataInverse = MathUtils.inverse(ata);
        checkForNaN(ataInverse);
        // multiply with A^T and b, i.e. (A^T x A)^-1 x A^T x b
        double[] multipliers = new double[nrUnknown];
        rowCount = 0;
        for (RowIterator it = data.iterator(); it.hasNext(); rowCount++) {
            DataRow row = it.next();
            exec.setMessage("Determining output " + (rowCount + 1) + " (\""
                    + row.getKey().getString() + "\")");
            myProgress++;
            exec.setProgress(myProgress / totalProgress);
            exec.checkCanceled();
            // does row containing missing values?
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
                multipliers[i] += buf * b;
            }
        }

        if (m_isCalcError) {
            assert m_error == 0.0;
            rowCount = 0;
            for (RowIterator it = data.iterator(); it.hasNext(); rowCount++) {
                DataRow row = it.next();
                exec.setMessage("Calculating error " + (rowCount + 1) + " (\""
                        + row.getKey().getString() + "\")");
                myProgress++;
                exec.setProgress(myProgress / totalProgress);
                exec.checkCanceled();
                // does row containing missing values?
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
                    out += multipliers[i] * buffer[i];
                }
                m_error += (b - out) * (b - out);
            }
        }
        // handle the optional PMML input
        PMMLPortObject inPMMLPort = (PMMLPortObject)inData[1];

        DataTableSpec outSpec = getLearningSpec(spec);
        double offset = multipliers[0];
        multipliers = Arrays.copyOfRange(multipliers, 1, multipliers.length);
        m_params = new LinearRegressionContent(
                outSpec, offset, multipliers, means);
        // cache the entire table as otherwise the color information
        // may be lost (filtering out the "colored" column)
        m_rowContainer = new DefaultDataArray(data, m_firstRowPaint,
                m_rowCountPaint);


        return new PortObject[]{m_params.createPortObject(inPMMLPort, spec,
                outSpec)};
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

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        m_nrRowsSkipped = 0;
        m_nrRows = 0;
        m_error = 0.0;
        m_params = null;
        m_actualUsedColumns = null;
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        return new PortObjectSpec[]{getOutputSpec(inSpecs)};
    }

    private PMMLPortObjectSpec getOutputSpec(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec tableSpec = (DataTableSpec)inSpecs[0];
        return LinearRegressionContent.createPortObjectSpec(
                (PMMLPortObjectSpec)inSpecs[1], tableSpec,
                getLearningSpec(tableSpec));
    }

    private DataTableSpec getLearningSpec(final DataTableSpec tableSpec)
            throws InvalidSettingsException {
        if (m_target == null) {
            throw new InvalidSettingsException("No target column set");
        }
        String[] includes = computeIncludes(tableSpec);
        // array containing element from m_include and also m_target
        String[] mustHaveColumns = new String[includes.length + 1];
        System.arraycopy(includes, 0, mustHaveColumns, 0, includes.length);
        mustHaveColumns[includes.length] = m_target;
        // check if contained in data and if DoubleValue-compatible.
        for (String include : mustHaveColumns) {
            DataColumnSpec colSpec = tableSpec.getColumnSpec(include);
            if (colSpec == null) {
                throw new InvalidSettingsException("No such column in data: "
                        + include);
            }
            if (!colSpec.getType().isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("Type of column \""
                        + include + "\" is not numeric: " + colSpec.getType());
            }
        }
        return FilterColumnTable.createFilterTableSpec(tableSpec,
                        mustHaveColumns);
    }

    /** Determines the list of variate columns (learning columns). This is
     * either the m_includes[] field or, if m_includeAll is set, the list
     * of double-compatible columns in the input table spec (excluding the
     * response column).
     * @param in Spec contributing the column list
     * @return A new array containg the variates
     * @throws InvalidSettingsException If no double-compatible learning columns
     * exist in the input table.
     */
    private String[] computeIncludes(final DataTableSpec in)
        throws InvalidSettingsException {
        String[] includes;
        if (m_includeAll) {
            List<String> includeList = new ArrayList<String>();
            for (DataColumnSpec s : in) {
                if (s.getType().isCompatible(DoubleValue.class)) {
                    String name = s.getName();
                    if (!name.equals(m_target)) {
                        includeList.add(name);
                    }
                }
            }
            includes = includeList.toArray(new String[includeList.size()]);
            if (includes.length == 0) {
                throw new InvalidSettingsException("No double-compatible "
                        + "variables (learning columns) in input table");
            }
        } else {
            if (m_includes == null) {
                throw new InvalidSettingsException("No settings available");
            }
            includes = m_includes.clone();
        }
        return includes;
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
     * Get all parameters to the currently learned model.
     *
     * @return a reference to the current values
     * @see LinRegDataProvider#getParams()
     */
    @Override
    public LinearRegressionContent getParams() {
        return m_params;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataArray getRowContainer() {
        return m_rowContainer;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public DataArray getDataArray(final int index) {
        return m_rowContainer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getLearningColumns() {
        return m_actualUsedColumns;
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

    private static final String CFG_SPEC = "spec";

    /**
     * {@inheritDoc}
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
            ModelContentRO specContent = c.getModelContent(CFG_SPEC);
            DataTableSpec outSpec = DataTableSpec.load(specContent);
            m_actualUsedColumns = specContent.getStringArray(
                    CFG_USED_COLUMNS, (String[]) null);
            ModelContentRO parContent = c.getModelContent(CFG_PARAMS);
            m_params = LinearRegressionContent.instantiateAndLoad(
                    parContent, outSpec);
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
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        ModelContent content = new ModelContent(CFG_SETTINGS);
        content.addInt(CFG_NR_ROWS, m_nrRows);
        content.addInt(CFG_NR_ROWS_SKIPPED, m_nrRowsSkipped);
        content.addDouble(CFG_ERROR, m_error);
        ModelContentWO specContent = content.addModelContent(CFG_SPEC);
        m_params.getSpec().save(specContent);
        specContent.addStringArray(CFG_USED_COLUMNS, m_actualUsedColumns);
        ModelContentWO parContent = content.addModelContent(CFG_PARAMS);
        m_params.save(parContent);
        File outFile = new File(internDir, FILE_SAVE);
        content.saveToXML(new BufferedOutputStream(new GZIPOutputStream(
                new FileOutputStream(outFile))));
        File dataFile = new File(internDir, FILE_DATA);
        DataContainer.writeToZip(m_rowContainer, dataFile, exec);
    }
}
