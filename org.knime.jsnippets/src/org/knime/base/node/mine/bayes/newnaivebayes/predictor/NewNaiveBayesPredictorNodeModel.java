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
 * History
 *   02.05.2006 (koetter): created
 */
package org.knime.base.node.mine.bayes.newnaivebayes.predictor;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.base.node.mine.bayes.newnaivebayes.datamodel.NaiveBayesModel;
import org.knime.base.node.mine.bayes.newnaivebayes.port.NaiveBayesPortObject;
import org.knime.base.node.mine.bayes.newnaivebayes.port.NaiveBayesPortObjectSpec;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the <code>NodeModel</code> implementation of the
 * "Naive Bayes Predictor" node.
 *
 * @author Tobias Koetter
 */
public class NewNaiveBayesPredictorNodeModel extends NodeModel {

    // our logger instance
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(NewNaiveBayesPredictorNodeModel.class);

    private static final int DATA_IN_PORT = 1;

    private static final int MODEL_IN_PORT = 0;


    /**The settings key for the include probability values boolean.*/
    protected static final String CFG_INCL_PROBABILITYVALS_KEY = "inclProbVals";

    private final SettingsModelBoolean m_inclProbVals =
        new SettingsModelBoolean(CFG_INCL_PROBABILITYVALS_KEY, false);

    /**The settings key for the laplace corrector.*/
    protected static final String CFG_LAPLACE_CORRECTOR_KEY =
        "laplaceCorrector";

    private final SettingsModelDouble m_laplaceCorrector =
        new SettingsModelDoubleBounded(CFG_LAPLACE_CORRECTOR_KEY, 0.0, 0.0,
                Double.MAX_VALUE);

    /**The settings key for the use log.*/
    protected static final String CFG_USE_LOG =
        "useLog";

    private final SettingsModelBoolean m_useLog =
        new SettingsModelBoolean(CFG_USE_LOG, true);


    /**The settings key for the normalization.*/
    protected static final String CFG_NORMALIZE =
        "normalize";

    private final SettingsModelBoolean m_normalize =
        new SettingsModelBoolean(CFG_NORMALIZE, true);

    /**Constructor for class NaiveBayesPredictorNodeModel.
     */
    protected NewNaiveBayesPredictorNodeModel() {
//      we have one data in and out port and one model in port
        super(new PortType[] {NaiveBayesPortObject.TYPE,
                BufferedDataTable.TYPE},
                new PortType[] {BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {

        LOGGER.debug("Entering execute(inData, exec) of class "
                + "NaiveBayesPredictorNodeModel.");
//      check input data
        assert (inData != null && inData.length == 2
                && inData[DATA_IN_PORT] != null
                && inData[MODEL_IN_PORT] != null);
        final PortObject dataObject = inData[DATA_IN_PORT];
        if (!(dataObject instanceof BufferedDataTable)) {
            throw new IllegalArgumentException("Invalid input data");
        }
        final BufferedDataTable data = (BufferedDataTable)dataObject;
        final PortObject modelObject = inData[MODEL_IN_PORT];
        if (!(modelObject instanceof NaiveBayesPortObject)) {
            throw new IllegalArgumentException("Invalid input data");
        }
        final NaiveBayesModel model =
            ((NaiveBayesPortObject)modelObject).getModel();
        exec.setMessage("Classifying rows...");
        if (model == null) {
            throw new Exception("Node not properly configured. "
                    + "No Naive Bayes Model available.");
        }
        final double laplaceCorrector = m_laplaceCorrector.getDoubleValue();
        final NewNaiveBayesCellFactory appender =
            new NewNaiveBayesCellFactory(model, data.getDataTableSpec(),
                    m_inclProbVals.getBooleanValue(), laplaceCorrector,
                    m_useLog.getBooleanValue(), m_normalize.getBooleanValue());
        final ColumnRearranger rearranger =
            new ColumnRearranger(data.getDataTableSpec());
        rearranger.append(appender);
        final BufferedDataTable returnVal =
            exec.createColumnRearrangeTable(data, rearranger, exec);
        LOGGER.debug("Exiting execute(inData, exec) of class "
                + "NaiveBayesPredictorNodeModel.");
        return new PortObject[] {returnVal};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //nothing to do
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        //check the input data
        assert (inSpecs != null && inSpecs.length == 2
                && inSpecs[DATA_IN_PORT] != null
                && inSpecs[MODEL_IN_PORT] != null);
        final PortObjectSpec modelObject = inSpecs[MODEL_IN_PORT];
        if (!(modelObject instanceof NaiveBayesPortObjectSpec)) {
            throw new IllegalArgumentException("Invalid input data");
        }
        final DataTableSpec trainingSpec =
            ((NaiveBayesPortObjectSpec)modelObject).getTableSpec();
        final DataColumnSpec classColumn =
            ((NaiveBayesPortObjectSpec)modelObject).getClassColumn();
        if (trainingSpec == null) {
            throw new InvalidSettingsException("No model spec available");
        }

        final PortObjectSpec inSpec = inSpecs[DATA_IN_PORT];
        if (!(inSpec instanceof DataTableSpec)) {
            throw new IllegalArgumentException("TableSpec must not be null");
        }
        final DataTableSpec spec = (DataTableSpec)inSpec;


        //check the input data for columns with the wrong name or wrong type
        final List<String> unknownCols = check4UnknownCols(trainingSpec, spec);
        if (unknownCols.size() >= spec.getNumColumns()) {
            setWarningMessage("No known attribute columns found use "
            + "class prior probability to predict the class membership");
        } else if (unknownCols.size() == 1) {
            setWarningMessage("Input column " + unknownCols.get(0)
                    + " is unknown and will be skipped.");
        } else if (unknownCols.size() > 1) {
            final StringBuilder buf = new StringBuilder();
            buf.append("The following input columns are unknown and "
                    + "will be skipped: ");
            for (int i = 0, length = unknownCols.size(); i < length; i++) {
                if (i != 0) {
                    buf.append(", ");
                }
                if (i > 3) {
                    buf.append("...");
                    break;
                }
                buf.append(unknownCols.get(i));
            }
            setWarningMessage(buf.toString());
        }
        //check if the learned model contains columns which are not in the
        //input data
        final List<String> missingInputCols =
            check4MissingCols(trainingSpec, classColumn.getName(), spec);
        if (missingInputCols.size() == 1) {
            setWarningMessage("Attribute " + missingInputCols.get(0)
                    + " is missing in the input data");
        } else if (missingInputCols.size() > 1) {
            final StringBuilder buf = new StringBuilder();
            buf.append("The following attributes are missing in "
                    + "the input data: ");
            for (int i = 0, length = missingInputCols.size(); i < length; i++) {
                if (i != 0) {
                    buf.append(", ");
                }
                if (i > 3) {
                    buf.append("...");
                    break;
                }
                buf.append(missingInputCols.get(i));
            }
            setWarningMessage(buf.toString());
        }
        final DataColumnSpec resultColSpecs =
            NewNaiveBayesCellFactory.createResultColSpecs(classColumn,
                spec, m_inclProbVals.getBooleanValue());
        if (resultColSpecs != null) {
            return new PortObjectSpec[] {
                    AppendedColumnTable.getTableSpec(spec, resultColSpecs)};
        }
        return null;
    }

    private List<String> check4MissingCols(final DataTableSpec trainingSpec,
            final String classCol, final DataTableSpec spec) {
        final List<String> missingInputCols = new ArrayList<String>();
        for (final DataColumnSpec trainColSpec : trainingSpec) {
            if (!trainColSpec.getName().equals(classCol)) {
                //check only for none class value columns
                if (spec.getColumnSpec(trainColSpec.getName()) == null) {
                    missingInputCols.add(trainColSpec.getName());
                }
            }
        }
        return missingInputCols;
    }

    private List<String> check4UnknownCols(final DataTableSpec trainingSpec,
            final DataTableSpec spec) {
        if (spec == null) {
            throw new NullPointerException("TableSpec must not be null");
        }

        final List<String> unknownCols = new ArrayList<String>();
        for (final DataColumnSpec colSpec : spec) {
            final DataColumnSpec trainColSpec =
                trainingSpec.getColumnSpec(colSpec.getName());
            if (trainColSpec == null || !colSpec.getType().equals(
                    trainColSpec.getType())) {
                unknownCols.add(colSpec.getName());
            }
        }
        return unknownCols;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_inclProbVals.saveSettingsTo(settings);
        m_laplaceCorrector.saveSettingsTo(settings);
        m_useLog.saveSettingsTo(settings);
        m_normalize.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        m_inclProbVals.loadSettingsFrom(settings);
        try {
            m_laplaceCorrector.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            //parameter has been introduced in version 2.1
            //set the value to 0 to simulate the old behaviour
            m_laplaceCorrector.setDoubleValue(0.0);
        }
        try {
            m_useLog.loadSettingsFrom(settings);
            m_normalize.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            m_useLog.setBooleanValue(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        if (settings.containsKey(m_laplaceCorrector.getKey())) {
            //parameter has been introduced in version 2.1
            //set the value to 0 to simulate the old behaviour
            m_laplaceCorrector.validateSettings(settings);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {
        //nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {
        //nothing to do
    }
}
