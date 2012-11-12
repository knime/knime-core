/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 */
package org.knime.base.node.mine.pca;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
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
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/**
 * The model class that implements the PCA on the input table.
 *
 * @author Uwe Nagel, University of Konstanz
 */
public class PCANodeModel extends NodeModel {

    /**
     * String used for fail on missing config.
     */
    static final String FAIL_MISSING = "failMissing";

    /**
     *
     */
    static final String INPUT_COLUMNS = "input_columns";

    /** Index of input data port. */
    public static final int DATA_INPORT = 0;

    /** Index of input data port. */
    public static final int DATA_OUTPORT = 0;

    /** Index of decomposition output port. */
    public static final int INFO_OUTPORT = 1;

    /** Index of covariance matrix output port. */

    public static final int MATRIX_OUTPORT = 0;

    /** numeric columns to be used as input. */
    private final SettingsModelFilterString m_inputColumns = new SettingsModelFilterString(
            PCANodeModel.INPUT_COLUMNS);

    private int[] m_inputColumnIndices = {};

    /** remove original columns? */
    private final SettingsModelBoolean m_removeOriginalCols = new SettingsModelBoolean(
            REMOVE_COLUMNS, false);

    /** fail on missing data? */
    private final SettingsModelBoolean m_failOnMissingValues = new SettingsModelBoolean(
            FAIL_MISSING, false);

    private String[] m_inputColumnNames;

    /**
     * description String for dimension.
     */
    static final String PCA_COL_PREFIX = "PCA dimension ";

    /** config String for remove columns. */
    static final String REMOVE_COLUMNS = "removeColumns";

    /**
     * config String for selecting whether the number of dimensions or the
     * minimum quality is configured.
     */
    public static final String DIMENSIONS_SELECTION = "output_dimensions_selected";

    private final SettingsModelPCADimensions m_dimSelection = new SettingsModelPCADimensions(
            DIMENSIONS_SELECTION, 2, 100, false);

    /**
     * One input, one output table.
     */
    PCANodeModel() {
        super(1, 1);

    }

    /**
     * select all compatible columns as input.
     *
     * @param inSpecs
     *            in specs
     */
    private void selectDefaultColumns(final PortObjectSpec[] inSpecs) {
        m_inputColumnIndices = PCANodeModel
        .getDefaultColumns((DataTableSpec) inSpecs[DATA_INPORT]);
        m_inputColumnNames = new String[m_inputColumnIndices.length];
        for (int i = 0; i < m_inputColumnIndices.length; i++) {
            m_inputColumnNames[i] = ((DataTableSpec) inSpecs[DATA_INPORT])
            .getColumnSpec(i).getName();
        }
    }

    /**
     * All {@link org.knime.core.data.def.IntCell} columns are converted to
     * {@link org.knime.core.data.def.DoubleCell} columns.
     *
     * {@inheritDoc}
     *
     */

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
    throws InvalidSettingsException {

        if (m_inputColumns.getIncludeList().size() == 0) {
            // default config fallback
            selectDefaultColumns(inSpecs);
            setWarningMessage("using as default all possible columns ("
                    + m_inputColumnIndices.length + ") for PCA!");
        } else {
            // read config from dialog
            m_inputColumnIndices = new int[m_inputColumns.getIncludeList()
                                           .size()];
            m_inputColumnNames = new String[m_inputColumnIndices.length];
            int colIndex = 0;
            for (final String colName : m_inputColumns.getIncludeList()) {
                final DataColumnSpec colspec = inSpecs[DATA_INPORT]
                                                       .getColumnSpec(colName);
                if (colspec == null) {
                    selectDefaultColumns(inSpecs);
                    m_dimSelection.setDimensions(m_inputColumnIndices.length);
                    setWarningMessage("using as default all possible columns ("
                            + m_inputColumnIndices.length + ") for PCA!");
                    break;
                } else if (!colspec.getType().isCompatible(DoubleValue.class)) {
                    throw new InvalidSettingsException("column \"" + colName
                            + "\" is not compatible with double");
                }
                m_inputColumnIndices[colIndex] = inSpecs[DATA_INPORT]
                                                         .findColumnIndex(colName);
                m_inputColumnNames[colIndex] = colName;
                colIndex++;
            }
        }

        int dim = m_dimSelection.getNeededDimensions();
        if (dim == -1) {
            if (!m_dimSelection.getDimensionsSelected()) {
                // cannot determine needed dimensions without knowing
                // decomposition
                return null;
            }
            m_dimSelection.setDimensionsSelected(false);
            m_dimSelection.setMinQuality(100);
            dim = m_inputColumnIndices.length;
        }
        if (dim > m_inputColumnIndices.length) {
            m_dimSelection.setDimensionsSelected(true);
            dim = m_inputColumnIndices.length;
            m_dimSelection.setDimensions(dim);
            setWarningMessage("dimensions resetted to " + dim);
        }
        final DataColumnSpec[] specs = createAddTableSpec(inSpecs[DATA_INPORT],
                dim);
        final DataTableSpec data = AppendedColumnTable.getTableSpec(
                inSpecs[DATA_INPORT], specs);
        if (m_removeOriginalCols.getBooleanValue()) {
            final ColumnRearranger columnRearranger = new ColumnRearranger(data);
            columnRearranger.remove(m_inputColumnIndices);
            return new DataTableSpec[] { columnRearranger.createSpec() };
        }

        final DataTableSpec[] outspec = new DataTableSpec[1];
        outspec[DATA_OUTPORT] = data;
        // outspec[INFO_OUTPORT] =
        // createDecompositionTableSpec(m_inputColumnIndices.length);
        // outspec[MATRIX_OUTPORT] =
        // createCovarianceMatrixSpec(m_inputColumnNames);
        return outspec;

    }

    /**
     * create table spec for output of spectral decomposition.
     *
     * @param columnNames
     *            names of the input columns
     * @return table spec (first col for eigenvalues, others for components of
     *         eigenvectors)
     */
    public static DataTableSpec createDecompositionTableSpec(
            final String[] columnNames) {
        final DataColumnSpecCreator createEVCol = new DataColumnSpecCreator(
                "eigenvalue", DoubleCell.TYPE);

        final DataColumnSpec[] colsSpecs = new DataColumnSpec[columnNames.length + 1];
        colsSpecs[0] = createEVCol.createSpec();

        for (int i = 1; i < colsSpecs.length; i++) {
            colsSpecs[i] = new DataColumnSpecCreator(columnNames[i - 1],
                    DoubleCell.TYPE).createSpec();
        }
        final DataTableSpec info = new DataTableSpec("spectral decomposition",
                colsSpecs);
        return info;
    }

    /**
     * get column indices for all double compatible columns.
     *
     * @param dataTableSpec
     *            table spec
     * @return array of indices
     */
    static int[] getDefaultColumns(final DataTableSpec dataTableSpec) {
        final LinkedList<Integer> cols = new LinkedList<Integer>();
        for (int i = 0; i < dataTableSpec.getNumColumns(); i++) {
            if (dataTableSpec.getColumnSpec(i).getType()
                    .isCompatible(DoubleValue.class)) {
                cols.add(i);
            }
        }
        final int[] ret = new int[cols.size()];
        int i = 0;
        for (final int t : cols) {
            ret[i++] = t;
        }
        return ret;
    }

    /**
     * create part of table spec to be added to the input table.
     *
     * @param inSpecs
     *            input specs (for unique column names)
     * @param resultDimensions
     *            number of dimensions in output
     * @return part of table spec to be added to input table
     */
    public static DataColumnSpec[] createAddTableSpec(
            final DataTableSpec inSpecs, final int resultDimensions) {
        // append pca columns
        final DataColumnSpec[] specs = new DataColumnSpec[resultDimensions];

        for (int i = 0; i < resultDimensions; i++) {
            final String colName = DataTableSpec.getUniqueColumnName(inSpecs,
                    PCA_COL_PREFIX + i);
            final DataColumnSpecCreator specCreator = new DataColumnSpecCreator(
                    colName, DataType.getType(DoubleCell.class));
            specs[i] = specCreator.createSpec();
        }
        return specs;
    }

    /**
     * Performs the PCA.
     *
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {

        // remove all non-numeric columns from the input date
        // final DataTable filteredTable =
        // filterNonNumericalColumns(inData[DATA_INPORT]);

        final BufferedDataTable dataTable = (BufferedDataTable) inData[DATA_INPORT];
        if (dataTable.getRowCount() == 0) {
            throw new IllegalArgumentException("Input table is empty!");
        }
        if (dataTable.getRowCount() == 1) {
            throw new IllegalArgumentException("Input table has only one row!");
        }

        final double[] meanVector = getMeanVector(dataTable,
                m_inputColumnIndices, false,
                exec.createSubExecutionContext(0.2));
        final double[][] m = new double[m_inputColumnIndices.length][m_inputColumnIndices.length];

        final int missingValues = getCovarianceMatrix(
                exec.createSubExecutionContext(0.2), dataTable,
                m_inputColumnIndices, meanVector, m);
        final Matrix covarianceMatrix = new Matrix(m);
        if (missingValues > 0) {
            if (m_failOnMissingValues.getBooleanValue()) {
                throw new IllegalArgumentException(
                "missing, infinite or impossible values in table");
            }
            setWarningMessage(missingValues
                    + " rows ignored because of missing"
                    + ", infinite or impossible values");

        }
        final ExecutionContext evdContext = exec.createSubExecutionContext(0.2);
        evdContext.setMessage("computing spectral decomposition");
        final EigenvalueDecomposition eig = covarianceMatrix.eig();
        exec.checkCanceled();
        evdContext.setProgress(0.8);
        final double[] evs = EigenValue.extractEVVector(eig);
        m_dimSelection.setEigenValues(evs);
        final int dimensions = m_dimSelection.getNeededDimensions();
        // don't remember these in case input changes
        m_dimSelection.setEigenValues(null);
        // adjust to selected numerical columns
        if (dimensions > m_inputColumnIndices.length || dimensions < 1) {
            throw new IllegalArgumentException(
                    "invalid number of dimensions to reduce to: " + dimensions);
        }
        exec.checkCanceled();
        evdContext.setProgress(0.9);
        final Matrix eigenvectors = EigenValue.getSortedEigenVectors(eig.getV()
                .getArray(), evs, dimensions);
        exec.checkCanceled();
        evdContext.setProgress(1);
        exec.checkCanceled();

        final DataColumnSpec[] specs = createAddTableSpec(
                (DataTableSpec) inData[DATA_INPORT].getSpec(), dimensions);

        final CellFactory fac = new CellFactory() {

            @Override
            public DataCell[] getCells(final DataRow row) {
                return convertInputRow(eigenvectors, row, meanVector,
                        m_inputColumnIndices, dimensions, false);
            }

            @Override
            public DataColumnSpec[] getColumnSpecs() {

                return specs;
            }

            @Override
            public void setProgress(final int curRowNr, final int rowCount,
                    final RowKey lastKey, final ExecutionMonitor texec) {
                texec.setProgress(curRowNr / (double) rowCount, "processing "
                        + curRowNr + " of " + rowCount);

            }

        };

        final ColumnRearranger cr = new ColumnRearranger(
                (DataTableSpec) inData[0].getSpec());
        cr.append(fac);
        if (m_removeOriginalCols.getBooleanValue()) {
            cr.remove(m_inputColumnIndices);
        }
        final BufferedDataTable result = exec.createColumnRearrangeTable(
                (BufferedDataTable) inData[0], cr, exec.createSubProgress(0.4));
        final PortObject[] out = new PortObject[1];
        out[DATA_OUTPORT] = result;
        // out[INFO_OUTPORT] =
        // createDecompositionOutputTable(exec, evs, eig.getV());
        // out[MATRIX_OUTPORT] =
        // createCovarianceTable(exec, covarianceMatrix.getArray(),
        // m_inputColumnNames);
        return out;
    }

    /**
     * create a table containing the given spectral decomposition.
     *
     * @param exec
     *            execution context for table creation
     * @param evd
     *            the spectral decomposition of the correlation matrix
     * @return the created table
     * @throws CanceledExecutionException
     */
    public static BufferedDataTable createDecompositionOutputTable(
            final ExecutionContext exec, final EigenvalueDecomposition evd,
            final String[] columnNames) throws CanceledExecutionException {
        final List<EigenValue> sortedEV = EigenValue.createSortedList(evd
                .getV().getArray(), EigenValue.extractEVVector(evd));
        final DataTableSpec outSpec = createDecompositionTableSpec(columnNames);
        final BufferedDataContainer result = exec.createDataContainer(outSpec);
        int i = 1;
        for (final EigenValue ev : sortedEV) {
            final DataCell[] values = new DataCell[sortedEV.size() + 1];
            values[0] = new DoubleCell(ev.getValue());
            final double[][] vector = ev.getVector().getArray();
            for (int j = 0; j < vector.length; j++) {

                values[j + 1] = new DoubleCell(vector[j][0]);
            }
            result.addRowToTable(new DefaultRow(new RowKey(i++
                    + ". eigenvector"), values));
            exec.checkCanceled();

        }
        result.close();
        exec.setProgress(1);
        return result.getTable();
    }

    /**
     * reduce a single input row to the principal components.
     *
     * @param eigenvectors
     *            transposed matrix of eigenvectors (eigenvectors in rows,
     *            number of eigenvectors corresponds to dimensions to be
     *            projected to)
     * @param row
     *            the row to convert
     * @param means
     *            mean values of the columns
     * @param inputColumnIndices
     *            indices of the input columns
     * @param resultDimensions
     *            number of dimensions to project to
     * @param failOnMissing
     *            throw exception if missing values are encountered
     * @return array of data cells to be added to the row
     */
    protected static DataCell[] convertInputRow(final Matrix eigenvectors,
            final DataRow row, final double[] means,
            final int[] inputColumnIndices, final int resultDimensions,
            final boolean failOnMissing) {
        // get row of input values
        boolean missingValues = false;
        for (int i = 0; i < inputColumnIndices.length; i++) {
            if (row.getCell(inputColumnIndices[i]).isMissing()) {
                missingValues = true;
                continue;
            }
        }
        if (missingValues && failOnMissing) {
            throw new IllegalArgumentException("table contains missing values");
        }
        // put each cell of a pca row into the row to append
        final DataCell[] cells = new DataCell[resultDimensions];

        if (missingValues) {
            for (int i = 0; i < resultDimensions; i++) {
                cells[i] = DataType.getMissingCell();
            }
        } else {
            final double[][] rowVec = new double[1][inputColumnIndices.length];
            for (int i = 0; i < rowVec[0].length; i++) {

                rowVec[0][i] = ((DoubleValue) row
                        .getCell(inputColumnIndices[i])).getDoubleValue()
                        - means[i];

            }
            final double[][] newRow = new Matrix(rowVec)
            .times(eigenvectors).getArray();

            for (int i = 0; i < resultDimensions; i++) {
                cells[i] = new DoubleCell(newRow[0][i]);
            }
        }
        return cells;
    }

    /**
     * Converts a {@link DataTable} to the 2D-double array representing its
     * covariance matrix. Only numeric attributes are included.
     *
     * @param exec
     *            the execution context for progress report (a subcontext)
     *
     * @param dataTable
     *            the {@link DataTable} to convert
     * @param numericIndices
     *            indices of input columns
     * @param means
     *            mean values of columns
     * @param dataMatrix
     *            matrix to write covariances to
     * @return number of ignored rows (containing missing values)
     * @throws CanceledExecutionException
     *             if execution is canceled
     */
    static int getCovarianceMatrix(final ExecutionContext exec,
            final BufferedDataTable dataTable, final int[] numericIndices,
            final double[] means, final double[][] dataMatrix)
    throws CanceledExecutionException {

        // create result 2-D array
        // fist dim corresponds to the rows, second dim to columns

        int counter = 0;
        int missingCount = 0;
        // for all rows
        ROW: for (final DataRow row : dataTable) {
            // ignore rows with missing cells
            for (int i = 0; i < numericIndices.length; i++) {
                if (row.getCell(numericIndices[i]).isMissing()) {
                    missingCount++;
                    continue ROW;
                }
                if (!row.getCell(numericIndices[i]).getType()
                        .isCompatible(DoubleValue.class)) {
                    throw new IllegalArgumentException("column "
                            + dataTable.getSpec()
                            .getColumnSpec(numericIndices[i]).getName()
                            + " has incompatible type!");
                }
                final double val = ((DoubleValue) row
                        .getCell(numericIndices[i])).getDoubleValue();
                if (Double.isInfinite(val) || Double.isNaN(val)) {
                    missingCount++;
                    continue ROW;
                }
            }
            // for all valid attributes
            for (int i = 0; i < numericIndices.length; i++) {
                for (int j = 0; j < numericIndices.length; j++) {

                    dataMatrix[i][j] += (((DoubleValue) row
                            .getCell(numericIndices[i])).getDoubleValue() - means[i])

                            * (((DoubleValue) row.getCell(numericIndices[j]))
                                    .getDoubleValue() - means[j]);
                    if (Double.isInfinite(dataMatrix[i][j])
                            || Double.isNaN(dataMatrix[i][j])) {
                        throw new IllegalArgumentException(
                                "computation failed for numerical problems"
                                + ", probably some numbers are too huge");
                    }
                }
            }
            counter++;
            exec.setProgress((double) counter / dataTable.getRowCount(),
                    "create covariance matrix, processing row " + counter
                    + " of " + dataTable.getRowCount());
            exec.checkCanceled();
        }
        if (counter < 2) {
            throw new IllegalArgumentException(
                    "Input table has too few rows with valid values! "
                    + "Do some columns only contain missing values?");
        }
        for (int i = 0; i < dataMatrix.length; i++) {
            for (int j = 0; j < dataMatrix[i].length; j++) {
                // counter equals number of rows, since we don't know
                // the complete population, we use #samples-1
                dataMatrix[i][j] /= (counter - 1);
            }
        }
        return missingCount;
    }

    /**
     * calculate means of all columns.
     *
     * @param dataTable
     *            input table
     * @param numericIndices
     *            indices of columns to use
     * @param failOnMissingValues
     *            if true, throw exception if missing values are encountered
     * @param exec
     *            execution context
     * @return vector of column mean values
     * @throws CanceledExecutionException
     */
    static double[] getMeanVector(final DataTable dataTable,
            final int[] numericIndices, final boolean failOnMissingValues,
            final ExecutionContext exec) throws CanceledExecutionException {
        final double[] means = new double[numericIndices.length];
        int numRows = 0;
        final double rowCount = ((BufferedDataTable) dataTable).getRowCount();
        int totalRowCount = 0;
        // calculate mean for each row and column
        ROW: for (final DataRow row : dataTable) {
            totalRowCount++;
            exec.checkCanceled();
            exec.setProgress(totalRowCount / rowCount,
                    "mean calculations, row " + totalRowCount + " of "
                    + rowCount);
            // ignore rows with missing cells
            for (int i = 0; i < numericIndices.length; i++) {
                if (row.getCell(numericIndices[i]).isMissing()) {
                    if (failOnMissingValues) {
                        throw new IllegalArgumentException(
                        "missing values in table");
                    }
                    continue ROW;
                }
                final double val = ((DoubleValue) row
                        .getCell(numericIndices[i])).getDoubleValue();
                if (Double.isInfinite(val) || Double.isNaN(val)) {

                    continue ROW;
                }

            }

            int i = 0;
            for (final Integer index : numericIndices) {
                means[i++] += ((DoubleValue) row.getCell(index))
                .getDoubleValue();
            }
            numRows++;

        }
        for (int i = 0; i < means.length; i++) {
            means[i] /= numRows;
        }
        return means;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        m_inputColumns.loadSettingsFrom(settings);
        m_removeOriginalCols.loadSettingsFrom(settings);
        m_failOnMissingValues.loadSettingsFrom(settings);
        m_dimSelection.loadSettingsFrom(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_inputColumnIndices = new int[] {};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_inputColumns.saveSettingsTo(settings);
        m_removeOriginalCols.saveSettingsTo(settings);
        m_failOnMissingValues.saveSettingsTo(settings);
        m_dimSelection.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        m_inputColumns.validateSettings(settings);
        m_removeOriginalCols.validateSettings(settings);
        m_failOnMissingValues.validateSettings(settings);
        m_dimSelection.validateSettings(settings);

    }

    /**
     * create data table from covariance matrix.
     *
     * @param exec
     *            execution context
     * @param m
     *            covariance matrix
     * @param inputColumnNames
     *            names of input columns the matrix was created from
     * @return table
     */
    public static BufferedDataTable createCovarianceTable(
            final ExecutionContext exec, final double[][] m,
            final String[] inputColumnNames) {
        final BufferedDataContainer bdt = exec
        .createDataContainer(createCovarianceMatrixSpec(inputColumnNames));
        for (int i = 0; i < m.length; i++) {
            final DataCell[] cells = new DataCell[inputColumnNames.length];
            for (int j = 0; j < m[i].length; j++) {
                cells[j] = new DoubleCell(m[i][j]);
            }
            bdt.addRowToTable(new DefaultRow(inputColumnNames[i], cells));
        }
        bdt.close();
        final BufferedDataTable covarianceTable = bdt.getTable();
        return covarianceTable;
    }

    public static DataTableSpec createCovarianceMatrixSpec(
            final String[] inputColumnNames) {
        final DataType[] types = new DataType[inputColumnNames.length];
        for (int i = 0; i < types.length; i++) {
            types[i] = DoubleCell.TYPE;
        }
        return new DataTableSpec("covariance matrix", inputColumnNames, types);

    }
}
