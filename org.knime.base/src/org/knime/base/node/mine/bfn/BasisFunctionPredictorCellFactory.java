/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.knime.base.data.filter.column.FilterColumnRow;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.SingleCellFactory;

/**
 * This predictor cell factory predicts the passed rows using the underlying
 * basisfunction model.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class BasisFunctionPredictorCellFactory extends SingleCellFactory {
    
    /*
     * TODO add fuzzy degree output for each class
     */
    
    private final List<BasisFunctionPredictorRow> m_model;
    
    private final double m_dontKnowClass;
    
    private final boolean m_normalize;
    
    private final int[] m_filteredRows;
    
    /**
     * Create new predictor cell factory. Only used to create the 
     * <code>ColumnRearranger</code> with the appended model spec.
     * @param targetSpec The spec of the targetColumn.
     */
    public BasisFunctionPredictorCellFactory(final DataColumnSpec targetSpec) {
        super(targetSpec);
        m_model = null;
        m_dontKnowClass = 0.0;
        m_normalize = false;
        m_filteredRows = null;
    }

    /**
     * Appends one column to the given data to make a prediction for each row
     * using the model which contains one {@link BasisFunctionPredictorRow}
     * column.
     * 
     * @param dataSpec the spec of the test data
     * @param modelSpecs names and types of the rule model
     * @param model the trained model as list of rows
     * @param applyColumn the name of the applied column
     * @param dontKnowClass the <i>don't know</i> class propability
     * @param normalize if true, resulting class degrees are normalized to sum
     *        up to one.
     * @throws NullPointerException if one of the arguments is <code>null</code>
     */
    public BasisFunctionPredictorCellFactory(final DataTableSpec dataSpec, 
            final DataColumnSpec[] modelSpecs,
            final List<BasisFunctionPredictorRow> model,
            final DataColumnSpec applyColumn, final double dontKnowClass,
            final boolean normalize) {
        super(applyColumn);
        // check input
        assert (model != null);
        assert (applyColumn != null);
        m_dontKnowClass = dontKnowClass;
        m_normalize = normalize;

        // keep the model for later mapping
        if (model.size() == 0) {
            throw new IllegalArgumentException("Model must not be empty.");
        }
        m_model = model;

        m_filteredRows = new int[modelSpecs.length - 1];
        for (int i = 0; i < m_filteredRows.length; i++) { // without class
            String name = modelSpecs[i].getName();
            m_filteredRows[i] = dataSpec.findColumnIndex(name);
        }
    }
    
    /**
     * Predicts an unknow row to the given model.
     * 
     * @param row The row to predict
     * @param model to this model
     * @param dontKnowClass with this don't know class probability
     * @param normalize if class degrees should be normalized
     * @return mapping class label to array of assigned class degrees
     */
    public static final Map<DataCell, double[]> predict(final DataRow row,
            final List<BasisFunctionPredictorRow> model,
            final double dontKnowClass, final boolean normalize) {
        // number of predcited classes: classLabel->activation,#hits
        final LinkedHashMap<DataCell, double[]> map = 
            new LinkedHashMap<DataCell, double[]>();
        DataCell missing = DataType.getMissingCell();
        double[] dontKnow;
        // add don't know class
        if (dontKnowClass >= 0.0 && dontKnowClass <= 1.0) {
            dontKnow = new double[]{dontKnowClass, 0.0};
            map.put(missing, dontKnow);
        } else {
            if (model.size() > 0) {
                BasisFunctionPredictorRow bf = model.iterator().next();
                dontKnow = new double[]{bf.getDontKnowClassDegree(), 0.0};
                map.put(missing, dontKnow);
            } else {
                map.put(missing, new double[]{0.0, 0.0});
                return map;
            }
        }

        // overall basisfunctions in the model
        for (Iterator<BasisFunctionPredictorRow> it = model.iterator(); 
                it.hasNext();) {
            BasisFunctionPredictorRow bf = it.next();
            // get its class label
            DataCell classInfo = bf.getClassLabel();
            double act = 0.0;
            double cls = 0.0;
            // check if class label is already used
            if (map.containsKey(classInfo)) {
                // get current activation
                act = map.get(classInfo)[0];
                // get number of bfs for this class
                cls = map.get(classInfo)[1] + 1.0;
            }
            // per default
            act = bf.compose(row, act);
            if (act >= dontKnowClass) {
                // compute and set (new) activation degree
                map.put(classInfo, new double[]{act, cls});
            } else {
                cls = map.get(missing)[1] + 1.0;
                map.put(missing, new double[]{dontKnow[0], cls});
            }
        }

        if (normalize) {
            double overall = 0.0;
            for (DataCell classLabel : map.keySet()) {
                double[] value = map.get(classLabel);
                overall += value[0];
            }
            if (overall > 0.0) {
                for (DataCell classLabel : map.keySet()) {
                    double[] value = map.get(classLabel);
                    map.put(classLabel, new double[]{value[0] / overall,
                            value[1]});
                }
            }
        }
        return map;
    }
    
    private static DataCell findBestClass(final Map<DataCell, double[]> map) {
        // find best class label
        DataCell best = null;
        // highest activation
        double hact = -1.0;
        // best class
        double bcls = -1;
        // overall class labels
        for (DataCell cur : map.keySet()) {
            double act = map.get(cur)[0];
            assert (act >= 0.0) : "activation = " + act;
            double cls = map.get(cur)[1];
            assert (cls >= 0.0) : "hits = " + cls;
            if (act > hact) {
                hact = act;
                bcls = cls;
                best = cur;
            } else if (act == hact) {
                if (cls > bcls) {
                    hact = act;
                    bcls = cls;
                    best = cur;
                }
            }
        }
        assert (best != null);
        return best;
    }

    /**
     * Predicts given row using the underlying basisfunction model.
     * @see org.knime.core.data.container.SingleCellFactory#getCell(DataRow)
     */
    @Override
    public DataCell getCell(final DataRow row) {
        DataRow wRow = new FilterColumnRow(row, m_filteredRows);
        return findBestClass(
                predict(wRow, m_model, m_dontKnowClass, m_normalize));
    }
}
