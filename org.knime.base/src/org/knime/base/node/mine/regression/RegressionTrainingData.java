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
 * Created on 04.11.2013 by hofer
 */
package org.knime.base.node.mine.regression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;

/** This class is a decorator for a DataTable.*/
public class RegressionTrainingData implements Iterable<RegressionTrainingRow> {
    private DataTable m_data;
    private List<Integer> m_learningCols;
    private Integer m_target;
    private Map<Integer, Boolean> m_isNominal;
    private Map<Integer, List<DataCell>> m_domainValues;

    private int m_parameterCount;

    /**
     * @param data training data.
     * @param spec port object spec.
     */
    public RegressionTrainingData(final DataTable data,
            final PMMLPortObjectSpec spec) {
        m_data = data;
        m_learningCols = new ArrayList<Integer>();
        m_isNominal = new HashMap<Integer, Boolean>();
        m_domainValues = new HashMap<Integer, List<DataCell>>();

        DataTableSpec inSpec = data.getDataTableSpec();
        m_parameterCount = 0;
        for (DataColumnSpec colSpec : spec.getLearningCols()) {
            int i = inSpec.findColumnIndex(colSpec.getName());
            if (colSpec.getType().isCompatible(NominalValue.class)) {
                // Create Design Variables
                m_learningCols.add(i);
                m_isNominal.put(i, true);
                List<DataCell> valueList = new ArrayList<DataCell>();
                valueList.addAll(colSpec.getDomain().getValues());
                Collections.sort(valueList,
                        colSpec.getType().getComparator());
                m_domainValues.put(i, valueList);
                m_parameterCount += valueList.size() - 1;
            } else {
                m_learningCols.add(i);
                m_isNominal.put(i, false);
                m_domainValues.put(i, null);
                m_parameterCount++;
            }
        }
        // the target
        DataColumnSpec colSpec = spec.getTargetCols().get(0);
        m_target = inSpec.findColumnIndex(colSpec.getName());
        if (colSpec.getType().isCompatible(NominalValue.class)) {
            // Create Design Variables
            m_isNominal.put(m_target, true);
            List<DataCell> valueList = new ArrayList<DataCell>();
            valueList.addAll(colSpec.getDomain().getValues());
            Collections.sort(valueList,
                    colSpec.getType().getComparator());
            m_domainValues.put(m_target, valueList);
        } else {
            m_isNominal.put(m_target, false);
            m_domainValues.put(m_target, null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<RegressionTrainingRow> iterator() {
        return new RegressionTrainingDataIterator(m_data.iterator(), m_target,
                m_parameterCount, m_learningCols,
                m_isNominal, m_domainValues);
    }

    /**
     * @return the regressorCount
     */
    public int getRegressorCount() {
        return m_parameterCount;
    }


    /**
     * @return the indices
     */
    public List<Integer> getActiveCols() {
        return m_learningCols;
    }

    /**
     * @return the isDesignVariable
     */
    public Map<Integer, Boolean> getIsNominal() {
        return m_isNominal;
    }

    /**
     * @return the values
     */
    public Map<Integer, List<DataCell>> getDomainValues() {
        return m_domainValues;
    }


}
