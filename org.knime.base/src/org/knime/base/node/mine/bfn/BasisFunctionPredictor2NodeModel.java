/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 */
package org.knime.base.node.mine.bfn;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import org.knime.base.node.mine.util.PredictorHelper;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
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
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.util.CheckUtils;

/**
 * The basis function predictor model performing a prediction on the data from
 * the first input and the radial basisfunction model from the second.
 *
 * @see BasisFunctionPredictor2CellFactory
 *
 * @author Thomas Gabriel, University of Konstanz
 * @since 2.9
 */
public abstract class BasisFunctionPredictor2NodeModel extends NodeModel {
    private double m_dontKnow = -1.0;

    private boolean m_ignoreDontKnow = false;

    private boolean m_appendClassProps = true;

    private final SettingsModelString m_predictionColumn = PredictorHelper.getInstance().createPredictionColumn();
    private final SettingsModelBoolean m_overridePrediction = PredictorHelper.getInstance().createChangePrediction();
    private final SettingsModelString m_suffix = PredictorHelper.getInstance().createSuffix();

    /**
     * Creates a new basisfunction predictor model with two inputs, the first
     * one which contains the data and the second with the model.
     * @param model type of the basisfunction model at the in-port
     */
    protected BasisFunctionPredictor2NodeModel(final PortType model) {
        super(new PortType[]{model, BufferedDataTable.TYPE},
              new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable[] execute(final PortObject[] portObj,
            final ExecutionContext exec)
            throws CanceledExecutionException, InvalidSettingsException {
        BasisFunctionPortObject pred = (BasisFunctionPortObject) portObj[0];
        final BufferedDataTable data = (BufferedDataTable) portObj[1];
        final DataTableSpec dataSpec = data.getDataTableSpec();
        ColumnRearranger colreg = createColumnRearranger(pred, dataSpec);
       return new BufferedDataTable[]{exec.createColumnRearrangeTable(
                data, colreg, exec)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                BasisFunctionPortObject pred = (BasisFunctionPortObject)((PortObjectInput)inputs[0]).getPortObject();
                ColumnRearranger colre = createColumnRearranger(pred, (DataTableSpec)inSpecs[1]);
                colre.createStreamableFunction(1, 0).runFinal(inputs, outputs, exec);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.NONDISTRIBUTED_NONSTREAMABLE, InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    private ColumnRearranger createColumnRearranger(final BasisFunctionPortObject pred, final DataTableSpec dataSpec) {
        if (pred.getBasisFunctions().size() == 0) {
            setWarningMessage("Rule model is emtpy, no rules available.");
        }
        final DataTableSpec modelSpec = pred.getSpec();
        int trainingColIndex = modelSpec.getNumColumns() - 5;
        int[] filteredColumns = new int[trainingColIndex];
        for (int i = 0; i < filteredColumns.length; i++) {
            filteredColumns[i] = dataSpec.findColumnIndex(
                    modelSpec.getColumnSpec(i).getName());
        }
        final ColumnRearranger colreg = new ColumnRearranger(dataSpec);
        colreg.append(new BasisFunctionPredictor2CellFactory(dataSpec,
            createSpec(dataSpec, modelSpec, trainingColIndex), filteredColumns, pred.getBasisFunctions(), m_dontKnow,
            normalizeClassification(), m_appendClassProps, modelSpec.getColumnNames()[trainingColIndex], m_suffix
                .getStringValue()));
        return colreg;
    }

    /**
     * Creates the output model spec.
     * @param dataSpec input data spec
     * @param modelSpec input model spec
     * @param modelClassIdx index with reflects the class column
     * @return the new model spec
     */
    public final DataColumnSpec[] createSpec(final DataTableSpec dataSpec,
            final DataTableSpec modelSpec, final int modelClassIdx) {
        DataColumnSpec trainingClass = modelSpec.getColumnSpec(modelClassIdx);
        Set<DataCell> possClasses =
            trainingClass.getDomain().getValues();
        final PredictorHelper predictorHelper = PredictorHelper.getInstance();
        String trainingClassName = trainingClass.getName();
        final DataColumnSpec[] specs;
        if (possClasses == null) {
            if (m_appendClassProps) {
                return null;
            } else {
                specs = new DataColumnSpec[1];
            }
        } else {
            specs = new DataColumnSpec[possClasses.size() + 1];
            Iterator<DataCell> it = possClasses.iterator();
            for (int i = 0; i < possClasses.size(); i++) {
                String colName =
                    predictorHelper.probabilityColumnName(trainingClassName, it.next().toString(),
                        m_suffix.getStringValue());
                specs[i] = new DataColumnSpecCreator(colName, DoubleCell.TYPE).createSpec();
            }
        }
        DataColumnSpecCreator newTargetSpec = new DataColumnSpecCreator(
                trainingClass);
        String applyColumn =
            predictorHelper.computePredictionColumnName(m_predictionColumn.getStringValue(),
                m_overridePrediction.getBooleanValue(), trainingClassName);
        newTargetSpec.setName(applyColumn);
        specs[specs.length - 1] = newTargetSpec.createSpec();
        return specs;
    }

    /**
     * @return <code>true</code> if normalization is required for output
     */
    public abstract boolean normalizeClassification();

//    /**
//     * @return the column name contained the winner prediction
//     */
//    public String getApplyColumn() {
//        return PredictorHelper.getInstance().computePredictionColumnName(m_predictionColumn, "????");
//    }

    /**
     * @return the <i>don't know</i> class probability between 0.0 and 1.0
     */
    public double getDontKnowClassDegree() {
        return m_dontKnow;
    }

    /**
     * @return true if class probability columns should be appended
     */
    public boolean appendClassProbabilities() {
        return m_appendClassProps;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec[] configure(final PortObjectSpec[] portObjSpec)
            throws InvalidSettingsException {
        // get model spec
        final DataTableSpec modelSpec = (DataTableSpec) portObjSpec[0];
        // get data spec
        final DataTableSpec dataSpec = (DataTableSpec) portObjSpec[1];

        String predCol = m_predictionColumn.getStringValue();
        CheckUtils.checkSetting(!m_overridePrediction.getBooleanValue() || (predCol != null && !predCol.trim().isEmpty()), "Prediction column name cannot be empty");
        // sanity check for empty set of nominal values
        int modelClassIdx = modelSpec.getNumColumns() - 5;
        DataColumnSpec[] modelSpecs =
            createSpec(dataSpec, modelSpec, modelClassIdx);
        if (modelSpecs == null) {
            return new DataTableSpec[]{null};
        }
        String[] columns = new String[modelClassIdx];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = modelSpec.getColumnSpec(i).getName();
        }
        final ColumnRearranger colreg = createRearranger(dataSpec, columns, modelSpec.getColumnNames()[modelClassIdx]);
        colreg.append(new BasisFunctionPredictor2CellFactory(modelSpecs, m_appendClassProps,
            modelSpec.getColumnNames()[modelClassIdx], m_suffix.getStringValue()));
        return new DataTableSpec[]{colreg.createSpec()};
    }

    /**
     * Creates a column rearranger based on the data spec. The new apply column
     * is appended.
     * @param dataSpec data spec
     * @param modelSpec model spec
     * @param trainingClassName name of the training column name
     * @return column rearranger from data spec
     * @throws InvalidSettingsException if the settings are not valid against
     *      data and/or model spec
     */
    public final ColumnRearranger createRearranger(final DataTableSpec dataSpec,
            final String[] modelSpec, final String trainingClassName)
            throws InvalidSettingsException {
        if (modelSpec.length == 0) {
            throw new InvalidSettingsException("Model spec must not be empty.");
        }
        // all model columns need to be in the data spec
        for (int i = 0; i < modelSpec.length; i++) {
            int idx = dataSpec.findColumnIndex(modelSpec[i]);
            if (idx >= 0) {
                DataType dataType = dataSpec.getColumnSpec(idx).getType();
                if (!dataType.isCompatible(DoubleValue.class)) {
                    throw new InvalidSettingsException("Data column \""
                        + dataSpec.getColumnSpec(idx).getName() + "\""
                        + " is not compatible with DoubleValue.");
                }
            } else {
                throw new InvalidSettingsException("Model column \""
                        + modelSpec[i] + "\" not in data spec.");
            }
        }
        return new ColumnRearranger(dataSpec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // prediction column name
        m_predictionColumn.loadSettingsFrom(settings);
        m_overridePrediction.loadSettingsFrom(settings);
        // don't know class
        m_dontKnow = settings
                .getDouble(BasisFunctionPredictor2NodeDialog.DONT_KNOW_PROP);
        m_ignoreDontKnow = settings.getBoolean(
                BasisFunctionPredictor2NodeDialog.CFG_DONT_KNOW_IGNORE, false);
        // append class probability columns
        m_appendClassProps = settings.getBoolean(
                BasisFunctionPredictor2NodeDialog.CFG_CLASS_PROBS, true);
        m_suffix.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        // prediction column name
        m_predictionColumn.saveSettingsTo(settings);
        m_overridePrediction.saveSettingsTo(settings);
        // don't know class
        settings.addDouble(BasisFunctionPredictor2NodeDialog.DONT_KNOW_PROP,
                m_dontKnow);
        settings.addBoolean(
                BasisFunctionPredictor2NodeDialog.CFG_DONT_KNOW_IGNORE,
                m_ignoreDontKnow);
        // append class probability columns
        settings.addBoolean(BasisFunctionPredictor2NodeDialog.CFG_CLASS_PROBS,
                m_appendClassProps);
        m_suffix.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        StringBuffer sb = new StringBuffer();
        // prediction column name
//        String s = null;
//        try {
//            s = settings.getString(
//                    BasisFunctionPredictorNodeDialog.APPLY_COLUMN);
//        } catch (InvalidSettingsException ise) {
//            sb.append(ise.getMessage() + "\n");
//        }
//        if (s == null || s.length() == 0) {
//            sb.append("Empty prediction column name not allowed.\n");
//        }
        // don't know class
        try {
            settings.getDouble(BasisFunctionPredictor2NodeDialog.DONT_KNOW_PROP);
        } catch (InvalidSettingsException ise) {
            sb.append(ise.getMessage());
        }
        if (sb.length() > 0) {
            throw new InvalidSettingsException(sb.toString());
        }
        m_suffix.validateSettings(settings);
        m_predictionColumn.validateSettings(settings);
        m_overridePrediction.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadInternals(final File internDir,
            final ExecutionMonitor exec) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveInternals(final File internDir,
            final ExecutionMonitor exec) {

    }
}
