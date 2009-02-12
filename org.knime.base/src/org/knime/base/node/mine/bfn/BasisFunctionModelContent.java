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
