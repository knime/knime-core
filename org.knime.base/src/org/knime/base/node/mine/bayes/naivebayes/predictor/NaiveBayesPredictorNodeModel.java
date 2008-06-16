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

import java.io.File;
import java.util.List;

import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.base.node.mine.bayes.naivebayes.datamodel.NaiveBayesModel;

/**
 * This is the <code>NodeModel</code> implementation of the
 * "Naive Bayes Predictor" node.
 *
 * @author Tobias Koetter
 */
public class NaiveBayesPredictorNodeModel extends NodeModel {

    // our logger instance
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(NaiveBayesPredictorNodeModel.class);

    private static final int DATA_IN_PORT = 0;

    private static final int MODEL_IN_PORT = 0;

    /**The settings key for the include probability values boolean.*/
    protected static final String CFG_INCL_PROBABILITYVALS_KEY = "inclProbVals";

    private final SettingsModelBoolean m_inclProbVals =
        new SettingsModelBoolean(CFG_INCL_PROBABILITYVALS_KEY, false);

    private NaiveBayesModel m_model;

    /**Constructor for class NaiveBayesPredictorNodeModel.
     */
    protected NaiveBayesPredictorNodeModel() {
//      we have one data in and out port and one model in port
        super(1, 1, 1, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        LOGGER.debug("Entering execute(inData, exec) of class "
                + "NaiveBayesPredictorNodeModel.");
//      check input data
        assert (inData != null && inData.length == 1
                && inData[DATA_IN_PORT] != null);
        final BufferedDataTable data = inData[DATA_IN_PORT];
        exec.setMessage("Classifying rows...");
        if (m_model == null) {
            throw new Exception("Node not properly configured. "
                    + "No Naive Bayes Model available.");
        }

        final NaiveBayesCellFactory appender =
            new NaiveBayesCellFactory(m_model, data.getDataTableSpec(),
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
        return new BufferedDataTable[] {returnVal};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_model = null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        //check the input data
        assert (inSpecs != null && inSpecs.length == 1
                && inSpecs[DATA_IN_PORT] != null);
        if (m_model == null) {
            throw new InvalidSettingsException("No model available");
        }
        final DataTableSpec spec = inSpecs[DATA_IN_PORT];
        if (spec == null) {
            throw new NullPointerException("TableSpec must not be null");
        }
        //check the input data for columns with the wrong name or wrong type
        final List<String> unknownCols = m_model.check4UnknownCols(spec);
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
            m_model.check4MissingCols(spec);
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

        return new DataTableSpec[] {
            AppendedColumnTable.getTableSpec(inSpecs[DATA_IN_PORT],
                    NaiveBayesCellFactory.createResultColSpecs(m_model,
                            inSpecs[DATA_IN_PORT],
                            m_inclProbVals.getBooleanValue()))
        };
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


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadModelContent(final int index,
            final ModelContentRO predParams) throws InvalidSettingsException {
        if (index != MODEL_IN_PORT) {
            throw new IndexOutOfBoundsException("Invalid model input-port: "
                    + index);
        }
        if (predParams == null) {
            m_model = null;
        } else {
            m_model = new NaiveBayesModel(predParams);
        }
    }
}
