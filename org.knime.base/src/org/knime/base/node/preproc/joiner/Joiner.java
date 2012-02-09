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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   23.11.2009 (Heiko Hofer): created
 */
package org.knime.base.node.preproc.joiner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.knime.base.data.sort.SortedTable;
import org.knime.base.node.preproc.joiner.Joiner2Settings.CompositionMode;
import org.knime.base.node.preproc.joiner.Joiner2Settings.DuplicateHandling;
import org.knime.base.node.preproc.joiner.Joiner2Settings.JoinMode;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.sort.MemoryService;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

/**
 * The joiner implements a database like join of two tables.
 *
 * @author Heiko Hofer
 */
public final class Joiner {
    /** Logger to print debug info to. */
    private static final NodeLogger LOGGER = NodeLogger
    .getLogger(Joiner.class);

    private final DataTableSpec m_leftDataTableSpec;
    private final DataTableSpec m_rightDataTableSpec;

    private final Joiner2Settings m_settings;

    private boolean m_multipleMatchCanOccur;
    private Set<Integer> m_multiLeftRetainAll;

    private InputDataRow.Settings m_inputDataRowSettings;
    private OutputDataRow.Settings m_outputDataRowSettings;

    private boolean m_retainRight;
    private boolean m_retainLeft;

    private HashMap<RowKey, Set<RowKey>> m_leftRowKeyMap;
    private HashMap<RowKey, Set<RowKey>> m_rightRowKeyMap;

    private List<String> m_leftSurvivors;
    private List<String> m_rightSurvivors;

    private final List<String> m_configWarnings;
    private final List<String> m_runtimeWarnings;

    private int m_numBits;
    private int m_bitMask;

    /** The memory service used to detect the low memory condition. */
    private MemoryService m_memService;

    /** The initial number of partitions the rows are read in. If not all
     * partitions fit in main memory, they are joined subsequently using as
     * much memory as possible.
     */
    private int m_numBitsInitial = 6;
    /** The maximal number of partitions (changed in testing routines). */
    private int m_numBitsMaximal = Integer.SIZE;


    /**
     * Creates a new instance.
     *
     * @param leftTableSpec The DataTableSpec of the left input table.
     * @param rightTableSpec The DataTableSpec of the right input table.
     * @param settings The settings object. This object might be used to change
     *            the settings of the Joiner.
     */
    public Joiner(final DataTableSpec leftTableSpec,
            final DataTableSpec rightTableSpec,
            final Joiner2Settings settings) {
        m_leftDataTableSpec = leftTableSpec;
        m_rightDataTableSpec = rightTableSpec;
        m_settings = settings;

        m_leftRowKeyMap = new HashMap<RowKey, Set<RowKey>>();
        m_rightRowKeyMap = new HashMap<RowKey, Set<RowKey>>();

        m_configWarnings = new ArrayList<String>();
        m_runtimeWarnings = new ArrayList<String>();

    }

    /**
     * @return the rowKeyMap
     */
    HashMap<RowKey, Set<RowKey>> getLeftRowKeyMap() {
        return m_leftRowKeyMap;
    }

    /**
     * @return the rowKeyMap
     */
    HashMap<RowKey, Set<RowKey>> getRightRowKeyMap() {
        return m_rightRowKeyMap;
    }

    /**
     * Creates a spec for the output table by taking care of duplicate columns.
     *
     * @param specs the specs of the two input tables
     * @return the spec of the output table
     * @throws InvalidSettingsException when settings are not supported
     */
    private DataTableSpec createSpec(final DataTableSpec[] specs)
    throws InvalidSettingsException {
        validateSettings(m_settings);

        m_configWarnings.clear();
        List<String> leftCols = getLeftIncluded(specs[0]);
        List<String> rightCols = getRightIncluded(specs[1]);

        List<String> duplicates = new ArrayList<String>();
        duplicates.addAll(leftCols);
        duplicates.retainAll(rightCols);

        if (m_settings.getDuplicateHandling().equals(
                DuplicateHandling.DontExecute)
                && !duplicates.isEmpty()) {
            throw new InvalidSettingsException(
                    "Found duplicate columns, won't execute. Fix it in "
                    + "\"Column Selection\" tab");
        }

        if (m_settings.getDuplicateHandling().equals(
                DuplicateHandling.Filter)) {

            for (String duplicate : duplicates) {
                DataType leftType = specs[0].getColumnSpec(duplicate).getType();
                DataType rightType =
                    specs[1].getColumnSpec(duplicate).getType();
                if (!leftType.equals(rightType)) {
                    m_configWarnings.add("The column \"" + duplicate
                            + "\" can be found in "
                            + "both input tables but with different data type. "
                            + "Only the one in the left input table will show "
                            + "up in the output table. Please change the "
                            + "Duplicate Column Handling if both columns "
                            + "should show up in the ouput table.");
                }
            }

            rightCols.removeAll(leftCols);
        }

        if ((!duplicates.isEmpty())
                && m_settings.getDuplicateColumnSuffix().equals("")) {
            throw new InvalidSettingsException("No suffix for duplicate "
                    + "columns provided.");
        }

        // check if data types of joining columns do match
        for (int i = 0; i < m_settings.getLeftJoinColumns().length; i++) {
            String leftJoinAttr = m_settings.getLeftJoinColumns()[i];
            boolean leftJoinAttrIsRowKey =
                Joiner2Settings.ROW_KEY_IDENTIFIER.equals(leftJoinAttr);
            DataType leftType = leftJoinAttrIsRowKey
            ? StringCell.TYPE
                    : specs[0].getColumnSpec(leftJoinAttr).getType();
            String rightJoinAttr = m_settings.getRightJoinColumns()[i];
            boolean rightJoinAttrIsRowKey =
                Joiner2Settings.ROW_KEY_IDENTIFIER.equals(rightJoinAttr);
            DataType rightType = rightJoinAttrIsRowKey
            ? StringCell.TYPE
                    : specs[1].getColumnSpec(rightJoinAttr).getType();
            if (!leftType.equals(rightType)) {
                String left = leftJoinAttrIsRowKey ? "Row ID" : leftJoinAttr;
                String right = rightJoinAttrIsRowKey ? "Row ID" : rightJoinAttr;
                // check different cases here to give meaningful error messages
                if (leftType.equals(DoubleCell.TYPE)
                        && rightType.equals(IntCell.TYPE)) {
                    throw new InvalidSettingsException("Type mismatch found of "
                            + "Joining Column Pair \""
                            + left + "\" and \"" + right + "\"."
                            + " Use \"Double to Int node\" to "
                            + "convert the type of \""
                            + left + "\" to integer.");
                } else if (leftType.equals(IntCell.TYPE)
                        && rightType.equals(DoubleCell.TYPE)) {
                    throw new InvalidSettingsException("Type mismatch found of "
                            + "Joining Column Pair \""
                            + left + "\" and \"" + right + "\"."
                            + " se \"Double to Int node\" to "
                            + "convert the type of \""
                            + right + "\" to integer.");
                } else if (leftType.isCompatible(DoubleValue.class)
                        && rightType.equals(StringCell.TYPE)) {
                    throw new InvalidSettingsException("Type mismatch found of "
                            + "Joining Column Pair \""
                            + left + "\" and \"" + right + "\"."
                            + " Use \"Number to String node\" to "
                            + "convert the type of \""
                            + left + "\" to string.");
                } else if (leftType.equals(StringCell.TYPE)
                        && rightType.isCompatible(DoubleValue.class)) {
                    throw new InvalidSettingsException("Type mismatch found of "
                            + "Joining Column Pair \""
                            + left + "\" and \"" + right + "\"."
                            + " Use \"Number to String node\" to "
                            + "convert the type of \""
                            + right + "\" to string.");
                } else {
                    throw new InvalidSettingsException("Type mismatch found of "
                            + "Joining Column Pair \""
                            + left + "\" and \"" + right + "\"."
                            + "This causes an empty output table.");
                }
            }
        }

        m_leftSurvivors = new ArrayList<String>();
        List<DataColumnSpec> takeSpecs = new ArrayList<DataColumnSpec>();
        for (int i = 0; i < specs[0].getNumColumns(); i++) {
            DataColumnSpec columnSpec = specs[0].getColumnSpec(i);
            if (leftCols.contains(columnSpec.getName())) {
                takeSpecs.add(columnSpec);
                m_leftSurvivors.add(columnSpec.getName());
            }
        }

        m_rightSurvivors = new ArrayList<String>();
        for (int i = 0; i < specs[1].getNumColumns(); i++) {
            DataColumnSpec columnSpec = specs[1].getColumnSpec(i);
            if (rightCols.contains(columnSpec.getName())) {
                if (duplicates.contains(columnSpec.getName())) {
                    String newName = columnSpec.getName();
                    do {
                        newName += m_settings.getDuplicateColumnSuffix();
                    } while (rightCols.contains(newName));

                    DataColumnSpecCreator dcsc =
                        new DataColumnSpecCreator(columnSpec);
                    dcsc.removeAllHandlers();
                    dcsc.setName(newName);
                    takeSpecs.add(dcsc.createSpec());
                    rightCols.add(newName);
                } else {
                    takeSpecs.add(columnSpec);
                }
                m_rightSurvivors.add(columnSpec.getName());
            }
        }

        return new DataTableSpec(takeSpecs.toArray(
                new DataColumnSpec[takeSpecs.size()]));
    }

    /**
     * @param dataTableSpec
     * @return
     */
    private List<String> getLeftIncluded(final DataTableSpec dataTableSpec)
    throws InvalidSettingsException {
        List<String> leftCols = new ArrayList<String>();
        for (DataColumnSpec column : dataTableSpec) {
            leftCols.add(column.getName());
        }
        // Check if left joining columns are in table spec
        Set<String> leftJoinCols = new HashSet<String>();
        leftJoinCols.addAll(Arrays.asList(m_settings.getLeftJoinColumns()));
        leftJoinCols.remove(Joiner2Settings.ROW_KEY_IDENTIFIER);
        if (!leftCols.containsAll(leftJoinCols)) {
            throw new InvalidSettingsException("The left input table has "
                    + "changed. Some joining columns do "
                    + "not longer exist");
        }
        // Check if left included columns are in table spec
        if (!m_settings.getLeftIncludeAll()) {
            List<String> leftIncludes =
                Arrays.asList(m_settings.getLeftIncludeCols());
            if (!leftCols.containsAll(leftIncludes)) {
                throw new InvalidSettingsException("The left input table has "
                        + "changed. Some columns in the include list do "
                        + "not longer exist");
            }
            leftCols.retainAll(leftIncludes);
        }
        if (m_settings.getRemoveLeftJoinCols()) {
            leftCols.removeAll(Arrays.asList(m_settings.getLeftJoinColumns()));
        }
        return leftCols;
    }

    /**
     * @param dataTableSpec
     * @return
     */
    private List<String> getRightIncluded(final DataTableSpec dataTableSpec)
    throws InvalidSettingsException {
        List<String> rightCols = new ArrayList<String>();
        for (DataColumnSpec column : dataTableSpec) {
            rightCols.add(column.getName());
        }
        // Check if right joining columns are in table spec
        Set<String> rightJoinCols = new HashSet<String>();
        rightJoinCols.addAll(Arrays.asList(m_settings.getRightJoinColumns()));
        rightJoinCols.remove(Joiner2Settings.ROW_KEY_IDENTIFIER);
        if (!rightCols.containsAll(rightJoinCols)) {
            throw new InvalidSettingsException("The right input table has "
                    + "changed. Some joining columns do "
                    + "not longer exist");
        }
        // Check if right included columns are in table spec
        if (!m_settings.getRightIncludeAll()) {
            List<String> rightIncludes =
                Arrays.asList(m_settings.getRightIncludeCols());
            if (!rightCols.containsAll(rightIncludes)) {
                throw new InvalidSettingsException("The right input table has "
                        + "changed. Some columns in the include list do "
                        + "not longer exist");
            }
            rightCols.retainAll(rightIncludes);
        }
        if (m_settings.getRemoveRightJoinCols()) {
            rightCols
            .removeAll(Arrays.asList(m_settings.getRightJoinColumns()));
        }
        return rightCols;
    }
    /**
     * Get warnings which occurred when processing the method
     * <code>getOutputSpec</code>.
     * @return The warning messages.
     */
    public List<String> getConfigWarnings() {
        return m_configWarnings;
    }

    /**
     * Get warnings which occurred when processing the method
     *  <code>computeJoinTable</code>.
     * @return The warning messages.
     */
    public List<String> getRuntimeWarnings() {
        return m_runtimeWarnings;
    }

    /**
     * Create the DataTableSpec of the output.
     *
     * @return The DataTableSpec of the output.
     * @throws InvalidSettingsException if the settings are inconsistent with
     *             the DataTableSpec elements given in the Constructor.
     */
    public DataTableSpec getOutputSpec() throws InvalidSettingsException {

        return createSpec(new DataTableSpec[]{m_leftDataTableSpec,
                m_rightDataTableSpec});
    }

    /**
     * Joins the <code>leftTable</code> and the <code>rightTable</code>.
     *
     * @param leftTable The left input table.
     * @param rightTable The right input table.
     * @param exec The Execution monitor for this execution.
     * @return The joined table.
     * @throws CanceledExecutionException when execution is canceled
     * @throws InvalidSettingsException when inconsistent settings are provided
     */
    public BufferedDataTable computeJoinTable(
            final BufferedDataTable leftTable,
            final BufferedDataTable rightTable, final ExecutionContext exec)
    throws CanceledExecutionException, InvalidSettingsException {
        m_runtimeWarnings.clear();
        m_leftRowKeyMap.clear();
        m_rightRowKeyMap.clear();

        // This does some input data checking, too
        DataTableSpec joinedTableSpec = createSpec(new DataTableSpec[] {
                leftTable.getDataTableSpec(),
                rightTable.getDataTableSpec()});

        if (m_settings.getDuplicateHandling().equals(
                DuplicateHandling.Filter)) {
            List<String> leftCols = getLeftIncluded(
                    leftTable.getDataTableSpec());
            List<String> rightCols = getRightIncluded(
                    rightTable.getDataTableSpec());
            List<String> duplicates = new ArrayList<String>();
            duplicates.addAll(leftCols);
            duplicates.retainAll(rightCols);
            // Check if duplicated columns have identical data
            compareDuplicates(leftTable, rightTable, duplicates);
        }

        BufferedDataTable outerTable = rightTable;
        BufferedDataTable innerTable = leftTable;


        m_retainRight = JoinMode.RightOuterJoin.equals(m_settings.getJoinMode())
        || JoinMode.FullOuterJoin.equals(m_settings.getJoinMode());
        m_retainLeft = JoinMode.LeftOuterJoin.equals(m_settings.getJoinMode())
        || JoinMode.FullOuterJoin.equals(m_settings.getJoinMode());

        // if multipleMatchCanOccur is true, to rows can be match more than
        // once. This is in general met with the MatchAny Option but only if
        // there are more than one join column.
        m_multipleMatchCanOccur = m_settings.getCompositionMode()
        .equals(CompositionMode.MatchAny)
        && m_settings.getLeftJoinColumns().length > 1;

        if (m_multipleMatchCanOccur) {
            m_multiLeftRetainAll =
                new HashSet<Integer>();
            for (int i = 0; i < leftTable.getRowCount(); i++) {
                m_multiLeftRetainAll.add(i);
            }
        }


        m_inputDataRowSettings = createInputDataRowSettings(leftTable,
                rightTable);
        int[] rightSurvivors = getIndecesOf(rightTable, m_rightSurvivors);
        m_outputDataRowSettings = new OutputDataRow.Settings(
                rightTable.getDataTableSpec(),
                rightSurvivors);

        if (null == m_memService) {
            m_memService = new MemoryService();
        }

        /* numBits -> numPartitions
         * 0 -> 1
         * 1 -> 2
         * 2 -> 4
         * 3 -> 8
         * 4 -> 16
         * 5 -> 32
         * 6 -> 64
         * 7 -> 128
         */
        m_numBits = m_numBitsInitial;
        int numPartitions = 0x0001 << m_numBits;
        m_bitMask = 0;
        for (int i = 0; i < m_numBits; i++) {
            m_bitMask += 0x0001 << i;
        }

        Set<Integer> pendingParts = new TreeSet<Integer>();
        for (int i = 0; i < numPartitions; i++) {
            pendingParts.add(i);
        }


        JoinContainer joinCont = new JoinContainer(
                m_outputDataRowSettings);

        double[] progressIntervals = new double[] {0.6, 0.2, 0.2};
        exec.setProgress(0.0);
        while (pendingParts.size() > 0) {
            Collection<Integer> processedParts = performJoin(
                    innerTable, outerTable,
                    joinCont, pendingParts, exec, progressIntervals[0]);
            pendingParts.removeAll(processedParts);
        }


        if (m_multipleMatchCanOccur) {
            // Add left outer joins
            int c = 0;
            for (Integer index : m_multiLeftRetainAll) {
                DataRow outRow = OutputDataRow.createDataRow(c, index, -1,
                        m_outputDataRowSettings);
                joinCont.addLeftOuter(outRow, exec);
                c++;
            }
        }
        joinCont.close();

        // numbers are needed to report progress more precisely
        int totalNumJoins = joinCont.getRowCount();
        int numMatches = null != joinCont.getMatches()
        ? joinCont.getMatches().getRowCount() : 0;
        int numLeftOuter = null != joinCont.getLeftOuter()
        ? joinCont.getLeftOuter().getRowCount() : 0;
        int numRightOuter = null != joinCont.getRightOuter()
        ? joinCont.getRightOuter().getRowCount() : 0;

        exec.setMessage("Sort Joined Partitions");
        Comparator<DataRow> joinComp = OutputDataRow.createRowComparator();
        SortedTable matches = null != joinCont.getMatches()
        ? new SortedTable(joinCont.getMatches(), joinComp, false,
                exec.createSubExecutionContext(
                        progressIntervals[1] * numMatches / totalNumJoins))
        : null;
        SortedTable leftOuter = null != joinCont.getLeftOuter()
        ? new SortedTable(joinCont.getLeftOuter(), joinComp, false,
                exec.createSubExecutionContext(
                        progressIntervals[1] * numLeftOuter / totalNumJoins))
        : null;
        SortedTable rightOuter = null != joinCont.getRightOuter()
        ? new SortedTable(joinCont.getRightOuter(), joinComp, false,
                exec.createSubExecutionContext(
                        progressIntervals[1] * numRightOuter / totalNumJoins))
        : null;

        exec.setMessage("Merge Joined Partitions");
        // Build sorted table
        int[] leftSurvivors = getIndecesOf(leftTable, m_leftSurvivors);

        DataHiliteOutputContainer oc =
            new DataHiliteOutputContainer(joinedTableSpec,
                    m_settings.getEnableHiLite(), leftTable,
                    leftSurvivors, rightSurvivors,
                    createRowKeyFactory(leftTable, rightTable));
        oc.addTableAndFilterDuplicates(matches,
                exec.createSubExecutionContext(
                        progressIntervals[2] * numMatches / totalNumJoins));
        oc.addTableAndFilterDuplicates(leftOuter,
                exec.createSubExecutionContext(
                        progressIntervals[2] * numLeftOuter / totalNumJoins));
        oc.addTableAndFilterDuplicates(rightOuter,
                exec.createSubExecutionContext(
                        progressIntervals[2] * numRightOuter / totalNumJoins));
        oc.close();

        m_leftRowKeyMap = oc.getLeftRowKeyMap();
        m_rightRowKeyMap = oc.getRightRowKeyMap();

        return oc.getTable();
    }

    /** This method start with reading the partitions of the inner table defined
     * in currParts. If memory is low partitions will be skipped or the
     * number of partitions will be raised which leads to smaller partitions.
     * Successfully read partitions will be joined. The return collection
     * defines the successfully processed partitions.
     *
     * @param innerTable The inner input table.
     * @param outerTable The outer input table.
     * @param innerPort The port (left or right) of the inner input table.
     * @param outerPort The port (left or right) of the outer input table.
     * @param outputContainer The container used for storing matches.
     * @param currParts The parts that will be read of the inner input table.
     * @param pendingParts The parts that are not processed yet.
     * @param exec The execution context.
     * @param progressDiff The difference in the progress monitor.
     * @return The partitions that were successfully processed (read + joined).
     * @throws CanceledExecutionException when execution is canceled
     */
    private Collection<Integer> performJoin(
            final BufferedDataTable innerTable,
            final BufferedDataTable outerTable,
            final JoinContainer outputContainer,
            final Collection<Integer> pendingParts,
            final ExecutionContext exec,
            final double progressDiff) throws CanceledExecutionException  {
        // Update increment for reporting progress
        double progress = exec.getProgressMonitor().getProgress();
        double numRows = innerTable.getRowCount()
        + outerTable.getRowCount();
        double inc = (progressDiff - progress) / numRows;

        Collection<Integer> currParts = new ArrayList<Integer>();
        currParts.addAll(pendingParts);
        setMessage("Read", exec, pendingParts, currParts);

        // Partition inner table
        Map <Integer, Map<JoinTuple, Set<Integer>>> innerHash =
            new HashMap<Integer, Map<JoinTuple, Set<Integer>>>();
        Map <Integer, Set<Integer>> innerIndexMap =
            new HashMap<Integer, Set<Integer>>();

        boolean tryToFreeMemory = true;
        int counter = 0;
        CloseableRowIterator innerIter = innerTable.iterator();
        while (innerIter.hasNext()) {
            exec.checkCanceled();
            if (!m_memService.isMemoryLow()) {
                DataRow row = innerIter.next();
                InputDataRow inputDataRow = new InputDataRow(row, counter,
                        InputDataRow.Settings.InDataPort.Left,
                        m_inputDataRowSettings);

                for (JoinTuple tuple : inputDataRow.getJoinTuples()) {
                    int index = tuple.hashCode() & m_bitMask;
                    if (currParts.contains(index)) {
                        addRow(innerHash, innerIndexMap,
                                index, tuple, inputDataRow);
                    }
                }
                counter++;
                // report progress
                progress += inc;
                exec.getProgressMonitor().setProgress(progress);
            } else {
                if (tryToFreeMemory) {
                    // Force full gc to be sure that there is not to much
                    // garbage
                    Runtime.getRuntime().gc();
                    tryToFreeMemory = false;
                    LOGGER.debug("Forced gc() while reading inner table.");
                    continue;
                } else {
                    tryToFreeMemory = true;

                    // Build list of partitions that are not empty
                    List<Integer> nonEmptyPartitions = new ArrayList<Integer>();
                    for (Integer i : currParts) {
                        if (null != innerHash.get(i)) {
                            nonEmptyPartitions.add(i);
                        }
                    }
                    int numNonEmpty = nonEmptyPartitions.size();
                    if (numNonEmpty > 1) {
                        // remove input partitions to free memory
                        List<Integer> removeParts = new ArrayList<Integer>();
                        for (int i = 0; i < numNonEmpty / 2; i++) {
                            removeParts.add(nonEmptyPartitions.get(i));
                        }
                        // remove collected data of the no longer processed
                        for (int i : removeParts) {
                            innerHash.remove(i);
                            if (m_retainLeft && !m_multipleMatchCanOccur) {
                                innerIndexMap.remove(i);
                            }
                        }
                        currParts.removeAll(removeParts);
                        LOGGER.debug("Skip partitions while "
                                + "reading inner table. Currently Processed: "
                                + currParts + ". Skip: " + removeParts);
                        // update increment for reporting progress
                        numRows += innerTable.getRowCount()
                        + outerTable.getRowCount();
                        inc = (progressDiff - progress) / numRows;

                        setMessage("Read", exec, pendingParts, currParts);
                    } else if (nonEmptyPartitions.size() == 1) {
                        if (m_numBits < m_numBitsMaximal) {
                            LOGGER.debug("Increase number of partitions while "
                                    + "reading inner table. Currently "
                                    + "Processed: " + nonEmptyPartitions);

                            // increase number of partitions
                            m_numBits = m_numBits + 1;
                            m_bitMask = m_bitMask | (0x0001 << (m_numBits - 1));
                            Set<Integer> pending = new TreeSet<Integer>();
                            pending.addAll(pendingParts);
                            pendingParts.clear();
                            for (int i : pending) {
                                pendingParts.add(i);
                                int ii = i | (0x0001 << (m_numBits - 1));
                                pendingParts.add(ii);
                            }

                            int currPart = nonEmptyPartitions.iterator().next();
                            currParts.clear();
                            currParts.add(currPart);
                            // update chunk size
                            retainPartitions(innerHash, innerIndexMap,
                                    currPart);
                            // update increment for reporting progress
                            numRows += innerTable.getRowCount()
                            + outerTable.getRowCount();
                            inc = (progressDiff - progress) / numRows;

                            setMessage("Read", exec, pendingParts, currParts);
                        } else {
                            // We have now 2^32 partitions.
                            // We can only keep going and hope that other nodes
                            // may free some memory.
                            LOGGER.warn("Memory is low. "
                                + "I have no chance to free memory. This may "
                                + "cause an endless loop.");
                        }
                    } else if (nonEmptyPartitions.size() < 1) {
                        // We have only empty partitions.
                        // Other node consume to much memory,
                        // we cannot free more memory
                        LOGGER.warn("Memory is low. "
                                + "I have no chance to free memory. This may "
                                + "cause an endless loop.");
                    }
                }
            }

        }

        setMessage("Join", exec, pendingParts, currParts);
        // Join with outer table
        joinInMemory(innerHash, innerIndexMap,
                currParts, outerTable,
                outputContainer,
                exec, inc);


        // Log which parts were successfully joined
        for (int part : currParts) {
            int numTuples = innerHash.get(part) != null
            ? innerHash.get(part).values().size() : 0;
            LOGGER.debug("Joined " + part + " with "
                    + numTuples + " tuples.");
        }

        // Garbage collector has problems without this explicit clearance.
        innerHash.clear();
        innerIndexMap.clear();

        // return successfully joined parts
        return currParts;
    }

    /**
     * @param exec
     * @param pendingParts
     * @param currParts
     */
    private void setMessage(final String activity,
            final ExecutionContext exec,
            final Collection<Integer> pendingParts,
            final Collection<Integer> currParts) {
        exec.setMessage(activity + " " + currParts.size()
                + " parts | Pending : "
                + (pendingParts.size() - currParts.size())
                + " parts | Total: "
                + (1 << m_numBits)
                + " parts.");
    }

    /**
     * Called when the number of partitions is doubled. The innerHash is
     * traversed and only those entries that are in the given part are
     * retained. The innerIndexMap is build up so that it contains only
     * the entries that are in the given part.
     */
    private void retainPartitions(
            final Map<Integer, Map<JoinTuple, Set<Integer>>> innerHash,
            final Map<Integer, Set<Integer>> innerIndexMap,
            final int part) {
        innerIndexMap.clear();

        Map<JoinTuple, Set<Integer>> thisInnerHash = innerHash.get(part);
        for (Iterator<JoinTuple> iter = thisInnerHash.keySet().iterator();
        iter.hasNext();) {
            JoinTuple tuple = iter.next();
            int index = tuple.hashCode() & m_bitMask;
            if (index != part) {
                iter.remove();
            } else if (m_retainLeft && !m_multipleMatchCanOccur) {
                Set<Integer> thisInnerIndexMap = innerIndexMap.get(index);
                if (null == thisInnerIndexMap) {
                    thisInnerIndexMap = new HashSet<Integer>();
                    innerIndexMap.put(index, thisInnerIndexMap);
                }
                for (Integer rowIndex : thisInnerHash.get(tuple)) {
                    thisInnerIndexMap.add(rowIndex);
                }
            }
        }
    }

    /**
     * Add a row to innerHash and innerIndexMap.
     * @param index The index of the row.
     * @param joinTuple The join tuples of the row.
     * @param row The row to be added.
     */
    private void addRow(
            final Map <Integer, Map<JoinTuple, Set<Integer>>> innerHash,
            final Map <Integer, Set<Integer>> innerIndexMap,
            final int index, final JoinTuple joinTuple,
            final InputDataRow row) {
        if (m_retainLeft  && !m_multipleMatchCanOccur) {
            Set<Integer> thisInnerIndexMap = innerIndexMap.get(index);
            if (null == thisInnerIndexMap) {
                thisInnerIndexMap = new HashSet<Integer>();
                innerIndexMap.put(index, thisInnerIndexMap);
            }
            thisInnerIndexMap.add(row.getIndex());
        }

        Map<JoinTuple, Set<Integer>> thisInnerHash = innerHash.get(index);
        if (null == thisInnerHash) {
            thisInnerHash = new HashMap<JoinTuple, Set<Integer>>();
            innerHash.put(index, thisInnerHash);
        }


        Set<Integer> c = thisInnerHash.get(joinTuple);
        if (null != c) {
            c.add(row.getIndex());
        } else {
            Set<Integer> list = new HashSet<Integer>();
            list.add(row.getIndex());
            thisInnerHash.put(joinTuple, list);
        }


    }


    /**
     * Join given rows in memory and append joined row to the outputCont.
     * @param innerHash Stores the rows of the input table in parts.
     * @param innerIndexMap The same number as found in innerHash used for
     * retainOuter option.
     *
     * @param currParts The parts of the outer table that will be joined.
     * @param outerTable The outer table.
     * @param outerPort The port (left or right) of the outer table.
     * @param outputCont The joined rows will be added to this container.
     * @param exec The {@link ExecutionContext}
     * @param incProgress The progress increment.
     * @throws CanceledExecutionException When execution is cancelled
     */
    private void joinInMemory(
            final Map <Integer, Map<JoinTuple, Set<Integer>>> innerHash,
            final Map <Integer, Set<Integer>> innerIndexMap,
            final Collection<Integer> currParts,
            final BufferedDataTable outerTable,
            final JoinContainer outputCont,
            final ExecutionContext exec,
            final double incProgress) throws CanceledExecutionException {
        double progress = exec.getProgressMonitor().getProgress();
        int counter = 0;
        for (DataRow row : outerTable) {
            progress += incProgress;
            exec.getProgressMonitor().setProgress(progress);
            exec.checkCanceled();

            InputDataRow outerRow = new InputDataRow(row, counter,
                    InputDataRow.Settings.InDataPort.Right,
                    m_inputDataRowSettings);

            boolean matchFoundForOuterRow = false;

            for (JoinTuple outerTuple : outerRow.getJoinTuples()) {
                int index = outerTuple.hashCode() & m_bitMask;
                if (!currParts.contains(index)) {
                    continue;
                }
                Map<JoinTuple, Set<Integer>> thisInnerHash =
                    innerHash.get(index);
                if (null == thisInnerHash) {
                    continue;
                }
                Set<Integer> thisInnerIndexMap = null;
                if (m_retainLeft  && !m_multipleMatchCanOccur) {
                    thisInnerIndexMap = innerIndexMap.get(index);
                }

                Set<Integer> innerRow = thisInnerHash.get(outerTuple);
                if (null != innerRow) {
                    matchFoundForOuterRow = true;
                    for (Integer singleInnerRow : innerRow) {
                        int outRowIndex =
                            outputCont.getRowCount();
                        DataRow outRow = OutputDataRow.createDataRow(
                                outRowIndex,
                                singleInnerRow, outerRow.getIndex(),
                                row,
                                m_outputDataRowSettings);
                        outputCont.addMatch(outRow, exec);
                        if (m_retainLeft && !m_multipleMatchCanOccur) {
                            thisInnerIndexMap.remove(singleInnerRow);
                        }
                        if (m_retainLeft && m_multipleMatchCanOccur) {
                            m_multiLeftRetainAll.remove(singleInnerRow);
                        }
                    }
                }
            }


            if (m_retainRight && !matchFoundForOuterRow) {
                int outRowIndex =
                    outputCont.getRowCount();
                DataRow outRow = OutputDataRow.createDataRow(outRowIndex,
                        -1, outerRow.getIndex(),
                        row,
                        m_outputDataRowSettings);
                outputCont.addRightOuter(outRow, exec);
            }
            counter++;
        }

        if (m_retainLeft && !m_multipleMatchCanOccur) {
            for (int index : innerIndexMap.keySet()) {
                // add every remaining item in the innerIndex
                for (Integer row : innerIndexMap.get(index)) {
                    int outRowIndex =
                        outputCont.getRowCount();
                    DataRow outRow = OutputDataRow.createDataRow(outRowIndex,
                            row, -1,
                            m_outputDataRowSettings);
                    outputCont.addLeftOuter(outRow, exec);
                }
            }
        }

    }

    private List<Integer> getLeftJoinIndices(
            final BufferedDataTable leftTable) {
        // Create list of indices for the joining columns (Element of the list
        // is -1 if RowKey should be joined).
        int numJoinAttributes = m_settings.getLeftJoinColumns().length;
        List<Integer> leftTableJoinIndices =
            new ArrayList<Integer>(numJoinAttributes);
        for (int i = 0; i < numJoinAttributes; i++) {
            String joinAttribute = m_settings.getLeftJoinColumns()[i];
            leftTableJoinIndices.add(
                    leftTable.getDataTableSpec().findColumnIndex(
                            joinAttribute));
        }
        return leftTableJoinIndices;
    }


    private List<Integer> getRightJoinIndices(
            final BufferedDataTable rightTable) {
        // Create list of indices for the joining columns (Element of the list
        // is -1 if RowKey should be joined).
        int numJoinAttributes = m_settings.getLeftJoinColumns().length;
        List<Integer> rightTableJoinIndices =
            new ArrayList<Integer>(numJoinAttributes);
        for (int i = 0; i < numJoinAttributes; i++) {
            String joinAttribute = m_settings.getRightJoinColumns()[i];
            rightTableJoinIndices.add(rightTable.getDataTableSpec()
                    .findColumnIndex(joinAttribute));
        }
        return rightTableJoinIndices;
    }

    private JoinedRowKeyFactory createRowKeyFactory(
            final BufferedDataTable leftTable,
            final BufferedDataTable rightTable) {
        List<Integer> leftTableJoinIndices = getLeftJoinIndices(leftTable);
        List<Integer> rightTableJoinIndices = getRightJoinIndices(rightTable);

        if (leftTableJoinIndices.size() == 1
                && leftTableJoinIndices.get(0) == -1
                && rightTableJoinIndices.size() == 1
                && rightTableJoinIndices.get(0) == -1) {
            // This is the special case of row key match row key
            return new UseSingleRowKeyFactory();
        } else {
            return new ConcatenateJoinedRowKeyFactory(
                    m_settings.getRowKeySeparator());
        }
    }

    private InputDataRow.Settings createInputDataRowSettings(
            final BufferedDataTable leftTable,
            final BufferedDataTable rightTable) {
        List<Integer> leftTableJoinIndices = getLeftJoinIndices(leftTable);
        List<Integer> rightTableJoinIndices = getRightJoinIndices(rightTable);



        // Build m_inputDataRowSettings
        Map<InputDataRow.Settings.InDataPort,
        List<Integer>> joiningIndicesMap =
            new HashMap<InputDataRow.Settings.InDataPort, List<Integer>>();
        joiningIndicesMap.put(InputDataRow.Settings.InDataPort.Left,
                leftTableJoinIndices);
        joiningIndicesMap.put(InputDataRow.Settings.InDataPort.Right,
                rightTableJoinIndices);


        InputDataRow.Settings inputDataRowSettings = new InputDataRow.Settings(
                joiningIndicesMap, m_multipleMatchCanOccur);

        return inputDataRowSettings;
    }


    /**
     * Validates the settings in the passed <code>NodeSettings</code> object.
     * The specified settings is checked for completeness and
     * consistency.
     *
     * @param s The settings to validate.
     * @throws InvalidSettingsException If the validation of the settings
     *             failed.
     */
    public static  void validateSettings(final Joiner2Settings s)
    throws InvalidSettingsException {
        if (s.getDuplicateHandling() == null) {
            throw new InvalidSettingsException(
                "No duplicate handling method selected");
        }
        if (s.getJoinMode() == null) {
            throw new InvalidSettingsException("No join mode selected");
        }
        if ((s.getLeftJoinColumns() == null)
                || s.getLeftJoinColumns().length < 1
                || s.getRightJoinColumns() == null
                || s.getRightJoinColumns().length < 1) {
            throw new InvalidSettingsException(
                "Please define at least one joining column pair.");
        }
        if (s.getLeftJoinColumns() != null
                && s.getRightJoinColumns() != null
                && s.getLeftJoinColumns().length
                != s.getRightJoinColumns().length) {
            throw new InvalidSettingsException(
                    "Number of columns selected from the left table and from "
                    + "the right table do not match");
        }
        if (DuplicateHandling.AppendSuffix.equals(s.getDuplicateHandling())
                && ((s.getDuplicateColumnSuffix() == null) || (s
                        .getDuplicateColumnSuffix().length() < 1))) {
            throw new InvalidSettingsException(
            "No suffix for duplicate columns provided");
        }
        if (s.getMaxOpenFiles() < 3) {
            throw new InvalidSettingsException(
            "Maximum number of open files must be at least 3.");
        }

    }

    /**
     * @param leftTable The left input table
     * @param rightTable The right input table
     * @param duplicates The list of columns to test for identity
     */
    private void compareDuplicates(final BufferedDataTable leftTable,
            final BufferedDataTable rightTable, final List<String> duplicates) {

        int[] leftIndex = getIndecesOf(leftTable, duplicates);
        int[] rightIndex = getIndecesOf(rightTable, duplicates);

        String[] messages = new String[duplicates.size()];

        CloseableRowIterator leftIter = leftTable.iterator();
        CloseableRowIterator rightIter = rightTable.iterator();
        while (leftIter.hasNext()) {
            if (!rightIter.hasNext()) {
                // right table has less rows
                m_runtimeWarnings.add("Possible problem in configuration "
                        + "found. The \"Duplicate Column Handling\" is "
                        + "configured to  filter duplicates, but the "
                        + "duplicate columns are not equal since the "
                        + "left table has more elements than the right "
                        + "table.");
                break;
            }
            DataRow left = leftIter.next();
            DataRow right = rightIter.next();
            for (int i = 0; i < duplicates.size(); i++) {
                if (null == messages[i]
                                     && !left.getCell(leftIndex[i]).equals(
                                             right.getCell(rightIndex[i]))) {
                    // Two cells do not match
                    messages[i] = "The column \"" + duplicates.get(i)
                    + "\" can be found in "
                    + "both input tables but the content is not "
                    + "equal. "
                    + "Only the one in the left input table will show "
                    + "up in the output table. Please change the "
                    + "Duplicate Column Handling if both columns "
                    + "should show up in the ouput table.";
                }
            }

        }
        if (rightIter.hasNext()) {
            // right table has more rows
            m_runtimeWarnings.add("Possible problem in configuration found. "
                    + "The \"Duplicate Column Handling\" is configured to "
                    + "filter duplicates, but the duplicate columns are not "
                    + "equal since the right table has more elements than the "
                    + "left table.");
        }
        for (int i = 0; i < duplicates.size(); i++) {
            if (null != messages[i]) {
                m_runtimeWarnings.add(messages[i]);
            }
        }
    }

    /**
     * Used in compareDuplicates.
     * @param table A DataTable
     * @param cols Columns of the table
     * @return the indices of the given columns in the table.
     */
    private int[] getIndecesOf(final BufferedDataTable table,
            final List<String> cols) {
        int[] indices = new int[cols.size()];
        int c = 0;

        for (String col : cols) {
            for (int i = 0; i < table.getDataTableSpec().getNumColumns(); i++) {
                if (table.getSpec().getColumnSpec(i).getName().equals(col)) {
                    indices[c] = i;
                }
            }
            c++;
        }
        return indices;
    }

    /**
     * Used for testing, only.
     * @param memoryService set memory service
     */
    void setMemoryService(final MemoryService memoryService) {
        m_memService = memoryService;
    }

    /**
     * Used for testing, only.
     * @param bits number of initial partitions will be 2^bits
     */
    void setNumBitsInitial(final int bits) {
        m_numBitsInitial = bits;
    }

    /**
     * Used for testing, only.
     * @param bits number of maximal partitions will be 2^numBits
     */
    void setNumBitsMaximal(final int bits) {
        m_numBitsMaximal = bits;
    }

}

