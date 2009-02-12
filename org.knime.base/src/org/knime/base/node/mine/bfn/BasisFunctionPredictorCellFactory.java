/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.knime.base.data.filter.column.FilterColumnRow;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.ExecutionMonitor;

/**
 * This predictor cell factory predicts the passed rows using the underlying
 * basisfunction model.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class BasisFunctionPredictorCellFactory implements CellFactory {
    
    private final Map<DataCell, List<BasisFunctionPredictorRow>> m_model;
    
    private final int[] m_filteredColumns;
    
    private final double m_dontKnowClass;
    
    private final boolean m_normClass;
    
    private final DataColumnSpec[] m_specs;
    
    private final boolean m_appendClassProps;
    
    /**
     * Create new predictor cell factory. Only used to create the 
     * <code>ColumnRearranger</code> with the appended model spec.
     * @param specs the appended column specs
     * @param appendClassProps if class probabilities should be append 
     */
    public BasisFunctionPredictorCellFactory(final DataColumnSpec[] specs,
            final boolean appendClassProps) {
        m_model = null;
        m_filteredColumns = null;
        m_dontKnowClass = Double.NaN;
        m_normClass = false;
        m_specs = specs;
        m_appendClassProps = appendClassProps;
    }

    /**
     * Appends one column to the given data to make a prediction for each row
     * using the model which contains one {@link BasisFunctionPredictorRow}
     * column.
     * @param dataSpec the spec of the test data
     * @param specs names and types of the rule model
     * @param filteredColumns use only those column for prediction (part of the
     *        of training data)
     * @param model the trained model as list of rows
     * @param dontKnowClass the don't know class probability
     * @param normClass normalize classification output
     * @param appendClassProps if class probabilities should be append  
     * @throws NullPointerException if one of the arguments is <code>null</code>
     */
    public BasisFunctionPredictorCellFactory(final DataTableSpec dataSpec, 
            final DataColumnSpec[] specs,
            final int[] filteredColumns,
            final Map<DataCell, List<BasisFunctionPredictorRow>> model,
            final double dontKnowClass,
            final boolean normClass,
            final boolean appendClassProps) {
        assert (model != null);
        m_model = model;
        m_dontKnowClass = dontKnowClass;
        m_normClass = normClass;
        m_specs = specs;
        m_filteredColumns = filteredColumns;
        m_appendClassProps = appendClassProps;
    }
    
    /**
     * Predicts an unknown row to the given model.
     * @param row the row to predict
     * @param model a list of rules
     * @return mapping class label to array of assigned class degrees
     */
    protected DataCell[] predict(final DataRow row,
            final Map<DataCell, List<BasisFunctionPredictorRow>> model) {
        // maps class to activation
        Map<DataCell, Double> map = new LinkedHashMap<DataCell, Double>();
        // overall basisfunctions in the model
        for (DataCell key : model.keySet()) {
            for (BasisFunctionPredictorRow bf : model.get(key)) {
                DataCell classInfo = bf.getClassLabel();
                double act;
                if (map.containsKey(classInfo)) {
                    act = bf.compose(row, map.get(classInfo));
                } else {
                    act = bf.compose(row, 0.0);
                }
                map.put(classInfo, act);
            }
        }
        
        // hash column specs
        DataTableSpec hash = new DataTableSpec(m_specs);
        
        // find best class activation index
        DataCell best = DataType.getMissingCell();
        // set default highest activation, not yet set
        double hact = -1.0; 
        double sumAct = 0.0;
        Double[] act = new Double[m_specs.length]; 
        for (DataCell cell : map.keySet()) {
            Double d = map.get(cell);
            if (d > hact || (d == hact && best.isMissing())) {
                hact = d;
                best = cell;
            }
            int idx = hash.findColumnIndex(cell.toString());
            if (idx >= 0) {
                act[idx] = d;
                sumAct += d;
            }
        }
  
        // all class values
        DataCell[] res = new DataCell[act.length];
        // skip last column which is the winner
        for (int i = 0; i < res.length - 1; i++) {
            if (act[i] == null) {
                res[i] = new DoubleCell(0.0);
            } else {
                if (m_normClass && sumAct > 0) {
                    res[i] = new DoubleCell(act[i] / sumAct);
                } else {
                    res[i] = new DoubleCell(act[i]);
                }
            }
        }
        // insert class label
        if (hact == 0.0 || hact < m_dontKnowClass) {
            res[res.length - 1] = DataType.getMissingCell();
        } else {
            res[res.length - 1] = best;
        }
        return res;
    }

    /**
     * Predicts given row using the underlying basis function model.
     * {@inheritDoc}
     */
    public DataCell[] getCells(final DataRow row) {
        DataRow wRow = new FilterColumnRow(row, m_filteredColumns);
        DataCell[] pred = predict(wRow, m_model);
        if (m_appendClassProps) {
            // complete prediction including class probs and label
            return pred;
        } else {
            // don't append class probabilities
            return new DataCell[]{pred[pred.length - 1]};
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public DataColumnSpec[] getColumnSpecs() {
        if (m_appendClassProps) {
            return m_specs;
        } else {
            return new DataColumnSpec[]{m_specs[m_specs.length - 1]};
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setProgress(final int curRowNr, final int rowCount, 
            final RowKey lastKey, final ExecutionMonitor exec) {
        exec.setProgress((double) curRowNr / rowCount,
                "Predicting row \"" + lastKey.getString() + "\"");
    }
}
