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
 *   Jan 9, 2015 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.data;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;

import org.apache.commons.math.random.RandomData;
import org.junit.Test;
import org.knime.base.node.mine.treeensemble2.data.memberships.DataMemberships;
import org.knime.base.node.mine.treeensemble2.data.memberships.DefaultDataIndexManager;
import org.knime.base.node.mine.treeensemble2.data.memberships.IDataIndexManager;
import org.knime.base.node.mine.treeensemble2.data.memberships.RootDataMemberships;
import org.knime.base.node.mine.treeensemble2.learner.NominalBinarySplitCandidate;
import org.knime.base.node.mine.treeensemble2.learner.NominalMultiwaySplitCandidate;
import org.knime.base.node.mine.treeensemble2.learner.SplitCandidate;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeEnsembleModel.TreeType;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeNominalBinaryCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeNominalBinaryCondition.SetLogic;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeNominalCondition;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration.ColumnSamplingMode;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration.MissingValueHandling;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration.SplitCriterion;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.Pair;

/**
 *
 * @author wiswedel
 */
public class TreeNominalColumnDataTest {

    // S - sunny, O - overcast, R - rain
    private static final String[] SMALL_COLUMN_DATA =
        TreeNumericColumnDataTest.asStringArray("S,S,O,R,R,R,O,S,S,R,S,O,O,R");

    // H - hot, M - mild, C - cold
    private static final String[] SMALL_TARGET_DATA =
            TreeNumericColumnDataTest.asStringArray("H,H,H,M,C,C,C,M,C,M,M,M,H,M");

    private static final double[] SMALL_TARGET_DATA_REGRESSION =
        new double[]{30, 32, 27, 25, 20, 17, 16, 23, 19, 25, 24, 21, 33, 22};

    private static final int[] TWO_CLASS_INDICES = new int[]{0, 1, 2, 3, 7, 9, 10, 11, 12, 13};

    private static Pair<TreeNominalColumnData, TreeTargetNominalColumnData>
        tennisData(final TreeEnsembleLearnerConfiguration config) {
        TestDataGenerator dataGen = new TestDataGenerator(config);
        final TreeNominalColumnData testColData = dataGen.createNominalAttributeColumn(SMALL_COLUMN_DATA, "test-col", 0);
        final TreeTargetNominalColumnData target = TestDataGenerator.createNominalTargetColumn(SMALL_TARGET_DATA);
        testColData.getMetaData().setAttributeIndex(0);
        return Pair.create(testColData, target);
    }

    private static Pair<TreeNominalColumnData, TreeTargetNumericColumnData>
        tennisDataRegression(final TreeEnsembleLearnerConfiguration config) {
        DataColumnSpec colSpec = new DataColumnSpecCreator("test-col", StringCell.TYPE).createSpec();
        TreeNominalColumnDataCreator colCreator = new TreeNominalColumnDataCreator(colSpec);
        DataColumnSpec targetSpec = new DataColumnSpecCreator("target-col", DoubleCell.TYPE).createSpec();
        TreeTargetColumnDataCreator targetCreator = new TreeTargetNumericColumnDataCreator(targetSpec);
        for (int i = 0; i < SMALL_COLUMN_DATA.length; i++) {
            final RowKey key = RowKey.createRowKey((long)i);
            colCreator.add(key, new StringCell(SMALL_COLUMN_DATA[i]));
            targetCreator.add(key, new DoubleCell(SMALL_TARGET_DATA_REGRESSION[i]));
        }
        final TreeNominalColumnData testColData = colCreator.createColumnData(0, config);
        testColData.getMetaData().setAttributeIndex(0);
        return Pair.create(testColData, (TreeTargetNumericColumnData)targetCreator.createColumnData());
    }

    private static Pair<TreeNominalColumnData, TreeTargetNominalColumnData>
        createPCATestData(final TreeEnsembleLearnerConfiguration config) {
        DataColumnSpec colSpec = new DataColumnSpecCreator("test-col", StringCell.TYPE).createSpec();
        final String[] attVals = new String[]{"A", "B", "C", "D", "E"};
        final String[] classes = new String[]{"T1", "T2", "T3"};
        TreeNominalColumnDataCreator colCreator = new TreeNominalColumnDataCreator(colSpec);
        DataColumnSpecCreator specCreator = new DataColumnSpecCreator("target-col", StringCell.TYPE);
        specCreator.setDomain(new DataColumnDomainCreator(
            Arrays.stream(classes).distinct().map(s -> new StringCell(s)).toArray(i -> new StringCell[i])).createDomain());
        DataColumnSpec targetSpec = specCreator.createSpec();
        TreeTargetColumnDataCreator targetCreator = new TreeTargetNominalColumnDataCreator(targetSpec);
        long rowKeyCounter = 0;
        final int[][] classDistributions =
            new int[][]{{40, 10, 10}, {10, 40, 10}, {20, 30, 10}, {20, 15, 25}, {10, 5, 45}};

        for (int i = 0; i < attVals.length; i++) {
            for (int j = 0; j < classes.length; j++) {
                for (int k = 0; k < classDistributions[i][j]; k++) {
                    RowKey key = RowKey.createRowKey(rowKeyCounter++);
                    colCreator.add(key, new StringCell(attVals[i]));
                    targetCreator.add(key, new StringCell(classes[j]));
                }
            }
        }
        final TreeNominalColumnData testColData = colCreator.createColumnData(0, config);
        testColData.getMetaData().setAttributeIndex(0);
        return Pair.create(testColData, (TreeTargetNominalColumnData)targetCreator.createColumnData());
    }

    private static String[] extractSubArray(final String[] values, final int[] indices) {
        String[] extracted = new String[indices.length];
        for (int i = 0; i < indices.length; i++) {
            extracted[i] = values[indices[i]];
        }
        return extracted;
    }

    private static Pair<TreeNominalColumnData, TreeTargetNominalColumnData>
        twoClassTennisData(final TreeEnsembleLearnerConfiguration config) {
        TestDataGenerator testGen = new TestDataGenerator(config);
        TreeTargetNominalColumnData targetCol = TestDataGenerator.createNominalTargetColumn(extractSubArray(SMALL_TARGET_DATA, TWO_CLASS_INDICES));
        final TreeNominalColumnData testColData = testGen.createNominalAttributeColumn(extractSubArray(SMALL_COLUMN_DATA, TWO_CLASS_INDICES), "test-col", 0);
        testColData.getMetaData().setAttributeIndex(0);
        return Pair.create(testColData, targetCol);

    }

    private static TreeData tennisTreeData(final TreeEnsembleLearnerConfiguration config) {
        Pair<TreeNominalColumnData, TreeTargetNominalColumnData> cols = tennisData(config);
        TreeNominalColumnData nomCol = cols.getFirst();
        TreeTargetNominalColumnData tarCol = cols.getSecond();

        return new TreeData(new TreeAttributeColumnData[]{nomCol}, tarCol, TreeType.Ordinary);
    }

    private static TreeData twoClassTennisTreeData(final TreeEnsembleLearnerConfiguration config) {
        Pair<TreeNominalColumnData, TreeTargetNominalColumnData> cols = twoClassTennisData(config);
        TreeNominalColumnData nomCol = cols.getFirst();
        TreeTargetNominalColumnData tarCol = cols.getSecond();

        return new TreeData(new TreeAttributeColumnData[]{nomCol}, tarCol, TreeType.Ordinary);
    }

    private static TreeData createTreeData(final Pair<TreeNominalColumnData, TreeTargetNominalColumnData> cols) {
        TreeAttributeColumnData nomCol = cols.getFirst();
        TreeTargetColumnData tarCol = cols.getSecond();

        return new TreeData(new TreeAttributeColumnData[]{nomCol}, tarCol, TreeType.Ordinary);
    }

    private static TreeData
        createTreeDataRegression(final Pair<TreeNominalColumnData, TreeTargetNumericColumnData> cols) {
        TreeAttributeColumnData nomCol = cols.getFirst();
        TreeTargetColumnData tarCol = cols.getSecond();

        return new TreeData(new TreeAttributeColumnData[]{nomCol}, tarCol, TreeType.Ordinary);
    }

    private static TreeEnsembleLearnerConfiguration createConfig(final boolean isRegression) throws InvalidSettingsException {
        final TreeEnsembleLearnerConfiguration config = new TreeEnsembleLearnerConfiguration(isRegression);
        config.setColumnSamplingMode(ColumnSamplingMode.None);
        config.setDataSelectionWithReplacement(false);
        config.setNrModels(1);
        config.setUseDifferentAttributesAtEachNode(false);
        config.setDataFractionPerTree(1.0);
        config.setUseBinaryNominalSplits(true);
        if (!isRegression) {
            config.setSplitCriterion(SplitCriterion.Gini);
        }
        return config;
    }

    /**
     * Creates a MockDataColMem object. <br>
     * This DataMemberships object assumes the case that the data column and the target have the same ordering
     * and the weight of all rows is 1.0.
     * @param length of the column
     * @return MockDataColMem that can be used for unit tests.
     */
    private static DataMemberships createMockDataMemberships(final int length) {
        final double[] weights = new double[length];
        Arrays.fill(weights, 1.0);
        final int[] indices = new int[length];
        for (int i = 0; i < length; i++) {
            indices[i] = i;
        }
        return new MockDataColMem(indices, indices, weights);
    }

    /**
     * Tests the method
     * {@link TreeNominalColumnData#calcBestSplitClassification(DataMemberships, ClassificationPriors, TreeTargetNominalColumnData, RandomData)}
     * in case of a two class problem.
     *
     * @throws Exception
     */
    @Test
    public void testCalcBestSplitClassificationBinaryTwoClass() throws Exception {
        TreeEnsembleLearnerConfiguration config = createConfig(false);
        config.setMissingValueHandling(MissingValueHandling.Surrogate);
        Pair<TreeNominalColumnData, TreeTargetNominalColumnData> twoClassTennisData = twoClassTennisData(config);
        TreeNominalColumnData columnData = twoClassTennisData.getFirst();
        TreeTargetNominalColumnData targetData = twoClassTennisData.getSecond();
        TreeData twoClassTennisTreeData = twoClassTennisTreeData(config);
        IDataIndexManager indexManager = new DefaultDataIndexManager(twoClassTennisTreeData);
        assertEquals(SplitCriterion.Gini, config.getSplitCriterion());
        double[] rowWeights = new double[TWO_CLASS_INDICES.length];
        Arrays.fill(rowWeights, 1.0);
        //        DataMemberships dataMemberships = TestDataGenerator.createMockDataMemberships(TWO_CLASS_INDICES.length);
        DataMemberships dataMemberships = new RootDataMemberships(rowWeights, twoClassTennisTreeData, indexManager);
        ClassificationPriors priors = targetData.getDistribution(rowWeights, config);
        SplitCandidate splitCandidate =
            columnData.calcBestSplitClassification(dataMemberships, priors, targetData, null);
        assertNotNull(splitCandidate);
        assertThat(splitCandidate, instanceOf(NominalBinarySplitCandidate.class));
        assertTrue(splitCandidate.canColumnBeSplitFurther());
        assertEquals(0.1371428, splitCandidate.getGainValue(), 0.00001); // manually via open office calc
        NominalBinarySplitCandidate binSplitCandidate = (NominalBinarySplitCandidate)splitCandidate;
        TreeNodeNominalBinaryCondition[] childConditions = binSplitCandidate.getChildConditions();
        assertEquals(2, childConditions.length);
        assertArrayEquals(new String[]{"R"}, childConditions[0].getValues());
        assertArrayEquals(new String[]{"R"}, childConditions[1].getValues());
        assertEquals(SetLogic.IS_NOT_IN, childConditions[0].getSetLogic());
        assertEquals(SetLogic.IS_IN, childConditions[1].getSetLogic());
        assertFalse(childConditions[0].acceptsMissings());
        assertFalse(childConditions[1].acceptsMissings());
    }

    /**
     * Tests the XGBoost Missing value handling in case of a two class problem <br>
     * currently not tested because missing value handling will probably be implemented differently.
     *
     * @throws Exception
     */
    //    @Test
    public void testCalcBestSplitCassificationBinaryTwoClassXGBoostMissingValue() throws Exception {
        final TreeEnsembleLearnerConfiguration config = createConfig(false);
        config.setMissingValueHandling(MissingValueHandling.XGBoost);
        final TestDataGenerator dataGen = new TestDataGenerator(config);
        // check correct behavior if no missing values are encountered during split search
        Pair<TreeNominalColumnData, TreeTargetNominalColumnData> twoClassTennisData = twoClassTennisData(config);
        TreeData treeData = dataGen.createTreeData(twoClassTennisData.getSecond(), twoClassTennisData.getFirst());
        IDataIndexManager indexManager = new DefaultDataIndexManager(treeData);
        double[] rowWeights = new double[TWO_CLASS_INDICES.length];
        Arrays.fill(rowWeights, 1.0);
        //        DataMemberships dataMemberships = TestDataGenerator.createMockDataMemberships(TWO_CLASS_INDICES.length);
        DataMemberships dataMemberships = new RootDataMemberships(rowWeights, treeData, indexManager);
        TreeTargetNominalColumnData targetData = twoClassTennisData.getSecond();
        TreeNominalColumnData columnData = twoClassTennisData.getFirst();
        ClassificationPriors priors = targetData.getDistribution(rowWeights, config);
        RandomData rd = TestDataGenerator.createRandomData();
        SplitCandidate splitCandidate = columnData.calcBestSplitClassification(dataMemberships, priors, targetData, rd);
        assertNotNull(splitCandidate);
        assertThat(splitCandidate, instanceOf(NominalBinarySplitCandidate.class));
        NominalBinarySplitCandidate binarySplitCandidate = (NominalBinarySplitCandidate)splitCandidate;
        TreeNodeNominalBinaryCondition[] childConditions = binarySplitCandidate.getChildConditions();
        assertEquals(2, childConditions.length);
        assertArrayEquals(new String[]{"R"}, childConditions[0].getValues());
        assertArrayEquals(new String[]{"R"}, childConditions[1].getValues());
        assertEquals(SetLogic.IS_NOT_IN, childConditions[0].getSetLogic());
        assertEquals(SetLogic.IS_IN, childConditions[1].getSetLogic());
        // check if missing values go left
        assertTrue(childConditions[0].acceptsMissings());
        assertFalse(childConditions[1].acceptsMissings());
        // check correct behavior if missing values are encountered during split search
        String dataContainingMissingsCSV = "S,?,O,R,S,R,S,O,O,?";
        columnData =
            dataGen.createNominalAttributeColumn(dataContainingMissingsCSV, "column containing missing values", 0);
        treeData = dataGen.createTreeData(targetData, columnData);
        indexManager = new DefaultDataIndexManager(treeData);
        dataMemberships = new RootDataMemberships(rowWeights, treeData, indexManager);
        splitCandidate = columnData.calcBestSplitClassification(dataMemberships, priors, targetData, null);
        assertNotNull(splitCandidate);
        binarySplitCandidate = (NominalBinarySplitCandidate)splitCandidate;
        assertEquals("Gain was not as expected", 0.08, binarySplitCandidate.getGainValue(), 1e-8);
        childConditions = binarySplitCandidate.getChildConditions();
        String[] conditionValues = new String[]{"O", "?"};
        assertArrayEquals("Values in nominal condition did not match", conditionValues, childConditions[0].getValues());
        assertArrayEquals("Values in nominal condition did not match", conditionValues, childConditions[1].getValues());
        assertEquals("Wrong set logic.", SetLogic.IS_NOT_IN, childConditions[0].getSetLogic());
        assertEquals("Wrong set logic.", SetLogic.IS_IN, childConditions[1].getSetLogic());
        assertFalse("Missig values are not sent to the correct child.", childConditions[0].acceptsMissings());
        assertTrue("Missig values are not sent to the correct child.", childConditions[1].acceptsMissings());
    }

    /**
     * Tests the XGBoost missing value handling variant, where for each split it is tried which direction for missing
     * values provides the better gain.
     *
     * @throws Exception
     */
    @Test
    public void testCalcBestSplitCassificationBinaryTwoClassXGBoostMissingValue1() throws Exception {
        final TreeEnsembleLearnerConfiguration config = createConfig(false);
        config.setMissingValueHandling(MissingValueHandling.XGBoost);
        final TestDataGenerator dataGen = new TestDataGenerator(config);
        // check correct behavior if no missing values are encountered during split search
        Pair<TreeNominalColumnData, TreeTargetNominalColumnData> twoClassTennisData = twoClassTennisData(config);
        String dataContainingMissingsCSV = "S,?,O,R,S,R,S,?,O,?";
        final TreeNominalColumnData columnData =
            dataGen.createNominalAttributeColumn(dataContainingMissingsCSV, "column containing missing values", 0);
        final TreeTargetNominalColumnData target = twoClassTennisData.getSecond();
        double[] rowWeights = new double[TWO_CLASS_INDICES.length];
        Arrays.fill(rowWeights, 1.0);
        // based on the ordering in the columnData
        final int[] originalIndex = new int[]{0, 4, 6, 2, 8, 3, 5, 1, 7, 9};
        final int[] columnIndex = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        final DataMemberships dataMem = new MockDataColMem(originalIndex, columnIndex, rowWeights);
        final SplitCandidate split = columnData.calcBestSplitClassification(dataMem,
            target.getDistribution(rowWeights, config), target, TestDataGenerator.createRandomData());
        assertThat(split, instanceOf(NominalBinarySplitCandidate.class));
        final NominalBinarySplitCandidate nomSplit = (NominalBinarySplitCandidate)split;
        TreeNodeNominalBinaryCondition[] childConditions = nomSplit.getChildConditions();
        assertEquals("Wrong gain value.", 0.18, nomSplit.getGainValue(), 1e-8);
        final String[] conditionValues = new String[]{"S", "R"};
        assertArrayEquals("Values in nominal condition did not match", conditionValues, childConditions[0].getValues());
        assertArrayEquals("Values in nominal condition did not match", conditionValues, childConditions[1].getValues());
        assertEquals("Wrong set logic.", SetLogic.IS_NOT_IN, childConditions[0].getSetLogic());
        assertEquals("Wrong set logic.", SetLogic.IS_IN, childConditions[1].getSetLogic());
        assertTrue("Missing values are not sent to the correct child.", childConditions[0].acceptsMissings());
        assertFalse("Missing values are not sent to the correct child.", childConditions[1].acceptsMissings());
    }

    /**
     * Tests the method
     * {@link TreeNominalColumnData#calcBestSplitClassification(DataMemberships, ClassificationPriors, TreeTargetNominalColumnData, RandomData)}
     * using binary splits.
     *
     * @throws Exception
     */
    @Test
    public void testCalcBestSplitClassificationBinary() throws Exception {
        final TreeEnsembleLearnerConfiguration config = createConfig(false);
        Pair<TreeNominalColumnData, TreeTargetNominalColumnData> tennisData = tennisData(config);
        TreeNominalColumnData columnData = tennisData.getFirst();
        TreeTargetNominalColumnData targetData = tennisData.getSecond();
        assertEquals(SplitCriterion.Gini, config.getSplitCriterion());
        double[] rowWeights = new double[SMALL_COLUMN_DATA.length];
        Arrays.fill(rowWeights, 1.0);
        TreeData tennisTreeData = tennisTreeData(config);
        IDataIndexManager indexManager = new DefaultDataIndexManager(tennisTreeData);
        DataMemberships dataMemberships = new RootDataMemberships(rowWeights, tennisTreeData, indexManager);
        ClassificationPriors priors = targetData.getDistribution(rowWeights, config);
        SplitCandidate splitCandidate =
            columnData.calcBestSplitClassification(dataMemberships, priors, targetData, null);
        assertNotNull(splitCandidate);
        assertThat(splitCandidate, instanceOf(NominalBinarySplitCandidate.class));
        assertTrue(splitCandidate.canColumnBeSplitFurther());
        assertEquals(0.0689342404, splitCandidate.getGainValue(), 0.00001); // manually via libre office calc
        NominalBinarySplitCandidate binSplitCandidate = (NominalBinarySplitCandidate)splitCandidate;
        TreeNodeNominalBinaryCondition[] childConditions = binSplitCandidate.getChildConditions();
        assertEquals(2, childConditions.length);
        assertArrayEquals(new String[]{"R"}, childConditions[0].getValues());
        assertArrayEquals(new String[]{"R"}, childConditions[1].getValues());
        assertEquals(SetLogic.IS_NOT_IN, childConditions[0].getSetLogic());
        assertEquals(SetLogic.IS_IN, childConditions[1].getSetLogic());

        BitSet inChild = columnData.updateChildMemberships(childConditions[0], dataMemberships);
        DataMemberships child1Memberships = dataMemberships.createChildMemberships(inChild);
        ClassificationPriors childTargetPriors = targetData.getDistribution(child1Memberships, config);
        SplitCandidate splitCandidateChild =
            columnData.calcBestSplitClassification(child1Memberships, childTargetPriors, targetData, null);

        assertNotNull(splitCandidateChild);
        assertThat(splitCandidateChild, instanceOf(NominalBinarySplitCandidate.class));
        assertEquals(0.0086419753, splitCandidateChild.getGainValue(), 0.00001); // manually via libre office calc

        inChild = columnData.updateChildMemberships(childConditions[1], dataMemberships);
        DataMemberships child2Memberships = dataMemberships.createChildMemberships(inChild);
        childTargetPriors = targetData.getDistribution(child2Memberships, config);
        splitCandidateChild =
            columnData.calcBestSplitClassification(child2Memberships, childTargetPriors, targetData, null);
        assertNull(splitCandidateChild);
    }

    /**
     * Tests the method
     * {@link TreeNominalColumnData#calcBestSplitClassification(DataMemberships, ClassificationPriors, TreeTargetNominalColumnData, RandomData)}
     * using binary splits. In this test case the data has more than two classes and the used algorithm is therefore PCA
     * based.
     *
     * @throws Exception
     */
    @Test
    public void testCalcBestSplitClassificationBinaryPCA() throws Exception {
        TreeEnsembleLearnerConfiguration config = createConfig(false);
        Pair<TreeNominalColumnData, TreeTargetNominalColumnData> pcaData = createPCATestData(config);
        TreeNominalColumnData columnData = pcaData.getFirst();
        TreeTargetNominalColumnData targetData = pcaData.getSecond();
        TreeData treeData = createTreeData(pcaData);
        assertEquals(SplitCriterion.Gini, config.getSplitCriterion());
        double[] rowWeights = new double[targetData.getNrRows()];
        Arrays.fill(rowWeights, 1.0);
        IDataIndexManager indexManager = new DefaultDataIndexManager(treeData);
        DataMemberships dataMemberships = new RootDataMemberships(rowWeights, treeData, indexManager);
        ClassificationPriors priors = targetData.getDistribution(rowWeights, config);
        SplitCandidate splitCandidate =
            columnData.calcBestSplitClassification(dataMemberships, priors, targetData, null);
        assertNotNull(splitCandidate);
        assertThat(splitCandidate, instanceOf(NominalBinarySplitCandidate.class));
        assertTrue(splitCandidate.canColumnBeSplitFurther());
        assertEquals(0.0659, splitCandidate.getGainValue(), 0.0001);
        NominalBinarySplitCandidate binarySplitCandidate = (NominalBinarySplitCandidate)splitCandidate;
        TreeNodeNominalBinaryCondition[] childConditions = binarySplitCandidate.getChildConditions();
        assertEquals(2, childConditions.length);
        assertArrayEquals(new String[]{"E"}, childConditions[0].getValues());
        assertArrayEquals(new String[]{"E"}, childConditions[1].getValues());
        assertEquals(SetLogic.IS_NOT_IN, childConditions[0].getSetLogic());
        assertEquals(SetLogic.IS_IN, childConditions[1].getSetLogic());
    }

    /**
     * Tests the XGBoost missing value handling in the case of binary splits calculated with the pca method (multiple classes)
     *
     * @throws Exception
     */
    @Test
    public void testCalcBestSplitClassificationBinaryPCAXGBoostMissingValueHandling() throws Exception {
        final TreeEnsembleLearnerConfiguration config = createConfig(false);
        config.setMissingValueHandling(MissingValueHandling.XGBoost);
        final TestDataGenerator dataGen = new TestDataGenerator(config);
        final RandomData rd = config.createRandomData();
        // test the case that there are no missing values in the training data
        final String noMissingCSV =    "a, a, a, b, b, b, b, c, c";
        final String noMissingTarget = "A, B, B, C, C, C, B, A, B";
        TreeNominalColumnData dataCol = dataGen.createNominalAttributeColumn(noMissingCSV, "noMissings", 0);
        TreeTargetNominalColumnData targetCol = TestDataGenerator.createNominalTargetColumn(noMissingTarget);
        DataMemberships dataMem = createMockDataMemberships(targetCol.getNrRows());
        SplitCandidate split = dataCol.calcBestSplitClassification(dataMem, targetCol.getDistribution(dataMem, config), targetCol, rd);
        assertNotNull("There is a possible split.", split);
        assertEquals("Incorrect gain.", 0.2086, split.getGainValue(), 1e-3);
        assertThat(split, instanceOf(NominalBinarySplitCandidate.class));
        NominalBinarySplitCandidate nomSplit = (NominalBinarySplitCandidate)split;
        assertTrue("No missing values in the column.", nomSplit.getMissedRows().isEmpty());
        TreeNodeNominalBinaryCondition[] conditions = nomSplit.getChildConditions();
        assertEquals("A binary split must have 2 child conditions.", 2, conditions.length);
        String[] values = new String[]{"a", "c"};
        assertArrayEquals("Wrong values in child condition.", values, conditions[0].getValues());
        assertArrayEquals("Wrong values in child condition.", values, conditions[1].getValues());
        assertEquals("Wrong set logic.",SetLogic.IS_NOT_IN, conditions[0].getSetLogic());
        assertEquals("Wrong set logic.",SetLogic.IS_IN, conditions[1].getSetLogic());
        assertFalse("Missing values should be sent to the majority child (i.e. right)", conditions[0].acceptsMissings());
        assertTrue("Missing values should be sent to the majority child (i.e. right)", conditions[1].acceptsMissings());

        // test the case that there are missing values in the training data
        final String missingCSV =    "a, a, a, b, b, b, b, c, c, ?";
        final String missingTarget = "A, B, B, C, C, C, B, A, B, C";
        dataCol = dataGen.createNominalAttributeColumn(missingCSV, "missings", 0);
        targetCol = TestDataGenerator.createNominalTargetColumn(missingTarget);
        dataMem = createMockDataMemberships(targetCol.getNrRows());
        split = dataCol.calcBestSplitClassification(dataMem, targetCol.getDistribution(dataMem, config), targetCol, rd);
        assertNotNull("There is a possible split.", split);
        assertEquals("Incorrect gain.", 0.24, split.getGainValue(), 1e-3);
        assertThat(split, instanceOf(NominalBinarySplitCandidate.class));
        nomSplit = (NominalBinarySplitCandidate)split;
        assertTrue("Split should handle missing values.", nomSplit.getMissedRows().isEmpty());
        conditions = nomSplit.getChildConditions();
        assertEquals("Wrong number of child conditions.", 2, conditions.length);
        assertArrayEquals("Wrong values in child condition.", values, conditions[0].getValues());
        assertArrayEquals("Wrong values in child condition.", values, conditions[1].getValues());
        assertEquals("Wrong set logic.",SetLogic.IS_NOT_IN, conditions[0].getSetLogic());
        assertEquals("Wrong set logic.",SetLogic.IS_IN, conditions[1].getSetLogic());
        assertTrue("Missing values should be sent to left child", conditions[0].acceptsMissings());
        assertFalse("Missing values should be sent to left child", conditions[1].acceptsMissings());
    }

    /**
     * Tests the method
     * {@link TreeNominalColumnData#calcBestSplitClassification(DataMemberships, ClassificationPriors, TreeTargetNominalColumnData, RandomData)}
     * using multiway splits
     *
     * @throws Exception
     */
    @Test
    public void testCalcBestSplitClassificationMultiWay() throws Exception {
        TreeEnsembleLearnerConfiguration config = createConfig(false);
        config.setUseBinaryNominalSplits(false);
        Pair<TreeNominalColumnData, TreeTargetNominalColumnData> tennisData = tennisData(config);
        TreeNominalColumnData columnData = tennisData.getFirst();
        TreeTargetNominalColumnData targetData = tennisData.getSecond();
        TreeData treeData = createTreeData(tennisData);
        assertEquals(SplitCriterion.Gini, config.getSplitCriterion());
        double[] rowWeights = new double[SMALL_COLUMN_DATA.length];
        Arrays.fill(rowWeights, 1.0);
        IDataIndexManager indexManager = new DefaultDataIndexManager(treeData);
        DataMemberships dataMemberships = new RootDataMemberships(rowWeights, treeData, indexManager);
        ClassificationPriors priors = targetData.getDistribution(rowWeights, config);
        SplitCandidate splitCandidate =
            columnData.calcBestSplitClassification(dataMemberships, priors, targetData, null);
        assertNotNull(splitCandidate);
        assertThat(splitCandidate, instanceOf(NominalMultiwaySplitCandidate.class));
        assertFalse(splitCandidate.canColumnBeSplitFurther());
        assertEquals(0.0744897959, splitCandidate.getGainValue(), 0.00001); // manually via libre office calc
        NominalMultiwaySplitCandidate multiWaySplitCandidate = (NominalMultiwaySplitCandidate)splitCandidate;
        TreeNodeNominalCondition[] childConditions = multiWaySplitCandidate.getChildConditions();
        assertEquals(3, childConditions.length);
        assertEquals("S", childConditions[0].getValue());
        assertEquals("O", childConditions[1].getValue());
        assertEquals("R", childConditions[2].getValue());
    }

    /**
     * This method tests the XGBoost missing value handling for classification in case of multiway splits.
     *
     * @throws Exception
     */
    @Test
    public void testCalcBestSplitClassificationMultiwayXGBoostMissingValueHandling() throws Exception {
        final TreeEnsembleLearnerConfiguration config = createConfig(false);
        config.setUseBinaryNominalSplits(false);
        config.setMissingValueHandling(MissingValueHandling.XGBoost);
        final TestDataGenerator dataGen = new TestDataGenerator(config);
        final RandomData rd = config.createRandomData();
        // test the case that there are no missing values in the training data
        final String noMissingCSV =    "a, a, a, b, b, b, b, c, c";
        final String noMissingTarget = "A, B, B, C, C, C, B, A, B";
        TreeNominalColumnData dataCol = dataGen.createNominalAttributeColumn(noMissingCSV, "noMissings", 0);
        TreeTargetNominalColumnData targetCol = TestDataGenerator.createNominalTargetColumn(noMissingTarget);
        DataMemberships dataMem = createMockDataMemberships(targetCol.getNrRows());
        SplitCandidate split = dataCol.calcBestSplitClassification(dataMem, targetCol.getDistribution(dataMem, config), targetCol, rd);
        assertNotNull("There is a possible split.", split);
        assertEquals("Incorrect gain.", 0.216, split.getGainValue(), 1e-3);
        assertThat(split, instanceOf(NominalMultiwaySplitCandidate.class));
        NominalMultiwaySplitCandidate nomSplit = (NominalMultiwaySplitCandidate)split;
        assertTrue("No missing values in the column.", nomSplit.getMissedRows().isEmpty());
        TreeNodeNominalCondition[] conditions = nomSplit.getChildConditions();
        assertEquals("Wrong number of child conditions.", 3, conditions.length);
        assertEquals("Wrong value in child condition.", "a", conditions[0].getValue());
        assertEquals("Wrong value in child condition.", "b", conditions[1].getValue());
        assertEquals("Wrong value in child condition.", "c", conditions[2].getValue());
        assertFalse("Missing values should be sent to the majority child (i.e. b)", conditions[0].acceptsMissings());
        assertTrue("Missing values should be sent to the majority child (i.e. b)", conditions[1].acceptsMissings());
        assertFalse("Missing values should be sent to the majority child (i.e. b)", conditions[2].acceptsMissings());

        // test the case that there are missing values in the training data
        final String missingCSV =    "a, a, a, b, b, b, b, c, c, ?";
        final String missingTarget = "A, B, B, C, C, C, B, A, B, C";
        dataCol = dataGen.createNominalAttributeColumn(missingCSV, "missings", 0);
        targetCol = TestDataGenerator.createNominalTargetColumn(missingTarget);
        dataMem = createMockDataMemberships(targetCol.getNrRows());
        split = dataCol.calcBestSplitClassification(dataMem, targetCol.getDistribution(dataMem, config), targetCol, rd);
        assertNotNull("There is a possible split.", split);
        assertEquals("Incorrect gain.", 0.2467, split.getGainValue(), 1e-3);
        assertThat(split, instanceOf(NominalMultiwaySplitCandidate.class));
        nomSplit = (NominalMultiwaySplitCandidate)split;
        assertTrue("Split should handle missing values.", nomSplit.getMissedRows().isEmpty());
        conditions = nomSplit.getChildConditions();
        assertEquals("Wrong number of child conditions.", 3, conditions.length);
        assertEquals("Wrong value in child condition.", "a", conditions[0].getValue());
        assertEquals("Wrong value in child condition.", "b", conditions[1].getValue());
        assertEquals("Wrong value in child condition.", "c", conditions[2].getValue());
        assertFalse("Missing values should be sent to b", conditions[0].acceptsMissings());
        assertTrue("Missing values should be sent to b", conditions[1].acceptsMissings());
        assertFalse("Missing values should be sent to b", conditions[2].acceptsMissings());
    }


    /**
     * Tests the method
     * {@link TreeNominalColumnData#calcBestSplitRegression(DataMemberships, RegressionPriors, TreeTargetNumericColumnData, RandomData)}
     * using binary splits
     *
     * @throws Exception
     */
    @Test
    public void testCalcBestSplitRegressionBinary() throws Exception {
        TreeEnsembleLearnerConfiguration config = new TreeEnsembleLearnerConfiguration(true);
        Pair<TreeNominalColumnData, TreeTargetNumericColumnData> tennisDataRegression = tennisDataRegression(config);
        TreeNominalColumnData columnData = tennisDataRegression.getFirst();
        TreeTargetNumericColumnData targetData = tennisDataRegression.getSecond();
        TreeData treeData = createTreeDataRegression(tennisDataRegression);
        double[] rowWeights = new double[SMALL_COLUMN_DATA.length];
        Arrays.fill(rowWeights, 1.0);
        IDataIndexManager indexManager = new DefaultDataIndexManager(treeData);
        DataMemberships dataMemberships = new RootDataMemberships(rowWeights, treeData, indexManager);
        RegressionPriors priors = targetData.getPriors(rowWeights, config);
        SplitCandidate splitCandidate = columnData.calcBestSplitRegression(dataMemberships, priors, targetData, null);
        assertNotNull(splitCandidate);
        assertThat(splitCandidate, instanceOf(NominalBinarySplitCandidate.class));
        assertTrue(splitCandidate.canColumnBeSplitFurther());
        assertEquals(32.9143, splitCandidate.getGainValue(), 0.0001);
        NominalBinarySplitCandidate binarySplitCandidate = (NominalBinarySplitCandidate)splitCandidate;
        TreeNodeNominalBinaryCondition[] childConditions = binarySplitCandidate.getChildConditions();
        assertEquals(2, childConditions.length);
        assertArrayEquals(new String[]{"R"}, childConditions[0].getValues());
        assertArrayEquals(new String[]{"R"}, childConditions[1].getValues());
        assertEquals(SetLogic.IS_NOT_IN, childConditions[0].getSetLogic());
        assertEquals(SetLogic.IS_IN, childConditions[1].getSetLogic());
    }

    /**
     * Tests the XGBoost missing value handling in case of a regression with binary splits.
     *
     * @throws Exception
     */
    @Test
    public void testCalcBestSplitRegressionBinaryXGBoostMissingValueHandling() throws Exception {
        final TreeEnsembleLearnerConfiguration config = createConfig(true);
        config.setMissingValueHandling(MissingValueHandling.XGBoost);
        final TestDataGenerator dataGen = new TestDataGenerator(config);
        final String noMissingCSV = "A, A, A, B, B, B, B, C, C";
        final String noMissingsTarget = "1, 2, 2, 7, 6, 5, 2, 3, 1";
        TreeNominalColumnData dataCol = dataGen.createNominalAttributeColumn(noMissingCSV, "noMissings", 0);
        TreeTargetNumericColumnData targetCol = TestDataGenerator.createNumericTargetColumn(noMissingsTarget);
        double[] weights = new double[9];
        Arrays.fill(weights, 1.0);
        int[] indices = new int[9];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }
        final RandomData rd = config.createRandomData();
        DataMemberships dataMemberships = new MockDataColMem(indices, indices, weights);
        // first test the case that there are no missing values during training (we still need to provide a missing value direction for prediction)
        SplitCandidate split = dataCol.calcBestSplitRegression(dataMemberships, targetCol.getPriors(weights, config), targetCol, rd);
        assertNotNull("SplitCandidate may not be null", split);
        assertThat(split, instanceOf(NominalBinarySplitCandidate.class));
        assertEquals("Wrong gain.", 22.755555, split.getGainValue(), 1e-5);
        assertTrue("No missing values in dataCol therefore the missedRows BitSet must be empty.", split.getMissedRows().isEmpty());
        NominalBinarySplitCandidate nomSplit = (NominalBinarySplitCandidate)split;
        TreeNodeNominalBinaryCondition[] conditions = nomSplit.getChildConditions();
        assertEquals("Binary split candidate must have two children.", 2, conditions.length);
        final String[] values = new String[]{"A", "C"};
        assertArrayEquals("Wrong values in split condition.",values, conditions[0].getValues());
        assertArrayEquals("Wrong values in split condition.",values, conditions[1].getValues());
        assertFalse("Missings should go with majority", conditions[0].acceptsMissings());
        assertTrue("Missings should go with majority", conditions[1].acceptsMissings());
        assertEquals("Wrong set logic.", SetLogic.IS_NOT_IN, conditions[0].getSetLogic());
        assertEquals("Wrong set logic.", SetLogic.IS_IN, conditions[1].getSetLogic());

        // test the case that there are missing values during training
        final String missingCSV =    "A, A, A, B, B, B, B, C, C, ?";
        final String missingTarget = "1, 2, 2, 7, 6, 5, 2, 3, 1, 8";
        dataCol = dataGen.createNominalAttributeColumn(missingCSV, "missing", 0);
        targetCol = TestDataGenerator.createNumericTargetColumn(missingTarget);
        weights = new double[10];
        Arrays.fill(weights, 1.0);
        indices = new int[10];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }
        dataMemberships = new MockDataColMem(indices, indices, weights);
        split = dataCol.calcBestSplitRegression(dataMemberships, targetCol.getPriors(weights, config), targetCol, rd);
        assertNotNull("SplitCandidate may not be null.", split);
        assertThat(split, instanceOf(NominalBinarySplitCandidate.class));
        assertEquals("Wrong gain.", 36.1, split.getGainValue(), 1e-5);
        assertTrue("Conditions should handle missing values therefore the missedRows BitSet must be empty.", split.getMissedRows().isEmpty());
        nomSplit = (NominalBinarySplitCandidate)split;
        conditions = nomSplit.getChildConditions();
        assertEquals("Binary split candidate must have two children.", 2, conditions.length);
        assertArrayEquals("Wrong values in split condition.",values, conditions[0].getValues());
        assertArrayEquals("Wrong values in split condition.",values, conditions[1].getValues());
        assertTrue("Missings should go with B (because there target values are similar)", conditions[0].acceptsMissings());
        assertFalse("Missings should go with B (because there target values are similar)", conditions[1].acceptsMissings());
        assertEquals("Wrong set logic.", SetLogic.IS_NOT_IN, conditions[0].getSetLogic());
        assertEquals("Wrong set logic.", SetLogic.IS_IN, conditions[1].getSetLogic());
    }

    /**
     * Tests the method
     * {@link TreeNominalColumnData#calcBestSplitRegression(DataMemberships, RegressionPriors, TreeTargetNumericColumnData, RandomData)}
     * using multiway splits.
     *
     * @throws Exception
     */
    @Test
    public void testCalcBestSplitRegressionMultiway() throws Exception {
        TreeEnsembleLearnerConfiguration config = createConfig(true);
        config.setUseBinaryNominalSplits(false);
        Pair<TreeNominalColumnData, TreeTargetNumericColumnData> tennisDataRegression = tennisDataRegression(config);
        TreeNominalColumnData columnData = tennisDataRegression.getFirst();
        TreeTargetNumericColumnData targetData = tennisDataRegression.getSecond();
        TreeData treeData = createTreeDataRegression(tennisDataRegression);
        double[] rowWeights = new double[SMALL_COLUMN_DATA.length];
        Arrays.fill(rowWeights, 1.0);
        IDataIndexManager indexManager = new DefaultDataIndexManager(treeData);
        DataMemberships dataMemberships = new RootDataMemberships(rowWeights, treeData, indexManager);
        RegressionPriors priors = targetData.getPriors(rowWeights, config);
        SplitCandidate splitCandidate = columnData.calcBestSplitRegression(dataMemberships, priors, targetData, null);
        assertNotNull(splitCandidate);
        assertThat(splitCandidate, instanceOf(NominalMultiwaySplitCandidate.class));
        assertFalse(splitCandidate.canColumnBeSplitFurther());
        assertEquals(36.9643, splitCandidate.getGainValue(), 0.0001);
        NominalMultiwaySplitCandidate multiwaySplitCandidate = (NominalMultiwaySplitCandidate)splitCandidate;
        TreeNodeNominalCondition[] childConditions = multiwaySplitCandidate.getChildConditions();
        assertEquals(3, childConditions.length);
        assertEquals("S", childConditions[0].getValue());
        assertEquals("O", childConditions[1].getValue());
        assertEquals("R", childConditions[2].getValue());
    }

    /**
     * This method tests the XGBoost missing value handling in case of a regression task and multiway splits.
     *
     * @throws Exception
     */
    @Test
    public void testCalcBestSplitRegressionMultiwayXGBoostMissingValueHandling() throws Exception {
        final TreeEnsembleLearnerConfiguration config = createConfig(true);
        config.setMissingValueHandling(MissingValueHandling.XGBoost);
        config.setUseBinaryNominalSplits(false);
        final TestDataGenerator dataGen = new TestDataGenerator(config);
        final String noMissingCSV = "A, A, A, B, B, B, B, C, C";
        final String noMissingsTarget = "1, 2, 2, 7, 6, 5, 2, 3, 1";
        TreeNominalColumnData dataCol = dataGen.createNominalAttributeColumn(noMissingCSV, "noMissings", 0);
        TreeTargetNumericColumnData targetCol = TestDataGenerator.createNumericTargetColumn(noMissingsTarget);
        double[] weights = new double[9];
        Arrays.fill(weights, 1.0);
        int[] indices = new int[9];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }
        final RandomData rd = config.createRandomData();
        DataMemberships dataMemberships = new MockDataColMem(indices, indices, weights);
        // first test the case that there are no missing values during training (we still need to provide a missing value direction for prediction)
        SplitCandidate split = dataCol.calcBestSplitRegression(dataMemberships, targetCol.getPriors(weights, config), targetCol, rd);
        assertNotNull("SplitCandidate may not be null", split);
        assertThat(split, instanceOf(NominalMultiwaySplitCandidate.class));
        assertEquals("Wrong gain.", 22.888888, split.getGainValue(), 1e-5);
        assertTrue("No missing values in dataCol therefore the missedRows BitSet must be empty.", split.getMissedRows().isEmpty());
        NominalMultiwaySplitCandidate nomSplit = (NominalMultiwaySplitCandidate)split;
        TreeNodeNominalCondition[] conditions = nomSplit.getChildConditions();
        assertEquals("3 nominal values therefore there must be 3 children.", 3, conditions.length);
        assertEquals("Wrong value.", "A", conditions[0].getValue());
        assertEquals("Wrong value.", "B", conditions[1].getValue());
        assertEquals("Wrong value.", "C", conditions[2].getValue());
        assertFalse("Missings should go with majority", conditions[0].acceptsMissings());
        assertTrue("Missings should go with majority", conditions[1].acceptsMissings());
        assertFalse("Missings should go with majority", conditions[2].acceptsMissings());

        // test the case that there are missing values during training
        final String missingCSV =    "A, A, A, B, B, B, B, C, C, ?";
        final String missingTarget = "1, 2, 2, 7, 6, 5, 2, 3, 1, 8";
        dataCol = dataGen.createNominalAttributeColumn(missingCSV, "missing", 0);
        targetCol = TestDataGenerator.createNumericTargetColumn(missingTarget);
        weights = new double[10];
        Arrays.fill(weights, 1.0);
        indices = new int[10];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }
        dataMemberships = new MockDataColMem(indices, indices, weights);
        split = dataCol.calcBestSplitRegression(dataMemberships, targetCol.getPriors(weights, config), targetCol, rd);
        assertNotNull("SplitCandidate may not be null.", split);
        assertThat(split, instanceOf(NominalMultiwaySplitCandidate.class));
//        assertEquals("Wrong gain.", 36.233333333, split.getGainValue(), 1e-5);
        assertTrue("Conditions should handle missing values therefore the missedRows BitSet must be empty.", split.getMissedRows().isEmpty());
        nomSplit = (NominalMultiwaySplitCandidate)split;
        conditions = nomSplit.getChildConditions();
        assertEquals("3 values (not counting missing values) therefore there must be 3 children.", 3, conditions.length);
        assertEquals("Wrong value.", "A", conditions[0].getValue());
        assertEquals("Wrong value.", "B", conditions[1].getValue());
        assertEquals("Wrong value.", "C", conditions[2].getValue());
        assertFalse("Missings should go with majority", conditions[0].acceptsMissings());
        assertTrue("Missings should go with majority", conditions[1].acceptsMissings());
        assertFalse("Missings should go with majority", conditions[2].acceptsMissings());
    }

    /**
     * Tests the method
     * {@link TreeNominalColumnData#updateChildMemberships(org.knime.base.node.mine.treeensemble2.model.TreeNodeCondition, DataMemberships)}
     * .
     *
     * @throws Exception
     */
    @Test
    public void testUpdateChildMemberships() throws Exception {
        // in this case it doesn't matter if we use regression or classification (as well as binary and multiway splits)
        final TreeEnsembleLearnerConfiguration config = createConfig(true);
        final TestDataGenerator dataGen = new TestDataGenerator(config);
        final String dataCSV = "A, A, A, A, B, B, B, C, C, C, ?, ?";
        TreeNominalColumnData col = dataGen.createNominalAttributeColumn(dataCSV, "test-col", 0);
        final int[] indices = new int[12];
        final double[] weights = new double[indices.length];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
            weights[i] = 1.0;
        }
        final DataMemberships dataMem = new MockDataColMem(indices, indices, weights);
        TreeNodeNominalBinaryCondition binCond =
            new TreeNodeNominalBinaryCondition(col.getMetaData(), BigInteger.valueOf(2), true, false);
        BitSet expected = new BitSet(12);
        BitSet inChild = col.updateChildMemberships(binCond, dataMem);
        expected.set(4, 7);
        assertEquals("The produced BitSet is incorrect.", expected, inChild);

        binCond = new TreeNodeNominalBinaryCondition(col.getMetaData(), BigInteger.valueOf(2), true, true);
        expected.clear();
        expected.set(4, 7);
        expected.set(10, 12);
        inChild = col.updateChildMemberships(binCond, dataMem);
        assertEquals("The produced BitSet is incorrect.", expected, inChild);

        binCond = new TreeNodeNominalBinaryCondition(col.getMetaData(), BigInteger.valueOf(2), false, false);
        expected.clear();
        expected.set(0, 4);
        expected.set(7, 10);
        inChild = col.updateChildMemberships(binCond, dataMem);
        assertEquals("The produced BitSet is incorrect.", expected, inChild);

        binCond = new TreeNodeNominalBinaryCondition(col.getMetaData(), BigInteger.valueOf(2), false, true);
        expected.clear();
        expected.set(0, 4);
        expected.set(7, 12);
        inChild = col.updateChildMemberships(binCond, dataMem);
        assertEquals("The produced BitSet is incorrect.", expected, inChild);

        binCond = new TreeNodeNominalBinaryCondition(col.getMetaData(), BigInteger.valueOf(5), true, false);
        expected.clear();
        expected.set(0, 4);
        expected.set(7, 10);
        inChild = col.updateChildMemberships(binCond, dataMem);
        assertEquals("The produced BitSet is incorrect.", expected, inChild);

        binCond = new TreeNodeNominalBinaryCondition(col.getMetaData(), BigInteger.valueOf(5), true, true);
        expected.clear();
        expected.set(0, 4);
        expected.set(7, 12);
        inChild = col.updateChildMemberships(binCond, dataMem);
        assertEquals("The produced BitSet is incorrect.", expected, inChild);

        TreeNodeNominalCondition multiCond = new TreeNodeNominalCondition(col.getMetaData(), 0, false);
        expected.clear();
        expected.set(0, 4);
        inChild = col.updateChildMemberships(multiCond, dataMem);
        assertEquals("The produced BitSet is incorrect.", expected, inChild);

        multiCond = new TreeNodeNominalCondition(col.getMetaData(), 0, true);
        expected.clear();
        expected.set(0, 4);
        expected.set(10, 12);
        inChild = col.updateChildMemberships(multiCond, dataMem);
        assertEquals("The produced BitSet is incorrect.", expected, inChild);

        multiCond = new TreeNodeNominalCondition(col.getMetaData(), 2, false);
        expected.clear();
        expected.set(7, 10);
        inChild = col.updateChildMemberships(multiCond, dataMem);
        assertEquals("The produced BitSet is incorrect.", expected, inChild);

        multiCond = new TreeNodeNominalCondition(col.getMetaData(), 2, true);
        expected.clear();
        expected.set(7, 12);
        inChild = col.updateChildMemberships(multiCond, dataMem);
        assertEquals("The produced BitSet is incorrect.", expected, inChild);
    }

}
