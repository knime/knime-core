/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *
 * History
 *   22.02.2007 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.learner;

import java.util.ArrayList;
import java.util.List;

/**
 * This class determines the best binary split for a nominal attribute. The
 * split consists of subsets for both partitions.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class SplitNominalBinary extends SplitNominal {

    private static final int TRUE_PARTITION = 0;

    private static final int FALSE_PARTITION = 1;

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
     * Holds the partitioning as gray code.
     */
    private long m_grayCodeValuePartitioning;

    /**
     * The number of nominal values. Also represents the number of bits relevant
     * for the gray code partitioning codeing.
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
     * @param minObjectsCount the minimumn number of objects in at least two
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
        // calculate the partition count array and the allover count
        double alloverCount = 0.0;
        double[] partitionCounter = new double[2];
        double[] classCounter =
                new double[table.getClassFrequencyArray().length];
        m_valuePartitionValidCount = new double[2];
        double[][] partitionHistogram = new double[2][];
        partitionHistogram[TRUE_PARTITION] =
                new double[nominalHistogram.getNumClassValues()];
        partitionHistogram[FALSE_PARTITION] =
                new double[nominalHistogram.getNumClassValues()];
        for (double[] oneNominalValueHisto : histogram) {
            int i = 0;
            for (double classCount : oneNominalValueHisto) {
                // initially all data rows belong to the false partition
                partitionHistogram[FALSE_PARTITION][i] += classCount;
                partitionCounter[FALSE_PARTITION] += classCount;
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
            // imediately switch to the next increment, as the empty subset
            // makes no sense
            counter.increment();
            while (counter.hasNext()) {
                // calculate the split quality and remember this split
                // if it is the best

                // for this first get the last change index of the nominal
                // value (represented by the gray code counter)
                int nominalValueIndex =
                        counter.getLastChangedGrayCodeBitIndex();
                boolean truePartition = counter.lastBitSetTrue();
                // the partition prefix is set according to, whether the
                // values must be added to the true partition or to the false
                // one
                int partitionPrefix;
                if (truePartition) {
                    // remove from the false partition and add to the true
                    // partition, i.e. prefix is 1
                    partitionPrefix = 1;
                } else {
                    // remove fromt the true partition and add to the false
                    // partition, i.e. prefix is -1
                    partitionPrefix = -1;
                }
                int i = 0;
                for (double classCount : histogram[nominalValueIndex]) {
                    partitionHistogram[FALSE_PARTITION][i] -=
                            partitionPrefix * classCount;
                    partitionCounter[FALSE_PARTITION] -=
                            partitionPrefix * classCount;
                    partitionHistogram[TRUE_PARTITION][i] +=
                            partitionPrefix * classCount;
                    partitionCounter[TRUE_PARTITION] +=
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
                        m_valuePartitionValidCount[FALSE_PARTITION] =
                                partitionCounter[FALSE_PARTITION];
                        m_valuePartitionValidCount[TRUE_PARTITION] =
                                partitionCounter[TRUE_PARTITION];
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
                    // / to the false / true partition
                    int i = 0;
                    for (double classCount : histogram[nominalValueMapping]) {
                        partitionHistogram[FALSE_PARTITION][i] -= classCount;
                        partitionCounter[FALSE_PARTITION] -= classCount;
                        partitionHistogram[TRUE_PARTITION][i] += classCount;
                        partitionCounter[TRUE_PARTITION] += classCount;
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
                            m_valuePartitionValidCount[FALSE_PARTITION] =
                                    partitionCounter[FALSE_PARTITION];
                            m_valuePartitionValidCount[TRUE_PARTITION] =
                                    partitionCounter[TRUE_PARTITION];
                        }
                    }

                    // remove / add the current chosen nominal value counts from
                    // / to the true / false partition such that the situation
                    // for the next nominal value is as before
                    i = 0;
                    for (double classCount : histogram[nominalValueMapping]) {
                        partitionHistogram[FALSE_PARTITION][i] += classCount;
                        partitionCounter[FALSE_PARTITION] += classCount;
                        partitionHistogram[TRUE_PARTITION][i] -= classCount;
                        partitionCounter[TRUE_PARTITION] -= classCount;
                        i++;
                    }
                }
                // if the best found nominal mapping is -1 means no valid
                // nominal value split could be found so break the loop
                if (currentBestNominalValueMapping < 0) {
                    break;
                }
                // after one iteration over all nominal values of the false
                // partition, get the best one and adapt the partition and histo
                // counts for the next iteration
                int i = 0;
                for (double cCnt : histogram[currentBestNominalValueMapping]) {
                    partitionHistogram[FALSE_PARTITION][i] -= cCnt;
                    partitionCounter[FALSE_PARTITION] -= cCnt;
                    partitionHistogram[TRUE_PARTITION][i] += cCnt;
                    partitionCounter[TRUE_PARTITION] += cCnt;
                    i++;
                }
                // also adapt the false and true set
                setFalse.remove(new Integer(currentBestNominalValueMapping));
                if (setTrue.contains(currentBestNominalValueMapping)) {
                    throw new RuntimeException("Nominal value already added: "
                            + currentBestNominalValueMapping);
                }
                setTrue.add(currentBestNominalValueMapping);
            } while (Double.isNaN(bestQualityMeasure)
                    || splitQualityMeasure.isBetter(
                            currentBestQualityMeasure, bestQualityMeasure));

            // now we know that (according to the heuristic) the current false /
            // true partition are the best split sets, so convert the
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
        List<Integer> trueList = new ArrayList<Integer>();
        List<Integer> falseList = new ArrayList<Integer>();
        long mask = 1;
        for (int i = 0; i < numNominalValues; i++) {
            if ((grayCode & mask) > 0) {
                // this nominal mapping belongs to the true partition
                trueList.add(i);
            } else {
                falseList.add(i);
            }
            mask = mask << 1;
        }

        // convert the lists to the 2D array
        m_nominalValuePartitioning[TRUE_PARTITION] = new int[trueList.size()];
        for (int i = 0; i < trueList.size(); i++) {
            m_nominalValuePartitioning[TRUE_PARTITION][i] = trueList.get(i);
        }

        m_nominalValuePartitioning[FALSE_PARTITION] = new int[falseList.size()];
        for (int i = 0; i < falseList.size(); i++) {
            m_nominalValuePartitioning[FALSE_PARTITION][i] = falseList.get(i);
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
     * Binary nominal splits can be furhter used.
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
        // position; if yes, the true partition is returned else the false one
        int mask = 1 << valueMapping;
        if ((m_grayCodeValuePartitioning & mask) > 0) {
            return TRUE_PARTITION;
        } else {
            return FALSE_PARTITION;
        }
    }

    /**
     * Returns an array of integer mappings corresponding to the false partition
     * nominal values.
     *
     * @return an array of integer mappings corresponding to the false partition
     *         nominal values
     */
    public int[] getIntMappingsLeftPartition() {
        return m_nominalValuePartitioning[FALSE_PARTITION];
    }

    /**
     * Returns an array of integer mappings corresponding to the true partition
     * nominal values.
     *
     * @return an array of integer mappings corresponding to the true partition
     *         nominal values
     */
    public int[] getIntMappingsRightPartition() {
        return m_nominalValuePartitioning[TRUE_PARTITION];
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
         * Creates a gray code counter with the specified bit lenngth. The
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
         * Returns true, if this counter has not reached its maximum value yet.
         *
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
         * Returns the index of the bit that was changed during the last
         * increment operation.
         *
         * @return the index of the bit that was changed during the last
         *         increment operation
         */
        public int getLastChangedGrayCodeBitIndex() {
            return m_lastChangedGrayCodeBitIndex;
        }

        /**
         * Returns true, if the last changed bit in the gray code was changed to
         * true (1). Fals, if the bit was set to false (0).
         *
         * @return true, if the last changed bit in the gray code was changed to
         *         true (1), fals, if the bit was set to false (0)
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
     * Main.
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
