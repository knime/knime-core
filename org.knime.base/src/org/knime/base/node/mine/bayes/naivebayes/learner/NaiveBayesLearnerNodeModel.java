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
package org.knime.base.node.mine.bayes.naivebayes.learner;

import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.GenericNodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import org.knime.base.node.mine.bayes.naivebayes.datamodel.NaiveBayesModel;
import org.knime.base.node.mine.bayes.naivebayes.port.NaiveBayesPortObject;
import org.knime.base.node.mine.bayes.naivebayes.port.NaiveBayesPortObjectSpec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the <code>NodeModel</code> implementation of the
 * "Naive Bayes Learner" node.
 *
 * @author Tobias Koetter
 */
public class NaiveBayesLearnerNodeModel extends GenericNodeModel {

    // our logger instance
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(NaiveBayesLearnerNodeModel.class);

    private static final String CFG_DATA = "naivebayesData";

    private static final String CFG_DATA_MODEL = "naivebayesDataModel";

    /**
     * Key to store the classification column in the settings.
     */
    public static final String CFG_CLASSIFYCOLUMN_KEY = "classifyColumn";

    /**
     * Key to store if the missing values should be skipped during learning.
     */
    public static final String CFG_SKIP_MISSING_VALUES = "skipMissingVals";

    /**
     * Key to store the maximum number of nominal values in the settings.
     */
    public static final String CFG_MAX_NO_OF_NOMINAL_VALS_KEY =
        "maxNoOfNomVals";
    /**
     * The number of the training data in port.
     */
    public static final int TRAINING_DATA_PORT = 0;

    /**
     * The number of the Bayes model out put port.
     */
    public static final int BAYES_MODEL_PORT = 0;

    /**
     * The name of the column which contains the classification Information.
     */
    private final SettingsModelString m_classifyColumnName =
        new SettingsModelString(CFG_CLASSIFYCOLUMN_KEY, null);

    private final SettingsModelBoolean m_skipMissingVals =
        new SettingsModelBoolean(CFG_SKIP_MISSING_VALUES, false);

    private final SettingsModelIntegerBounded m_maxNoOfNominalVals =
        new SettingsModelIntegerBounded(
            NaiveBayesLearnerNodeModel.CFG_MAX_NO_OF_NOMINAL_VALS_KEY, 20,
            0, Integer.MAX_VALUE);

    private NaiveBayesModel m_model = null;

    /**
     *
     *
     */
    protected NaiveBayesLearnerNodeModel() {
//      we have one data in port for the training data and one model out port
        //for the prediction result
        super(new PortType[] {BufferedDataTable.TYPE},
                new PortType[] {NaiveBayesPortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            InvalidSettingsException {
        LOGGER.debug("Entering execute of "
                + NaiveBayesLearnerNodeModel.class.getName());
//      check input data
        assert (inData != null && inData.length == 1
                && inData[TRAINING_DATA_PORT] != null);
        final PortObject inObject = inData[TRAINING_DATA_PORT];
        if (!(inObject instanceof BufferedDataTable)) {
            throw new IllegalArgumentException("Invalid input data");
        }
        final BufferedDataTable trainingTable = (BufferedDataTable)inObject;
        final String colName = m_classifyColumnName.getStringValue();
        final boolean skipMissingVals = m_skipMissingVals.getBooleanValue();
        final int maxNoOfNomVals = m_maxNoOfNominalVals.getIntValue();
        m_model = new NaiveBayesModel(trainingTable, colName, exec,
                maxNoOfNomVals, skipMissingVals);
        final List<String> missingModels =
            m_model.getAttributesWithMissingVals();
        if (missingModels.size() > 0) {
            final StringBuilder buf = new StringBuilder();
            buf.append("The following attributes contain missing values: ");
            for (int i = 0, length = missingModels.size(); i < length; i++) {
                if (i != 0) {
                    buf.append(", ");
                }
                if (i > 3) {
                    buf.append("...(see View)");
                    break;
                }
                buf.append(missingModels.get(i));
            }
            setWarningMessage(buf.toString());
        }
        if (m_model.containsSkippedAttributes()) {
            setWarningMessage(m_model.getSkippedAttributesString(3));
        }
        LOGGER.debug("Exiting execute of "
                + NaiveBayesLearnerNodeModel.class.getName());
        // return no data tables (empty array)
        return new PortObject[] {
                new NaiveBayesPortObject(trainingTable.getDataTableSpec(),
                        m_model)};
    }

    /**
     * @return Returns the naivebayesModel.
     */
    protected NaiveBayesModel getNaiveBayesModel() {
        return m_model;
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
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        //        check the internal variables if they are valid
        final String classColumn = m_classifyColumnName.getStringValue();
        if (classColumn == null
        || classColumn.length() < 1) {
            throw new InvalidSettingsException(
                    "Please define the classification column");
        }
        final PortObjectSpec inSpec = inSpecs[TRAINING_DATA_PORT];
        if (!(inSpec instanceof DataTableSpec)) {
            throw new IllegalArgumentException("Invalid input data");
        }
        final DataTableSpec tableSpec = (DataTableSpec)inSpec;
        if (tableSpec.findColumnIndex(classColumn) < 0) {
            throw new InvalidSettingsException(
                "Please define the classification column");
        }
        if (tableSpec.getNumColumns() < 2) {
            throw new InvalidSettingsException(
                    "Input table should contain at least 2 columns");
        }
        final int maxNoOfNominalVals = m_maxNoOfNominalVals.getIntValue();
        //check if the table contains at least one nominal column
        //and check each nominal column with a valid domain
        //if it contains more values than allowed
        boolean containsNominalCol = false;
        final List<String> toBigNominalColumns = new ArrayList<String>();
        for (int i = 0, length = tableSpec.getNumColumns();
                i < length; i++) {
            final DataColumnSpec colSpec = tableSpec.getColumnSpec(i);
            if (colSpec.getType()
                    .isCompatible(NominalValue.class)) {
                containsNominalCol = true;
                final DataColumnDomain domain = colSpec.getDomain();
                if (domain != null && domain.getValues() != null) {
                    if (domain.getValues().size() > maxNoOfNominalVals) {
                        //the domain is available and contains too many
                        //unique values
                        if (colSpec.getName().equals(
                                classColumn)) {
                            //throw an exception if the class column
                            //contains too many unique values
                            throw new InvalidSettingsException(
                        "Class column domain contains too many unique values"
                                    + " (" + domain.getValues().size() + ")");
                        }
                        toBigNominalColumns.add(colSpec.getName()
                                + " (" + domain.getValues().size() + ")");
                    }
                }
            }
        }
        if (!containsNominalCol) {
            throw new InvalidSettingsException(
                    "No possible class attribute found in input table");
        }
        if (toBigNominalColumns.size() == 1) {
            setWarningMessage("Column " + toBigNominalColumns.get(0)
                    + " will possibly be skipped.");
        } else if (toBigNominalColumns.size() > 1) {
            final StringBuilder buf = new StringBuilder();
            buf.append("The following columns will possibly be skipped: ");
            for (int i = 0, length = toBigNominalColumns.size(); i < length;
                i++) {
                if (i != 0) {
                    buf.append(", ");
                }
                if (i > 3) {
                    buf.append("...");
                    break;
                }
                buf.append(toBigNominalColumns.get(i));

            }
            setWarningMessage(buf.toString());
        }
        if (tableSpec.getNumColumns() - toBigNominalColumns.size() < 1) {
            throw new InvalidSettingsException("Not enough valid columns");
        }

        return new PortObjectSpec[]{new NaiveBayesPortObjectSpec(tableSpec,
                tableSpec.getColumnSpec(classColumn))};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        assert (settings != null);
        m_classifyColumnName.saveSettingsTo(settings);
        m_skipMissingVals.saveSettingsTo(settings);
        m_maxNoOfNominalVals.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_classifyColumnName.loadSettingsFrom(settings);
        m_skipMissingVals.loadSettingsFrom(settings);
        m_maxNoOfNominalVals.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {

        final SettingsModelString colName =
            m_classifyColumnName.createCloneWithValidatedValue(settings);
        if (colName == null || colName.getStringValue().trim().length() < 1) {
            throw new InvalidSettingsException("No class column selected");
        }
        final SettingsModelIntegerBounded maxNoOfNomVals =
            m_maxNoOfNominalVals.createCloneWithValidatedValue(settings);
        if (maxNoOfNomVals.getIntValue() < 0) {
            throw new InvalidSettingsException("Maximum number of unique "
                    + "nominal values should be a positive number");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        final File modelFile = new File(nodeInternDir, CFG_DATA);
        final FileInputStream modelIn = new FileInputStream(modelFile);
//        because the loadFromXML method returns the content of the root tag
//        we don't need to ask for the content of the root tag
        final ModelContentRO myModel = ModelContent.loadFromXML(modelIn);
        try {
            m_model = new NaiveBayesModel(myModel);
        } catch (final Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        final File modelFile = new File(nodeInternDir, CFG_DATA);
        final FileOutputStream modelOut = new FileOutputStream(modelFile);
        final ModelContent myModel = new ModelContent(CFG_DATA_MODEL);
        m_model.savePredictorParams(myModel);
        myModel.saveToXML(modelOut);
    }
}
