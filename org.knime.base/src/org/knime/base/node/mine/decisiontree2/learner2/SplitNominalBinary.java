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
 *
 * History
 *   22.02.2007 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.learner2;

import java.util.ArrayList;
import java.util.List;

/**
 * This class determines the best binary split for a nominal attribute. The
 * split consists of subsets for both partitions.
 *
 * @author Christoph Sieb, University of Konstanz
 * 
 * @since 2.6
 */
public class SplitNominalBinary extends SplitNominal {

    /** index for left partition of a binary nominal split. */
    // used to be TRUE_PARTITION
    public static final int LEFT_PARTITION = 0;

    /** index for right partition of a binary nominal split. */
    //used to be FALSE_PARTITION
    public static final int RIGHT_PARTITION = 1;

    /**
     * The partition count for the valid (non-missing) nominal values.
     */
    private double[] m_valuePartitionValidCount;

    /**
     * The count for all valid (non-missing) nominal values.
     */
    private double m_alloverValidCount;

    /**
     * Consists of two integer arrays that hold the nominal mappings for each
     * partition.
     */
    private int[][] m_nominalValuePartitioning;

    /**
     * Holds the partitioning as gray code. Sounds complicated but allows
     * to iterate over all possible partitions moving only one value in (or
     * out) at each step which makes the new entropy calculations easier.
     */
    private long m_grayCodeValuePartitioning;

    /**
     * The number of nominal values. Also represents the number of bits relevant
     * for the gray code partitioning encoding. Note that this only works for
     * up to 63 bits (=nominal values) which is gigantic enough for a full
     * search anyway.
     */
    private int m_numNominalValues;

    /**
     * Constructs the best split for the given nominal attribute. The results
     * can be retrieved from getter methods.
     *
     * @param table the attribute list for which to create the split
     * @param attributeIndex the index of the attribute for which to calculate
     *            the split
     * @param splitQualityMeasure the split quality measure (e.g. gini or gain
     *            ratio)
     * @param minObjectsCount the minimum number of objects in at least two
     *            partitions
     * @param maxNumDifferentValues the maximum number of different nominal
     *            values for which all possible subsets are calculated; if above
     *            this threshold, a heuristic is applied
     */
    public SplitNominalBinary(final InMemoryTable table,
            final int attributeIndex,
            final SplitQualityMeasure splitQualityMeasure,
            final double minObjectsCount, final int maxNumDifferentValues) {

        super(table, attributeIndex, splitQualityMeasure);

        assert table.isNominal(attributeIndex);

        // get the counting histogram for this nominal attribute
        NominalValueHistogram nominalHistogram =
                table.getNominalValueHistogram(attributeIndex);
        double missingValueCount = nominalHistogram.getMissingValueCount();
        // get the underlying basic 2D array
        double[][] histogram = nominalHistogram.getHistogram();
        m_numNominalValues = histogram.length;
        // calculate the partition count array and the total count
        double alloverCount = 0.0;
        double[] partitionCounter = new double[2];
        double[] classCounter =
                new double[table.getClassFrequencyArray().length];
        m_valuePartitionValidCount = new double[2];
        double[][] partitionHistogram = new double[2][];
        partitionHistogram[LEFT_PARTITION] =
                new double[nominalHistogram.getNumClassValues()];
        partitionHistogram[RIGHT_PARTITION] =
                new double[nominalHistogram.getNumClassValues()];
        for (double[] oneNominalValueHisto : histogram) {
            int i = 0;
            for (double classCount : oneNominalValueHisto) {
                // initially all data rows belong to the right (false) partition
                // that is our left set contains no values at all at start.
                partitionHistogram[RIGHT_PARTITION][i] += classCount;
                partitionCounter[RIGHT_PARTITION] += classCount;
                classCounter[i] += classCount;
                alloverCount += classCount;
                i++;
            }
        }

        // init the quality measure
        m_splitQualityMeasure.initQualityMeasure(classCounter, alloverCount);

        // init: not a number means this is no valid split
        double bestQualityMeasure = Double.NaN;

        // if the number of different nominal values does not exceed
        // the specified threshold, all possible subsets are evaluated
        if (m_numNominalValues <= maxNumDifferentValues) {
            GrayCodeCounter counter =
                    new GrayCodeCounter(m_numNominalValues, true);
            long bestGrayCode = counter.getGrayCode();
            // immediately switch to the next increment, as the empty subset
            // on the left side makes no sense
            counter.increment();
            while (counter.hasNext()) {
                // calculate the split quality and remember this split
                // if it is the best

                // for this first get the last changed index of the nominal
                // value (represented by the gray code counter). We know that
                // only one value was moved from left to right (or vice versa)
                // since this is the beauty of gray code counting!
                int nominalValueIndex =
                        counter.getLastChangedGrayCodeBitIndex();
                boolean truePartition = counter.lastBitSetTrue();
                // the partition prefix tells us whether the
                // values must be added to the left or right
                // partition
                int partitionPrefix;
                if (truePartition) {
                    // remove from the right partition and add to the left
                    // partition, i.e. prefix is 1
                    partitionPrefix = 1;
                } else {
                    // remove from the left partition and add to the right
                    // partition (prefix -1)
                    partitionPrefix = -1;
                }
                int i = 0;
                for (double classCount : histogram[nominalValueIndex]) {
                    partitionHistogram[RIGHT_PARTITION][i] -=
                            partitionPrefix * classCount;
                    partitionCounter[RIGHT_PARTITION] -=
                            partitionPrefix * classCount;
                    partitionHistogram[LEFT_PARTITION][i] +=
                            partitionPrefix * classCount;
                    partitionCounter[LEFT_PARTITION] +=
                            partitionPrefix * classCount;
                    i++;
                }

                // now calculate the quality measure if there are enough
                // examples for each partition
                if (enoughExamplesPerPartition(partitionCounter,
                        minObjectsCount)) {
                    double qualityMeasure =
                            m_splitQualityMeasure.measureQuality(
                                    alloverCount, partitionCounter,
                                    partitionHistogram, missingValueCount);
                    if (Double.isNaN(bestQualityMeasure)
                            || splitQualityMeasure.isBetter(qualityMeasure,
                                    bestQualityMeasure)) {
                        bestQualityMeasure = qualityMeasure;
                        // also remember the subsets
                        bestGrayCode = counter.getGrayCode();
                        // save the partition count to the member variable
                        m_valuePartitionValidCount[RIGHT_PARTITION] =
                                partitionCounter[RIGHT_PARTITION];
                        m_valuePartitionValidCount[LEFT_PARTITION] =
                                partitionCounter[LEFT_PARTITION];
                    }
                }

                // increment to the next subset
                counter.increment();
            }

            // convert the binary (long) gray code representation of
            // the partitioning by a 2D array representation
            m_grayCodeValuePartitioning = bestGrayCode;
            convertGrayCodeToPartitioning(bestGrayCode, m_numNominalValues);
        } else {
            // if the number of different nominal values exceeds
            // the specified threshold, not all possible subsets are evaluated
            // but a heuristic is applied; the heuristic takes the best 1 subset
            // then based on the best 1 subset the best 2 subset as long as
            // the quality index improves, if not it stops; this greedy
            // algorithm guarantees moderate runtime for many nominal values

            // create two sets; the first contains initially all nominal values
            // (i.e. its mappings) the second is empty
            List<Integer> setFalse = new ArrayList<Integer>();
            for (int i = 0; i < m_numNominalValues; i++) {
                setFalse.add(i);
            }
            List<Integer> setTrue = new ArrayList<Integer>();
            double currentBestQualityMeasure = Double.NaN;
            int currentBestNominalValueMapping = -1;
            do {
                // first set the current best measure to the best one
                bestQualityMeasure = currentBestQualityMeasure;
                // and reset the current best nominal value mapping
                currentBestNominalValueMapping = -1;
                // at the beginning of each iteration the histo and counter
                // arrays represent the true and false set
                // then all values from the the false set are assumed to belong
                // to the true set now the best one (if also better than the
                // allover best quality measure)
                // is then moved to the true set and the histo and counter array
                // is permanently adapted for the next iteration
                for (int nominalValueMapping : setFalse) {
                    // remove / add the current chosen nominal value counts from
                    // / to the right / left partition
                    int i = 0;
                    for (double classCount : histogram[nominalValueMapping]) {
                        partitionHistogram[RIGHT_PARTITION][i] -= classCount;
                        partitionCounter[RIGHT_PARTITION] -= classCount;
                        partitionHistogram[LEFT_PARTITION][i] += classCount;
                        partitionCounter[LEFT_PARTITION] += classCount;
                        i++;
                    }

                    // now calculate the quality measure if there are enough
                    // examples for each partition
                    if (enoughExamplesPerPartition(partitionCounter,
                            minObjectsCount)) {
                        double qualityMeasure =
                                m_splitQualityMeasure.measureQuality(
                                        alloverCount, partitionCounter,
                                        partitionHistogram, missingValueCount);
                        if (Double.isNaN(currentBestQualityMeasure)
                                || splitQualityMeasure.isBetter(
                                        qualityMeasure,
                                        currentBestQualityMeasure)) {
                            currentBestQualityMeasure = qualityMeasure;
                            currentBestNominalValueMapping =
                                    nominalValueMapping;
                            // save the partition count to the member variable
                            m_valuePartitionValidCount[RIGHT_PARTITION] =
                                    partitionCounter[RIGHT_PARTITION];
                            m_valuePartitionValidCount[LEFT_PARTITION] =
                                    partitionCounter[LEFT_PARTITION];
                        }
                    }

                    // remove / add the current chosen nominal value counts from
                    // / to the left / right partition such that the situation
                    // for the next nominal value is as before
                    i = 0;
                    for (double classCount : histogram[nominalValueMapping]) {
                        partitionHistogram[RIGHT_PARTITION][i] += classCount;
                        partitionCounter[RIGHT_PARTITION] += classCount;
                        partitionHistogram[LEFT_PARTITION][i] -= classCount;
                        partitionCounter[LEFT_PARTITION] -= classCount;
                        i++;
                    }
                }
                // if the best found nominal mapping is -1 means no valid
                // nominal value split could be found so break the loop
                if (currentBestNominalValueMapping < 0) {
                    break;
                }
                // after one iteration over all nominal values of the right
                // partition, get the best one and adapt the partition and
                // histogram counts for the next iteration
                int i = 0;
                for (double cCnt : histogram[currentBestNominalValueMapping]) {
                    partitionHistogram[RIGHT_PARTITION][i] -= cCnt;
                    partitionCounter[RIGHT_PARTITION] -= cCnt;
                    partitionHistogram[LEFT_PARTITION][i] += cCnt;
                    partitionCounter[LEFT_PARTITION] += cCnt;
                    i++;
                }
                // also adapt the right and left set
                setFalse.remove(new Integer(currentBestNominalValueMapping));
                if (setTrue.contains(currentBestNominalValueMapping)) {
                    throw new RuntimeException("Nominal value already added: "
                            + currentBestNominalValueMapping);
                }
                setTrue.add(currentBestNominalValueMapping);
            } while (Double.isNaN(bestQualityMeasure)
                    || splitQualityMeasure.isBetter(
                            currentBestQualityMeasure, bestQualityMeasure));

            // now we know that (according to the heuristic) the current left /
            // right partition are the best split sets, so convert the
            // lists to a grey code (long number) representation, then convert
            // this to the array representation
            m_grayCodeValuePartitioning = 0;
            for (int mappingFalse : setTrue) {
                long mask = 1 << mappingFalse;
                m_grayCodeValuePartitioning |= mask;
            }
            convertGrayCodeToPartitioning(m_grayCodeValuePartitioning,
                    m_numNominalValues);
        }

        m_alloverValidCount = alloverCount;
        if (!Double.isNaN(bestQualityMeasure) && bestQualityMeasure != 0.0) {
            bestQualityMeasure =
                    m_splitQualityMeasure.postProcessMeasure(
                            bestQualityMeasure, alloverCount,
                            m_valuePartitionValidCount, missingValueCount);
        }
        setBestQualityMeasure(bestQualityMeasure);
    }

    /**
     * Converts the gray code representation of the partitioning to the 2D int
     * array representation (stored in a member variable).
     *
     * @param grayCode the gray code to convert
     * @param numNominalValues the number of different nominal values, i.e. the
     *            required bit length of the gray code (each bit represents a
     *            nominal value)
     */
    private void convertGrayCodeToPartitioning(final long grayCode,
            final int numNominalValues) {
        m_nominalValuePartitioning = new int[2][];
        List<Integer> leftList = new ArrayList<Integer>();  // was trueList
        List<Integer> rightList = new ArrayList<Integer>();  // was falseList
        long mask = 1;
        for (int i = 0; i < numNominalValues; i++) {
            if ((grayCode & mask) > 0) {
                // this nominal mapping belongs to the left (true) partition
                leftList.add(i);
            } else {
                rightList.add(i);
            }
            mask = mask << 1;
        }

        // convert the lists to the 2D array
        m_nominalValuePartitioning[LEFT_PARTITION] = new int[leftList.size()];
        for (int i = 0; i < leftList.size(); i++) {
            m_nominalValuePartitioning[LEFT_PARTITION][i] = leftList.get(i);
        }

        m_nominalValuePartitioning[RIGHT_PARTITION] = new int[rightList.size()];
        for (int i = 0; i < rightList.size(); i++) {
            m_nominalValuePartitioning[RIGHT_PARTITION][i] = rightList.get(i);
        }
    }

    /**
     * Checks if there are at least two partitions with at least the given
     * minimum number of objects.
     *
     * @param partitionCounter the array with the counts of the partitions
     * @param minObjectsCount the min number of objects
     * @return true if there are at least two partitions with at least the given
     *         minimum number of objects
     */
    private static boolean enoughExamplesPerPartition(
            final double[] partitionCounter, final double minObjectsCount) {
        int count = 0;
        for (int i = 0; i < partitionCounter.length; i++) {
            if (partitionCounter[i] >= minObjectsCount) {
                count++;
            }
        }
        return count >= 2;
    }

    /**
     * The number of partitions of a binary nominal split is 2.
     *
     * {@inheritDoc}
     */
    @Override
    public int getNumberPartitions() {
        return 2;
    }

    /**
     * Binary nominal splits can be further used.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean canBeFurtherUsed() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPartitionForRow(final DataRowWeighted row) {
        double value = row.getValue(getAttributeIndex());
        if (Double.isNaN(value)) {
            return -1;
        }
        int valueMapping = (int)value;
        // create the mask that corresponds to the bit at the position of
        // the valueMapping, then check if the gray code is true at that
        // position; if yes, the index of the left partition is returned
        // otherwise the right one
        int mask = 1 << valueMapping;
        if ((m_grayCodeValuePartitioning & mask) > 0) {
            return LEFT_PARTITION;
        } else {
            return RIGHT_PARTITION;
        }
    }

    /**
     * Returns an array of integer mappings corresponding to the right partition
     * nominal values. (This used to be the "false" partition.)
     *
     * @return an array of integer mappings corresponding to the right partition
     *         nominal values
     */
    public int[] getIntMappingsRightPartition() {
        return m_nominalValuePartitioning[RIGHT_PARTITION];
    }

    /**
     * Returns an array of integer mappings corresponding to the left partition
     * nominal values. (This used to be the "true" partition.)
     *
     * @return an array of integer mappings corresponding to the left partition
     *         nominal values
     */
    public int[] getIntMappingsLeftPartition() {
        return m_nominalValuePartitioning[LEFT_PARTITION];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getPartitionWeights() {
        double[] weights = new double[getNumberPartitions()];
        for (int i = 0; i < weights.length; i++) {
            weights[i] = m_valuePartitionValidCount[i] / m_alloverValidCount;
        }
        return weights;
    }

    /**
     * This class is used to enumerate all nominal subsets in a binary gray code
     * manner. As the nominal values are encoded by integers the encoding is
     * performed as one bit for each value. I.e. 5 nominal values results in a
     * binary number with 5 digits. There exist 2^n subsets (the empty set and
     * the full set are not interesting, i.e. 2^n - 2 subsets) and thus 2^n gray
     * codes. The advantage of gray codes is that only one bit changes during
     * counting. Thus, the computation can be done by simply adding / removing
     * the counts for a given nominal value from the two partitions. Therefore
     * this {@link GrayCodeCounter} enable consecutive retrieval of the gray
     * codes and also the index of the bit which was last changed. The index
     * represents then the mapping of the nominal value.
     *
     * @author Christoph Sieb, University of Konstanz
     */
    private static class GrayCodeCounter {

        private long m_grayCodeCounter;

        private long m_incrementCounter;

        private long m_maxNumIncrements;

        private int m_lastChangedGrayCodeBitIndex;

        /**
         * Creates a gray code counter with the specified bit length. The
         * maximum length is 63 (the 64th bit is used as counting border). I.e.
         * at most for 63 different nominal values the subsets can be
         * calculated. This is enough, as the calculation time for more than 15
         * bits is prohibitive high. More than 15 different nominal values must
         * be calculated with a heuristic.
         *
         * @param length the length of this gray code counter
         * @param noComplementary if true, only half the gray codes are created,
         *            as the other half are the complementary sets
         */
        public GrayCodeCounter(final int length,
                final boolean noComplementary) {
            if (length > 63) {
                throw new IllegalArgumentException("The maximum length is 63!");
            }
            m_grayCodeCounter = 0;
            m_incrementCounter = 0;
            m_maxNumIncrements = 1 << length;
            if (noComplementary) {
                m_maxNumIncrements = m_maxNumIncrements / 2;
            }
            m_lastChangedGrayCodeBitIndex = -1;
        }

        /**
         * @return true, if this counter has not reached its maximum value yet
         */
        public boolean hasNext() {
            return m_incrementCounter < m_maxNumIncrements;
        }

        /**
         * Returns the gray code as long value. The bits of the long represent
         * the binary gray code.
         *
         * @return the gray code as long value
         */
        public long getGrayCode() {
            return m_grayCodeCounter;
        }

        /**
         * Returns the position of this counter. The position is the number of
         * increments that have been taken place so far.
         * 
         * @return current position of this counter
         */
        public long getNumIncrements() {
            return m_incrementCounter;
        }

        /**
         * Increments this gray counter according to the gray code properties.
         */
        public void increment() {
            long binaryBitsThatChanged =
                    m_incrementCounter ^ (m_incrementCounter + 1);

            long lastChangedGrayCodeBit =
                    binaryBitsThatChanged ^ (binaryBitsThatChanged >> 1);
            m_grayCodeCounter = m_grayCodeCounter ^ lastChangedGrayCodeBit;
            m_lastChangedGrayCodeBitIndex =
                    getFirstSetBitIndex(lastChangedGrayCodeBit);
            m_incrementCounter++;
        }

        /**
         * @return the index of the bit that was changed during the last
         *         increment operation
         */
        public int getLastChangedGrayCodeBitIndex() {
            return m_lastChangedGrayCodeBitIndex;
        }

        /**
         * @return true, if the last changed bit in the gray code was changed to
         *         true (1), false, if the bit was set to false (0)
         */
        public boolean lastBitSetTrue() {
            int lastChangedIndex = getLastChangedGrayCodeBitIndex();
            if (lastChangedIndex < 0) {
                return false;
            }
            long mask = 1 << lastChangedIndex;
            return (m_grayCodeCounter & mask) > 0;
        }

        private int getFirstSetBitIndex(final long number) {
            if (number <= 0) {
                return -1;
            }
            long mask = 1;
            int shiftCount = 0;
            while ((number & mask) == 0) {
                mask = mask << 1;
                shiftCount++;
            }
            return shiftCount;
        }
    }

    /**
     * Main. Small test for GrayCodeCounter.
     * 
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        GrayCodeCounter counter = new GrayCodeCounter(4, false);
        while (counter.hasNext()) {
            System.out.print(counter.getLastChangedGrayCodeBitIndex() + ":"
                    + counter.lastBitSetTrue() + ":");
            System.out.println(Long.toBinaryString(counter.getGrayCode()));
            counter.increment();
        }
    }
}
