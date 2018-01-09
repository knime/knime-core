/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jan 10, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.node.regressiontree.predictor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.base.data.filter.column.FilterColumnRow;
import org.knime.base.node.mine.treeensemble.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble.model.RegressionTreeModel;
import org.knime.base.node.mine.treeensemble.model.RegressionTreeModelPortObject;
import org.knime.base.node.mine.treeensemble.model.RegressionTreeModelPortObjectSpec;
import org.knime.base.node.mine.treeensemble.model.TreeModelRegression;
import org.knime.base.node.mine.treeensemble.model.TreeNodeRegression;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.UniqueNameGenerator;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public final class RegressionTreePredictorCellFactory extends AbstractCellFactory {

    private final RegressionTreePredictor m_predictor;

    private final DataTableSpec m_learnSpec;

    private final int[] m_learnColumnInRealDataIndices;

    private RegressionTreePredictorCellFactory(final RegressionTreePredictor predictor,
        final DataColumnSpec[] appendSpecs, final int[] learnColumnInRealDataIndices) {
        super(appendSpecs);
        setParallelProcessing(true);
        m_predictor = predictor;
        m_learnSpec = predictor.getModelSpec().getLearnTableSpec();
        m_learnColumnInRealDataIndices = learnColumnInRealDataIndices;
    }

    /**
     * @param predictor
     * @return factory based on RegressionTreePredictor <b>predictor</b>
     * @throws InvalidSettingsException
     *  */
    public static RegressionTreePredictorCellFactory createFactory(final RegressionTreePredictor predictor)
        throws InvalidSettingsException {
        DataTableSpec testDataSpec = predictor.getDataSpec();
        RegressionTreeModelPortObjectSpec modelSpec = predictor.getModelSpec();
        RegressionTreePredictorConfiguration configuration = predictor.getConfiguration();
        UniqueNameGenerator nameGen = new UniqueNameGenerator(testDataSpec);
        List<DataColumnSpec> newColsList = new ArrayList<DataColumnSpec>();
        String targetColName = configuration.getPredictionColumnName();
        DataColumnSpec targetCol = nameGen.newColumn(targetColName, DoubleCell.TYPE);
        newColsList.add(targetCol);
        DataColumnSpec[] newCols = newColsList.toArray(new DataColumnSpec[newColsList.size()]);
        int[] learnColumnInRealDataIndices = modelSpec.calculateFilterIndices(testDataSpec);
        return new RegressionTreePredictorCellFactory(predictor, newCols, learnColumnInRealDataIndices);
    }

    /** {@inheritDoc} */
    @Override
    public DataCell[] getCells(final DataRow row) {
        RegressionTreeModelPortObject modelObject = m_predictor.getModelObject();
        final RegressionTreeModel treeModel = modelObject.getModel();
        int size = 1;
        DataCell[] result = new DataCell[size];
        DataRow filterRow = new FilterColumnRow(row, m_learnColumnInRealDataIndices);
        PredictorRecord record = treeModel.createPredictorRecord(filterRow, m_learnSpec);
        if (record == null) { // missing value
            Arrays.fill(result, DataType.getMissingCell());
            return result;
        }

        TreeModelRegression tree = treeModel.getTreeModel();
        TreeNodeRegression match = tree.findMatchingNode(record);
        double nodeMean = match.getMean();
        result[0] = new DoubleCell(nodeMean);
        return result;
    }
}
