/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   May 16, 2020 (carlwitt): created
 */
package org.knime.core.data.join;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.data.join.HybridHashJoin.DiskBackedHashPartitions.DiskBucket;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.data.join.results.JoinContainer;
import org.knime.core.data.join.results.JoinResults;
import org.knime.core.data.join.results.LeftRightSortedJoinContainer;
import org.knime.core.data.join.results.NWayMergeContainer;
import org.knime.core.data.join.results.UnorderedJoinContainer;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.util.Pair;

/**
 *
 * Classic hybrid hash join, as described for instance in [1]. Degrades gracefully from in-memory hash join to
 * disk-based hash join depending on how much main memory is available by partitioning input tables and flushing only
 * those partitions to disk that do not fit into memory. <br/>
 *
 * The algorithm proceeds in three phases:
 * <ol>
 * <li>Single pass over the smaller table (hash input), which is partitioned and indexed in memory. If memory runs low,
 * partitions are flushed to disk. See {@link HashIndex#toDisk(DiskBackedHashPartitions)}.</li>
 * <li>Single pass over the bigger table (probe input). Rows that hash to a partition whose counterpart is held in
 * memory are processed directly. Other rows are flushed to disk.</li>
 * <li>Load and join matching partitions that have been flushed to disk.</li>
 * </ol>
 *
 * [1] Garcia-Molina, Hector, Jeffrey D. Ullman, and Jennifer Widom. Database system implementation.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 4.2
 */
public class HybridHashJoin extends JoinImplementation {

    private InputTable m_hashSide;

    /**
     * References the table (left table or right table) with fewer rows or the input table that is not streamable.
     */
    private final BufferedDataTable m_hash;

    /**
     * References the table (either left table or right table) with more rows or the input table that is streamable.
     */
    private final BufferedDataTable m_probe;

    private final JoinTableSettings m_hashSettings;
    private final JoinTableSettings m_probeSettings;

    private DiskBackedHashPartitions m_partitioner;

    private JoinContainer m_joinResults;


    /**
     * @param settings
     * @param exec
     * @throws InvalidSettingsException
     */
    protected HybridHashJoin(final JoinSpecification settings, final ExecutionContext exec) throws InvalidSettingsException {
        super(settings, exec);

        InputTable probeTable = HashIndex.biggerTable(settings);
        Supplier<? extends InvalidSettingsException> exceptionSupplier = () -> new InvalidSettingsException(probeTable + " input table is not present in the join table settings.");
        m_probe = settings.getSettings(probeTable).getTable().orElseThrow(exceptionSupplier);
        m_hash = settings.getSettings(probeTable.other()).getTable().orElseThrow(exceptionSupplier);

        m_hashSide = probeTable.other();
        m_hashSettings = m_joinSpecification.getSettings(probeTable.other());
        m_probeSettings = m_joinSpecification.getSettings(probeTable);

        System.out.println(String.format("probeTable %s", probeTable));

        m_progress = new JoinProgressMonitor();

    }

    /**
     *
     * Only hash input buckets move between main memory and disk. They do so only during partitioning the hash input.
     * Probe input rows are either directly processed or go to disk.
     *
     * @param hashInput
     * @param probeInput
     *
     * @return
     * @throws CanceledExecutionException
     */
    @Override
    public JoinResults join() throws CanceledExecutionException {

        // catch empty join
        if (!(m_joinSpecification.isRetainMatched() || m_joinSpecification.isRetainUnmatched(InputTable.LEFT)
            || m_joinSpecification.isRetainUnmatched(InputTable.RIGHT))) {
            return new UnorderedJoinContainer(m_joinSpecification, m_exec, false, false);
        }

        boolean deduplicateResults = !m_joinSpecification.isConjunctive();
        boolean deferMatches =  ! m_joinSpecification.isConjunctive();
        System.out.println(String.format("deferMatches=%s", deferMatches));

        switch (m_joinSpecification.getOutputRowOrder()) {
            case ARBITRARY:
                // TODO redundant
                m_joinResults = new UnorderedJoinContainer(m_joinSpecification, m_exec, deduplicateResults, deferMatches);
                break;
            case DETERMINISTIC:
                m_joinResults =
                    new NWayMergeContainer(m_joinSpecification, m_exec, m_left == m_probe, deduplicateResults, deferMatches);
                break;
            case LEFT_RIGHT:
                if (m_left == m_probe) {
                    // if the left table happens to be the probe table, legacy and deterministic order are the same
                    m_joinResults =
                        new NWayMergeContainer(m_joinSpecification, m_exec, m_left == m_probe, deduplicateResults, deferMatches);
                } else {
                    // otherwise we need to do the full sort
                    m_joinResults = new LeftRightSortedJoinContainer(m_joinSpecification, m_exec, deduplicateResults, deferMatches);
                }
                break;
            default:
                throw new IllegalStateException();
        }

        // ordered join containers need the row offsets to sort the output
        // for a disjunctive join, offsets are used to reject duplicate results and collect deferred unmatched rows
        boolean storeRowOffsets = !(m_joinResults instanceof UnorderedJoinContainer) || deduplicateResults;
        m_partitioner = new DiskBackedHashPartitions(storeRowOffsets, m_joinResults);

        m_progress.m_numBuckets = m_partitioner.m_numPartitions;

        // build an index of the hash input, partially on disk if necessary
        phase1();

        // single pass over the probe input, write rows to disk that can't be processed in memory
        phase2();

        // join rows that have been flushed to disk
        phase3();

        return m_joinResults;
    }

    /**
     * Index the hash input by partitioning the hash input into buckets. Keep as many buckets as possible in memory.
     *
     * @throws CanceledExecutionException
     */
    private void phase1() throws CanceledExecutionException {

        System.out.println("    HybridHashJoin.phase1()");
        m_progress.setMessage("Phase 1/3: Processing smaller table");
        m_progress.reset();

        // materialize only columns that either make it into the output or are needed for joining
        try (CloseableRowIterator hashRows =
            m_hash.filter(TableFilter.materializeCols(m_hashSettings.m_materializeColumnIndices)).iterator()) {

            int rowOffset = 0;
            while (hashRows.hasNext()) {

                // if memory is running low and there are hash buckets in-memory
                if (m_progress.isMemoryLow(100)) {
                    m_partitioner.flushNextBucket();
                    m_progress.setNumPartitionsOnDisk(m_partitioner.getNumPartitionsOnDisk());
                }

                DataRow hashRow = hashRows.next();

                m_partitioner.addHash(rowOffset, hashRow);

                m_progress.setProgressAndCheckCanceled(1. * rowOffset / m_hash.size());

                rowOffset++;

            } // all hash input rows processed
        } // close hash input row iterator

        // the unmatched hash rows (due to incomplete join attributes) have been added in hash input row order
        m_joinResults.sortedChunkEnd();

        // hash input has been processed completely, close buckets that have been migrated to disk (if any)
        m_partitioner.closeHashBuckets();

    }

    /**
     * Partitioning the probe input and joining with in-memory hash partitions.
     * <ul>
     * <li>process probe rows immediately for which the matching bucket is in memory</li>
     * <li>flush probe rows to disk for which the matching bucket is not in memory</li>
     * </ul>
     *
     * @throws CanceledExecutionException
     */
    private void phase2() throws CanceledExecutionException {

        System.out.println(String.format("    HybridHashJoin.phase2() with %s in-memory hash partitions", m_partitioner.getNumInMemoryParititions()));

        m_progress.setMessage("Phase 2/3: Processing larger table");

        // materialize only columns that either make it into the output or are needed for joining
        int[] materializeColumnIndices = m_probeSettings.m_materializeColumnIndices;
        TableFilter materializeFilter = TableFilter.materializeCols(materializeColumnIndices);
        try (CloseableRowIterator probeRows = m_probe.filter(materializeFilter).iterator()) {

            int rowOffset = 0;
            while (probeRows.hasNext()) {

                DataRow probeRow = probeRows.next();

                // join directly or send to disk if hash input partition is not in memory
                m_partitioner.processProbeRow(rowOffset, probeRow);

                m_progress.setProgressAndCheckCanceled(1.0 * rowOffset / m_probe.size());

                rowOffset++;

            }

        } // all probe input rows assigned to partitions

        // probe input partitioning is done; no more rows will be added to probe buckets
        m_partitioner.closeProbeBuckets();

        m_joinResults.sortedChunkEnd();

        System.out.println("phase 2 finalize: in-memory hash indexes");

        for (HashIndex hashIndex : m_partitioner.inMemoryHashPartitions()) {
            // handle unmatched in-memory hash rows
            hashIndex.forUnmatchedHashRows(m_joinResults.unmatched(m_hashSide));
            m_joinResults.sortedChunkEnd();
        }

    }

    /**
     * Optionally join the rows that went to disk because not enough memory was available
     *
     * @throws CanceledExecutionException
     */
    private void phase3() throws CanceledExecutionException {

//        System.out.println("    HybridHashJoin.phase3()");

        // since each table is sorted according to natural join order, we can do a n-way merge of the partial results
        m_progress.setMessage("Phase 3/3: Processing data on disk");
        m_progress.setBucketSizes(m_partitioner.m_probeBuckets, m_partitioner.m_hashBucketsOnDisk);

        // join the pairs of buckets that haven't been processed in memory
        int i = 0;
        for (Pair<DiskBucket, DiskBucket> pair : m_partitioner.unprocessedPartitions()) {
            System.out.println(" ! Block Hash Join over partition " + (i++));
            DiskBucket probeBucket = pair.getFirst();
            DiskBucket hashBucket = pair.getSecond();
            probeBucket.join(hashBucket, m_joinResults);
            m_joinResults.sortedChunkEnd();
        }

    }

    @FunctionalInterface
    interface PartitionHandler {
        void accept(int partition) throws CanceledExecutionException;
    }

    /**
     * Defines a partitioning of join column values into hash buckets. This partitioning scheme is used to partition the
     * hash input table and the probe input table.
     *
     * <h2>Usage</h2> The hash input is processed by calling {@link #addHash(int, JoinTuple, DataRow)} on every row.
     * Memory footprint of the join can be reduced by calling {@link #flushNextBucket()} in case heap space is running
     * low. The probe input is processed in a single pass in
     * {@link HybridHashJoin#phase2()} using
     * {@link #processProbeRow(JoinResults, int, DataRow, JoinTuple)}. The rows that have been flushed to disk are
     * processed in {@link HybridHashJoin#phase3()} using
     * {@link #unprocessedPartitions()}. <br/>
     *
     * <h2>Internals</h2> The hash rows are stored and indexed internally as {@link HashIndex} objects.
     * {@link #flushNextBucket()} converts a {@link HashIndex} to a {@link DiskBucket} object. The actual hashing from
     * join values to partitions happens in {@link #getPartition(Object)}. Rows are annotated with row offsets if
     * {@link #m_storeRowOffsets} is true. The intermediate table specification for flushing rows to disk is defined in
     * {@link DiskBucket}, which also adds a column for the row offsets in case it is needed.
     *
     * @author Carl Witt, KNIME AG, Zurich, Switzerland
     */
    class DiskBackedHashPartitions {

        private final ObjLongConsumer<DataRow> m_unmatchedProbeRows;

        private final ObjLongConsumer<DataRow> m_unmatchedHashRows;

        /**
         * The number of buckets to use for partitioning join tuples. Each bucket either has a corresponding in-memory
         * hash index or results in two files on disk, one for the probe input rows and one for the hash input rows.
         */
        private final int m_numPartitions;

        /**
         * The index for the i-th partition of the hash input.
         */
        private final HashIndex[] m_hashBucketsInMemory;

        /**
         * The buffer for rows of the i-th partition of the hash input.
         */
        private final DiskBucket[] m_hashBucketsOnDisk;

        /**
         * The buffer for rows of the i-th partition of the probe input. If the i-th partition of the hash input is on
         * disk, a probe row is processed in {@link #processProbeRow(JoinResults, int, DataRow, JoinTuple)} by flushing
         * it to disk via {@link DiskBucket#add(DataRow, long)} instead of joining it directly.
         */
        private final DiskBucket[] m_probeBuckets;

        /**
         * Whether to annotate data rows with their offset in their source table. Only needed when the output needs
         * sorting. Causes {@link DataRow}s to be wrapped as {@link OrderedRow}s.
         */
        private final boolean m_storeRowOffsets;

        /**
         * The format of the table in which to flush rows to disk. m_workingSpecs[{@link #PROBE}] for probe input and
         * m_workingSpecs[{@link #HASH}] for hash input table. Contains only the included columns and join columns as
         * returned by {@link JoinTableSettings#m_materializeColumnIndices} and possibly a column for the row's offset
         * if sorting of the results is required.
         */
//        private final JoinTableSettings[] m_workingSpecs = new JoinTableSettings[2];
//      private final IdentityHashMap<BufferedDataTable, DataTableSpec> m_workingSpecs = new IdentityHashMap<>();

        /**
         * Invariant: <br/>
         * <ul>
         * <li>for i ≥ firstInMemoryHashBucket, hashBucketsInMemory[i] is used and hashBucketsOnDisk[i] is null</li>
         * <li>for i < firstInMemoryHashBucket, hashBucketsInMemory[i] == null and hashBucketsOnDisk[i] is
         * functional</li>
         * </ul>
         * If heap space runs low, hash indexes are dropped and the indexed rows flushed to disk via
         * {@link #flushNextBucket()}, which increases firstInMemoryHashBucket by one.
         */
        private int m_firstInMemoryHashBucket;

        public DiskBackedHashPartitions(final boolean storeRowOffsets, final JoinResults results) {

            m_storeRowOffsets = storeRowOffsets;

            // we create two files per hash bucket, one for the hash input partition and one for the probe input partition
            // note that the effective number of buckets is limited by the number of rows in the hash table (for each row, at most one bucket is migrated to disk)
            m_numPartitions = Math.max(1, getMaxOpenFiles() / 2);
//            coveredPartitions = new int[m_numPartitions];

            InputTable hashSide = HashIndex.smallerTable(m_joinSpecification);

            Supplier<HashIndex> newHashIndex =
                () -> new HashIndex(m_joinSpecification, results, hashSide, getProgress()::getCancelChecker);

            m_hashBucketsInMemory = Stream.generate(newHashIndex).limit(m_numPartitions).toArray(HashIndex[]::new);

            // the buckets of the hash input are kept in memory for offsets firstInMemoryBucket...numBuckets-1
            // the offsets 0...firstInMemoryBucket-1 are stored on disk.
            m_firstInMemoryHashBucket = 0;

            // filled when hash buckets are migrated to disk
            m_hashBucketsOnDisk = new DiskBucket[m_numPartitions];

            InputTable probeSide = m_probe == m_left ? InputTable.LEFT : InputTable.RIGHT;
            m_unmatchedProbeRows = m_joinResults.unmatched(probeSide);
            m_unmatchedHashRows = m_joinResults.unmatched(probeSide.other());


            // create intermediate table specs for flushing rows to disk
//            m_workingSpecs[PROBE] = m_tableSettings.get(m_probe).condensed(storeRowOffsets);
//            m_workingSpecs[HASH] = m_tableSettings.get(m_hash).condensed(storeRowOffsets);

//            System.out.println(String.format("m_workingSpecs[PROBE]=%s", m_workingSpecs[PROBE]));
//            System.out.println(String.format("m_workingSpecs[HASH]=%s", m_workingSpecs[HASH]));

            // initialize each bucket with its own copy of the working table settings such that we can use them to
            // point to the bucket's intermediate table
            m_probeBuckets = Stream.generate(() -> m_probeSettings)
                    .map(DiskBucket::new)
                    .limit(m_numPartitions)
                    .toArray(DiskBucket[]::new);

//            for (BufferedDataTable inputTable : new BufferedDataTable[]{m_probe, m_hash}) {
//                m_workingSpecs.put(inputTable, workingTableSpec(storeRowOffsets, inputTable));
//            }

        }

        /**
         * Add a row from the hash input to the partitioned table. If the partition is held in memory, add the row to an
         * in-memory index structure. If the partition has been flushed to disk, flush it to disk. Annotates the row
         * with the provided row offset if {@link #m_storeRowOffsets} is true. Used in phase 1 to build the indexes per
         * partition.
         *
         * @param rowOffset this is added to the data row if {@link #m_storeRowOffsets} for sorting rows later. Is
         *            ignored otherwise.
         * @param hashRow
         * @throws CanceledExecutionException
         */
        public void addHash(final int rowOffset, final DataRow hashRow) throws CanceledExecutionException {

            DataCell[] joinAttributeValues = JoinTuple.get(m_hashSettings, hashRow);

            if (joinAttributeValues == null) {
                m_unmatchedHashRows.accept(hashRow, rowOffset);
            } else {
                if (m_joinSpecification.isConjunctive()) {
                    int partition = getPartition(JoinTuple.conjunctiveHashCode(joinAttributeValues));
                    addHashToPartition(partition, joinAttributeValues, hashRow, rowOffset);
                } else {
                    coveredPartitions(joinAttributeValues,
                        partition -> addHashToPartition(partition, joinAttributeValues, hashRow, rowOffset));
                }
            }


        }

        /**
         * @param joinAttributeValues
         * @throws CanceledExecutionException
         */
        private void coveredPartitions(final DataCell[] joinAttributeValues, final PartitionHandler partitionHandler) throws CanceledExecutionException {

            final boolean[] addToPartition = new boolean[m_numPartitions];

            // invoke handler only once on each partition
            Arrays.fill(addToPartition, false);

            for (int clause = 0; clause < m_joinSpecification.numConjunctiveGroups(); clause++) {
                int partition = getPartition(JoinTuple.disjunctiveHashCode(joinAttributeValues, clause));
                addToPartition[partition] = true;
            }

            for (int partition = 0; partition < addToPartition.length; partition++) {
                if(addToPartition[partition]) {
                    partitionHandler.accept(partition);
                }
            }

        }

        /**
         *
         * @param partition a partition offset, as returned by {@link #getPartition(int)}
         * @param joinAttributeValues
         * @param hashRow
         * @param rowOffset
         */
        private void addHashToPartition(final int partition, final DataCell[] joinAttributeValues, final DataRow hashRow, final int rowOffset) {

            boolean partitionInMemory = partition >= m_firstInMemoryHashBucket;
            if (partitionInMemory) {
                // if the hash bucket is in memory, add and index row
//                System.out.println(String.format("hashRow to memory [%s] %s", partition, hashRow));
                m_hashBucketsInMemory[partition].addHashRow(joinAttributeValues, hashRow, rowOffset);
            } else {
                // if the bucket is on disk store for joining later
                // hash rows don't need a row offset, they are already sorted.
//                System.out.println(String.format("hashRow to disk [%s] %s", partition, hashRow));
                m_hashBucketsOnDisk[partition].add(hashRow, rowOffset);
            }

        }

        /**
         * If the probe row's hash index is in memory, directly produce the output into the join container. If the probe
         * row's partition is on disk, flush it for later joining.
         * @param rowOffset
         * @param probeRow
         * @param joinAttributeValues
         *
         * @throws CanceledExecutionException
         */
        void processProbeRow(final int rowOffset, final DataRow probeRow) throws CanceledExecutionException {

            DataCell[] joinAttributeValues = JoinTuple.get(m_probeSettings, probeRow);

            // like in SQL, NULL ≠ NULL. A row with missing values in join columns must not match anything.
            if (joinAttributeValues == null) {
                m_unmatchedProbeRows.accept(probeRow, rowOffset);
            } else {

                if (m_joinSpecification.isConjunctive()) {
                    int partition = getPartition(JoinTuple.conjunctiveHashCode(joinAttributeValues));
                    addProbeToPartition(rowOffset, probeRow, partition);
                } else {
                    coveredPartitions(joinAttributeValues,
                        partition -> addProbeToPartition(rowOffset, probeRow, partition));
                }
            }
        }

        /**
         * This is called once per probe row in the conjunctive case and for every
         * {@link #coveredPartitions(DataCell[], PartitionHandler)} in the disjunctive case.
         *
         * @param rowOffset
         * @param probeRow
         * @param partition
         * @throws CanceledExecutionException
         */
        private void addProbeToPartition(final int rowOffset, final DataRow probeRow, final int partition) throws CanceledExecutionException {

            boolean partitionInMemory = partition >= m_firstInMemoryHashBucket;
            if (partitionInMemory) {
                m_hashBucketsInMemory[partition].joinSingleRow(probeRow, rowOffset);
//                System.out.println(String.format("probe row join %s", probeRow));
                m_progress.incProbeRowsProcessedInMemory();
            } else {
                // if hash bucket is on disk, process this row later, when joining the matching buckets
                m_probeBuckets[partition].add(probeRow, rowOffset);
                // can't tell if the row is matched or unmatched until the join of the disk buckets
//                System.out.println(String.format("probe row to disk [%s] %s", partition, probeRow));
                m_progress.incProbeRowsProcessedFromDisk();
            }

        }

        /**
         * Determine the partition that contains the given join column value combination.
         *
         * @param hashCode an integer denoting a hash for the join column value combination of the data row
         * @return the partition id, corresponding to array offset in {@link #m_probeBuckets},
         *         {@link #m_hashBucketsInMemory}, and {@link #m_hashBucketsOnDisk}.
         */
        public int getPartition(final int hashCode) {
            return Math.abs(hashCode % m_numPartitions);
        }

        /**
         * Migrate the next hash bucket to disk. Has no effect if all partitions are on disk.
         */
        public void flushNextBucket() {
            if (m_firstInMemoryHashBucket < m_hashBucketsInMemory.length) {
                System.out.println("HybridHashJoin.DiskBackedHashPartitions.flush bucket "+m_firstInMemoryHashBucket);
                // migrate the next in-memory hash bucket to disk
                m_hashBucketsOnDisk[m_firstInMemoryHashBucket] =
                    m_hashBucketsInMemory[m_firstInMemoryHashBucket].toDisk(this);
                // in memory bucket is never needed again
                m_hashBucketsInMemory[m_firstInMemoryHashBucket] = null;
                m_firstInMemoryHashBucket++;
            }
            // if there's no in-memory bucket, do nothing
        }

        /**
         * Signal that we're done processing hash input rows. Converts open hash buckets on disk from
         * {@link BufferedDataContainer}s to {@link BufferedDataTable}s.
         */
        public void closeHashBuckets() {
            Arrays.stream(m_hashBucketsOnDisk).filter(Objects::nonNull).forEach(DiskBucket::close);
        }

        public void closeProbeBuckets() {
            Arrays.stream(m_probeBuckets).forEach(DiskBucket::close);
        }

        /**
         * @return
         */
        public List<Pair<DiskBucket, DiskBucket>> unprocessedPartitions() {
            return IntStream.range(0, m_firstInMemoryHashBucket).mapToObj(
                partition -> new Pair<DiskBucket, DiskBucket>(m_probeBuckets[partition], m_hashBucketsOnDisk[partition]))
                .collect(Collectors.toList());
        }

        /**
         * @return all hash table partitions that were held in memory
         */
        public List<HashIndex> inMemoryHashPartitions(){
//            System.out.println("HybridHashJoin.DiskBackedHashPartitions.inMemoryHashPartitions() first in memory partition: " + m_firstInMemoryHashBucket);
            return IntStream.range(m_firstInMemoryHashBucket, m_numPartitions).mapToObj(i -> m_hashBucketsInMemory[i])
                .collect(Collectors.toList());
        }

        /** @return the number of hash buckets that are currently held in memory. */
        public int getNumInMemoryParititions() {
            return m_numPartitions - m_firstInMemoryHashBucket;
        }

        /** @return the number of partitions that stored on disk. */
        public int getNumPartitionsOnDisk() {
            return m_firstInMemoryHashBucket;
        }

        /**
         * Stores a partition of an input table (either probe or hash input), such that every row's join column value
         * combination in this partition hashes to the same value. A disk bucket is used in phase 3 to read the rows
         * that have been written to disk in phase 2 back into memory. <br/>
         * <br/>
         *
         * Internally, the rows are flushed to disk using a {@link BufferedDataTable} with a condensed spec that depends
         * on whether this is a partition for the probe input or the hash input and whether to store row offsets, see
         * {@link JoinTableSettings#condensed(boolean)}.
         *
         * @author Carl Witt, KNIME AG, Zurich, Switzerland
         */
        class DiskBucket {

            /**
             * The table from which the rows in this partition originate.
             */
            final JoinTableSettings m_superTable;

            /**
             * Defines the format of the table in disk layout and holds it ({@link JoinTableSettings#getTable()}) if at
             * least one row is added to this bucket.
             */
            final JoinTableSettings m_workingTable;

            /**
             * Buffer for flushing working rows to disk. Is null until the first row is added.
             * After {@link #close()}, the {@link BufferedDataTable} is stored in {@link #m_superTable}.
             */
            BufferedDataContainer m_container = null;

            /**
             * @param forTable the settings for the table for which this bucket is used. The format of the table in
             *            which to flush rows to disk, generated via {@link JoinTableSettings#condensed(boolean)}.
             *            Contains only the included columns and join columns and possibly a column for the row's offset
             *            if sorting of the results is required.
             */
            DiskBucket(final JoinTableSettings forTable) {
                m_superTable = forTable;
                m_workingTable = m_superTable.condensed(m_storeRowOffsets);
            }

            /**
             * This is called in
             * {@link HybridHashJoin#phase2()} to
             * flush rows to disk that do not fit into memory.
             * @param row
             * @param rowOffset
             */
            void add(final DataRow row, final long rowOffset) {
                // lazy initialization of the data container, only when rows are actually added
                if (m_container == null) {
//                    System.out.println(String.format("workingTableSpec=%s", m_workingTable.getTableSpec()));
                    m_container = m_exec.createDataContainer(m_workingTable.getTableSpec());
                }

                // remove unused columns and add row offset if necessary
//                System.out.println(String.format("row=%s", row));
                DataRow rowInDiskLayout = m_superTable.condensed(row, rowOffset, m_storeRowOffsets);
//                System.out.println(String.format("row In Disk Layout=%s", rowInDiskLayout));
                m_container.addRowToTable(rowInDiskLayout);

            }

            /**
             * Called at the end of
             * {@link HybridHashJoin#phase2()},
             * after the last probe row has been added to a probe disk bucket.
             */
            void close() {

                if(m_container != null) {
                    m_container.close();
                    m_workingTable.setTable(m_container.getTable());
                    m_container = null;
                }
            }

            /**
             * Called during {@link HybridHashJoin#phase3()}, to read the
             * working rows back from disk and outputs partial results in sorted chunk row format.
             *
             * @throws CanceledExecutionException
             */
            void join(final DiskBucket other, final JoinContainer container)
                throws CanceledExecutionException {

                JoinSpecification joinDiskBuckets = m_joinSpecification.specWith(m_workingTable, other.m_workingTable);
                container.setJoinSpecification(joinDiskBuckets);

                // perform an in-memory join that falls back to nested loop if necessary.
                // (a recursive hybrid hash join would fail if the partitioning attributes are constant)
                BlockHashJoin nestedLoop = new BlockHashJoin(m_exec, m_progress, joinDiskBuckets, m_storeRowOffsets);
                nestedLoop.join(container);

            }

            /**
             * @param diskBucket
             * @param outputContainer
             * @param createSubExecutionContext
             */
            public void joinStream(final DiskBucket diskBucket, final NWayMergeContainer outputContainer,
                final ExecutionContext createSubExecutionContext) {
                // TODO Auto-generated method stub

            }

        }

    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected StreamableFunction getStreamableFunction() {
        // TODO Auto-generated method stub
        return null;
    }

}
