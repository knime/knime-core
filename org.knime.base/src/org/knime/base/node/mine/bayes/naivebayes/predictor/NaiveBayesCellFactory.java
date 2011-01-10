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
 * -------------------------------------------------------------------
 *
 * History
 *   02.05.2006 (koetter): created
 */
package org.knime.base.node.mine.bayes.naivebayes.predictor;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionMonitor;

import org.knime.base.data.append.column.AppendedCellFactory;
import org.knime.base.node.mine.bayes.naivebayes.datamodel.NaiveBayesModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Naive Bayes <code>AppendCellFactory</code> class which uses the given
 * <code>NaiveBayesModel</code> to predict the class membership of each row.
 * @author Tobias Koetter, University of Konstanz
 */
public class NaiveBayesCellFactory implements AppendedCellFactory,
CellFactory {

    private static final String WINNER_COLUMN_NAME = "Winner(Naive Bayes)";

    /**
     * The <code>NaiveBayesModel</code> which holds all necessary information
     * to calculate the probability for new records.
     */
    private final NaiveBayesModel m_model;

    private final List<String> m_sortedClassVals;

    private final DataTableSpec m_tableSpec;

    private final String[] m_attributeNames;

    private final boolean m_inclClassProbVals;

    private final double m_laplaceCorrector;

    /**Constructor for class NaiveBayesAlgorithm.
     * @param model the <code>NaiveBayesModel</code> which holds all necessary
     * information to calculate the probability for new records.
     * @param tableSpec the basic table specification
     * @param inclClassProbVals if the probability per class instance should
     * be appended as columns
     */
    public NaiveBayesCellFactory(final NaiveBayesModel model,
            final DataTableSpec tableSpec, final boolean inclClassProbVals) {
        this(model, tableSpec, inclClassProbVals, 0.0);
    }
    /**Constructor for class NaiveBayesAlgorithm.
     * @param model the <code>NaiveBayesModel</code> which holds all necessary
     * information to calculate the probability for new records.
     * @param tableSpec the basic table specification
     * @param inclClassProbVals if the probability per class instance should
     * be appended as columns
     * @param laplaceCorrector the Laplace corrector to use. A value greater 0
     * overcomes zero counts
     */
    public NaiveBayesCellFactory(final NaiveBayesModel model,
            final DataTableSpec tableSpec, final boolean inclClassProbVals,
            final double laplaceCorrector) {
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
        if (laplaceCorrector < 0) {
            throw new IllegalArgumentException(
                    "Laplace corrector should be positive");
        }
        m_laplaceCorrector = laplaceCorrector;
    }

    /**
     * Creates the column specification of the result columns and returns
     * them in the order they should be appended to the original table
     * specification.
     * @param model the {@link NaiveBayesModel} to use
     * @param inSpec the <code>DataTableSpec</code> of the input data to check
     * if the winner column name already exists
     * @param inclClassProbVals if the probability values should be displayed
     * @return <code>DataColumnSpec[]</code> with the column specifications
     * of the result columns
     */
    public static DataColumnSpec[] createResultColSpecs(
            final NaiveBayesModel model, final DataTableSpec inSpec,
            final boolean inclClassProbVals) {
        final String colName =
            DataTableSpec.getUniqueColumnName(inSpec, WINNER_COLUMN_NAME);
        final DataColumnSpecCreator colSpecCreator =
            new DataColumnSpecCreator(colName, model.getClassColumnDataType());
        final DataColumnSpec classColSpec = colSpecCreator.createSpec();
        if (!inclClassProbVals) {
            return new DataColumnSpec[] {classColSpec};
        }
        final List<String> classValues = model.getSortedClassValues();
        final Collection<DataColumnSpec> colSpecs =
            new ArrayList<DataColumnSpec>(classValues.size() + 1);
        colSpecs.add(classColSpec);
        colSpecCreator.setType(DoubleCell.TYPE);
        for (final String classVal : classValues) {
            colSpecCreator.setName(classVal);
            colSpecs.add(colSpecCreator.createSpec());
        }
        return colSpecs.toArray(new DataColumnSpec[0]);
    }

    /**
     * Creates the column specification of the result columns and returns
     * them in the order they should be appended to the original table
     * specification.
     * @param classColumn the class column spec
     * @param inSpec the <code>DataTableSpec</code> of the input data to check
     * if the winner column name already exists
     * @param inclClassProbVals if the probability values should be displayed
     * @return <code>DataColumnSpec[]</code> with the column specifications
     * of the result columns
     */
    public static DataColumnSpec createResultColSpecs(
            final DataColumnSpec classColumn, final DataTableSpec inSpec,
            final boolean inclClassProbVals) {
        if (inclClassProbVals) {
            return null;
        }
        final String colName =
            DataTableSpec.getUniqueColumnName(inSpec, WINNER_COLUMN_NAME);
        final DataColumnSpecCreator colSpecCreator =
            new DataColumnSpecCreator(colName, classColumn.getType());
        final DataColumnSpec classColSpec = colSpecCreator.createSpec();
        return classColSpec;
    }

    /**
     * @return the specification of the result columns in the order they
     * should be append at the end of the original table specification
     */
    public DataColumnSpec[] getResultColumnsSpec() {
        return createResultColSpecs(m_model, m_tableSpec, m_inclClassProbVals);
    }

    /**
     * {@inheritDoc}
     */
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
    public DataCell[] getCells(final DataRow row) {
        final String mostLikelyClass =
            m_model.getMostLikelyClass(m_attributeNames, row,
                    m_laplaceCorrector);
        if (mostLikelyClass == null) {
            throw new IllegalStateException("No class found for value");
        }
        final StringCell classCell = new StringCell(mostLikelyClass);
        if (!m_inclClassProbVals) {
            return new DataCell[] {classCell};
        }
        final Collection<DataCell> resultCells =
            new ArrayList<DataCell>(m_sortedClassVals.size() + 1);
        //add the class cell first
        resultCells.add(classCell);
        final double[] classProbs = m_model.getClassProbabilities(
                m_attributeNames, row, m_sortedClassVals, true,
                m_laplaceCorrector);
        //add the probability per class
        for (final double classVal : classProbs) {
            resultCells.add(new DoubleCell(classVal));
        }
        return resultCells.toArray(new DataCell[0]);
    }

    /**
     * {@inheritDoc}
     */
    public DataColumnSpec[] getColumnSpecs() {
        return getResultColumnsSpec();
    }

    /**
     * {@inheritDoc}
     */
    public void setProgress(final int curRowNr, final int rowCount,
            final RowKey lastKey, final ExecutionMonitor exec) {
        exec.setProgress(1.0 / rowCount * curRowNr);
    }
}
