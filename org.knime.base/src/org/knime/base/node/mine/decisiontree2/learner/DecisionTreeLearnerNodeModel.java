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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.base.node.mine.decisiontree2.PMMLDecisionTreePortObject;
import org.knime.base.node.mine.decisiontree2.model.DecisionTree;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNode;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeLeaf;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeSplitContinuous;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeSplitNominal;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeSplitNominalBinary;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
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
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
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

    // TODO /**
    // * Key to store the confidence threshold for tree pruning in the settings.
    // */
    // public static final String KEY_PRUNING_CONFIDENCE_THRESHOLD =
    // "significanceTh";

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

    /** Index of model out port. */
    public static final int MODEL_OUTPORT = 0;

    /**
     * The minimum number records expected per node.
     */
    public static final double DEFAULT_MIN_NUM_RECORDS_PER_NODE = 2;

    /**
     * The default whether to use the average as the split point is false.
     */
    public static final boolean DEFAULT_SPLIT_AVERAGE = false;

    /**
     * The constant for mdl pruning.
     */
    public static final String PRUNING_MDL = "MDL";

    // TODO /**
    // * The constant for estimated error pruning.
    // */
    // public static final String PRUNING_ESTIMATED_ERROR = "Estimated error";

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
            SPLIT_QUALITY_GAIN_RATIO;

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
    private String m_classifyColumn;

    /**
     * The column index which contains the classification Information.
     */
    private int m_classColumnIndex;

    // TODO /**
    // * The pruning confidence threshold.
    // */
    // private double m_pruningConfidenceThreshold =
    // DEFAULT_PRUNING_CONFIDENCE_THRESHOLD;

    /**
     * The pruning method used for pruning.
     */
    private String m_pruningMethod = DEFAULT_PRUNING_METHOD;

    /**
     * The quality measure to determine the split point.
     */
    private String m_splitQualityMeasureType = DEFAULT_SPLIT_QUALITY_MEASURE;

    /**
     * The quality measure to determine the split point.
     */
    private SplitQualityMeasure m_splitQualityMeasure;

    /**
     * The number of records stored for the view.
     */
    private SettingsModelIntegerBounded m_numberRecordsStoredForView =
            new SettingsModelIntegerBounded(KEY_NUMBER_VIEW_RECORDS,
                    DEFAULT_NUMBER_RECORDS_FOR_VIEW, 0, Integer.MAX_VALUE);

    /**
     * The number of attributes of the input data table.
     */
    private int m_numberAttributes;

    /**
     * Counter for the generated decision tree nodes. This is an atomic integer
     * to guarantee thread safety.
     */
    private AtomicInteger m_counter = new AtomicInteger(0);

    /**
     * Counts the number of rows already assigned to a leaf node. This value is
     * used to report progress for the building process
     */
    private AtomicDouble m_finishedCounter;

    private double m_alloverRowCount;

    private double m_minNumberRecordsPerNode = DEFAULT_MIN_NUM_RECORDS_PER_NODE;

    private boolean m_buildInMemory = true;

    private boolean m_averageSplitpoint = DEFAULT_SPLIT_AVERAGE;

    private boolean m_binaryNominalSplitMode =
            DEFAULT_BINARY_NOMINAL_SPLIT_MODE;

    /**
     * The maximum number of nominal values for which all subsets are calculated
     * (results in the optimal binary split); this parameter is only use if
     * <code>binaryNominalSplits</code> is <code>true</code>; if the number
     * of nominal values is higher, a heuristic is applied.
     */
    private int m_maxNumNominalsForCompleteComputation =
            DEFAULT_MAX_BIN_NOMINAL_SPLIT_COMPUTATION;

    private ParallelProcessing m_parallelProcessing =
            new ParallelProcessing(DEFAULT_NUM_PROCESSORS);

    /**
     * The decision tree model to be induced by the execute method.
     */
    private DecisionTree m_decisionTree;

    /**
     * Inits a new Decision Tree model with one data in- and one model output
     * port.
     */
    // public DecisionTreeLearnerNodeModel() {
    // super(1, 0, 0, 1);
    // reset();
    // }
    public DecisionTreeLearnerNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE},
                new PortType[]{PMMLDecisionTreePortObject.TYPE});
    }

    // /**
    // * Saves decision tree to model out port.
    // *
    // * @param index The outport index.
    // * @param predParams holding the model afterwards.
    // * @throws InvalidSettingsException if settings are wrong
    // */
    // @Override
    // protected void saveModelContent(final int index,
    // final ModelContentWO predParams) throws InvalidSettingsException {
    //
    // assert index == MODEL_OUTPORT : index;
    //
    // m_decisionTree.saveToPredictorParams(predParams, false);
    // }

    /**
     * Start of decision tree induction.
     *
     * @param exec the execution context for this run
     * @param data the input data to build the decision tree from
     * @return an empty data table array, as just a model is provided
     * @see NodeModel#execute(BufferedDataTable[],ExecutionContext)
     * @throws CanceledExecutionException if canceled.
     * @throws IllegalArgumentException if the table has less than 2 records
     * @throws IllegalAccessException if an illegal partitioning is accessed
     */
    @Override
    protected PortObject[] execute(final PortObject[] data,
            final ExecutionContext exec) throws CanceledExecutionException,
            IllegalArgumentException, IllegalAccessException {

        // stringbuilder that holds the warning message displayed after
        // execution
        StringBuilder warningMessageSb = new StringBuilder();
        // reset the number available threads
        m_parallelProcessing.reset();

        long completeTime = System.currentTimeMillis();
        LOGGER.info("Number available threads: "
                + m_parallelProcessing.getMaxNumberThreads()
                + " used threads: "
                + m_parallelProcessing.getCurrentThreadsInUse());

        // TODO
        // if (LOGGER.isDebugEnabled()) {
        // LOGGER.debug("Pruning confidence TH: "
        // + m_pruningConfidenceThreshold);
        // }

        exec.setProgress("Preparing...");

        // check input data
        assert (data != null && data.length == 1 && data[DATA_INPORT] != null);

        BufferedDataTable inData = (BufferedDataTable)data[DATA_INPORT];

        // the data table must have more than 2 records
        if (inData.getRowCount() <= 1) {
            throw new IllegalArgumentException(
                    "Input data table must have at least 2 records!");
        }

        // get class column index
        m_classColumnIndex =
                inData.getDataTableSpec().findColumnIndex(m_classifyColumn);
        assert m_classColumnIndex > -1;

        // create initial In-Memory table
        exec.setProgress("Create initial In-Memory table...");
        LOGGER.info("Create initial In-Memory table...");
        long timer = System.currentTimeMillis();
        InMemoryTableCreator tableCreator =
                new InMemoryTableCreator(inData, m_classColumnIndex,
                        m_minNumberRecordsPerNode);
        InMemoryTable initialTable =
                tableCreator.createInMemoryTable(exec
                        .createSubExecutionContext(0.05));
        LOGGER.info("Initial table created in (ms) "
                + (System.currentTimeMillis() - timer));
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
        // the allover row count is used to report progress
        m_alloverRowCount = initialTable.getSumOfWeights();

        // set the finishing counter
        // this counter will always be incremented when a leaf node is
        // created, as this determines the recursion end and can thus
        // be used for progress indication
        m_finishedCounter = new AtomicDouble(0);

        // get the number of attributes
        m_numberAttributes = initialTable.getNumAttributes();

        // create the quality measure
        if (m_splitQualityMeasureType.equals(SPLIT_QUALITY_GINI)) {
            m_splitQualityMeasure = new SplitQualityGini();
        } else {
            m_splitQualityMeasure = new SplitQualityGainRatio();
        }

        // build the tree
        // before this set the node counter to 0
        m_counter.set(0);
        LOGGER.info("Building tree...");
        exec.setProgress("Building tree...");
        timer = System.currentTimeMillis();

        DecisionTreeNode root = null;
        root = buildTree(initialTable, exec, 0, m_splitQualityMeasure);

        LOGGER.info("Tree built in (ms) "
                + (System.currentTimeMillis() - timer));

        // the decision tree will be saved after execution in the method
        // "saveModelContent()"
        m_decisionTree = new DecisionTree(root, m_classifyColumn);

        // prune the tree
        timer = System.currentTimeMillis();
        exec.setProgress("Prune tree with " + m_pruningMethod + "...");
        LOGGER.info("Pruning tree with " + m_pruningMethod + "...");
        pruneTree();
        LOGGER.info("Tree pruned in (ms) "
                + (System.currentTimeMillis() - timer));

        // add highlight patterns and color information
        long patternTime = System.currentTimeMillis();
        addHiliteAndColorInfo(inData);
        LOGGER.info("Time for pattern adding: "
                + (System.currentTimeMillis() - patternTime));

        LOGGER.info("Number nodes: " + m_decisionTree.getNumberNodes());

        LOGGER.info("All over time: "
                + (System.currentTimeMillis() - completeTime));

        // set the warning message if available
        if (warningMessageSb.length() > 0) {
            setWarningMessage(warningMessageSb.toString());
        }

        // no data out table is created -> return an empty table array
        return new PortObject[]{getPMMLOutPortObject(inData.getDataTableSpec())};
    }

    /**
     * @return
     */
    private PortObject getPMMLOutPortObject(final DataTableSpec spec) {
        Set<String> learnCols = new LinkedHashSet<String>();
        for(int i = 0; i < spec.getNumColumns(); i++) {
            learnCols.add(spec.getColumnSpec(i).getName());
        }
        Set<String> targetSet = new LinkedHashSet<String>();
        targetSet.add(m_classifyColumn);
        PMMLPortObjectSpec outSpec = new PMMLPortObjectSpec(
                spec, learnCols, Collections.EMPTY_SET, targetSet);
        return new PMMLDecisionTreePortObject(m_decisionTree, outSpec);
    }

    private void addHiliteAndColorInfo(final BufferedDataTable inData) {

        try {
            // add the maximum number covered patterns for highliting
            int maxRowsForHiliting = m_numberRecordsStoredForView.getIntValue();
            int count = 0;
            Iterator<DataRow> rowIterator = inData.iterator();
            while (rowIterator.hasNext() && maxRowsForHiliting > count) {
                DataRow row = rowIterator.next();
                m_decisionTree
                        .addCoveredPattern(row, inData.getDataTableSpec());
                count++;
            }
            // add the rest just for coloring
            while (rowIterator.hasNext()) {
                DataRow row = rowIterator.next();
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
        if (m_pruningMethod.equals(PRUNING_MDL)) {
            Pruner.mdlPruning(m_decisionTree);
            // TODO } else if (m_pruningMethod.equals(PRUNING_ESTIMATED_ERROR))
            // {
            // Pruner.estimatedErrorPruning(m_decisionTree,
            // m_pruningConfidenceThreshold);
        } else if (m_pruningMethod.equals(PRUNING_NO)) {
            // do nothing
        } else {
            throw new IllegalArgumentException("Pruning methdod "
                    + m_pruningMethod + " not allowed.");
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
            final SplitQualityMeasure splitQualityMeasure)
            throws CanceledExecutionException, IllegalAccessException {

        exec.checkCanceled();
        // derive this node's id from the counter
        int nodeId = m_counter.getAndIncrement();
        LOGGER.info("At depth " + depth);

        DataCell majorityClass = table.getMajorityClassAsCell();
        HashMap<DataCell, Double> frequencies = table.getClassFrequencies();
        // if the distribution allows for a leaf
        if (table.isPureEnough()) {
            // free memory
            table.freeUnderlyingDataRows();
            double value =
                    m_finishedCounter.incrementAndGet(table.getSumOfWeights());
            exec
                    .setProgress(value / m_alloverRowCount, "Created node"
                            + nodeId);
            return new DecisionTreeNodeLeaf(nodeId, majorityClass, frequencies);
        } else {
            // find the best splits for all attributes
            long time = System.currentTimeMillis();
            LOGGER.info("Find best split...");
            SplitFinder splittFinder =
                    new SplitFinder(table, splitQualityMeasure,
                            m_averageSplitpoint, m_minNumberRecordsPerNode,
                            m_binaryNominalSplitMode,
                            m_maxNumNominalsForCompleteComputation);
            // check for enough memory
            checkMemory();

            // get the best split among the best attribute splits
            Split split = splittFinder.getSplit();
            LOGGER.info("Best split found.");
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Best split found in "
                        + (System.currentTimeMillis() - time) + " ms.");

                LOGGER.info("Partition data...");
                time = System.currentTimeMillis();
            }

            // if no best split could be evaluated, create a leaf node
            if (split == null || !split.isValidSplit()) {
                table.freeUnderlyingDataRows();
                double value =
                        m_finishedCounter.incrementAndGet(table
                                .getSumOfWeights());
                exec.setProgress(value / m_alloverRowCount, "Created node"
                        + nodeId);
                return new DecisionTreeNodeLeaf(nodeId, majorityClass,
                        frequencies);
            }

            // partition the attribute lists according to this split
            LOGGER.info("Partition data... ");
            Partitioner partitioner =
                    new Partitioner(table, split, m_minNumberRecordsPerNode);
            LOGGER.info("Data partitioned.");
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Data partitioned in "
                        + (System.currentTimeMillis() - time) + " ms.");
            }

            if (!partitioner.couldBeUsefulPartitioned()) {
                table.freeUnderlyingDataRows();
                double value =
                        m_finishedCounter.incrementAndGet(table
                                .getSumOfWeights());
                exec.setProgress(value / m_alloverRowCount, "Created node"
                        + nodeId);
                return new DecisionTreeNodeLeaf(nodeId, majorityClass,
                        frequencies);
            }

            // get the just created partitions
            InMemoryTable[] partitionTables = partitioner.getPartitionTables();

            // recursively build the child nodes
            DecisionTreeNode[] children =
                    new DecisionTreeNode[partitionTables.length];

            ArrayList<ParallelBuilding> threads =
                    new ArrayList<ParallelBuilding>();

            int i = 0;
            for (InMemoryTable partitionTable : partitionTables) {
                exec.checkCanceled();
                if (partitionTable.getNumberDataRows() * m_numberAttributes < 10000
                        || !m_parallelProcessing.isThreadAvailable()) {
                    children[i] = buildTree(partitionTable, exec, depth + 1,
                            splitQualityMeasure);
                } else {
                    String threadName =
                            "Build thread, node: " + nodeId + "." + i;
                    ParallelBuilding buildThread =
                            new ParallelBuilding(threadName, partitionTable,
                                    exec, depth + 1, i);
                    LOGGER.info("Start new parallel building thread: "
                            + threadName);
                    threads.add(buildThread);
                    buildThread.start();
                }
                i++;
            }

            // retrieve all results from the thread array
            // the getResultNode method is a blocking method until
            // the result is available
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
                return new DecisionTreeNodeSplitContinuous(nodeId,
                        majorityClass, frequencies, split
                                .getSplitAttributeName(), children, splitValue);
            } else if (split instanceof SplitNominalNormal) {
                // else the attribute is nominal
                DataCell[] splitValues =
                        ((SplitNominalNormal)split).getSplitValues();
                return new DecisionTreeNodeSplitNominal(nodeId, majorityClass,
                        frequencies, split.getSplitAttributeName(),
                        splitValues, children);
            } else {
                // binary nominal
                SplitNominalBinary splitNominalBinary =
                        (SplitNominalBinary)split;
                DataCell[] splitValues = splitNominalBinary.getSplitValues();
                return new DecisionTreeNodeSplitNominalBinary(nodeId,
                        majorityClass, frequencies, split
                                .getSplitAttributeName(), splitValues,
                        splitNominalBinary.getIntMappingsLeftPartition(),
                        splitNominalBinary.getIntMappingsRightPartition(),
                        children);
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
        // check spec with selected column
        DataColumnSpec columnSpec = inSpec.getColumnSpec(m_classifyColumn);
        if (columnSpec == null
                || !columnSpec.getType().isCompatible(NominalValue.class)) {
            // if no useful column is selected guess one
            // get the first useful one starting at the end of the table
            for (int i = inSpec.getNumColumns() - 1; i >= 0; i--) {
                if (inSpec.getColumnSpec(i).getType().isCompatible(
                        NominalValue.class)) {
                    m_classifyColumn = inSpec.getColumnSpec(i).getName();
                    super.setWarningMessage("Guessing target column: \""
                            + m_classifyColumn + "\".");
                    break;
                }
                throw new InvalidSettingsException("Table contains no nominal"
                        + " attribute for classification.");
            }
        }

        return new PortObjectSpec[]{createPMMLSpec(inSpec)};
    }

    private PMMLPortObjectSpec createPMMLSpec(final DataTableSpec spec) 
        throws InvalidSettingsException {
        PMMLPortObjectSpecCreator creator = new PMMLPortObjectSpecCreator(spec);
        Set<String> targetCols = new HashSet<String>();
        targetCols.add(m_classifyColumn);
        creator.setTargetColsNames(targetCols);
        return creator.createSpec();
    }

    /**
     * Loads the class column and the classification value in the model.
     *
     * @param settings the settings object to which the settings are stored
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     * @throws InvalidSettingsException if there occur erros during saving the
     *             settings
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {

        m_classifyColumn = settings.getString(KEY_CLASSIFYCOLUMN);
        // TODO
        // m_pruningConfidenceThreshold =
        // (float)settings.getDouble(KEY_PRUNING_CONFIDENCE_THRESHOLD,
        // DEFAULT_PRUNING_CONFIDENCE_THRESHOLD);
        m_numberRecordsStoredForView.loadSettingsFrom(settings);
        m_buildInMemory =
                settings.getBoolean(KEY_MEMORY_OPTION, DEFAULT_MEMORY_OPTION);
        m_minNumberRecordsPerNode =
                settings.getInt(KEY_MIN_NUMBER_RECORDS_PER_NODE,
                        (int)DEFAULT_MIN_NUM_RECORDS_PER_NODE);
        m_pruningMethod =
                settings.getString(KEY_PRUNING_METHOD, DEFAULT_PRUNING_METHOD);
        m_splitQualityMeasureType =
                settings.getString(KEY_SPLIT_QUALITY_MEASURE,
                        DEFAULT_SPLIT_QUALITY_MEASURE);
        m_averageSplitpoint =
                settings.getBoolean(KEY_SPLIT_AVERAGE, DEFAULT_SPLIT_AVERAGE);
        m_parallelProcessing.setNumberThreads(settings.getInt(
                KEY_NUM_PROCESSORS, DEFAULT_NUM_PROCESSORS));
        m_maxNumNominalsForCompleteComputation =
                settings.getInt(KEY_MAX_NUM_NOMINAL_VALUES,
                        DEFAULT_MAX_BIN_NOMINAL_SPLIT_COMPUTATION);
        m_binaryNominalSplitMode =
                settings.getBoolean(KEY_BINARY_NOMINAL_SPLIT_MODE,
                        DEFAULT_BINARY_NOMINAL_SPLIT_MODE);
    }

    /**
     * Saves the class column and the classification value in the settings.
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        assert (settings != null);
        settings.addString(KEY_CLASSIFYCOLUMN, m_classifyColumn);
        // TODO settings.addDouble(KEY_PRUNING_CONFIDENCE_THRESHOLD,
        // m_pruningConfidenceThreshold);
        m_numberRecordsStoredForView.saveSettingsTo(settings);
        settings.addBoolean(KEY_MEMORY_OPTION, m_buildInMemory);
        settings.addInt(KEY_MIN_NUMBER_RECORDS_PER_NODE,
                (int)m_minNumberRecordsPerNode);
        settings.addString(KEY_PRUNING_METHOD, m_pruningMethod);
        settings
                .addString(KEY_SPLIT_QUALITY_MEASURE, m_splitQualityMeasureType);
        settings.addBoolean(KEY_SPLIT_AVERAGE, m_averageSplitpoint);
        settings.addInt(KEY_NUM_PROCESSORS, m_parallelProcessing
                .getMaxNumberThreads());
        settings.addInt(KEY_MAX_NUM_NOMINAL_VALUES,
                m_maxNumNominalsForCompleteComputation);
        settings.addBoolean(KEY_BINARY_NOMINAL_SPLIT_MODE,
                m_binaryNominalSplitMode);
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
        String classifyColumn = settings.getString(KEY_CLASSIFYCOLUMN);
        if (classifyColumn == null || classifyColumn.equals("")) {
            throw new InvalidSettingsException("Classification column not set.");
        }

        // TODO double significance =
        // settings.getDouble(KEY_PRUNING_CONFIDENCE_THRESHOLD);
        //
        // if (significance < 0 || significance > 0.5) {
        // throw new InvalidSettingsException(
        // "Significance threshold must be in the range of 0.0 - 0.5");
        // }
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

        private InMemoryTable m_table;

        private ExecutionContext m_exec;

        private int m_depth;

        private DecisionTreeNode m_resultNode;

        private Exception m_exception;

        private int m_threadIndex;

        private ParallelBuilding(final String name, final InMemoryTable table,
                final ExecutionContext exec, final int depth,
                final int threadIndex) {
            super(name);
            m_table = table;
            m_exec = exec;
            m_depth = depth;
            m_resultNode = null;
            m_threadIndex = threadIndex;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            try {
                m_resultNode = buildTree(m_table, m_exec, m_depth,
                        (SplitQualityMeasure)m_splitQualityMeasure.clone());
                LOGGER.info("Thread: " + getName() + " finished");
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
