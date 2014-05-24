/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   23.10.2013 (gabor): created
 */
package org.knime.base.node.mine.scorer.numeric;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.summary.SumOfSquares;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is the model implementation of NumericScorer. Computes the distance between the a numeric column's values and
 * predicted values.
 *
 * @author Gabor Bakos
 */
class NumericScorerNodeModel extends NodeModel {

    /**
     *
     */
    private static final String MEAN_SIGNED_DIFFERENCE = "meanSignedDifference";

    /**
     *
     */
    private static final String RMSD = "rmsd";

    /**
     *
     */
    private static final String MEAN_SQUARED_ERROR = "meanSquaredError";

    /**
     *
     */
    private static final String MEAN_ABS_ERROR = "meanAbsError";

    /**
     *
     */
    private static final String R2 = "R2";

    /**
     *
     */
    private static final String INTERNALS_XML_GZ = "internals.xml.gz";

    static final String CFGKEY_REFERENCE = "reference";

    static final String DEFAULT_REFERENCE = "";

    static final String CFGKEY_PREDICTED = "predicted";

    static final String DEFAULT_PREDICTED = "";

    static final String CFGKEY_OUTPUT = "output column";

    static final String DEFAULT_OUTPUT = "";

    private static final String CFGKEY_OVERRIDE_OUTPUT = "override default output name";

    static final boolean DEFAULT_OVERRIDE_OUTPUT = false;

    static final SettingsModelColumnName createReference() {
        return new SettingsModelColumnName(CFGKEY_REFERENCE, DEFAULT_REFERENCE);
    }

    static final SettingsModelColumnName createPredicted() {
        return new SettingsModelColumnName(CFGKEY_PREDICTED, DEFAULT_PREDICTED);
    }

    /**
     * @return The {@link SettingsModelBoolean} for the overriding output.
     */
    static SettingsModelBoolean createOverrideOutput() {
        return new SettingsModelBoolean(CFGKEY_OVERRIDE_OUTPUT, DEFAULT_OVERRIDE_OUTPUT);
    }

    /**
     * @return A new {@link SettingsModelString} for the output column name.
     */
    static SettingsModelString createOutput() {
        return new SettingsModelString(CFGKEY_OUTPUT, DEFAULT_OUTPUT);
    }

    private final SettingsModelColumnName m_reference = createReference();

    private final SettingsModelColumnName m_predicted = createPredicted();

    private final SettingsModelBoolean m_overrideOutput = createOverrideOutput();

    private final SettingsModelString m_outputColumnName = createOutput();

    private double m_rSquare = Double.NaN, m_meanAbsError = Double.NaN, m_meanSquaredError = Double.NaN,
            m_rmsd = Double.NaN, m_meanSignedDifference = Double.NaN;

    /**
     * Constructor for the node model.
     */
    protected NumericScorerNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        DataTableSpec spec = inData[0].getSpec();
        BufferedDataContainer container = exec.createDataContainer(createOutputSpec(spec));
        int referenceIdx = spec.findColumnIndex(m_reference.getColumnName());
        int predictionIdx = spec.findColumnIndex(m_predicted.getColumnName());
        final Mean meanObserved = new Mean(), meanPredicted = new Mean();
        final Mean absError = new Mean(), squaredError = new Mean();
        final Mean signedDiff = new Mean();
        final SumOfSquares ssTot = new SumOfSquares(), ssRes = new SumOfSquares();
        int skippedRowCount = 0;
        for (DataRow row : inData[0]) {
            DataCell refCell = row.getCell(referenceIdx);
            DataCell predCell = row.getCell(predictionIdx);
            if (refCell.isMissing()) {
                skippedRowCount++;
                continue;
            }
            double ref = ((DoubleValue)refCell).getDoubleValue();
            if (predCell.isMissing()) {
                throw new IllegalArgumentException("Missing value in prediction column in row: " + row.getKey());
            }
            double pred = ((DoubleValue)predCell).getDoubleValue();
            meanObserved.increment(ref);
            meanPredicted.increment(pred);
            absError.increment(Math.abs(ref - pred));
            squaredError.increment((ref - pred) * (ref - pred));
            signedDiff.increment(pred - ref);
        }
        for (DataRow row : inData[0]) {
            DataCell refCell = row.getCell(referenceIdx);
            DataCell predCell = row.getCell(predictionIdx);
            if (refCell.isMissing()) {
                continue;
            }
            double ref = ((DoubleValue)refCell).getDoubleValue();
            double pred = ((DoubleValue)predCell).getDoubleValue();
            ssTot.increment(ref - meanObserved.getResult());
            ssRes.increment(ref - pred);
        }
        container.addRowToTable(new DefaultRow("R^2", m_rSquare = 1 - ssRes.getResult() / ssTot.getResult()));
        container.addRowToTable(new DefaultRow("mean absolute error", m_meanAbsError = absError.getResult()));
        container.addRowToTable(new DefaultRow("mean squared error", m_meanSquaredError = squaredError.getResult()));
        container.addRowToTable(new DefaultRow("root mean squared deviation", m_rmsd =
            Math.sqrt(squaredError.getResult())));
        container.addRowToTable(new DefaultRow("mean signed difference", m_meanSignedDifference =
            signedDiff.getResult()));
        container.close();
        if (skippedRowCount > 0) {
            setWarningMessage("Skipped " + skippedRowCount
                + " rows, because the reference column contained missing values there.");
        }
        return new BufferedDataTable[]{container.getTable()};
    }

    /**
     * @param spec Input table spec.
     * @return Output table spec.
     */
    private DataTableSpec createOutputSpec(final DataTableSpec spec) {
        String o = m_outputColumnName.getStringValue();
        final String output = m_overrideOutput.getBooleanValue() ? o : m_predicted.getColumnName();
        return new DataTableSpec("Scores", new DataColumnSpecCreator(output, DoubleCell.TYPE).createSpec());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_rSquare = m_meanAbsError = m_meanSquaredError = m_rmsd = m_meanSignedDifference = Double.NaN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final DataColumnSpec reference = inSpecs[0].getColumnSpec(m_reference.getColumnName());
        if (reference == null) {
            if (m_reference.getColumnName().equals(DEFAULT_REFERENCE)) {
                throw new InvalidSettingsException("No columns selected for reference");
            }
            throw new InvalidSettingsException("No such column in input table: " + m_reference.getColumnName());
        }
        if (!reference.getType().isCompatible(DoubleValue.class)) {
            throw new InvalidSettingsException("The reference column (" + m_reference.getColumnName()
                + ") is not double valued: " + reference.getType());
        }
        final DataColumnSpec predicted = inSpecs[0].getColumnSpec(m_predicted.getColumnName());
        if (predicted == null) {
            if (m_predicted.getColumnName().equals(DEFAULT_PREDICTED)) {
                throw new InvalidSettingsException("No columns selected for prediction");
            }
            throw new InvalidSettingsException("No such column in input table: " + m_predicted.getColumnName());
        }
        if (!predicted.getType().isCompatible(DoubleValue.class)) {
            throw new InvalidSettingsException("The prediction column (" + m_predicted.getColumnName()
                + ") is not double valued: " + predicted.getType());
        }
        return new DataTableSpec[]{createOutputSpec(inSpecs[0])};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_reference.saveSettingsTo(settings);
        m_predicted.saveSettingsTo(settings);
        m_overrideOutput.saveSettingsTo(settings);
        m_outputColumnName.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_reference.loadSettingsFrom(settings);
        m_predicted.loadSettingsFrom(settings);
        m_overrideOutput.loadSettingsFrom(settings);
        m_outputColumnName.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_reference.validateSettings(settings);
        m_predicted.validateSettings(settings);
        m_overrideOutput.validateSettings(settings);
        m_outputColumnName.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        File f = new File(internDir, INTERNALS_XML_GZ);
        InputStream in = new GZIPInputStream(new BufferedInputStream(new FileInputStream(f)));
        try {
            NodeSettingsRO set = NodeSettings.loadFromXML(in);
            m_rSquare = set.getDouble(R2);
            m_meanAbsError = set.getDouble(MEAN_ABS_ERROR);
            m_meanSquaredError = set.getDouble(MEAN_SQUARED_ERROR);
            m_rmsd = set.getDouble(RMSD);
            m_meanSignedDifference = set.getDouble(MEAN_SIGNED_DIFFERENCE);
        } catch (InvalidSettingsException ise) {
            throw new IOException("Unable to read internals", ise);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        NodeSettings set = new NodeSettings("scorer");
        set.addDouble(R2, m_rSquare);
        set.addDouble(MEAN_ABS_ERROR, m_meanAbsError);
        set.addDouble(MEAN_SQUARED_ERROR, m_meanSquaredError);
        set.addDouble(RMSD, m_rmsd);
        set.addDouble(MEAN_SIGNED_DIFFERENCE, m_meanSignedDifference);

        set.saveToXML(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(new File(internDir,
            INTERNALS_XML_GZ)))));
    }

    /**
     * @return the R^2 value
     */
    public double getRSquare() {
        return m_rSquare;
    }

    /**
     * @return the mean absolute error
     */
    public double getMeanAbsError() {
        return m_meanAbsError;
    }

    /**
     * @return the mean squared error
     */
    public double getMeanSquaredError() {
        return m_meanSquaredError;
    }

    /**
     * @return the root mean squared deviation
     */
    public double getRmsd() {
        return m_rmsd;
    }

    /**
     * @return the mean signed difference
     */
    public double getMeanSignedDifference() {
        return m_meanSignedDifference;
    }
}
