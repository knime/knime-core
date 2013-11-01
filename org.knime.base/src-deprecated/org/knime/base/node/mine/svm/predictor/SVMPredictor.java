/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
