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
 *   11.02.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.data;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.knime.base.node.mine.treeensemble2.data.memberships.DataMemberships;
import org.knime.base.node.mine.treeensemble2.data.memberships.DefaultDataIndexManager;
import org.knime.base.node.mine.treeensemble2.data.memberships.RootDataMemberships;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeEnsembleModel.TreeType;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
public class TreeTargetNumericColumnDataTest {

    private final static double DELTA = 1e-6;

    /**
     * Tests the {@link TreeTargetNumericColumnData#getPriors(DataMemberships, TreeEnsembleLearnerConfiguration)} and
     * {@link TreeTargetNumericColumnData#getPriors(double[], TreeEnsembleLearnerConfiguration)} methods.
     */
    @Test
    public void testGetPriors() {
        String targetCSV = "1,4,3,5,6,7,8,12,22,1";
        // irrelevant but necessary to build TreeDataObject
        String someAttributeCSV = "A,B,A,B,A,A,B,A,A,B";
        TreeEnsembleLearnerConfiguration config = new TreeEnsembleLearnerConfiguration(true);
        TestDataGenerator dataGen = new TestDataGenerator(config);
        TreeTargetNumericColumnData target = TestDataGenerator.createNumericTargetColumn(targetCSV);
        TreeNominalColumnData attribute = dataGen.createNominalAttributeColumn(someAttributeCSV, "test-col", 0);
        TreeData data = new TreeData(new TreeAttributeColumnData[]{attribute}, target, TreeType.Ordinary);
        double[] weights = new double[10];
        Arrays.fill(weights, 1.0);
        DataMemberships rootMem = new RootDataMemberships(weights, data, new DefaultDataIndexManager(data));
        RegressionPriors datMemPriors = target.getPriors(rootMem, config);
        assertEquals(6.9, datMemPriors.getMean(), DELTA);
        assertEquals(69, datMemPriors.getYSum(), DELTA);
        assertEquals(352.9, datMemPriors.getSumSquaredDeviation(), DELTA);
    }

    /**
     * Tests the {@link TreeTargetNumericColumnData#getMedian()} method.
     */
    @Test
    public void testGetMedian() {
        String target1CSV = "1,4,3,5,6,7,8,12,22,1";
        String target2CSV = "111,103,101,102,99,22,10";
        TreeTargetNumericColumnData target1 = TestDataGenerator.createNumericTargetColumn(target1CSV);
        TreeTargetNumericColumnData target2 = TestDataGenerator.createNumericTargetColumn(target2CSV);

        assertEquals(5.5, target1.getMedian(), DELTA);
        assertEquals(101.0, target2.getMedian(), DELTA);
    }

}
