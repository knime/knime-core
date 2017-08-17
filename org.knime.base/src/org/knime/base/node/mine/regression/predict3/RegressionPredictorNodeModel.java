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
 * -------------------------------------------------------------------
 *
 * History
 *   Apr 30, 2010 (hofer): created
 */
package org.knime.base.node.mine.regression.predict3;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionContent;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionContent.FunctionName;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionContent.ModelType;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionTranslator;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLPredictor;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.pmml.PMMLModelType;
import org.w3c.dom.Node;

/**
 * Node model for the general regression predictor.
 * <p>Despite being public no official API.
 * @author Heiko Hofer
 */
public final class RegressionPredictorNodeModel extends NodeModel {
    private final RegressionPredictorSettings m_settings;

    /** The node logger for this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(RegressionPredictorNodeModel.class);


    /** Initialization with 1 data input, 1 model input and 1 data output. */
    public RegressionPredictorNodeModel() {
        super(new PortType[]{PMMLPortObject.TYPE,
                BufferedDataTable.TYPE},
                new PortType[]{BufferedDataTable.TYPE});
        m_settings = new RegressionPredictorSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        RegressionPredictorSettings s = new RegressionPredictorSettings();
        s.loadSettings(settings);

        if (s.getHasCustomPredictionName()
            && (s.getCustomPredictionName() == null || s.getCustomPredictionName().isEmpty())) {
            throw new InvalidSettingsException("Please define a name for the prediction column.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        PMMLPortObject port = (PMMLPortObject)inData[0];
        List<Node> models = port.getPMMLValue().getModels(PMMLModelType.GeneralRegressionModel);
        if (models.isEmpty()) {
            LOGGER.warn("No regression models in the input PMML.");
            @SuppressWarnings("deprecation")
            org.knime.base.node.mine.regression.predict.RegressionPredictorNodeModel regrPredictor =
                    new org.knime.base.node.mine.regression.predict.RegressionPredictorNodeModel();
            @SuppressWarnings("deprecation")
            PortObject[] regrPredOut = regrPredictor.execute(inData, exec);
            if (regrPredOut.length > 0 && regrPredOut[0] instanceof BufferedDataTable) {
                BufferedDataTable regrPredOutTable = (BufferedDataTable)regrPredOut[0];
                // replace name of prediction column (the last column of regrPredOutTable)
                return new PortObject[]{adjustSpecOfRegressionPredictorTable(regrPredOutTable, inData, exec)};
            } else {
                return regrPredOut;
            }
        }
        PMMLGeneralRegressionTranslator trans = new PMMLGeneralRegressionTranslator();
        port.initializeModelTranslator(trans);

        BufferedDataTable data = (BufferedDataTable)inData[1];
        DataTableSpec spec = data.getDataTableSpec();
        ColumnRearranger c = createRearranger(trans.getContent(), port.getSpec(), spec);
        BufferedDataTable out = exec.createColumnRearrangeTable(data, c, exec);
        return new BufferedDataTable[]{out};
    }


    private BufferedDataTable adjustSpecOfRegressionPredictorTable(final BufferedDataTable table,
        final PortObject[] inData, final ExecutionContext exec) throws InvalidSettingsException {
        String predColumn = determinPredictedColumName(inData);
        if (predColumn != null) {
            DataColumnSpec[] colSpecs = getColumnSpecs(table.getSpec());
            colSpecs[colSpecs.length - 1] = replaceNameOf(colSpecs[colSpecs.length - 1], predColumn);
            return exec.createSpecReplacerTable(table, new DataTableSpec(colSpecs));
        } else {
            return table;
        }
    }

    private DataColumnSpec replaceNameOf(final DataColumnSpec colSpec, final String name) {
        DataColumnSpecCreator creator = new DataColumnSpecCreator(colSpec);
        creator.setName(name);
        return creator.createSpec();
    }

    private DataColumnSpec[] getColumnSpecs(final DataTableSpec spec) {
        DataColumnSpec[] colSpecs = new DataColumnSpec[spec.getNumColumns()];
        for (int i = 0; i < spec.getNumColumns(); i++) {
            colSpecs[i] = spec.getColumnSpec(i);
        }
        return colSpecs;
    }

    private String determinPredictedColumName(final PortObject[] inData) throws InvalidSettingsException {
        PortObjectSpec[] outPortSpec = configure(new PortObjectSpec[] {inData[0].getSpec(), inData[1].getSpec()});
        if (outPortSpec != null) {
            DataTableSpec outSpec = (DataTableSpec)outPortSpec[0];
            return outSpec.getColumnSpec(outSpec.getNumColumns() - 1).getName();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to reset, the node has no internal state
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        PMMLPortObjectSpec regModelSpec = (PMMLPortObjectSpec)inSpecs[0];
        DataTableSpec dataSpec = (DataTableSpec)inSpecs[1];
        if (dataSpec == null || regModelSpec == null) {
            throw new InvalidSettingsException(
                    "No input specification available");
        }
        if (regModelSpec.getTargetCols().get(0).getType().isCompatible(DoubleValue.class)
                && m_settings.getIncludeProbabilities()) {
            setWarningMessage("The option \"Append columns with predicted probabilities\""
                + " has only an effect for nominal targets");
        }

        if (null != RegressionPredictorCellFactory.createColumnSpec(regModelSpec, dataSpec,
                m_settings)) {
            ColumnRearranger c = new ColumnRearranger(dataSpec);
            c.append(new RegressionPredictorCellFactory(regModelSpec, dataSpec, m_settings) {
                @Override
                public DataCell[] getCells(final DataRow row) {
                    // not called during configure
                    return null;
                }
            });
            DataTableSpec outSpec = c.createSpec();
            return new DataTableSpec[]{outSpec};
        } else {
            return null;
        }

    }





    private ColumnRearranger createRearranger(
            final PMMLGeneralRegressionContent content,
            final PMMLPortObjectSpec pmmlSpec,
            final DataTableSpec inDataSpec)
            throws InvalidSettingsException {
        if (content == null) {
            throw new InvalidSettingsException("No input");
        }

        // the predictor can only predict linear regression models
        if (!(content.getModelType().equals(ModelType.multinomialLogistic)
                || content.getModelType().equals(ModelType.generalLinear))) {
            throw new InvalidSettingsException("Model Type: "
                    + content.getModelType() + " is not supported.");
        }
        if (content.getModelType().equals(ModelType.generalLinear)
                && !content.getFunctionName().equals(FunctionName.regression)) {
            throw new InvalidSettingsException("Function Name: "
                    + content.getFunctionName() + " is not supported for linear regression.");
        }
        if (content.getModelType().equals(ModelType.multinomialLogistic)
                && !content.getFunctionName().equals(FunctionName.classification)) {
            throw new InvalidSettingsException("Function Name: "
                    + content.getFunctionName() + " is not supported for logistic regression.");
        }

        // check if all factors are in the given data table and that they
        // are nominal values
        for (PMMLPredictor factor : content.getFactorList()) {
            DataColumnSpec columnSpec = inDataSpec.getColumnSpec(
                    factor.getName());
            if (null == columnSpec) {
                throw new InvalidSettingsException("The column \""
                        + factor.getName()
                        + "\" is in the model but not in given table.");
            }
            if (!columnSpec.getType().isCompatible(NominalValue.class)) {
                throw new InvalidSettingsException("The column \""
                        + factor.getName() + "\" is supposed to be nominal.");
            }
        }

        // check if all covariates are in the given data table and that they
        // are numeric values
        Pattern pattern = Pattern.compile("(.*)\\[\\d+\\]");
        for (PMMLPredictor covariate : content.getCovariateList()) {
            DataColumnSpec columnSpec =
                    inDataSpec.getColumnSpec(covariate.getName());
            if (null == columnSpec) {
                Matcher matcher = pattern.matcher(covariate.getName());
                boolean found = matcher.matches();
                columnSpec = inDataSpec.getColumnSpec(matcher.group(1));
                found = found && null != columnSpec;
                if (!found) {
                    throw new InvalidSettingsException("The column \""
                            + covariate.getName()
                            + "\" is in the model but not in given table.");
                }
            }
            if (columnSpec != null && !columnSpec.getType().isCompatible(DoubleValue.class)
                && !(content.getVectorLengths().containsKey(columnSpec.getName())
                    && ((columnSpec.getType().isCollectionType()
                        && columnSpec.getType().getCollectionElementType().isCompatible(DoubleValue.class))
                        || columnSpec.getType().isCompatible(BitVectorValue.class)
                        || columnSpec.getType().isCompatible(ByteVectorValue.class)))) {
                throw new InvalidSettingsException(
                    "The column \"" + covariate.getName() + "\" is supposed to be numeric.");
            }
        }

        ColumnRearranger c = new ColumnRearranger(inDataSpec);
        if (content.getModelType().equals(ModelType.generalLinear)) {
            c.append(new LinReg2Predictor(content, inDataSpec,
                    pmmlSpec, pmmlSpec.getTargetFields().get(0), m_settings));
        } else {
            c.append(new LogRegPredictor(content, inDataSpec,
                pmmlSpec, pmmlSpec.getTargetFields().get(0), m_settings));
        }
        return c;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // do nothing, the node has no internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // do nothing, the node has no internal state
    }


}
