/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   30.03.2007 (thiel): created
 */
package org.knime.base.node.mine.sota.predictor;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.knime.base.node.mine.sota.SotaPortObject;
import org.knime.base.node.mine.sota.SotaPortObjectSpec;
import org.knime.base.node.mine.util.PredictorHelper;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;

/**
 *
 * @author Kilian Thiel, University of Konstanz
 */
public class SotaPredictorNodeModel extends NodeModel {

    /** The configuration key for the append probabilities. */
    static final String CFGKEY_APPEND_PROBABILITIES = "append probabilities";

    /** The default value for the append probabilities. */
    static final boolean DEFAULT_APPEND_PROBABILITIES = false;

    /** @return A new model for append probabilities. */
    static SettingsModelBoolean createAppendProbabilities() {
        return new SettingsModelBoolean(CFGKEY_APPEND_PROBABILITIES, DEFAULT_APPEND_PROBABILITIES);
    }

    private SettingsModelBoolean m_appendProbs = createAppendProbabilities();

    private SettingsModelBoolean m_changePrediction = PredictorHelper.getInstance().createChangePrediction();

    private SettingsModelString m_customPrediction = PredictorHelper.getInstance().createPredictionColumn();

    private SettingsModelString m_probSuffix = PredictorHelper.getInstance().createSuffix();

    /**
     * Creates new instance of <code>SotaPredictorNodeModel</code>.
     */
    public SotaPredictorNodeModel() {
        super(
            new PortType[]{PortTypeRegistry.getInstance().getPortType(SotaPortObject.class), BufferedDataTable.TYPE},
            new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

        if (!(inSpecs[0] instanceof SotaPortObjectSpec)) {
            throw new InvalidSettingsException("Given spec at index 1 is not a SotaPortObjectSpec!");
        }
        if (!(inSpecs[1] instanceof DataTableSpec)) {
            throw new InvalidSettingsException("Given spec at index 0 is not a DataTableSpec!");
        }

        SotaPortObjectSpec sp = (SotaPortObjectSpec)inSpecs[0];

        if (!sp.hasClassColumn()) {
            setWarningMessage("Given model is trained on data without a "
                + "class column, which makes prediction pretty odd. " + "Predicted class is set to \"NoClassDefined\".");
        }

        if (!sp.validateSpec((DataTableSpec)inSpecs[1])) {
            throw new InvalidSettingsException("Input data is not compatible " + "with given sota model!");
        }

        return new PortObjectSpec[]{createOutputTableSpec((DataTableSpec)inSpecs[1], sp)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec)
        throws CanceledExecutionException, Exception {

        if (!(inData[1] instanceof BufferedDataTable)) {
            throw new IllegalArgumentException("Given inport object at " + "index 0 is not a BufferedDataTable!");
        } else if (!(inData[0] instanceof SotaPortObject)) {
            throw new IllegalArgumentException("Given inport object at " + "index 1 is not a SotaPortObject");
        }

        BufferedDataTable bdt = (BufferedDataTable)inData[1];
        SotaPortObject spo = (SotaPortObject)inData[0];

        exec.checkCanceled();

        // build data table to use
        int[] indicesOfIncludedCols = new int[bdt.getDataTableSpec().getNumColumns()];
        for (int i = 0; i < bdt.getDataTableSpec().getNumColumns(); i++) {
            indicesOfIncludedCols[i] = i;
        }

        exec.checkCanceled();

        ColumnRearranger cr = new ColumnRearranger(bdt.getDataTableSpec());
        final SotaPortObjectSpec sotaSpec = (SotaPortObjectSpec)spo.getSpec();
        cr.append(new SotaPredictorCellFactory(spo.getSotaRoot(), indicesOfIncludedCols, spo.getDistance(),
            m_appendProbs.getBooleanValue(), sotaSpec.getClassColumnSpec(), m_probSuffix.getStringValue(),
            createOutColSpecs(sotaSpec)));

        return new BufferedDataTable[]{exec.createColumnRearrangeTable(bdt, cr, exec)};
    }

    /**
     * Creates the outgoing <code>DataTableSpec</code> by adding a column to the incoming <code>DataTableSpec</code>
     * which contains the predicted class.
     *
     * @param incomingSpec The incoming <code>DataTableSpec</code>.
     * @return the outgoing <code>DataTableSpec</code>.
     * @deprecated Try the private {@link #createOutputTableSpec(DataTableSpec)} instead.
     */
    @Deprecated
    public static DataTableSpec createDataTableSpec(final DataTableSpec incomingSpec) {
        DataColumnSpecCreator creator = new DataColumnSpecCreator("Predicted class", StringCell.TYPE);
        return new DataTableSpec(incomingSpec, new DataTableSpec(creator.createSpec()));
    }

    private DataTableSpec createOutputTableSpec(final DataTableSpec incomingSpec, final SotaPortObjectSpec sotaSpec) throws InvalidSettingsException {
        final DataColumnSpec[] colSpecs = createOutColSpecs(sotaSpec);
        return new DataTableSpec(incomingSpec, new DataTableSpec(colSpecs));
    }

    /**
     * @param sotaSpec
     * @return
     * @throws InvalidSettingsException
     */
    private DataColumnSpec[] createOutColSpecs(final SotaPortObjectSpec sotaSpec) throws InvalidSettingsException {
        final PredictorHelper ph = PredictorHelper.getInstance();
        final DataColumnSpec classColumnSpec = sotaSpec.getClassColumnSpec();
        String trainingColumnName = classColumnSpec == null ? "No class" : classColumnSpec.getName();
        final String predColName =
            ph.checkedComputePredictionColumnName(m_customPrediction.getStringValue(), m_changePrediction.getBooleanValue(),
                trainingColumnName);
        final DataColumnSpec[] colSpecs;
        if (m_appendProbs.getBooleanValue() && sotaSpec.hasClassColumn()) {
            assert classColumnSpec != null;
            @SuppressWarnings("null")
            Set<DataCell> values = classColumnSpec.getDomain().getValues();
            if (values != null) {
                colSpecs = new DataColumnSpec[values.size() + 1];
                int idx = 0;
                for (DataCell dataCell : values) {
                    colSpecs[idx++] =
                        new DataColumnSpecCreator(ph.probabilityColumnName(trainingColumnName, dataCell.toString(),
                            m_probSuffix.getStringValue()), DoubleCell.TYPE).createSpec();
                }
            } else {
                colSpecs = new DataColumnSpec[1];
            }
        } else {
            colSpecs = new DataColumnSpec[1];
        }
        colSpecs[colSpecs.length - 1] = new DataColumnSpecCreator(predColName, StringCell.TYPE).createSpec();
        return colSpecs;
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
        m_appendProbs.saveSettingsTo(settings);
        m_changePrediction.saveSettingsTo(settings);
        m_customPrediction.saveSettingsTo(settings);
        m_probSuffix.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        //Do not report problems if there was no settings before
        //Since 2.10
        try {
            SettingsModelString s = PredictorHelper.getInstance().createPredictionColumn();
            s.loadSettingsFrom(settings);
            if (s.getStringValue().isEmpty()) {
                throw new InvalidSettingsException("The prediction column name cannot be empty.");
            }
        } catch (InvalidSettingsException ex) {
            //Ignore
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        try {
            m_appendProbs.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            m_appendProbs.setBooleanValue(DEFAULT_APPEND_PROBABILITIES);
        }
        try {
            m_changePrediction.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            m_changePrediction.setBooleanValue(false);
        }
        try {
            m_customPrediction.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            //For compatibility reasons
            m_changePrediction.setBooleanValue(true);
            m_customPrediction.setStringValue("Predicted class");
        }
        try {
            m_probSuffix.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            m_probSuffix.setStringValue("");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // Nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // Nothing to do ...
    }
}
