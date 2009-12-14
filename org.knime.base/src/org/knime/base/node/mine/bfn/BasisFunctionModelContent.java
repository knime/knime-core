/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   20.02.2008 (gabriel): created
 */
package org.knime.base.node.mine.bfn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class BasisFunctionModelContent {
    
    private final DataTableSpec m_spec;
    
    private final Map<DataCell, List<BasisFunctionPredictorRow>> m_bfs;
    
    /**
     * @return model spec
     */
    public DataTableSpec getSpec() {
        return m_spec;
    }
    
    /**
     * @return basisfunctions rules by class
     */
    public Map<DataCell, List<BasisFunctionPredictorRow>> getBasisFunctions() {
        return m_bfs;
    }
    
    /**
     * Loads a this basisfunction content from the given file directory.
     * @param modelCont model content to read rules and spec from
     * @param cr used to instantiate basisfunction predictor rows
     * @throws InvalidSettingsException if rule and/or spec can't be read
     */
    public BasisFunctionModelContent(final ModelContentRO modelCont,
            final BasisFunctionPortObject.Creator cr)
            throws InvalidSettingsException {
        m_bfs = new LinkedHashMap<DataCell, List<BasisFunctionPredictorRow>>();
        // load rules
        ModelContentRO ruleModel = modelCont.getModelContent("rules");
        for (String key : ruleModel.keySet()) {
            ModelContentRO bfParam = ruleModel.getModelContent(key);
            BasisFunctionPredictorRow bf = cr.createPredictorRow(bfParam);
            List<BasisFunctionPredictorRow> rows = 
                m_bfs.get(bf.getClassLabel());
            if (rows == null) {
                rows = new ArrayList<BasisFunctionPredictorRow>();
                m_bfs.put(bf.getClassLabel(), rows);
            }
            rows.add(bf);
        }
        // load spec
        ModelContentRO modelInfo = modelCont.getModelContent("model_spec");
        String[] keySet = modelInfo.keySet().toArray(new String[0]);
        DataColumnSpec[] modelSpec = new DataColumnSpec[keySet.length];
        for (int i = 0; i < keySet.length; i++) {
            modelSpec[i] = DataColumnSpec.load(modelInfo.getConfig(keySet[i]));
        }
        m_spec = new DataTableSpec(modelSpec);
    }
    
    /**
     * Creates a new basis function model object.
     * @param bfs basisfunction rules by class
     * @param spec model spec
     */
    public BasisFunctionModelContent(
            final DataTableSpec spec,
            final Map<DataCell, List<BasisFunctionLearnerRow>> bfs) {
        m_bfs = new LinkedHashMap<DataCell, List<BasisFunctionPredictorRow>>();
        for (DataCell key : bfs.keySet()) {
            List<BasisFunctionPredictorRow> rows = 
                new ArrayList<BasisFunctionPredictorRow>();
            m_bfs.put(key, rows);
            for (BasisFunctionLearnerRow bf : bfs.get(key)) {
                rows.add(bf.getPredictorRow());
            }
        }
        m_spec = spec;
    }

    /**
     * Save the given rule model and model spec into this model content object.
     * @param modelCont save spec and rule to
     */
    public void save(final ModelContentWO modelCont) {
        // save rules
        ModelContentWO ruleSpec = modelCont.addModelContent("rules");
        for (DataCell key : m_bfs.keySet()) {
            for (BasisFunctionPredictorRow bf : m_bfs.get(key)) {
                ModelContentWO bfParam = ruleSpec.addModelContent(
                        bf.getId().toString());
                bf.save(bfParam);
            }
        }
        // save spec
        ModelContentWO modelSpec = modelCont.addModelContent("model_spec");
        for (int i = 0; i < m_spec.getNumColumns(); i++) {
            DataColumnSpec cspec = m_spec.getColumnSpec(i);
            cspec.save(modelSpec.addConfig(cspec.getName()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        int total = 0;
        for (DataCell key : m_bfs.keySet()) {
            if (buf.length() > 0) {
                buf.append(" ,");
            }
            int t = m_bfs.get(key).size();
            buf.append(key.toString() + " (" + t + ")");
            total += t;
        }
        return total + " rules for class: " + buf.toString();
    }
    
}
