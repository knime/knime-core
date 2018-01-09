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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.BitSet;

import org.apache.commons.math.random.RandomData;
import org.junit.Test;
import org.knime.base.node.mine.treeensemble2.data.memberships.DataMemberships;
import org.knime.base.node.mine.treeensemble2.data.memberships.DefaultDataIndexManager;
import org.knime.base.node.mine.treeensemble2.data.memberships.IDataIndexManager;
import org.knime.base.node.mine.treeensemble2.data.memberships.RootDataMemberships;
import org.knime.base.node.mine.treeensemble2.learner.NumericMissingSplitCandidate;
import org.knime.base.node.mine.treeensemble2.learner.NumericSplitCandidate;
import org.knime.base.node.mine.treeensemble2.learner.SplitCandidate;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeEnsembleModel.TreeType;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeNumericCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeNumericCondition.NumericOperator;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration.ColumnSamplingMode;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration.MissingValueHandling;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration.SplitCriterion;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.Pair;

import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;

/**
 *
 * @author wiswedel
 */
public class TreeNumericColumnDataTest {

    public static final double[] asDataArray(final String dataCSV) {
        Iterable<Double> data = Doubles.stringConverter().convertAll(Arrays.asList(dataCSV.split(", *")));
        return Doubles.toArray(Lists.newArrayList(data));
    }

    static final String[] asStringArray(final String stringCSV) {
        return stringCSV.split(", *");
    }

    public static TreeOrdinaryNumericColumnData createNumericColumnData(final TreeEnsembleLearnerConfiguration config,
        final double[] data, final String name, final int attributeIndex) {
        DataColumnSpec colSpec = new DataColumnSpecCreator(name, DoubleCell.TYPE).createSpec();
        TreeOrdinaryNumericColumnDataCreator colCreator = new TreeOrdinaryNumericColumnDataCreator(colSpec);
        for (int i = 0; i < data.length; i++) {
            final RowKey key = RowKey.createRowKey(i);
            if (Double.isNaN(data[i])) {
                colCreator.add(key, new MissingCell(null));
            } else {
                colCreator.add(key, new DoubleCell(data[i]));
            }
        }
        return colCreator.createColumnData(attributeIndex, config);
    }

    private static Pair<TreeOrdinaryNumericColumnData, TreeTargetNominalColumnData>
        exampleData(final TreeEnsembleLearnerConfiguration config, final double[] data, final String[] target) {
        TestDataGenerator dataGen = new TestDataGenerator(config);
        return Pair.create(dataGen.createNumericAttributeColumnData(data, "test-col", 0),
            TestDataGenerator.createNominalTargetColumn(target));
    }

    public static TreeData
        createTreeDataClassification(final Pair<TreeOrdinaryNumericColumnData, TreeTargetNominalColumnData> cols) {
        TreeOrdinaryNumericColumnData numCol = cols.getFirst();
        numCol.getMetaData().setAttributeIndex(0);
        TreeTargetNominalColumnData tarCol = cols.getSecond();
        return new TreeData(new TreeAttributeColumnData[]{numCol}, tarCol, TreeType.Ordinary);
    }

    private static TreeData
        createTreeDataRegression(final Pair<TreeOrdinaryNumericColumnData, TreeTargetNumericColumnData> cols) {
        TreeOrdinaryNumericColumnData numCol = cols.getFirst();
        numCol.getMetaData().setAttributeIndex(0);
        TreeTargetNumericColumnData tarCol = cols.getSecond();
        return new TreeData(new TreeAttributeColumnData[]{numCol}, tarCol, TreeType.Ordinary);
    }

    @Test
    public void testCalcBestSplitClassification() throws Exception {
        TreeEnsembleLearnerConfiguration config = createConfig();
        /* data from J. Fuernkranz, Uni Darmstadt:
         * http://www.ke.tu-darmstadt.de/lehre/archiv/ws0809/mldm/dt.pdf */
        final double[] data = asDataArray("60,70,75,85, 90, 95, 100,120,125,220");
        final String[] target = asStringArray("No,No,No,Yes,Yes,Yes,No, No, No, No");
        Pair<TreeOrdinaryNumericColumnData, TreeTargetNominalColumnData> exampleData =
            exampleData(config, data, target);
        RandomData rd = config.createRandomData();
        TreeNumericColumnData columnData = exampleData.getFirst();
        TreeTargetNominalColumnData targetData = exampleData.getSecond();
        assertEquals(SplitCriterion.Gini, config.getSplitCriterion());
        double[] rowWeights = new double[data.length];
        Arrays.fill(rowWeights, 1.0);
        TreeData treeData = createTreeDataClassification(exampleData);
        IDataIndexManager indexManager = new DefaultDataIndexManager(treeData);
        DataMemberships dataMemberships = new RootDataMemberships(rowWeights, treeData, indexManager);
        ClassificationPriors priors = targetData.getDistribution(rowWeights, config);
        SplitCandidate splitCandidate = columnData.calcBestSplitClassification(dataMemberships, priors, targetData, rd);
        assertNotNull(splitCandidate);
        assertThat(splitCandidate, instanceOf(NumericSplitCandidate.class));
        assertTrue(splitCandidate.canColumnBeSplitFurther());
        assertEquals(/*0.42 - 0.300 */0.12, splitCandidate.getGainValue(), 0.00001); // libre office calc
        NumericSplitCandidate numSplitCandidate = (NumericSplitCandidate)splitCandidate;
        TreeNodeNumericCondition[] childConditions = numSplitCandidate.getChildConditions();
        assertEquals(2, childConditions.length);
        assertEquals((95.0 + 100.0) / 2.0, childConditions[0].getSplitValue(), 0.0);
        assertEquals((95.0 + 100.0) / 2.0, childConditions[1].getSplitValue(), 0.0);
        assertEquals(NumericOperator.LessThanOrEqual, childConditions[0].getNumericOperator());
        assertEquals(NumericOperator.LargerThan, childConditions[1].getNumericOperator());

        double[] childRowWeights = new double[data.length];
        System.arraycopy(rowWeights, 0, childRowWeights, 0, rowWeights.length);
        BitSet inChild = columnData.updateChildMemberships(childConditions[0], dataMemberships);
        DataMemberships childMemberships = dataMemberships.createChildMemberships(inChild);
        ClassificationPriors childTargetPriors = targetData.getDistribution(childMemberships, config);
        SplitCandidate splitCandidateChild =
            columnData.calcBestSplitClassification(childMemberships, childTargetPriors, targetData, rd);
        assertNotNull(splitCandidateChild);
        assertThat(splitCandidateChild, instanceOf(NumericSplitCandidate.class));
        assertEquals(0.5, splitCandidateChild.getGainValue(), 0.00001); // manually via libre office calc
        TreeNodeNumericCondition[] childConditions2 = ((NumericSplitCandidate)splitCandidateChild).getChildConditions();
        assertEquals(2, childConditions2.length);
        assertEquals((75.0 + 85.0) / 2.0, childConditions2[0].getSplitValue(), 0.0);

        System.arraycopy(rowWeights, 0, childRowWeights, 0, rowWeights.length);
        inChild = columnData.updateChildMemberships(childConditions[1], dataMemberships);
        childMemberships = dataMemberships.createChildMemberships(inChild);
        childTargetPriors = targetData.getDistribution(childMemberships, config);
        splitCandidateChild =
            columnData.calcBestSplitClassification(childMemberships, childTargetPriors, targetData, rd);
        assertNull(splitCandidateChild);
    }

    /**
     * This test is outdated and will likely be removed soon.
     *
     * @throws Exception
     */
    //    @Test
    public void testCalcBestSplitClassificationMissingValStrategy1() throws Exception {
        TreeEnsembleLearnerConfiguration config = createConfig();
        final double[] data = asDataArray("1, 2, 3, 4, 5, 6, 7, NaN, NaN, NaN");
        final String[] target = asStringArray("Y, Y, Y, Y, N, N, N, Y, Y, Y");
        Pair<TreeOrdinaryNumericColumnData, TreeTargetNominalColumnData> exampleData =
            exampleData(config, data, target);
        double[] rowWeights = new double[data.length];
        Arrays.fill(rowWeights, 1.0);
        RandomData rd = config.createRandomData();
        TreeNumericColumnData columnData = exampleData.getFirst();
        TreeTargetNominalColumnData targetData = exampleData.getSecond();
        TreeData treeData = createTreeDataClassification(exampleData);
        IDataIndexManager indexManager = new DefaultDataIndexManager(treeData);
        DataMemberships dataMemberships = new RootDataMemberships(rowWeights, treeData, indexManager);
        ClassificationPriors priors = targetData.getDistribution(rowWeights, config);
        SplitCandidate splitCandidate = columnData.calcBestSplitClassification(dataMemberships, priors, targetData, rd);
        assertNotNull(splitCandidate);
        assertThat(splitCandidate, instanceOf(NumericMissingSplitCandidate.class));
        assertTrue(splitCandidate.canColumnBeSplitFurther());
        assertEquals(0.42, splitCandidate.getGainValue(), 0.0001);

        TreeNodeNumericCondition[] childConditions =
            ((NumericMissingSplitCandidate)splitCandidate).getChildConditions();
        assertEquals(2, childConditions.length);
        assertEquals(NumericOperator.LessThanOrEqualOrMissing, childConditions[0].getNumericOperator());
        assertEquals(NumericOperator.LargerThan, childConditions[1].getNumericOperator());
        assertEquals(4.5, childConditions[0].getSplitValue(), 0.0);
    }

    /**
     * Test splits at last possible split position - even if no change in target can be observed, see example data in
     * method body.
     * @throws Exception
     */
    @Test
    public void testCalcBestSplitClassificationSplitAtEnd() throws Exception {
        // Index:  1 2 3 4 5 6 7 8
        // Value:  1 1|2 2 2|3 3 3
        // Target: A A|A A A|A A B
        double[] data = asDataArray("1,1,2,2,2,3,3,3");
        String[] target = asStringArray("A,A,A,A,A,A,A,B");
        TreeEnsembleLearnerConfiguration config = createConfig();
        RandomData rd = config.createRandomData();
        Pair<TreeOrdinaryNumericColumnData, TreeTargetNominalColumnData> exampleData =
            exampleData(config, data, target);
        TreeNumericColumnData columnData = exampleData.getFirst();
        TreeTargetNominalColumnData targetData = exampleData.getSecond();
        double[] rowWeights = new double[data.length];
        Arrays.fill(rowWeights, 1.0);
        TreeData treeData = createTreeDataClassification(exampleData);
        IDataIndexManager indexManager = new DefaultDataIndexManager(treeData);
        DataMemberships dataMemberships = new RootDataMemberships(rowWeights, treeData, indexManager);
        ClassificationPriors priors = targetData.getDistribution(rowWeights, config);
        SplitCandidate splitCandidate = columnData.calcBestSplitClassification(dataMemberships, priors, targetData, rd);
        assertNotNull(splitCandidate);
        assertThat(splitCandidate, instanceOf(NumericSplitCandidate.class));
        assertTrue(splitCandidate.canColumnBeSplitFurther());
        assertEquals(/*0.21875 - 0.166666667 */0.05208, splitCandidate.getGainValue(), 0.001); // manually calculated
        NumericSplitCandidate numSplitCandidate = (NumericSplitCandidate)splitCandidate;
        TreeNodeNumericCondition[] childConditions = numSplitCandidate.getChildConditions();
        assertEquals(2, childConditions.length);
        assertEquals((2.0 + 3.0) / 2.0, childConditions[0].getSplitValue(), 0.0);
        assertEquals(NumericOperator.LessThanOrEqual, childConditions[0].getNumericOperator());

        double[] childRowWeights = new double[data.length];
        System.arraycopy(rowWeights, 0, childRowWeights, 0, rowWeights.length);
        BitSet inChild = columnData.updateChildMemberships(childConditions[0], dataMemberships);
        DataMemberships childMemberships = dataMemberships.createChildMemberships(inChild);
        ClassificationPriors childTargetPriors = targetData.getDistribution(childMemberships, config);
        SplitCandidate splitCandidateChild =
            columnData.calcBestSplitClassification(childMemberships, childTargetPriors, targetData, rd);

        assertNull(splitCandidateChild);

        System.arraycopy(rowWeights, 0, childRowWeights, 0, rowWeights.length);
        inChild = columnData.updateChildMemberships(childConditions[1], dataMemberships);
        childMemberships = dataMemberships.createChildMemberships(inChild);
        childTargetPriors = targetData.getDistribution(childMemberships, config);
        splitCandidateChild =
            columnData.calcBestSplitClassification(childMemberships, childTargetPriors, targetData, null);
        assertNull(splitCandidateChild);
    }

    /**
     * Test splits at last possible split position - even if no change in target can be observed, see example data in
     * method body.
     * @throws Exception
     */
    @Test
    public void testCalcBestSplitClassificationSplitAtStart() throws Exception {
        // Index:  1 2 3 4 5 6 7
        // Value:  1 1 1|2 2|3 3
        // Target: A A A|A A|A B
        double[] data = asDataArray("1,1,1,2,2,3,3");
        String[] target = asStringArray("A,A,A,A,B,A,B");
        TreeEnsembleLearnerConfiguration config = createConfig();
        Pair<TreeOrdinaryNumericColumnData, TreeTargetNominalColumnData> exampleData =
            exampleData(config, data, target);
        TreeNumericColumnData columnData = exampleData.getFirst();
        TreeTargetNominalColumnData targetData = exampleData.getSecond();
        double[] rowWeights = new double[data.length];
        Arrays.fill(rowWeights, 1.0);
        TreeData treeData = createTreeDataClassification(exampleData);
        IDataIndexManager indexManager = new DefaultDataIndexManager(treeData);
        DataMemberships dataMemberships = new RootDataMemberships(rowWeights, treeData, indexManager);
        ClassificationPriors priors = targetData.getDistribution(rowWeights, config);
        RandomData rd = config.createRandomData();
        SplitCandidate splitCandidate = columnData.calcBestSplitClassification(dataMemberships, priors, targetData, rd);
        double gain = (1.0 - Math.pow(5.0 / 7.0, 2.0) - Math.pow(2.0 / 7.0, 2.0)) - 0.0
            - 4.0 / 7.0 * (1.0 - Math.pow(2.0 / 4.0, 2.0) - Math.pow(2.0 / 4.0, 2.0));
        assertEquals(gain, splitCandidate.getGainValue(), 0.000001); // manually calculated
        NumericSplitCandidate numSplitCandidate = (NumericSplitCandidate)splitCandidate;
        TreeNodeNumericCondition[] childConditions = numSplitCandidate.getChildConditions();
        assertEquals(2, childConditions.length);
        assertEquals((1.0 + 2.0) / 2.0, childConditions[0].getSplitValue(), 0.0);
    }

    private static TreeEnsembleLearnerConfiguration createConfig() throws InvalidSettingsException {
        TreeEnsembleLearnerConfiguration config = new TreeEnsembleLearnerConfiguration(false);
        config.setColumnSamplingMode(ColumnSamplingMode.None);
        config.setSplitCriterion(SplitCriterion.Gini);
        config.setNrModels(1);
        config.setDataSelectionWithReplacement(false);
        config.setUseDifferentAttributesAtEachNode(false);
        config.setDataFractionPerTree(1.0);
        return config;
    }

    @Test
    public void testCalcBestSplitRegression() throws InvalidSettingsException {
        String dataCSV = "1,2,3,4,5,6,7,8,9,10";
        String targetCSV = "1,5,4,4.3,6.5,6.5,4,3,3,4";
        TreeEnsembleLearnerConfiguration config = new TreeEnsembleLearnerConfiguration(true);
        config.setNrModels(1);
        config.setDataSelectionWithReplacement(false);
        config.setUseDifferentAttributesAtEachNode(false);
        config.setDataFractionPerTree(1.0);
        config.setColumnSamplingMode(ColumnSamplingMode.None);
        TestDataGenerator dataGen = new TestDataGenerator(config);
        RandomData rd = config.createRandomData();
        TreeTargetNumericColumnData target = TestDataGenerator.createNumericTargetColumn(targetCSV);
        TreeNumericColumnData attribute = dataGen.createNumericAttributeColumn(dataCSV, "test-col", 0);
        TreeData data = new TreeData(new TreeAttributeColumnData[]{attribute}, target, TreeType.Ordinary);
        double[] weights = new double[10];
        Arrays.fill(weights, 1.0);
        DataMemberships rootMem = new RootDataMemberships(weights, data, new DefaultDataIndexManager(data));
        SplitCandidate firstSplit =
            attribute.calcBestSplitRegression(rootMem, target.getPriors(rootMem, config), target, rd);
        // calculated via OpenOffice calc
        assertEquals(10.885444, firstSplit.getGainValue(), 1e-5);
        TreeNodeCondition[] firstConditions = firstSplit.getChildConditions();
        assertEquals(2, firstConditions.length);
        for (int i = 0; i < firstConditions.length; i++) {
            assertThat(firstConditions[i], instanceOf(TreeNodeNumericCondition.class));
            TreeNodeNumericCondition numCond = (TreeNodeNumericCondition)firstConditions[i];
            assertEquals(1.5, numCond.getSplitValue(), 0);
        }

        // left child contains only one row therefore only look at right child
        BitSet expectedInChild = new BitSet(10);
        expectedInChild.set(1, 10);
        BitSet inChild = attribute.updateChildMemberships(firstConditions[1], rootMem);
        assertEquals(expectedInChild, inChild);
        DataMemberships childMem = rootMem.createChildMemberships(inChild);
        SplitCandidate secondSplit =
            attribute.calcBestSplitRegression(childMem, target.getPriors(childMem, config), target, rd);
        assertEquals(6.883555, secondSplit.getGainValue(), 1e-5);
        TreeNodeCondition[] secondConditions = secondSplit.getChildConditions();
        for (int i = 0; i < secondConditions.length; i++) {
            assertThat(secondConditions[i], instanceOf(TreeNodeNumericCondition.class));
            TreeNodeNumericCondition numCond = (TreeNodeNumericCondition)secondConditions[i];
            assertEquals(6.5, numCond.getSplitValue(), 0);
        }
    }

    /**
     * This method tests if the conditions for child nodes are correct in case of XGBoostMissingValueHandling
     *
     * @throws Exception
     */
    @Test
    public void testXGBoostMissingValueHandling() throws Exception {
        TreeEnsembleLearnerConfiguration config = createConfig();
        config.setMissingValueHandling(MissingValueHandling.XGBoost);
        final TestDataGenerator dataGen = new TestDataGenerator(config);
        final RandomData rd = config.createRandomData();
        final int[] indices = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        final double[] weights = new double[10];
        Arrays.fill(weights, 1.0);
        final MockDataColMem dataMem = new MockDataColMem(indices, indices, weights);
        final String dataCSV = "1,2,2,3,4,5,6,7,NaN,NaN";
        final String target1CSV = "A,A,A,A,B,B,B,B,A,A";
        final String target2CSV = "A,A,A,A,B,B,B,B,B,B";
        final double expectedGain = 0.48;
        final TreeNumericColumnData col = dataGen.createNumericAttributeColumn(dataCSV, "testCol", 0);
        final TreeTargetNominalColumnData target1 = TestDataGenerator.createNominalTargetColumn(target1CSV);
        final SplitCandidate split1 =
            col.calcBestSplitClassification(dataMem, target1.getDistribution(weights, config), target1, rd);
        assertEquals("Wrong gain.", expectedGain, split1.getGainValue(), 1e-8);
        final TreeNodeCondition[] childConds1 = split1.getChildConditions();
        final TreeNodeNumericCondition numCondLeft1 = (TreeNodeNumericCondition)childConds1[0];
        assertEquals("Wrong split point.", 3.5, numCondLeft1.getSplitValue(), 1e-8);
        assertTrue("Missings were not sent in the correct direction.", numCondLeft1.acceptsMissings());
        final TreeNodeNumericCondition numCondRight1 = (TreeNodeNumericCondition)childConds1[1];
        assertEquals("Wrong split point.", 3.5, numCondRight1.getSplitValue(), 1e-8);
        assertFalse("Missings were not sent in the correct direction.", numCondRight1.acceptsMissings());

        final TreeTargetNominalColumnData target2 = TestDataGenerator.createNominalTargetColumn(target2CSV);
        final SplitCandidate split2 =
            col.calcBestSplitClassification(dataMem, target2.getDistribution(weights, config), target2, rd);
        assertEquals("Wrong gain.", expectedGain, split2.getGainValue(), 1e-8);
        final TreeNodeCondition[] childConds2 = split2.getChildConditions();
        final TreeNodeNumericCondition numCondLeft2 = (TreeNodeNumericCondition)childConds2[0];
        assertEquals("Wrong split point.", 3.5, numCondLeft2.getSplitValue(), 1e-8);
        assertFalse("Missings were not sent in the correct direction.", numCondLeft2.acceptsMissings());
        final TreeNodeNumericCondition numCondRight2 = (TreeNodeNumericCondition)childConds2[1];
        assertEquals("Wrong split point.", 3.5, numCondRight2.getSplitValue(), 1e-8);
        assertTrue("Missings were not sent in the correct direction.", numCondRight2.acceptsMissings());
    }

    /**
     * Tests the {@link TreeNumericColumnData#updateChildMemberships(TreeNodeCondition, DataMemberships)} methods with
     * different conditions including missing values.
     *
     * @throws Exception
     */
    @Test
    public void testUpdateChildMemberships() throws Exception {
        final TreeEnsembleLearnerConfiguration config = createConfig();
        final TestDataGenerator dataGen = new TestDataGenerator(config);
        final int[] indices = new int[]{0, 1, 2, 3, 4, 5, 6};
        final double[] weights = new double[7];
        Arrays.fill(weights, 1.0);
        final DataMemberships dataMem = new MockDataColMem(indices, indices, weights);
        final String noMissingsCSV = "-50, -3, -2, 2, 25, 100, 101";
        final TreeNumericColumnData col = dataGen.createNumericAttributeColumn(noMissingsCSV, "noMissings-col", 0);
        // less than or equals
        TreeNodeNumericCondition numCond =
            new TreeNodeNumericCondition(col.getMetaData(), -2, NumericOperator.LessThanOrEqual, false);
        BitSet inChild = col.updateChildMemberships(numCond, dataMem);
        BitSet expected = new BitSet(3);
        expected.set(0, 3);
        assertEquals("The produced BitSet is incorrect.", expected, inChild);
        // greater than
        numCond = new TreeNodeNumericCondition(col.getMetaData(), 10, NumericOperator.LargerThan, false);
        inChild = col.updateChildMemberships(numCond, dataMem);
        expected.clear();
        expected.set(4, 7);
        assertEquals("The produced BitSet is incorrect", expected, inChild);

        // with missing values
        final String missingsCSV = "-2, 0, 1, 43, 61, 66, NaN";
        final TreeNumericColumnData colWithMissings =
            dataGen.createNumericAttributeColumn(missingsCSV, "missings-col", 0);
        // less than or equal or missing
        numCond = new TreeNodeNumericCondition(colWithMissings.getMetaData(), 12, NumericOperator.LessThanOrEqual, true);
        inChild = colWithMissings.updateChildMemberships(numCond, dataMem);
        expected.clear();
        expected.set(0, 3);
        expected.set(6);
        assertEquals("The produced BitSet is incorrect", expected, inChild);
        // less than or equals not missing
        numCond = new TreeNodeNumericCondition(colWithMissings.getMetaData(), 12, NumericOperator.LessThanOrEqual, false);
        inChild = colWithMissings.updateChildMemberships(numCond, dataMem);
        expected.clear();
        expected.set(0, 3);
        assertEquals("The produced BitSet is incorrect", expected, inChild);
        // larger than or missing
        numCond = new TreeNodeNumericCondition(colWithMissings.getMetaData(), 43, NumericOperator.LargerThan, true);
        inChild = colWithMissings.updateChildMemberships(numCond, dataMem);
        expected.clear();
        expected.set(4, 7);
        assertEquals("The produced BitSet is incorrect", expected, inChild);
        // larger than not missing
        numCond = new TreeNodeNumericCondition(colWithMissings.getMetaData(), 12, NumericOperator.LargerThan, false);
        inChild = colWithMissings.updateChildMemberships(numCond, dataMem);
        expected.clear();
        expected.set(3, 6);
        assertEquals("The produced BitSet is incorrect", expected, inChild);
    }

}
