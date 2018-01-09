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
 * -------------------------------------------------------------------
 *
 * History
 *   27.07.2005 (mb): created
 */
package org.knime.base.node.mine.decisiontree2.predictor2;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.base.node.mine.decisiontree2.PMMLDecisionTreeTranslator;
import org.knime.base.node.mine.decisiontree2.model.DecisionTree;
import org.knime.base.node.mine.util.PredictorHelper;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
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
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.pmml.PMMLModelType;
import org.knime.core.util.Pair;
import org.w3c.dom.Node;

/**
 * <p>Despite being public no official API.
 * @author Michael Berthold, University of Konstanz
 */
public final class DecTreePredictorNodeModel extends NodeModel {
    /** Index of input data port. */
    public static final int INDATAPORT = 1;

    /** Index of input model (=decision tree) port. */
    public static final int INMODELPORT = 0;

    /** The node logger for this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(DecTreePredictorNodeModel.class);

    /** XML tag name in configuration file for max num pattern for hiliting. */
    public static final String MAXCOVERED = "UseGainRatio";

    private final SettingsModelIntegerBounded m_maxNumCoveredPattern =
            createMaxNumPatternSettings();

    /** @return a new settings models for the maximum number of pattern stored
     *          for hiliting within the dec tree view.
     */
    static SettingsModelIntegerBounded createMaxNumPatternSettings() {
        return new SettingsModelIntegerBounded(MAXCOVERED,
                    /* default */10000,
                    /* min: */0,
                    /* max: */Integer.MAX_VALUE);
    }

    /** XML tag name in configuration file for show distribution flag. */
    public static final String SHOW_DISTRIBUTION = "ShowDistribution";

    /** Field was added for version 2.1. */
    private final SettingsModelBoolean m_showDistribution
            = new SettingsModelBoolean(SHOW_DISTRIBUTION, false);

    private final SettingsModelString m_predictionColumn = PredictorHelper.getInstance().createPredictionColumn();

    private final SettingsModelBoolean m_overridePrediction = PredictorHelper.getInstance().createChangePrediction();

    private final SettingsModelString m_probabilitySuffix = PredictorHelper.getInstance().createSuffix();

    private DecisionTree m_decTree;

    /**
     * Creates a new predictor for PMMLDecisionTreePortObject models as input
     * and one additional data input, and the scored data as output.
     */
    public DecTreePredictorNodeModel() {
        super(new PortType[]{PMMLPortObject.TYPE,
              BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE});
        m_decTree = null;
    }

    /**
     * @return internal tree structure or <code>null</code> if it does not
     *         exist
     */
    protected DecisionTree getDecisionTree() {
        return m_decTree;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_maxNumCoveredPattern.saveSettingsTo(settings);
        m_showDistribution.saveSettingsTo(settings);
        m_predictionColumn.saveSettingsTo(settings);
        m_overridePrediction.saveSettingsTo(settings);
        m_probabilitySuffix.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // no checks as fields were added after first release
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        try {
            m_maxNumCoveredPattern.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ise) {
            m_maxNumCoveredPattern.setIntValue(10000);
        }

        try {
            m_showDistribution.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ise) {
            m_showDistribution.setBooleanValue(false);
        }

        try {
            m_predictionColumn.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ise) {
            m_predictionColumn.setStringValue("Prediction (DecTree)");
        }

        try {
            m_overridePrediction.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ise) {
            m_overridePrediction.setBooleanValue(false);
        }

        try {
            m_probabilitySuffix.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ise) {
            //We are ok with the default.
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObject[] execute(final PortObject[] inPorts,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        exec.setMessage("Decision Tree Predictor: Loading predictor...");
        PMMLPortObject port = (PMMLPortObject)inPorts[INMODELPORT];

        List<Node> models = port.getPMMLValue().getModels(
                PMMLModelType.TreeModel);
        if (models.isEmpty()) {
            String msg = "Decision Tree evaluation failed: "
                   + "No tree model found.";
            LOGGER.error(msg);
            throw new RuntimeException(msg);
        }
        PMMLDecisionTreeTranslator trans = new PMMLDecisionTreeTranslator();
        port.initializeModelTranslator(trans);
        DecisionTree decTree = trans.getDecisionTree();

        decTree.resetColorInformation();
        BufferedDataTable inData = (BufferedDataTable)inPorts[INDATAPORT];
        // get column with color information
        String colorColumn = null;
        for (DataColumnSpec s : inData.getDataTableSpec()) {
            if (s.getColorHandler() != null) {
                colorColumn = s.getName();
                break;
            }
        }
        decTree.setColorColumn(colorColumn);
        exec.setMessage("Decision Tree Predictor: start execution.");
        PortObjectSpec[] inSpecs = new PortObjectSpec[] {
                inPorts[0].getSpec(), inPorts[1].getSpec() };
        DataTableSpec outSpec = createOutTableSpec(inSpecs);
        BufferedDataContainer outData = exec.createDataContainer(outSpec);
        long coveredPattern = 0;
        long nrPattern = 0;
        long rowCount = 0;
        final long numberRows = inData.size();
        exec.setMessage("Classifying...");
        List<String> predictionValues = getPredictionStrings((PMMLPortObjectSpec)inPorts[INMODELPORT].getSpec());
        for (DataRow thisRow : inData) {
            DataCell cl = null;
            LinkedHashMap<String, Double> classDistrib = null;
            try {
                Pair<DataCell, LinkedHashMap<DataCell, Double>> pair
                        = decTree.getWinnerAndClasscounts(
                                thisRow, inData.getDataTableSpec());
                cl = pair.getFirst();
                LinkedHashMap<DataCell, Double> classCounts =
                   pair.getSecond();

                classDistrib = getDistribution(classCounts);
                if (coveredPattern < m_maxNumCoveredPattern.getIntValue()) {
                    // remember this one for HiLite support
                    decTree.addCoveredPattern(thisRow, inData
                            .getDataTableSpec());
                    coveredPattern++;
                } else {
                    // too many patterns for HiLite - at least remember color
                    decTree.addCoveredColor(thisRow, inData
                            .getDataTableSpec());
                }
                nrPattern++;
            } catch (Exception e) {
                LOGGER.error("Decision Tree evaluation failed: "
                        + e.getMessage());
                throw e;
            }
            if (cl == null) {
                LOGGER.error("Decision Tree evaluation failed: result empty");
                throw new Exception("Decision Tree evaluation failed.");
            }

            DataCell[] newCells = new DataCell[outSpec.getNumColumns()];
            int numInCells = thisRow.getNumCells();
            for (int i = 0; i < numInCells; i++) {
                newCells[i] = thisRow.getCell(i);
            }

            if (m_showDistribution.getBooleanValue()) {
                assert predictionValues.size() >= newCells.length - 1 - numInCells : "Could not determine the prediction values: "
                    + newCells.length + "; " + numInCells + "; " + predictionValues;
                for (int i = numInCells; i < newCells.length - 1; i++) {
                    String predClass = predictionValues.get(i - numInCells);
                    if (classDistrib != null
                            && classDistrib.get(predClass) != null) {
                        newCells[i] = new DoubleCell(
                                classDistrib.get(predClass));
                    } else {
                        newCells[i] = new DoubleCell(0.0);
                    }
                }
            }
            newCells[newCells.length - 1] = cl;

            outData.addRowToTable(new DefaultRow(thisRow.getKey(), newCells));

            rowCount++;
            if (rowCount % 100 == 0) {
                exec.setProgress(rowCount / (double) numberRows,
                        "Classifying... Row " + rowCount + " of " + numberRows);
            }
            exec.checkCanceled();
        }
        if (coveredPattern < nrPattern) {
            // let the user know that we did not store all available pattern
            // for HiLiting.
            this.setWarningMessage("Tree only stored first "
                    + m_maxNumCoveredPattern.getIntValue() + " (of "
                    + nrPattern + ") rows for HiLiting!");
        }
        outData.close();
        m_decTree = decTree;
        exec.setMessage("Decision Tree Predictor: end execution.");
        return new BufferedDataTable[]{outData.getTable()};
    }

    /**
     * @param spec
     * @return
     * @throws InvalidSettingsException when the PMML document is not valid
     */
    private List<String> getPredictionStrings(final PMMLPortObjectSpec spec) throws InvalidSettingsException {
        List<DataCell> predictionValues = getPredictionValues(spec);
        if (predictionValues == null) {
            return Collections.emptyList();
        }
        List<String> ret = new ArrayList<String>(predictionValues.size());
        for (DataCell dataCell : predictionValues) {
            ret.add(dataCell.toString());
        }
        return ret;
    }

    /**
     * @param classCounts
     * @return
     */
    private static LinkedHashMap<String, Double> getDistribution(
            final LinkedHashMap<DataCell, Double> classCounts) {
        LinkedHashMap<String, Double> dist
                = new LinkedHashMap<String, Double>(classCounts.size());
        Double total = 0.0;
        for (Double count : classCounts.values()) {
            total += count;
        }
        if (total == 0.0) {
            return null;
        } else {
            for (Entry<DataCell, Double> classCount : classCounts.entrySet()) {
                dist.put(classCount.getKey().toString(),
                        classCount.getValue() / total);
            }
            return dist;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_decTree = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        String predCol = m_predictionColumn.getStringValue();
        CheckUtils.checkSetting(!m_overridePrediction.getBooleanValue()
            || (predCol != null && !predCol.trim().isEmpty()), "Prediction column name cannot be empty");

        PMMLPortObjectSpec treeSpec = (PMMLPortObjectSpec)inSpecs[INMODELPORT];
        DataTableSpec inSpec = (DataTableSpec)inSpecs[1];
        for (String learnColName : treeSpec.getLearningFields()) {
            if (!inSpec.containsName(learnColName)) {
                throw new InvalidSettingsException(
                        "Learning column \"" + learnColName
                        + "\" not found in input "
                        + "data to be predicted");
            }
        }
        return new PortObjectSpec[]{createOutTableSpec(inSpecs)};
    }

    private List<DataCell> getPredictionValues(
            final PMMLPortObjectSpec treeSpec) throws InvalidSettingsException {
        String targetCol = treeSpec.getTargetFields().get(0);
        DataColumnSpec colSpec =
                treeSpec.getDataTableSpec().getColumnSpec(targetCol);

        if (!colSpec.getType().isCompatible(StringValue.class)) {
            throw new InvalidSettingsException("This predictor only supports target fields of data type string (got "
                    +  colSpec.getType() + ")");
        }

        //Replaced LinkedList because later it is used to get values by index
        ArrayList<DataCell> predValues = new ArrayList<DataCell>();
        if (colSpec.getDomain().hasValues()) {
            predValues.addAll(colSpec.getDomain().getValues());
        } else if (colSpec.getType() == BooleanCell.TYPE) {
            predValues.add(BooleanCell.FALSE);
            predValues.add(BooleanCell.TRUE);
        } else {
            return null;
        }
        return predValues;
    }

    private DataTableSpec createOutTableSpec(
            final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        List<DataCell>  predValues = null;
        if (m_showDistribution.getBooleanValue()) {
            predValues = getPredictionValues(
                    (PMMLPortObjectSpec)inSpecs[INMODELPORT]);
            if (predValues == null) {
                return null; // no out spec can be determined
            }
        }

        int numCols = (predValues == null ? 0 : predValues.size()) + 1;

        DataTableSpec inSpec = (DataTableSpec)inSpecs[INDATAPORT];
        DataColumnSpec[] newCols = new DataColumnSpec[numCols];

        /* Set bar renderer and domain [0,1] as default for the double cells
         * containing the distribution */
//        DataColumnProperties propsRendering = new DataColumnProperties(
//                Collections.singletonMap(
//                        DataValueRenderer.PROPERTY_PREFERRED_RENDERER,
//                        DoubleBarRenderer.DESCRIPTION));
        DataColumnDomain domain = new DataColumnDomainCreator(
                new DoubleCell(0.0), new DoubleCell(1.0))
                .createDomain();

        PredictorHelper predictorHelper = PredictorHelper.getInstance();
        String trainingColumnName = ((PMMLPortObjectSpec)inSpecs[INMODELPORT]).getTargetFields().iterator().next();
        // add all distribution columns
        for (int i = 0; i < numCols - 1; i++) {
            assert predValues != null;
            DataColumnSpecCreator colSpecCreator =
                new DataColumnSpecCreator(predictorHelper.probabilityColumnName(trainingColumnName, predValues.get(i)
                    .toString(), m_probabilitySuffix.getStringValue()), DoubleCell.TYPE);
            //            colSpecCreator.setProperties(propsRendering);
            colSpecCreator.setDomain(domain);
            newCols[i] = colSpecCreator.createSpec();
        }
        //add the prediction column
        String predictionColumnName =
            predictorHelper.computePredictionColumnName(m_predictionColumn.getStringValue(),
                m_overridePrediction.getBooleanValue(), trainingColumnName);
        newCols[numCols - 1] = new DataColumnSpecCreator(predictionColumnName, StringCell.TYPE).createSpec();
        DataTableSpec newColSpec =
                new DataTableSpec(newCols);
        return new DataTableSpec(inSpec, newColSpec);
    }

    private static final String INTERNALS_FILE_NAME = "DecTreeClassifier.bin";

    /**
     * Load internals.
     *
     * @param nodeInternDir The intern node directory to load tree from.
     * @param exec Used to report progress or cancel saving.
     * @throws IOException Always, since this method has not been implemented
     *             yet.
     * @see org.knime.core.node.NodeModel
     *      #loadInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {

        // read the decision tree
        File internalsFile = new File(nodeInternDir, INTERNALS_FILE_NAME);
        if (!internalsFile.exists()) {
            // file to load internals from not available
            setWarningMessage("Internal model could not be loaded.");
            return;
        }

        BufferedInputStream in2 =
                new BufferedInputStream(new GZIPInputStream(
                        new FileInputStream(internalsFile)));

        ModelContentRO binModel = ModelContent.loadFromXML(in2);

        try {
            m_decTree = new DecisionTree(binModel);
        } catch (InvalidSettingsException ise) {
            LOGGER.warn("Model (internals) could not be loaded.", ise);
            setWarningMessage("Internal model could not be loaded.");
        }
    }

    /**
     * Save internals.
     *
     * @param nodeInternDir The intern node directory to save table to.
     * @param exec Used to report progress or cancel saving.
     * @throws IOException Always, since this method has not been implemented
     *             yet.
     * @see org.knime.core.node.NodeModel
     *      #saveInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {

        // write the tree as pred params
        ModelContent model = new ModelContent(INTERNALS_FILE_NAME);
        m_decTree.saveToPredictorParams(model, true);

        File internalsFile = new File(nodeInternDir, INTERNALS_FILE_NAME);
        BufferedOutputStream out2 =
                new BufferedOutputStream(new GZIPOutputStream(
                        new FileOutputStream(internalsFile)));

        model.saveToXML(out2);
        out2.close();
    }
}
