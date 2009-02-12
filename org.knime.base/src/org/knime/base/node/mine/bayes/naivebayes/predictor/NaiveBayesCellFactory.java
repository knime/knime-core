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
 * -------------------------------------------------------------------
 *
 * History
 *   02.05.2006 (koetter): created
 */
package org.knime.base.node.mine.bayes.naivebayes.predictor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.knime.base.data.append.column.AppendedCellFactory;
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
import org.knime.base.node.mine.bayes.naivebayes.datamodel.NaiveBayesModel;

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

    /**Constructor for class NaiveBayesAlgorithm.
     * @param model the <code>NaiveBayesModel</code> which holds all necessary
     * information to calculate the probability for new records.
     * @param tableSpec the basic table specification
     * @param inclClassProbVals if the probability per class instance should
     * be appended as columns
     */
    public NaiveBayesCellFactory(final NaiveBayesModel model,
            final DataTableSpec tableSpec, final boolean inclClassProbVals) {
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
            m_model.getMostLikelyClass(m_attributeNames, row);
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
        final double[] classProbs = m_model.getClassPobabilites(
                m_attributeNames, row, m_sortedClassVals, true);
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
