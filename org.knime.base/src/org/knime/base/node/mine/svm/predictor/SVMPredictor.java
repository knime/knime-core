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
 * ---------------------------------------------------------------------
 * 
 * History
 *   02.10.2007 (cebron): created
 */
package org.knime.base.node.mine.svm.predictor;

import java.util.ArrayList;

import org.knime.base.node.mine.svm.Svm;
import org.knime.base.node.mine.svm.util.DoubleVector;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionMonitor;

/**
 * This {@link CellFactory} produces the class values for each
 * input {@link DataRow}.
 * @author cebron, University of Konstanz
 */
public class SVMPredictor implements CellFactory {

    private Svm[] m_svms;
    
    private int[] m_colindices;
    
    /**
     * Constructor.
     * @param svms the Support Vector Machine(s) to use.
     * @param colindices the column indices to use in each row.
     */
    public SVMPredictor(final Svm[] svms, final int[] colindices) {
        m_svms = svms;
        m_colindices = colindices;
    }
    /**
     * {@inheritDoc}
     */
    public DataCell[] getCells(final DataRow row) {
        ArrayList<Double> values = new ArrayList<Double>();
        for (int i = 0; i < m_colindices.length; i++) {
            if (row.getCell(m_colindices[i]).isMissing()) {
                return new DataCell[]{DataType.getMissingCell()};
            }
            DoubleValue dv = (DoubleValue) row.getCell(m_colindices[i]);
            values.add(dv.getDoubleValue());
        }
        String classvalue = doPredict(values);
        return new DataCell[]{new StringCell(classvalue)};
    }

    /**
     * Given a vector, find out it's class.
     * 
     * @param values the parameters.
     */
    private String doPredict(final ArrayList<Double> values) {
        DoubleVector vector = new DoubleVector(values, "not_known_yet");
        int pos = 0;
        double bestDistance = m_svms[0].distance(vector);
        for (int i = 1; i < m_svms.length; ++i) {
            if (m_svms[i].distance(vector) > bestDistance) {
                pos = i;
                bestDistance = m_svms[i].distance(vector);
            }
        }
        return m_svms[pos].getPositive();
    }
    
    /**
     * {@inheritDoc}
     */
    public DataColumnSpec[] getColumnSpecs() {
        DataColumnSpecCreator colspeccreator =
                new DataColumnSpecCreator("SVM Prediction", StringCell.TYPE);
        return new DataColumnSpec[]{colspeccreator.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    public void setProgress(final int curRowNr, final int rowCount,
            final RowKey lastKey, final ExecutionMonitor exec) {
        exec.setProgress((double)curRowNr / (double)rowCount, "Classifying");
    }

}
