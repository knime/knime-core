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
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.mine.decisiontree2.learner;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.base.node.mine.decisiontree2.PMMLArrayType;
import org.knime.base.node.mine.decisiontree2.PMMLDecisionTreeTranslator;
import org.knime.base.node.mine.decisiontree2.PMMLMissingValueStrategy;
import org.knime.base.node.mine.decisiontree2.PMMLOperator;
import org.knime.base.node.mine.decisiontree2.PMMLPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLSetOperator;
import org.knime.base.node.mine.decisiontree2.PMMLSimplePredicate;
import org.knime.base.node.mine.decisiontree2.PMMLSimpleSetPredicate;
import org.knime.base.node.mine.decisiontree2.model.DecisionTree;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNode;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeLeaf;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeSplitPMML;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
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
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;

/**
 * Implements a decision tree induction algorithm based on C4.5 and SPRINT.
 *
 * @author Christoph Sieb, University of Konstanz
 *
 * @see DecisionTreeLearnerNodeFactory
 */
public class DecisionTreeLearnerNodeModel extends NodeModel {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(DecisionTreeLearnerNodeModel.class);

    private static final String SAVE_INTERNALS_FILE_NAME = "TreeInternals.zip";

    /**
     * Key to store the classification column in the settings.
     */
    public static final String KEY_CLASSIFYCOLUMN = "classifyColumn";

    /**
     * Key to store the confidence threshold for tree pruning in the settings.
     */
    public static final String KEY_PRUNING_CONFIDENCE_THRESHOLD =
         "significanceTh";

    /**
     * Key to store the confidence threshold for tree pruning in the settings.
     */
    public static final String KEY_PRUNING_METHOD = "pruningMethod";

    /**
     * Key to store the split quality measure in the settings.
     */
    public static final String KEY_SPLIT_QUALITY_MEASURE =
            "splitQualityMeasure";

    /**
     * Key to store the memory option (memory build or on disk).
     */
    public static final String KEY_MEMORY_OPTION = "inMemory";

    /**
     * Key to store the split average in the settings.
     */
    public static final String KEY_SPLIT_AVERAGE = "splitAverage";

    /**
     * Key to store the number of records stored for the view.
     */
    public static final String KEY_NUMBER_VIEW_RECORDS = "numverRecordsToView";

    /**
     * Key to store the minimum number of records per node.
     */
    public static final String KEY_MIN_NUMBER_RECORDS_PER_NODE =
            "minNumberRecordsPerNode";

    /**
     * Key to store whether to use the binary nominal split mode.
     */
    public static final String KEY_BINARY_NOMINAL_SPLIT_MODE =
            "binaryNominalSplit";

    /**
     * Key to store the number of processors to use.
     */
    public static final String KEY_NUM_PROCESSORS = "numProcessors";

    /**
     * Key to store the max number of nominal values for which to compute all
     * subsets.
     */
    public static final String KEY_MAX_NUM_NOMINAL_VALUES =
            "maxNumNominalValues";

    /** Index of input data port. */
    public static final int DATA_INPORT = 0;
    /** Index of optional model in port. */
    public static final int MODEL_INPORT = 1;

    /** Index of model out port. */
    public static final int MODEL_OUTPORT = 0;

    /**
     * The minimum number records expected per node.
     */
    public static final int DEFAULT_MIN_NUM_RECORDS_PER_NODE = 2;

    /**
     * The default whether to use the average as the split point is false.
     */
    public static final boolean DEFAULT_SPLIT_AVERAGE = false;

    /**
     * The constant for mdl pruning.
     */
    public static final String PRUNING_MDL = "MDL";

    /**
     * The constant for estimated error pruning.
     */
    public static final String PRUNING_ESTIMATED_ERROR = "Estimated error";

    /**
     * The constant for estimated error pruning.
     */
    public static final String PRUNING_NO = "No pruning";

    /**
     * The constant for the gini index split quality measure.
     */
    public static final String SPLIT_QUALITY_GINI = "Gini index";

    /**
     * The constant for the gain ratio split quality measure.
     */
    public static final String SPLIT_QUALITY_GAIN_RATIO = "Gain ratio";

    /**
     * The default pruning method.
     */
    public static final String DEFAULT_PRUNING_METHOD = PRUNING_NO;

    /**
     * The default split quality measure.
     */
    public static final String DEFAULT_SPLIT_QUALITY_MEASURE =
            SPLIT_QUALITY_GINI;

    /**
     * The default confidence threshold for pruning.
     */
    public static final double DEFAULT_PRUNING_CONFIDENCE_THRESHOLD = 0.25;

    /**
     * The default build option (memory or on disk).
     */
    public static final boolean DEFAULT_MEMORY_OPTION = true;

    /**
     * The default number of records stored for the view.
     */
    public static final int DEFAULT_NUMBER_RECORDS_FOR_VIEW = 10000;

    /**
     * The default binary split mode (off).
     */
    public static final boolean DEFAULT_BINARY_NOMINAL_SPLIT_MODE = false;

    /**
     * The default for the maximum number of nominal values for which all
     * subsets are calculated (results in the optimal binary split); this
     * parameter is only use if <code>binaryNominalSplits</code> is
     * <code>true</code>; if the number of nominal values is higher, a
     * heuristic is applied.
     */
    public static final int DEFAULT_MAX_BIN_NOMINAL_SPLIT_COMPUTATION = 10;

    /**
     * The default number of records stored for the view.
     */
    public static final int MAX_NUM_PROCESSORS =
            Runtime.getRuntime().availableProcessors();

    /**
     * The default number of records stored for the view.
     */
    public static final int DEFAULT_NUM_PROCESSORS = MAX_NUM_PROCESSORS;

    /**
     * The column which contains the classification Information.
     */
    private final SettingsModelString m_classifyColumn =
        DecisionTreeLearnerNodeDialog.createSettingsClassColumn();

//     /**
//      * The pruning confidence threshold.
//      */
//     private SettingsModelDoubleBounded m_pruningConfidenceThreshold =
//         DecisionTreeLearnerNodeDialog.createSettingsConfidenceValue();

    /**
     * The pruning method used for pruning.
     */
    private final SettingsModelString m_pruningMethod =
        DecisionTreeLearnerNodeDialog.createSettingsPruningMethod();

    /**
     * The quality measure to determine the split point.
     */
    private final SettingsModelString m_splitQualityMeasureType =
        DecisionTreeLearnerNodeDialog.createSettingsQualityMeasure();

    /**
     * The number of records stored for the view.
     */
    private final SettingsModelIntegerBounded m_numberRecordsStoredForView =
            DecisionTreeLearnerNodeDialog.createSettingsNumberRecordsForView();

    /**
     * The number of attributes of the input data table.
     */
    private int m_numberAttributes;

    /**
     * Counter for the generated decision tree nodes. This is an atomic integer
     * to guarantee thread safety.
     */
    private final AtomicInteger m_counter = new AtomicInteger(0);

    /**
     * Counts the number of rows already assigned to a leaf node. This value is
     * used to report progress for the building process
     */
    private AtomicDouble m_finishedCounter;

    private double m_alloverRowCount;

    private final SettingsModelIntegerBounded m_minNumberRecordsPerNode =
        DecisionTreeLearnerNodeDialog.createSettingsMinNumRecords();

    private final SettingsModelBoolean m_averageSplitpoint =
        DecisionTreeLearnerNodeDialog.createSettingsSplitPoint();

    private final SettingsModelBoolean m_binaryNominalSplitMode =
            DecisionTreeLearnerNodeDialog.createSettingsBinaryNominalSplit();

    /**
     * The maximum number of nominal values for which all subsets are calculated
     * (results in the optimal binary split); this parameter is only use if
     * <code>binaryNominalSplits</code> is <code>true</code>; if the number
     * of nominal values is higher, a heuristic is applied.
     */
    private final SettingsModelIntegerBounded m_maxNumNominalsForCompleteComputation =
            DecisionTreeLearnerNodeDialog.createSettingsMaxNominalValues();

    private final SettingsModelIntegerBounded m_parallelProcessing =
            DecisionTreeLearnerNodeDialog.createSettingsNumProcessors();

    /**
     * The decision tree model to be induced by the execute method.
     */
    private DecisionTree m_decisionTree;

    /**
     * Inits a new Decision Tree model with one data in- and one model output
     * port. In addition it has an optional model input.
     */
    public DecisionTreeLearnerNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE,
                new PortType(PMMLPortObject.class, true)},
                new PortType[]{PMMLPortObject.TYPE});
    }

    /**
     * Start of decision tree induction.
     *
     * @param exec the execution context for this run
     * @param data the input data to build the decision tree from
     * @return an empty data table array, as just a model is provided
     * @throws Exception any type of exception, e.g. for cancellation,
     *         invalid input,...
     * @see NodeModel#execute(BufferedDataTable[],ExecutionContext)
     */
    @Override
    protected PortObject[] execute(final PortObject[] data,
            final ExecutionContext exec) throws Exception {
        // holds the warning message displayed after execution
        StringBuilder warningMessageSb = new StringBuilder();
        ParallelProcessing parallelProcessing = new ParallelProcessing(
                m_parallelProcessing.getIntValue());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Number available threads: "
                    + parallelProcessing.getMaxNumberThreads()
                    + " used threads: "
                    + parallelProcessing.getCurrentThreadsInUse());
        }

        exec.setProgress("Preparing...");

        // check input data
        assert (data != null && data[DATA_INPORT] != null);

        BufferedDataTable inData = (BufferedDataTable)data[DATA_INPORT];
        // get column with color information
        String colorColumn = null;
        for (DataColumnSpec s : inData.getDataTableSpec()) {
            if (s.getColorHandler() != null) {
                colorColumn = s.getName();
                break;
            }
        }
        // the data table must have more than 2 records
        if (inData.getRowCount() <= 1) {
            throw new IllegalArgumentException(
                    "Input data table must have at least 2 records!");
        }

        // get class column index
        int classColumnIndex = inData.getDataTableSpec().findColumnIndex(
                m_classifyColumn.getStringValue());
        assert classColumnIndex > -1;

        // create initial In-Memory table
        exec.setProgress("Create initial In-Memory table...");
        InMemoryTableCreator tableCreator =
                new InMemoryTableCreator(inData, classColumnIndex,
                        m_minNumberRecordsPerNode.getIntValue());
        InMemoryTable initialTable =
                tableCreator.createInMemoryTable(exec
                        .createSubExecutionContext(0.05));
        int removedRows = tableCreator.getRemovedRowsDueToMissingClassValue();
        if (removedRows == inData.getRowCount()) {
            throw new IllegalArgumentException("Class column contains only "
                    + "missing values");
        }
        if (removedRows > 0) {
            warningMessageSb.append(removedRows);
            warningMessageSb
                    .append(" rows removed due to missing class value;");
        }
        // the all over row count is used to report progress
        m_alloverRowCount = initialTable.getSumOfWeights();

        // set the finishing counter
        // this counter will always be incremented when a leaf node is
        // created, as this determines the recursion end and can thus
        // be used for progress indication
        m_finishedCounter = new AtomicDouble(0);

        // get the number of attributes
        m_numberAttributes = initialTable.getNumAttributes();

        // create the quality measure
        final SplitQualityMeasure splitQualityMeasure;
        if (m_splitQualityMeasureType.getStringValue().equals(
                SPLIT_QUALITY_GINI)) {
            splitQualityMeasure = new SplitQualityGini();
        } else {
            splitQualityMeasure = new SplitQualityGainRatio();
        }

        // build the tree
        // before this set the node counter to 0
        m_counter.set(0);
        exec.setMessage("Building tree...");

        DecisionTreeNode root = null;
        root = buildTree(initialTable, exec, 0, splitQualityMeasure,
                parallelProcessing);

        // the decision tree model saved as PMML at the second out-port
        m_decisionTree = new DecisionTree(root,
                m_classifyColumn.getStringValue(),
                /* strategy has to be set explicitly as the default in PMML is
                    none, which means rows with missing values are not
                    classified. */
                PMMLMissingValueStrategy.LAST_PREDICTION);
        m_decisionTree.setColorColumn(colorColumn);


        // prune the tree
        exec.setMessage("Prune tree with "
                + m_pruningMethod.getStringValue() + "...");
        pruneTree();

        // add highlight patterns and color information
        exec.setMessage("Adding hilite and color info to tree...");
        addHiliteAndColorInfo(inData);
        LOGGER.info("Decision tree consisting of "
                + m_decisionTree.getNumberNodes()
                + " nodes created with pruning method "
                + m_pruningMethod.getStringValue());
        // set the warning message if available
        if (warningMessageSb.length() > 0) {
            setWarningMessage(warningMessageSb.toString());
        }

        // reset the number available threads
        parallelProcessing.reset();
        parallelProcessing = null;

        // no data out table is created -> return an empty table array
        exec.setMessage("Creating PMML decision tree model...");

        // handle the optional PMML input
        PMMLPortObject inPMMLPort = (PMMLPortObject)data[1];
        DataTableSpec inSpec = inData.getSpec();
        PMMLPortObjectSpec outPortSpec = createPMMLPortObjectSpec(
                inPMMLPort == null ? null : inPMMLPort.getSpec(),
                        inSpec);
        PMMLPortObject outPMMLPort = new PMMLPortObject(outPortSpec,
                inPMMLPort, inData.getSpec());
        outPMMLPort.addModelTranslater(new PMMLDecisionTreeTranslator(
                m_decisionTree));

        return new PortObject[]{outPMMLPort};
    }

    private PMMLPortObjectSpec createPMMLPortObjectSpec(
            final PMMLPortObjectSpec modelSpec, final DataTableSpec spec) {
        String targetCol = m_classifyColumn.getStringValue();
        List<String> learnCols = new LinkedList<String>();
        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataColumnSpec columnSpec = spec.getColumnSpec(i);
            String col = columnSpec.getName();
            if (!col.equals(targetCol)
                    && (columnSpec.getType().isCompatible(DoubleValue.class)
                    || columnSpec.getType().isCompatible(NominalValue.class))) {
                learnCols.add(spec.getColumnSpec(i).getName());
            }
        }
        String[] usedCols = learnCols.toArray(new String[learnCols.size() + 1]);
        usedCols[usedCols.length - 1] = targetCol;
        PMMLPortObjectSpecCreator pmmlSpecCreator =
                new PMMLPortObjectSpecCreator(modelSpec, spec);
        pmmlSpecCreator.setLearningColsNames(learnCols);
        pmmlSpecCreator.setTargetColName(targetCol);
        return pmmlSpecCreator.createSpec();
    }

    private void addHiliteAndColorInfo(final BufferedDataTable inData) {
        try {
            // add the maximum number covered patterns for highliting
            int maxRowsForHiliting = m_numberRecordsStoredForView.getIntValue();
            int count = 0;
            Iterator<DataRow> rowIterator = inData.iterator();
            // get class column index
            int classColumnIndex = inData.getDataTableSpec().findColumnIndex(
                    m_classifyColumn.getStringValue());
            while (rowIterator.hasNext() && maxRowsForHiliting > count) {
                DataRow row = rowIterator.next();
                if (row.getCell(classColumnIndex).isMissing()) {
                    continue;
                }
                m_decisionTree.addCoveredPattern(row,
                        inData.getDataTableSpec());
                count++;
            }
            // add the rest just for coloring
            while (rowIterator.hasNext()) {
                DataRow row = rowIterator.next();
                if (row.getCell(classColumnIndex).isMissing()) {
                    continue;
                }
                m_decisionTree.addCoveredColor(row, inData.getDataTableSpec());
            }
        } catch (Exception e) {
            LOGGER.error("Error during adding patterns for histogram coloring",
                    e);
        }
    }

    private void pruneTree() {
        // the tree is pruned according to the training error any way
        // i.e. if the error rate in the subtree is as high as the error
        // rate in a given node, the subtree is pruned (this means if there
        // is no improvement according to the training data)
        Pruner.trainingErrorPruning(m_decisionTree);

        // now prune according to the selected pruning method
        if (m_pruningMethod.getStringValue().equals(PRUNING_MDL)) {
            Pruner.mdlPruning(m_decisionTree);
            // TODO } else if (m_pruningMethod.equals(PRUNING_ESTIMATED_ERROR))
            // {
            // Pruner.estimatedErrorPruning(m_decisionTree,
            // m_pruningConfidenceThreshold);
        } else if (m_pruningMethod.getStringValue().equals(PRUNING_NO)) {
            // do nothing
        } else {
            throw new IllegalArgumentException("Pruning methdod "
                    + m_pruningMethod.getStringValue() + " not allowed.");
        }
    }

    /**
     * Recursively induces the decision tree.
     *
     * @param table the {@link InMemoryTable} representing the data for this
     *            node to determine the split and after that perform
     *            partitioning
     * @param exec the execution context for progress information
     * @param depth the current recursion depth
     */
    private DecisionTreeNode buildTree(final InMemoryTable table,
            final ExecutionContext exec, final int depth,
            final SplitQualityMeasure splitQualityMeasure,
            final ParallelProcessing parallelProcessing)
            throws CanceledExecutionException, IllegalAccessException {

        exec.checkCanceled();
        // derive this node's id from the counter
        int nodeId = m_counter.getAndIncrement();

        DataCell majorityClass = table.getMajorityClassAsCell();
        LinkedHashMap<DataCell, Double> frequencies =
            table.getClassFrequencies();
        // if the distribution allows for a leaf
        if (table.isPureEnough()) {
            // free memory
            table.freeUnderlyingDataRows();
            double value =
                    m_finishedCounter.incrementAndGet(table.getSumOfWeights());
            exec.setProgress(value / m_alloverRowCount, "Created node with id "
                            + nodeId + " at level " + depth);
            return new DecisionTreeNodeLeaf(nodeId, majorityClass, frequencies);
        } else {
            // find the best splits for all attributes
            SplitFinder splittFinder =
                    new SplitFinder(table, splitQualityMeasure,
                          m_averageSplitpoint.getBooleanValue(),
                          m_minNumberRecordsPerNode.getIntValue(),
                          m_binaryNominalSplitMode.getBooleanValue(),
                          m_maxNumNominalsForCompleteComputation.getIntValue());
            // check for enough memory
            checkMemory();

            // get the best split among the best attribute splits
            Split split = splittFinder.getSplit();

            // if no best split could be evaluated, create a leaf node
            if (split == null || !split.isValidSplit()) {
                table.freeUnderlyingDataRows();
                double value =
                        m_finishedCounter.incrementAndGet(table
                                .getSumOfWeights());
                exec.setProgress(value / m_alloverRowCount,
                       "Created node with id " + nodeId + " at level " + depth);
                return new DecisionTreeNodeLeaf(nodeId, majorityClass,
                        frequencies);
            }

            // partition the attribute lists according to this split
            Partitioner partitioner = new Partitioner(table, split,
                    m_minNumberRecordsPerNode.getIntValue());

            if (!partitioner.couldBeUsefulPartitioned()) {
                table.freeUnderlyingDataRows();
                double value =
                        m_finishedCounter.incrementAndGet(table
                                .getSumOfWeights());
                exec.setProgress(value / m_alloverRowCount,
                       "Created node with id " + nodeId + " at level " + depth);
                return new DecisionTreeNodeLeaf(nodeId, majorityClass,
                        frequencies);
            }

            // get the just created partitions
            InMemoryTable[] partitionTables = partitioner.getPartitionTables();

            // recursively build the  child nodes
            DecisionTreeNode[] children =
                    new DecisionTreeNode[partitionTables.length];

            ArrayList<ParallelBuilding> threads =
                    new ArrayList<ParallelBuilding>();

            int i = 0;
            for (InMemoryTable partitionTable : partitionTables) {
                exec.checkCanceled();
                if (partitionTable.getNumberDataRows() * m_numberAttributes
                        < 10000 || !parallelProcessing.isThreadAvailable()) {
                    children[i] = buildTree(partitionTable, exec, depth + 1,
                            splitQualityMeasure, parallelProcessing);
                } else {
                    String threadName =
                            "Build thread, node: " + nodeId + "." + i;
                    ParallelBuilding buildThread =
                            new ParallelBuilding(threadName, partitionTable,
                                    exec, depth + 1, i, splitQualityMeasure,
                                    parallelProcessing);
                    LOGGER.debug("Start new parallel building thread: "
                            + threadName);
                    threads.add(buildThread);
                    buildThread.start();
                }
                i++;
            }

            // retrieve all results from the thread array the getResultNode
            // method is a blocking method until the result is available
            // NOTE: the non parallel calculated children have been
            // already assigned to the child array
            for (ParallelBuilding buildThread : threads) {
                children[buildThread.getThreadIndex()] =
                        buildThread.getResultNode();
                exec.checkCanceled();
                if (buildThread.getException() != null) {
                    for (ParallelBuilding buildThread2 : threads) {
                        buildThread2.stop();
                    }
                    throw new RuntimeException(buildThread.getException()
                            .getMessage());
                }
            }
            threads.clear();

            if (split instanceof SplitContinuous) {
                double splitValue =
                        ((SplitContinuous)split).getBestSplitValue();
//                return new DecisionTreeNodeSplitContinuous(nodeId,
//                        majorityClass, frequencies, split
//                              .getSplitAttributeName(), children, splitValue);
                String splitAttribute = split.getSplitAttributeName();
                PMMLPredicate[] splitPredicates =
                        new PMMLPredicate[]{
                                new PMMLSimplePredicate(splitAttribute,
                                        PMMLOperator.LESS_OR_EQUAL, Double
                                                .toString(splitValue)),
                                new PMMLSimplePredicate(splitAttribute,
                                        PMMLOperator.GREATER_THAN, Double
                                                .toString(splitValue))};
                return new DecisionTreeNodeSplitPMML(nodeId, majorityClass,
                        frequencies, splitAttribute,
                        splitPredicates, children);
            } else if (split instanceof SplitNominalNormal) {
                // else the attribute is nominal
                DataCell[] splitValues =
                        ((SplitNominalNormal)split).getSplitValues();
//                return new DecisionTreeNodeSplitNominal(nodeId, majorityClass,
//                        frequencies, split.getSplitAttributeName(),
//                        splitValues, children);
                int num = children.length;
                PMMLPredicate[] splitPredicates = new PMMLPredicate[num];
                String splitAttribute = split.getSplitAttributeName();
                for (int j = 0; j < num; j++) {
                    splitPredicates[j] = new PMMLSimplePredicate(splitAttribute,
                            PMMLOperator.EQUAL, splitValues[j].toString());
                }
                return new DecisionTreeNodeSplitPMML(nodeId, majorityClass,
                        frequencies, splitAttribute,
                        splitPredicates, children);
            } else {
                // binary nominal
                SplitNominalBinary splitNominalBinary =
                        (SplitNominalBinary)split;
                DataCell[] splitValues = splitNominalBinary.getSplitValues();
//                return new DecisionTreeNodeSplitNominalBinary(nodeId,
//                        majorityClass, frequencies, split
//                                .getSplitAttributeName(), splitValues,
//                        splitNominalBinary.getIntMappingsLeftPartition(),
//                        splitNominalBinary.getIntMappingsRightPartition(),
//                        children/* children[0]=left, ..[1] right */);
                String splitAttribute = split.getSplitAttributeName();
                int[][] indices = new int[][] {
                        splitNominalBinary.getIntMappingsLeftPartition(),
                        splitNominalBinary.getIntMappingsRightPartition()
                };
                PMMLPredicate[] splitPredicates = new PMMLPredicate[2];
                for (int j = 0; j < splitPredicates.length; j++) {
                    PMMLSimpleSetPredicate pred = null;
                    pred = new PMMLSimpleSetPredicate(splitAttribute,
                            PMMLSetOperator.IS_IN);
                    pred.setArrayType(PMMLArrayType.STRING);
                    LinkedHashSet<String> values = new LinkedHashSet<String>();
                    for (int index : indices[j]) {
                       values.add(splitValues[index].toString());
                    }
                    pred.setValues(values);
                    splitPredicates[j] = pred;
                }
                return new DecisionTreeNodeSplitPMML(nodeId, majorityClass,
                        frequencies, splitAttribute,
                        splitPredicates, children);
            }
        }
    }

    /**
     * Resets all internal data.
     */
    @Override
    protected void reset() {
        // reset the tree
        m_decisionTree = null;
        m_counter.set(0);
    }

    /**
     * The number of the class column must be > 0 and < number of input columns.
     *
     * @param inSpecs the tabel specs on the input port to use for configuration
     * @see NodeModel#configure(DataTableSpec[])
     * @throws InvalidSettingsException thrown if the configuration is not
     *             correct
     * @return the table specs for the output ports
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {

        DataTableSpec inSpec = (DataTableSpec)inSpecs[DATA_INPORT];
        PMMLPortObjectSpec modelSpec
                = (PMMLPortObjectSpec)inSpecs[MODEL_INPORT];
        // check spec with selected column
        String classifyColumn = m_classifyColumn.getStringValue();
        DataColumnSpec columnSpec = inSpec.getColumnSpec(classifyColumn);
        boolean isValid = columnSpec != null
            && columnSpec.getType().isCompatible(NominalValue.class);
        if (classifyColumn != null && !isValid) {
            throw new InvalidSettingsException("Class column \""
                    + classifyColumn + "\" not found or incompatible");
        }
        if (classifyColumn == null) { // auto-guessing
            assert !isValid : "No class column set but valid configuration";
            // if no useful column is selected guess one
            // get the first useful one starting at the end of the table
            for (int i = inSpec.getNumColumns() - 1; i >= 0; i--) {
                if (inSpec.getColumnSpec(i).getType().isCompatible(
                        NominalValue.class)) {
                    m_classifyColumn.setStringValue(
                            inSpec.getColumnSpec(i).getName());
                    super.setWarningMessage("Guessing target column: \""
                            + m_classifyColumn.getStringValue() + "\".");
                    break;
                }
            }
            throw new InvalidSettingsException("Table contains no nominal"
                    + " attribute for classification.");
        }
        return new PortObjectSpec[]{createPMMLPortObjectSpec(modelSpec,
                inSpec)};
    }

    /**
     * Loads the class column and the classification value in the model.
     *
     * @param settings the settings object to which the settings are stored
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     * @throws InvalidSettingsException if there occur errors during saving the
     *             settings
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {

        m_classifyColumn.loadSettingsFrom(settings);
        // m_pruningConfidenceThreshold.loadSettingsFrom(settings);
        m_numberRecordsStoredForView.loadSettingsFrom(settings);
        m_minNumberRecordsPerNode.loadSettingsFrom(settings);
        m_pruningMethod.loadSettingsFrom(settings);
        m_splitQualityMeasureType.loadSettingsFrom(settings);
        m_averageSplitpoint.loadSettingsFrom(settings);
        m_parallelProcessing.loadSettingsFrom(settings);
        m_maxNumNominalsForCompleteComputation.loadSettingsFrom(settings);
        m_binaryNominalSplitMode.loadSettingsFrom(settings);
    }

    /**
     * Saves the class column and the classification value in the settings.
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_classifyColumn.saveSettingsTo(settings);
        // m_pruningConfidenceThreshold.saveSettings(settings);
        m_numberRecordsStoredForView.saveSettingsTo(settings);
        m_minNumberRecordsPerNode.saveSettingsTo(settings);
        m_pruningMethod.saveSettingsTo(settings);
        m_splitQualityMeasureType.saveSettingsTo(settings);
        m_averageSplitpoint.saveSettingsTo(settings);
        m_parallelProcessing.saveSettingsTo(settings);
        m_maxNumNominalsForCompleteComputation.saveSettingsTo(settings);
        m_binaryNominalSplitMode.saveSettingsTo(settings);
    }

    /**
     * This method validates the settings. That is:
     * <ul>
     * <li>The number of the class column must be an integer > 0</li>
     * <li>The positive value <code>DataCell</code> must not be null</li>
     * </ul>
     * {@inheritDoc}
     *
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        SettingsModelString classifyColumn =
            m_classifyColumn.createCloneWithValidatedValue(settings);
        if (classifyColumn.getStringValue() == null
                || classifyColumn.getStringValue().equals("")) {
            throw new InvalidSettingsException(
                    "Classification column not set.");
        }

        m_averageSplitpoint.validateSettings(settings);
        m_binaryNominalSplitMode.validateSettings(settings);
        m_pruningMethod.validateSettings(settings);
        m_numberRecordsStoredForView.validateSettings(settings);
        // m_pruningConfidenceThreshold.validateSettings(settings);
        m_minNumberRecordsPerNode.validateSettings(settings);
        m_splitQualityMeasureType.validateSettings(settings);
        m_maxNumNominalsForCompleteComputation.validateSettings(settings);
        m_parallelProcessing.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

        File internalsFile = new File(nodeInternDir, SAVE_INTERNALS_FILE_NAME);
        if (!internalsFile.exists()) {
            // file to load internals from not available
            return;
        }

        BufferedInputStream in =
                new BufferedInputStream(new GZIPInputStream(
                        new FileInputStream(internalsFile)));

        ModelContentRO decisionTreeModel = ModelContent.loadFromXML(in);

        try {
            m_decisionTree = new DecisionTree(decisionTreeModel);
        } catch (Exception e) {
            // continue, but inform the user via a message
            setWarningMessage("Internal model could not be loaded: "
                    + e.getMessage() + ". The view will not display properly.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

        ModelContent decisionTreeModel =
                new ModelContent(SAVE_INTERNALS_FILE_NAME);
        m_decisionTree.saveToPredictorParams(decisionTreeModel, true);

        File internalsFile = new File(nodeInternDir, SAVE_INTERNALS_FILE_NAME);
        BufferedOutputStream out =
                new BufferedOutputStream(new GZIPOutputStream(
                        new FileOutputStream(internalsFile)));

        decisionTreeModel.saveToXML(out);
    }

    /**
     * Returns the decision tree model.
     *
     * @return the decision tree model
     */
    public DecisionTree getDecisionTree() {
        return m_decisionTree;
    }

    private final class ParallelBuilding extends Thread {

        private final InMemoryTable m_table;

        private final ExecutionContext m_exec;

        private final int m_depth;

        private DecisionTreeNode m_resultNode;

        private Exception m_exception;

        private final int m_threadIndex;

        private final SplitQualityMeasure m_splitQM;

        private final ParallelProcessing m_parallelProcessing;

        private ParallelBuilding(final String name, final InMemoryTable table,
                final ExecutionContext exec, final int depth,
                final int threadIndex, final SplitQualityMeasure splitQM,
                final ParallelProcessing parallelProcessing) {
            super(name);
            m_table = table;
            m_exec = exec;
            m_depth = depth;
            m_resultNode = null;
            m_threadIndex = threadIndex;
            m_splitQM = splitQM;
            m_parallelProcessing = parallelProcessing;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            try {
                m_resultNode = buildTree(m_table, m_exec, m_depth,
                        (SplitQualityMeasure) m_splitQM.clone(),
                        m_parallelProcessing);
                LOGGER.debug("Thread: " + getName() + " finished");
            } catch (Exception e) {
                m_exception = e;
            }

            m_parallelProcessing.threadTaskFinished();
            synchronized (this) {
                notifyAll();
            }
        }

        /**
         * Returns a possible exception after execution.
         *
         * @return a possible exception after execution
         */
        public Exception getException() {
            return m_exception;
        }

        /**
         * Returns the result node created in this thread.
         *
         * @return the result node created in this threads
         */
        public DecisionTreeNode getResultNode() {

            // if the result is not available yet
            synchronized (this) {
                if (m_exception != null) {
                    throw new RuntimeException(m_exception.getMessage());
                }
                if (m_resultNode == null) {
                    try {
                        this.wait();
                    } catch (Exception e) {
                        // do nothing
                    }
                }
            }
            return m_resultNode;
        }

        /**
         * @return the threadIndex
         */
        public int getThreadIndex() {
            return m_threadIndex;
        }
    }

    /**
     * Checks the memory footprint. If too few memory a useful exception is
     * thrown.
     */
    static void checkMemory() {
        if (criticalMemoryFootprint()) {
            // check twice with garbage collector run
            // Runtime.getRuntime().gc();
            if (criticalMemoryFootprint()) {
                throw new RuntimeException("Not enough memory available. "
                        + "Increase memory via JVM option -Xmx or take "
                        + "a data sample.");
            }
        }
    }

    /**
     * Returns whether the memory footprint is critical.
     *
     * @return whether the memory footprint is critical
     */
    public static boolean criticalMemoryFootprint() {
        // get the maximum amount of memory that can be allocated
        // by the JVM (this corresponds to the -Xmx parameter)
        long maxMemory = Runtime.getRuntime().maxMemory();
        // get the amount of memory currently allocated by the JVM
        // (total mem <= max mem)
        long allocatedMemory = Runtime.getRuntime().totalMemory();
        // get the amount of free memory according to the allocated memory
        long freeMemory = Runtime.getRuntime().freeMemory();
        // calculate the used memory
        long usedMemory = allocatedMemory - freeMemory;
        // calculate the fraction of used memory according to the maximum
        // amount of memory (relative usage)
        float usedRelative = (float)usedMemory / (float)maxMemory;
        // calculate the maximum amount of free memory
        long maxFreeMemory = maxMemory - usedMemory;

        // if the relative usage is above a threshold the footprint is critical
        // but 10M are enough (absolute threshold)
        return usedRelative > 0.95 && maxFreeMemory < 10000000;
    }
}
