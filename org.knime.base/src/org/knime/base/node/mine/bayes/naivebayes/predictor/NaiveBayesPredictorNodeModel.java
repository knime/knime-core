/*
 * ------------------------------------------------------------------
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
 *   02.05.2006 (koetter): created
 */
package org.knime.base.node.mine.bayes.naivebayes.predictor;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.GenericNodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.base.node.mine.bayes.naivebayes.datamodel.NaiveBayesModel;
import org.knime.base.node.mine.bayes.naivebayes.port.NaiveBayesPortObject;
import org.knime.base.node.mine.bayes.naivebayes.port.NaiveBayesPortObjectSpec;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the <code>NodeModel</code> implementation of the
 * "Naive Bayes Predictor" node.
 *
 * @author Tobias Koetter
 */
public class NaiveBayesPredictorNodeModel extends GenericNodeModel {

    // our logger instance
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(NaiveBayesPredictorNodeModel.class);

    private static final int DATA_IN_PORT = 1;

    private static final int MODEL_IN_PORT = 0;


    /**The settings key for the include probability values boolean.*/
    protected static final String CFG_INCL_PROBABILITYVALS_KEY = "inclProbVals";

    private final SettingsModelBoolean m_inclProbVals =
        new SettingsModelBoolean(CFG_INCL_PROBABILITYVALS_KEY, false);

    /**Constructor for class NaiveBayesPredictorNodeModel.
     */
    protected NaiveBayesPredictorNodeModel() {
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

        final NaiveBayesCellFactory appender =
            new NaiveBayesCellFactory(model, data.getDataTableSpec(),
                    m_inclProbVals.getBooleanValue());
        final ColumnRearranger rearranger =
            new ColumnRearranger(data.getDataTableSpec());
        rearranger.append(appender);
        final BufferedDataTable returnVal =
            exec.createColumnRearrangeTable(data, rearranger, exec);
//        final DataColumnSpec[] colSpecs = appender.getResultColumnsSpec();
//        final AppendedColumnTable appTable =
//            new AppendedColumnTable(data, appender, colSpecs);
//        final BufferedDataTable returnVal =
//            exec.createBufferedDataTable(appTable, exec);
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
            NaiveBayesCellFactory.createResultColSpecs(classColumn,
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        m_inclProbVals.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) {
        //no settings to check
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
