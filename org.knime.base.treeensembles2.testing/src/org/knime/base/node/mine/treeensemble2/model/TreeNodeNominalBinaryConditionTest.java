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
 *   05.04.2016 (adrian): created
 */
package org.knime.base.node.mine.treeensemble2.model;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.Map;

import org.junit.Test;
import org.knime.base.node.mine.decisiontree2.PMMLBooleanOperator;
import org.knime.base.node.mine.decisiontree2.PMMLCompoundPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLOperator;
import org.knime.base.node.mine.decisiontree2.PMMLPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLSetOperator;
import org.knime.base.node.mine.decisiontree2.PMMLSimplePredicate;
import org.knime.base.node.mine.decisiontree2.PMMLSimpleSetPredicate;
import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.data.TestDataGenerator;
import org.knime.base.node.mine.treeensemble2.data.TreeNominalColumnData;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;

import com.google.common.collect.Maps;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
public class TreeNodeNominalBinaryConditionTest {

    /**
     * This method tests the
     * {@link TreeNodeNominalBinaryCondition#testCondition(org.knime.base.node.mine.treeensemble2.data.PredictorRecord)}
     * method.
     *
     * @throws Exception
     */
    @Test
    public void testTestCondition() throws Exception {
        final TreeEnsembleLearnerConfiguration config = new TreeEnsembleLearnerConfiguration(false);
        final TestDataGenerator dataGen = new TestDataGenerator(config);
        final TreeNominalColumnData col = dataGen.createNominalAttributeColumn("A,A,B,C,C,D", "testcol", 0);
        TreeNodeNominalBinaryCondition cond = new TreeNodeNominalBinaryCondition(col.getMetaData(), BigInteger.valueOf(1), true, false);
        final Map<String, Object> map = Maps.newHashMap();
        final String colName = col.getMetaData().getAttributeName();
        map.put(colName, 0);
        PredictorRecord record = new PredictorRecord(map);
        assertTrue("The value A was not accepted but should have been.", cond.testCondition(record));
        map.clear();
        map.put(colName, 1);
        assertFalse("The value B was falsely accepted", cond.testCondition(record));
        map.clear();
        map.put(colName, 2);
        assertFalse("The value C was falsely accepted", cond.testCondition(record));
        map.clear();
        map.put(colName, 3);
        assertFalse("The value D was falsely accepted", cond.testCondition(record));
        map.clear();
        map.put(colName, PredictorRecord.NULL);
        assertFalse("The condition falsely accepted missing values", cond.testCondition(record));

        cond = new TreeNodeNominalBinaryCondition(col.getMetaData(), BigInteger.valueOf(5), true, true);
        map.clear();
        map.put(colName, 0);
        assertTrue("The value A was falsely rejected.", cond.testCondition(record));
        map.clear();
        map.put(colName, 2);
        assertTrue("The value C was falsely rejected.", cond.testCondition(record));
        map.clear();
        map.put(colName, PredictorRecord.NULL);
        assertTrue("Missing values were falsely rejected.", cond.testCondition(record));
        map.clear();
        map.put(colName, 1);
        assertFalse("The value B was falsely accepted.", cond.testCondition(record));
        map.clear();
        map.put(colName, 3);
        assertFalse("The value B was falsely accepted.", cond.testCondition(record));

        cond = new TreeNodeNominalBinaryCondition(col.getMetaData(), BigInteger.valueOf(5), false, true);
        map.clear();
        map.put(colName, 0);
        assertFalse("The value A was falsely accepted.", cond.testCondition(record));
        map.clear();
        map.put(colName, 2);
        assertFalse("The value C was falsely accepted.", cond.testCondition(record));
        map.clear();
        map.put(colName, PredictorRecord.NULL);
        assertTrue("Missing values were falsely rejected.", cond.testCondition(record));
        map.clear();
        map.put(colName, 1);
        assertTrue("The value B was falsely rejected.", cond.testCondition(record));
        map.clear();
        map.put(colName, 3);
        assertTrue("The value D was falsely rejected.", cond.testCondition(record));

        cond = new TreeNodeNominalBinaryCondition(col.getMetaData(), BigInteger.valueOf(5), false, false);
        map.clear();
        map.put(colName, 0);
        assertFalse("The value A was falsely accepted.", cond.testCondition(record));
        map.clear();
        map.put(colName, 2);
        assertFalse("The value C was falsely accepted.", cond.testCondition(record));
        map.clear();
        map.put(colName, PredictorRecord.NULL);
        assertFalse("Missing values were falsely accepted.", cond.testCondition(record));
        map.clear();
        map.put(colName, 1);
        assertTrue("The value B was falsely rejected.", cond.testCondition(record));
        map.clear();
        map.put(colName, 3);
        assertTrue("The value D was falsely rejected.", cond.testCondition(record));
    }

    /**
     * This method tests the
     * {@link TreeNodeNominalBinaryCondition#toPMMLPredicate()} method.
     *
     * @throws Exception
     */
    @Test
    public void testToPMMLPredicate() throws Exception {
        final TreeEnsembleLearnerConfiguration config = new TreeEnsembleLearnerConfiguration(false);
        final TestDataGenerator dataGen = new TestDataGenerator(config);
        final TreeNominalColumnData col = dataGen.createNominalAttributeColumn("A,A,B,C,C,D", "testcol", 0);
        TreeNodeNominalBinaryCondition cond = new TreeNodeNominalBinaryCondition(col.getMetaData(), BigInteger.valueOf(1), true, false);
        PMMLPredicate predicate = cond.toPMMLPredicate();
        assertThat(predicate, instanceOf(PMMLSimpleSetPredicate.class));
        PMMLSimpleSetPredicate setPredicate = (PMMLSimpleSetPredicate)predicate;
        assertEquals("Wrong attribute", col.getMetaData().getAttributeName(), setPredicate.getSplitAttribute());
        assertEquals("Wrong set predicate", PMMLSetOperator.IS_IN, setPredicate.getSetOperator());
        assertArrayEquals("Wrong values",new String[]{"A"}, setPredicate.getValues().toArray(new String[1]));

        cond = new TreeNodeNominalBinaryCondition(col.getMetaData(), BigInteger.valueOf(2), false, true);
        predicate = cond.toPMMLPredicate();
        assertEquals("Wrong attribute", col.getMetaData().getAttributeName(), predicate.getSplitAttribute());
        assertThat(predicate, instanceOf(PMMLCompoundPredicate.class));
        PMMLCompoundPredicate compoundPredicate = (PMMLCompoundPredicate)predicate;
        assertEquals("Wrong boolean operator", PMMLBooleanOperator.OR, compoundPredicate.getBooleanOperator());
        LinkedList<PMMLPredicate>preds = compoundPredicate.getPredicates();
        assertEquals("Number of predicates did not match.", 2, preds.size());
        assertThat(preds.get(0), instanceOf(PMMLSimpleSetPredicate.class));
        setPredicate = (PMMLSimpleSetPredicate)preds.get(0);
        assertEquals("Wrong attribute", col.getMetaData().getAttributeName(), setPredicate.getSplitAttribute());
        assertEquals("Wrong set predicate", PMMLSetOperator.IS_NOT_IN, setPredicate.getSetOperator());
        assertArrayEquals("Wrong values", new String[]{"B"}, setPredicate.getValues().toArray(new String[1]));
        assertThat(preds.get(1), instanceOf(PMMLSimplePredicate.class));
        PMMLSimplePredicate simplePredicate = (PMMLSimplePredicate)preds.get(1);
        assertEquals("Should be isMissing",PMMLOperator.IS_MISSING, simplePredicate.getOperator());
    }
}
