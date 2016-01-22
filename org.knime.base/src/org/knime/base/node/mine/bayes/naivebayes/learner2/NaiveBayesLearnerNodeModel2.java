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
 */
package org.knime.base.node.mine.bayes.naivebayes.learner2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.mine.bayes.naivebayes.datamodel2.AttributeModel;
import org.knime.base.node.mine.bayes.naivebayes.datamodel2.NaiveBayesModel;
import org.knime.base.node.mine.bayes.naivebayes.datamodel2.PMMLNaiveBayesModelTranslator;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.NominalValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;

/**
 * This is the <code>NodeModel</code> implementation of the
 * "Naive Bayes Learner" node.
 *
 * @author Tobias Koetter
 */
public class NaiveBayesLearnerNodeModel2 extends NodeModel {

    // our logger instance
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(NaiveBayesLearnerNodeModel2.class);

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
    public static final String CFG_MAX_NO_OF_NOMINAL_VALS_KEY = "maxNoOfNomVals";

    /**Key to store the pmml compatibility flag.*/
    private static final String CFG_PMML_COMPATIBLE = "compatiblePMML";
    /**
     * The number of the training data in port.
     */
    public static final int TRAINING_DATA_PORT = 0;

    /**
     * The number of the optional PMML model in port.
     */
    public static final int MODEL_INPORT = 1;

    /**
     * The number of the Bayes model out put port.
     */
    public static final int BAYES_MODEL_PORT = 0;

    private final SettingsModelString m_classifyColumnName = createClassifyColumnModel();

    private final SettingsModelBoolean m_pmmlCompatible = createPMMLCompatibilityFlagModel();

    private final SettingsModelBoolean m_ignoreMissingVals = createIgnoreMissingValsModel();

    private final SettingsModelIntegerBounded m_maxNoOfNominalVals = createMaxNominalValsModel();

    private final SettingsModelDouble m_threshold = createThresholdModel();

    /**
     * @return the Laplace corrector model
     */
    static SettingsModelDoubleBounded createThresholdModel() {
        return new SettingsModelDoubleBounded("threshold", 0.0, 0.0, Double.MAX_VALUE);
    }

    /**
     * @return the maximum number of nominal values
     */
    static SettingsModelIntegerBounded createMaxNominalValsModel() {
        return new SettingsModelIntegerBounded(
            NaiveBayesLearnerNodeModel2.CFG_MAX_NO_OF_NOMINAL_VALS_KEY, 20,
            0, Integer.MAX_VALUE);
    }

    /**
     * @return the classify column model
     */
    static SettingsModelString createClassifyColumnModel() {
        return new SettingsModelString(CFG_CLASSIFYCOLUMN_KEY, null);
    }

    /**
     * @return the PMML compatibility flag model
     */
    static SettingsModelBoolean createPMMLCompatibilityFlagModel() {
        return new SettingsModelBoolean(CFG_PMML_COMPATIBLE, false);
    }

    /**
     * @return the ignore missing value flag model
     */
    static SettingsModelBoolean createIgnoreMissingValsModel() {
        final SettingsModelBoolean model = new SettingsModelBoolean(CFG_SKIP_MISSING_VALUES, false);
        model.setEnabled(!createPMMLCompatibilityFlagModel().getBooleanValue());
        return model;
    }

    private NaiveBayesModel m_model = null;

    private boolean m_pmmlInEnabled;

    /**
     * Constructor.
     */
    protected NaiveBayesLearnerNodeModel2() {
        this(true);
    }

    /**
     * Constructor.
     * @param pmmlInEnabled true if the optional PMML input is accessible
     */
    public NaiveBayesLearnerNodeModel2(final boolean pmmlInEnabled) {
        super(pmmlInEnabled ? new PortType[]{BufferedDataTable.TYPE, PMMLPortObject.TYPE_OPTIONAL} : new PortType[]{BufferedDataTable.TYPE},
            new PortType[]{PMMLPortObject.TYPE, BufferedDataTable.TYPE});
        m_pmmlCompatible.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                if (m_pmmlCompatible.getBooleanValue()) {
                    //PMML compatible needs to ignore missing values
                    m_ignoreMissingVals.setBooleanValue(true);
                }
                m_ignoreMissingVals.setEnabled(!m_pmmlCompatible.getBooleanValue());
            }
        });
        m_pmmlInEnabled = pmmlInEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        //        check the internal variables if they are valid
        final PortObjectSpec inSpec = inSpecs[TRAINING_DATA_PORT];
        if (!(inSpec instanceof DataTableSpec)) {
            throw new IllegalArgumentException("Invalid input data");
        }
        final DataTableSpec tableSpec = (DataTableSpec)inSpec;
        if (m_classifyColumnName.getStringValue() == null) {
            String predictedClassName = null;
            for (DataColumnSpec colSpec : tableSpec) {
                if (colSpec.getType().isCompatible(NominalValue.class)) {
                    if (predictedClassName == null) {
                        predictedClassName = colSpec.getName();
                    } else {
                        throw new InvalidSettingsException("Please define the classification column");
                    }
                }
            }
            m_classifyColumnName.setStringValue(predictedClassName);
            setWarningMessage("Classification column preset to " + predictedClassName);
        }
        final String classColumn = m_classifyColumnName.getStringValue();
        final DataColumnSpec classColSpec = tableSpec.getColumnSpec(classColumn);
        if (classColSpec == null) {
            throw new InvalidSettingsException("Classification column not found in input table");
        }
        if (tableSpec.getNumColumns() < 2) {
            throw new InvalidSettingsException("Input table should contain at least 2 columns");
        }
        final int maxNoOfNominalVals = m_maxNoOfNominalVals.getIntValue();
        //and check each nominal column with a valid domain if it contains more values than allowed
        //this needs to be in sync with the NaiveBayesModel.createModelMap method!!!
        final List<String> ignoredColumns = new LinkedList<>();
        final List<String> toBigNominalColumns = new LinkedList<>();
        final List<String> learnCols = new LinkedList<>();
        for (final DataColumnSpec colSpec : tableSpec) {
            final AttributeModel model =
                NaiveBayesModel.getCompatibleModel(colSpec, classColumn, maxNoOfNominalVals,
                    m_ignoreMissingVals.getBooleanValue(), m_pmmlCompatible.getBooleanValue());
            if (model == null) {
                //the column type is not supported by Naive Bayes
                ignoredColumns.add(colSpec.getName());
                continue;
            }
            final DataType colType = colSpec.getType();
            if (colType.isCompatible(NominalValue.class)) {
                final DataColumnDomain domain = colSpec.getDomain();
                if (domain != null && domain.getValues() != null) {
                    if (domain.getValues().size() > maxNoOfNominalVals) {
                        //the domain is available and contains too many
                        //unique values
                        if (colSpec.getName().equals(classColumn)) {
                            //throw an exception if the class column
                            //contains too many unique values
                            throw new InvalidSettingsException(
                                "Class column domain contains too many unique values" + " (count: "
                                    + domain.getValues().size() + ")");
                        }
                        toBigNominalColumns.add(colSpec.getName() + " (count: " + domain.getValues().size() + ")");
                    }
                }
                learnCols.add(model.getAttributeName());
            }
        }
        warningMessage("The following columns will possibly be skipped due to too many values: ", toBigNominalColumns);
        warningMessage("The following columns are not supported and thus will be ignored: ", ignoredColumns);

        if (learnCols.size() < 1) {
            throw new InvalidSettingsException("Not enough valid columns");
        }
        final PMMLPortObjectSpec modelSpec = m_pmmlInEnabled ? (PMMLPortObjectSpec)inSpecs[MODEL_INPORT] : null;
        final PMMLPortObjectSpec pmmlSpec = createPMMLSpec(tableSpec, modelSpec, learnCols, classColumn);
        return new PortObjectSpec[]{pmmlSpec, NaiveBayesModel.createStatisticsTableSpec(classColSpec.getType(),
            m_ignoreMissingVals.getBooleanValue())};
    }

    private void warningMessage(final String message, final List<String> colNames) {
        if (!colNames.isEmpty()) {
            final StringBuilder buf = new StringBuilder();
            buf.append(message);
            for (int i = 0, length = colNames.size(); i < length; i++) {
                if (i != 0) {
                    buf.append(", ");
                }
                if (i == 4) {
                    setWarningMessage(buf.toString() + "... (see log file for details)");
                }
                buf.append(colNames.get(i));

            }
            if (colNames.size() < 4) {
                setWarningMessage(buf.toString());
            } else {
                LOGGER.info(buf.toString());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec)
            throws CanceledExecutionException, InvalidSettingsException {
        LOGGER.debug("Entering execute of " + NaiveBayesLearnerNodeModel2.class.getName());
        assert (inData != null && ((inData.length == 2 && m_pmmlInEnabled) || (inData.length == 1 && !m_pmmlInEnabled))
                && inData[TRAINING_DATA_PORT] != null);
        final PortObject inObject = inData[TRAINING_DATA_PORT];
        if (!(inObject instanceof BufferedDataTable)) {
            throw new IllegalArgumentException("Invalid input data");
        }
        final BufferedDataTable trainingTable = (BufferedDataTable)inObject;
        final boolean ignoreMissingVals = m_ignoreMissingVals.getBooleanValue();
        final boolean pmmlCompatible = m_pmmlCompatible.getBooleanValue();
        final int maxNoOfNomVals = m_maxNoOfNominalVals.getIntValue();
        m_model = new NaiveBayesModel(trainingTable, m_classifyColumnName.getStringValue(), exec, maxNoOfNomVals,
            ignoreMissingVals, pmmlCompatible, m_threshold.getDoubleValue());
        final List<String> missingModels = m_model.getAttributesWithMissingVals();
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
        LOGGER.debug("Exiting execute of " + NaiveBayesLearnerNodeModel2.class.getName());

        // handle the optional PMML input
        final PMMLPortObject inPMMLPort = m_pmmlInEnabled ? (PMMLPortObject)inData[MODEL_INPORT] : null;
        final DataTableSpec tableSpec = trainingTable.getSpec();
        final PMMLPortObjectSpec outPortSpec = createPMMLSpec(tableSpec,
            inPMMLPort == null ? null : inPMMLPort.getSpec(),
                m_model.getPMMLLearningCols(), m_model.getClassColumnName());
        final PMMLPortObject outPMMLPort = new PMMLPortObject(outPortSpec, inPMMLPort, tableSpec);
        outPMMLPort.addModelTranslater(new PMMLNaiveBayesModelTranslator(m_model));
        return new PortObject[]{outPMMLPort, m_model.getStatisticsTable()};
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

    private PMMLPortObjectSpec createPMMLSpec(final DataTableSpec tableSpec, final PMMLPortObjectSpec modelSpec,
        final List<String> learnCols, final String classColumn) {
        final PMMLPortObjectSpecCreator pmmlSpecCreator = new PMMLPortObjectSpecCreator(modelSpec, tableSpec);
        pmmlSpecCreator.setLearningColsNames(learnCols);
        pmmlSpecCreator.setTargetColName(classColumn);
        final PMMLPortObjectSpec pmmlSpec = pmmlSpecCreator.createSpec();
        return pmmlSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_classifyColumnName.saveSettingsTo(settings);
        m_ignoreMissingVals.saveSettingsTo(settings);
        m_maxNoOfNominalVals.saveSettingsTo(settings);
        m_pmmlCompatible.saveSettingsTo(settings);
        m_threshold.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_classifyColumnName.loadSettingsFrom(settings);
        m_ignoreMissingVals.loadSettingsFrom(settings);
        m_maxNoOfNominalVals.loadSettingsFrom(settings);
        m_pmmlCompatible.loadSettingsFrom(settings);
        m_threshold.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        final SettingsModelString colName = m_classifyColumnName.createCloneWithValidatedValue(settings);
        if (colName == null || colName.getStringValue() == null || colName.getStringValue().trim().length() < 1) {
            throw new InvalidSettingsException("No class column selected");
        }
        final SettingsModelIntegerBounded maxNoOfNomVals =
            m_maxNoOfNominalVals.createCloneWithValidatedValue(settings);
        if (maxNoOfNomVals.getIntValue() < 0) {
            throw new InvalidSettingsException("Maximum number of unique nominal values should be a positive number");
        }
        m_pmmlCompatible.validateSettings(settings);
        m_threshold.validateSettings(settings);
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
