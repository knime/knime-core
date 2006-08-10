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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

/**
 * This predictor table appends one additional column to a {@link DataTable}
 * which is the predicted class (label) for each input row.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class BasisFunctionPredictorTable implements DataTable {
    
    /*
     * TODO add fuzzy degree output for each class
     */
    
    /**
     * The underlying data to be applied.
     */
    private final DataTable m_data;

    /**
     * A map from row key to <code>DataCell</code> holding the class label.
     */
    private final HashMap<DataCell, DataCell> m_classLabels;

    /**
     * Our table spec. Derived from the table containing the test patterns
     */
    private final DataTableSpec m_tableSpec;

    /**
     * Appends one column to the given data to make a prediction for each row
     * using the model which contains one {@link BasisFunctionPredictorRow}
     * column.
     * 
     * @param exec the execution monitor
     * @param data the data to apply
     * @param modelSpecs names and types of the rule model
     * @param model the trained model as list of rows
     * @param applyColumn the name of the applied column
     * @param dontKnowClass the <i>don't know</i> class propability
     * @throws NullPointerException if one of the arguments is <code>null</code>
     * @throws CanceledExecutionException if canceled
     */
    public BasisFunctionPredictorTable(final ExecutionMonitor exec,
            final BufferedDataTable data, final DataColumnSpec[] modelSpecs,
            final List<BasisFunctionPredictorRow> model,
            final String applyColumn, final double dontKnowClass)
            throws CanceledExecutionException {

        // check input
        assert (model != null);
        assert (data != null);
        assert (applyColumn != null);

        // remember input data to just add one column (applied data)
        m_data = data;

        // keep the model for later mapping
        if (model.size() == 0) {
            throw new IllegalArgumentException("Model must not be empty.");
        }

        String[] modelCols = new String[modelSpecs.length - 1];
        for (int i = 0; i < modelCols.length; i++) { // without class
            modelCols[i] = modelSpecs[i].getName();
        }
        FilterColumnTable filter = new FilterColumnTable(data, modelCols);

        // keep mapping from data key to predicted class label
        m_classLabels = new HashMap<DataCell, DataCell>();

        // number of rows
        int numRows = data.getRowCount();
        int rowCount = 1;

        // overall data rows
        for (RowIterator dataIt = filter.iterator(); dataIt.hasNext(); rowCount++) {
            exec.checkCanceled();
            exec.setProgress(Math.min(0.99, 1.0 * rowCount / numRows),
                    "Predicting... ");

            // get current row
            final DataRow row = dataIt.next();

            // apply current row
            Map<DataCell, double[]> map = predict(row, model, dontKnowClass,
                    false);

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
            // add row key with best class to the map
            m_classLabels.put(row.getKey().getId(), best);
        }
        // create our table spec - which is the table spec from the data table
        // plus one additional column with the most common DataCell type
        m_tableSpec = createDataTableSpec(data.getDataTableSpec(), applyColumn);
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
        final LinkedHashMap<DataCell, double[]> map = new LinkedHashMap<DataCell, double[]>();
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
        for (Iterator<BasisFunctionPredictorRow> it = model.iterator(); it.hasNext();) {
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

    /**
     * Creates a new data table spec with one additional applied column.
     * 
     * @param dataSpec the table spec of the input data
     * @param applyColumn the name of the new, applied column
     * @return the new data table spec
     */
    public static DataTableSpec createDataTableSpec(
            final DataTableSpec dataSpec, final String applyColumn) {
        // create applied spec
        DataColumnSpec colSpec = new DataColumnSpecCreator(applyColumn,
                StringCell.TYPE).createSpec();
        DataTableSpec applySpec = new DataTableSpec(
                new DataColumnSpec[]{colSpec});
        return new DataTableSpec(dataSpec, applySpec);
    }

    /**
     * @see org.knime.core.data.DataTable#getDataTableSpec()
     */
    public DataTableSpec getDataTableSpec() {
        return m_tableSpec;
    }

    /**
     * @see org.knime.core.data.DataTable#iterator()
     */
    public RowIterator iterator() {
        return new BasisFunctionPredictorRowIterator(m_data.iterator(),
                m_classLabels);
    }
}
