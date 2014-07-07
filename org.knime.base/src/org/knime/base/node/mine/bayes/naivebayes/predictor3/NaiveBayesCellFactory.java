/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.mine.bayes.naivebayes.predictor3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.knime.base.data.append.column.AppendedCellFactory;
import org.knime.base.node.mine.bayes.naivebayes.datamodel2.NaiveBayesModel;
import org.knime.base.node.mine.util.PredictorHelper;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.pmml.PMMLDataDictionaryTranslator;

/**
 * Naive Bayes <code>AppendCellFactory</code> class which uses the given
 * <code>NaiveBayesModel</code> to predict the class membership of each row.
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 */
class NaiveBayesCellFactory implements AppendedCellFactory, CellFactory {

    /**
     * The <code>NaiveBayesModel</code> which holds all necessary information
     * to calculate the probability for new records.
     */
    private final NaiveBayesModel m_model;

    private final List<String> m_sortedClassVals;

    private final DataTableSpec m_tableSpec;

    private final String[] m_attributeNames;

    private final boolean m_inclClassProbVals;

    private final String m_columnName;

    private final String m_suffix;

    /**Constructor for class NaiveBayesAlgorithm.
     * @param model the <code>NaiveBayesModel</code> which holds all necessary
     * information to calculate the probability for new records.
     * @param columnName the prediction column name
     * @param tableSpec the basic table specification
     * @param inclClassProbVals if the probability per class instance should
     * be appended as columns
     * @param suffix the suffix for the probability columns
     */
    NaiveBayesCellFactory(final NaiveBayesModel model, final String columnName,
            final DataTableSpec tableSpec, final boolean inclClassProbVals, final String suffix) {
        this.m_columnName = columnName;
        this.m_suffix = suffix;
        if (model == null) {
           throw new NullPointerException("Model must not be null.");
        }
        m_model = model;
        m_sortedClassVals = model.getSortedClassValues();
        if (tableSpec == null) {
            throw new NullPointerException("TableSpec must not be null.");
         }
        m_tableSpec = tableSpec;
        m_inclClassProbVals = inclClassProbVals;
        m_attributeNames = new String[tableSpec.getNumColumns()];
        for (int i = 0, length = tableSpec.getNumColumns(); i < length; i++) {
            m_attributeNames[i] = tableSpec.getColumnSpec(i).getName();
        }
    }

    /**
     * Creates the column specification of the result columns and returns
     * them in the order they should be appended to the original table
     * specification.
     * @param model the {@link NaiveBayesModel} to use
     * @param predictionColName the name of the prediction column
     * @param inSpec the <code>DataTableSpec</code> of the input data to check
     * if the winner column name already exists
     * @param inclClassProbVals if the probability values should be displayed
     * @param suffix the suffix for the probability columns
     * @return <code>DataColumnSpec[]</code> with the column specifications
     * of the result columns
     */
    private static DataColumnSpec[] createResultColSpecs(
            final NaiveBayesModel model, final String predictionColName, final DataTableSpec inSpec,
            final boolean inclClassProbVals, final String suffix) {
        final DataColumnSpec classColSpec =
                createPredictedClassColSpec(predictionColName, model.getClassColumnDataType(), inSpec);
        if (!inclClassProbVals) {
            return new DataColumnSpec[] {classColSpec};
        }
        final List<String> classValues = model.getSortedClassValues();
        final Collection<DataColumnSpec> colSpecs = new ArrayList<>(classValues.size() + 1);
        final DataColumnSpecCreator colSpecCreator = new DataColumnSpecCreator("dummy", DoubleCell.TYPE);
        final PredictorHelper predictorHelper = PredictorHelper.getInstance();
        for (final String classVal : classValues) {
            colSpecCreator.setName(predictorHelper.probabilityColumnName(model.getClassColumnName(), classVal, suffix));
            colSpecs.add(colSpecCreator.createSpec());
        }
        colSpecs.add(classColSpec);
        return colSpecs.toArray(new DataColumnSpec[0]);
    }

    /**
     * Creates the column specification of the result columns and returns
     * them in the order they should be appended to the original table
     * specification.
     * @param classColumnName the class column name
     * @param classType the class column data type
     * @param inSpec the <code>DataTableSpec</code> of the input data to check
     * if the winner column name already exists
     * @param inclClassProbVals if the probability values should be displayed
     * @return <code>DataColumnSpec[]</code> with the column specifications
     * of the result columns
     */
    static DataColumnSpec createResultColSpecs(
            final String classColumnName, final DataType classType, final DataTableSpec inSpec,
            final boolean inclClassProbVals) {
        if (inclClassProbVals) {
            return null;
        }
        final DataColumnSpec classColSpec = createPredictedClassColSpec(classColumnName, classType, inSpec);
        return classColSpec;
    }

    private static DataColumnSpec createPredictedClassColSpec(final String classColumnName, final DataType classType,
        final DataTableSpec inSpec) {
        final String colName = DataTableSpec.getUniqueColumnName(inSpec, classColumnName);
        //we have to do this back and forth conversion because long data cells are converted into double by PMML
        //that is why we convert the KNIME type to PMML to see what PMML uses as type and then use the PMML type
        //to inver the right KNIME type
        final DataType pmmlConformDataType = PMMLDataDictionaryTranslator.getKNIMEDataType(
         PMMLDataDictionaryTranslator.getPMMLDataType(classType));
        final DataColumnSpecCreator colSpecCreator =
                new DataColumnSpecCreator(colName, pmmlConformDataType);
        final DataColumnSpec classColSpec = colSpecCreator.createSpec();
        return classColSpec;
    }

    /**
     * @return the specification of the result columns in the order they
     * should be append at the end of the original table specification
     */
    DataColumnSpec[] getResultColumnsSpec() {
        return createResultColSpecs(m_model, m_columnName, m_tableSpec, m_inclClassProbVals, m_suffix);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell[] getAppendedCell(final DataRow row) {
       return getCells(row);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (m_model != null) {
            return m_model.toString();
        }
        return "Model not available";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell[] getCells(final DataRow row) {
        final DataCell predictedClassCell = m_model.getMostLikelyClassCell(m_attributeNames, row);
        if (predictedClassCell == null) {
            throw new IllegalStateException("No class found for row with id " + row.getKey());
        }
        if (!m_inclClassProbVals) {
            return new DataCell[] {predictedClassCell};
        }
        final Collection<DataCell> resultCells = new ArrayList<>(m_sortedClassVals.size() + 1);
        final double[] classProbs = m_model.getClassProbabilities(m_attributeNames, row, m_sortedClassVals, true);
        //add the probability per class
        for (final double classVal : classProbs) {
            resultCells.add(new DoubleCell(classVal));
        }
        //add the class cell last
        resultCells.add(predictedClassCell);
        return resultCells.toArray(new DataCell[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataColumnSpec[] getColumnSpecs() {
        return getResultColumnsSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProgress(final int curRowNr, final int rowCount,
            final RowKey lastKey, final ExecutionMonitor exec) {
        exec.setProgress(1.0 / rowCount * curRowNr);
    }
}
