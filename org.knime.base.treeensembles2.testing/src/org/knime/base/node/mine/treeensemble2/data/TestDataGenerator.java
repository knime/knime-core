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
 *   08.02.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.data;

import java.util.Arrays;

import org.apache.commons.math.random.JDKRandomGenerator;
import org.apache.commons.math.random.RandomData;
import org.apache.commons.math.random.RandomDataImpl;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeEnsembleModel.TreeType;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;

import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;

/**
 * This class can be used to create individual columns or whole TreeData objects for testing
 *
 * @author Adrian Nembach, KNIME.com
 */
public class TestDataGenerator {

    // S - sunny, O - overcast, R - rain
    public static final String TENNIS_COLUMN_DATA_CSV =
        "S,S,O,R,R,R,O,S,S,R,S,O,O,R";

    // H - hot, M - mild, C - cold
    public static final String TENNIS_TARGET_DATA_CSV =
        "H,H,H,M,C,C,C,M,C,M,M,M,H,M";

    private final TreeEnsembleLearnerConfiguration m_config;

    public TestDataGenerator(final TreeEnsembleLearnerConfiguration config) {
        m_config = config;
    }

    public TreeData createTennisData() {
        TreeTargetNominalColumnData target = createNominalTargetColumn(TENNIS_TARGET_DATA_CSV);
        TreeNominalColumnData col = createNominalAttributeColumn(TENNIS_COLUMN_DATA_CSV, "test-col", 0);
        return new TreeData(new TreeAttributeColumnData[]{col}, target, TreeType.Ordinary);
    }

    private static final double[] asDataArray(final String dataCSV) {
        Iterable<Double> data = Doubles.stringConverter().convertAll(Arrays.asList(dataCSV.split(", *")));
        return Doubles.toArray(Lists.newArrayList(data));
    }

    private static final String[] asStringArray(final String stringCSV) {
        return stringCSV.split(", *");
    }

    public static TreeTargetNominalColumnData createNominalTargetColumn(final String stringCSV) {
        String[] values = asStringArray(stringCSV);
        return createNominalTargetColumn(values);
    }

    public static TreeTargetNominalColumnData createNominalTargetColumn(final String[] values) {
        DataColumnDomainCreator dc = new DataColumnDomainCreator(Arrays.stream(values).distinct().map(s -> new StringCell(s))
            .toArray(i -> new StringCell[i]));
        DataColumnSpecCreator specCreator = new DataColumnSpecCreator("test-target", StringCell.TYPE);
        specCreator.setDomain(dc.createDomain());
        DataColumnSpec targetSpec = specCreator.createSpec();
        TreeTargetColumnDataCreator targetCreator = new TreeTargetNominalColumnDataCreator(targetSpec);
        for (int i = 0; i < values.length; i++) {
            RowKey rowKey = RowKey.createRowKey((long)i);
            targetCreator.add(rowKey, new StringCell(values[i]));
        }
        return (TreeTargetNominalColumnData)targetCreator.createColumnData();
    }

    public TreeOrdinaryNumericColumnData createNumericAttributeColumn(final String dataCSV, final String name,
        final int attributeIndex) {
        double[] data = asDataArray(dataCSV);
        return createNumericAttributeColumnData(data, name, attributeIndex);
    }

    public TreeOrdinaryNumericColumnData createNumericAttributeColumnData(final double[] values, final String name, final int attributeIndex) {
        DataColumnSpec colSpec = new DataColumnSpecCreator(name, DoubleCell.TYPE).createSpec();
        TreeOrdinaryNumericColumnDataCreator colCreator = new TreeOrdinaryNumericColumnDataCreator(colSpec);
        for (int i = 0; i < values.length; i++) {
            final RowKey key = RowKey.createRowKey((long)i);
            if (Double.isNaN(values[i])) {
                colCreator.add(key, new MissingCell(null));
            } else {
                colCreator.add(key, new DoubleCell(values[i]));
            }
        }
        TreeOrdinaryNumericColumnData col = colCreator.createColumnData(0, m_config);
        col.getMetaData().setAttributeIndex(attributeIndex);
        return col;
    }

    /**
     * Creates a TreeNominalColumnData for testing purposes.
     * If the column should contain missing values, insert a '?' for the rows that should be missing
     *
     * @param stringCSV csv representation of column
     * @param name the name the column should have
     * @param attributeIndex the index of the column
     * @return column containing nominal values
     */
    public TreeNominalColumnData createNominalAttributeColumn(final String stringCSV, final String name,
        final int attributeIndex) {
        String[] values = asStringArray(stringCSV);
        return createNominalAttributeColumn(values, name, attributeIndex);
    }

    public TreeNominalColumnData createNominalAttributeColumn(final String[] values, final String name, final int attributeIndex) {
        DataColumnSpec colSpec = new DataColumnSpecCreator(name, StringCell.TYPE).createSpec();
        TreeNominalColumnDataCreator colCreator = new TreeNominalColumnDataCreator(colSpec);
        for (int i = 0; i < values.length; i++) {
            RowKey rowKey = RowKey.createRowKey((long)i);
            if (values[i].equals("?")) {
                colCreator.add(rowKey, new MissingCell(null));
            } else {
                colCreator.add(rowKey, new StringCell(values[i]));
            }
        }
        TreeNominalColumnData col = colCreator.createColumnData(0, m_config);
        col.getMetaData().setAttributeIndex(attributeIndex);
        return col;
    }

    public static TreeTargetNumericColumnData createNumericTargetColumn(final String dataCSV) {
        double[] values = asDataArray(dataCSV);
        DataColumnSpec targetSpec = new DataColumnSpecCreator("test-target", DoubleCell.TYPE).createSpec();
        TreeTargetNumericColumnDataCreator targetCreator = new TreeTargetNumericColumnDataCreator(targetSpec);
        for (int i = 0; i < values.length; i++) {
            RowKey rowKey = RowKey.createRowKey((long)i);
            targetCreator.add(rowKey, new DoubleCell(values[i]));
        }
        return targetCreator.createColumnData();
    }

    public static RandomData createRandomData() {
        JDKRandomGenerator randomGenerator = new JDKRandomGenerator();
        randomGenerator.setSeed(System.currentTimeMillis());
        return new RandomDataImpl(randomGenerator);
    }

    /**
     * Creates a TreeData object of type Ordinary (meaning no vector values)
     *
     * @param target the target of the tree ensemble should be learned for
     * @param columns the attribute columns
     * @return TreeData object that is used for learning TreeEnsembles
     */
    public TreeData createTreeData(final TreeTargetColumnData target, final TreeAttributeColumnData... columns) {
        return new TreeData(columns, target, TreeType.Ordinary);
    }


}
