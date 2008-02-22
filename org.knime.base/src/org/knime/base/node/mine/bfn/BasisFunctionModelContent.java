/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.PortType;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class BasisFunctionModelContent extends ModelContent {

    /** The <code>PortType</code> for basisfunction models. */
    static final PortType TYPE = new PortType(DataTableSpec.class, 
            BasisFunctionModelContent.class);
    
    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec getSpec() {
        try {
            return loadSpec();
        } catch (InvalidSettingsException ise) {
            return null;
        }
    }
    
    /**
     * Save the given rule model and model spec into this model content object.
     * @param name the name of this model content
     * @param bfs the rules to save
     * @param spec the model spec to save
     */
    public BasisFunctionModelContent(final String name,
            final Map<DataCell, List<BasisFunctionLearnerRow>> bfs,
            final DataTableSpec spec) {
        super(name);
        ModelContentWO ruleSpec = super.addModelContent("rules");
        for (DataCell key : bfs.keySet()) {
            List<BasisFunctionLearnerRow> list = bfs.get(key);
            for (BasisFunctionLearnerRow bf : list) {
                BasisFunctionPredictorRow predBf = bf.getPredictorRow();
                ModelContentWO bfParam = ruleSpec.addModelContent(bf.getKey()
                        .getId().toString());
                predBf.save(bfParam);
            }
        }
        // add used columns
        ModelContentWO modelSpec = super.addModelContent("model_spec");
        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataColumnSpec cspec = spec.getColumnSpec(i);
            cspec.save(modelSpec.addConfig(cspec.getName()));
        }
    }
    
    /**
     * Reads the rule model used for prediction from the
     * <code>ModelContentRO</code> object.
     * @param model predictor model used to create the rules
     * @return a list of basisfunction rules
     * @throws InvalidSettingsException if the model contains invalid settings
     */
    public List<BasisFunctionPredictorRow> loadBasisFunctions(
            final BasisFunctionPredictorNodeModel model) 
            throws InvalidSettingsException {
        List<BasisFunctionPredictorRow> rows = 
            new ArrayList<BasisFunctionPredictorRow>();
        ModelContentRO ruleModel = super.getModelContent("rules");
        for (String key : ruleModel.keySet()) {
            ModelContentRO bfParam = ruleModel.getModelContent(key);
            BasisFunctionPredictorRow bf = model.createPredictorRow(bfParam);
            rows.add(bf);
        }
        return rows;
    }
    
    /**
     * Loads the model spec from this model content.
     * @return a new data table spec as model spec
     * @throws InvalidSettingsException if the spec could not be loaded
     */
    public DataTableSpec loadSpec() throws InvalidSettingsException {
        // load model info
        ModelContentRO modelInfo = super.getModelContent("model_spec");
        Set<String> keySet = modelInfo.keySet();
        DataColumnSpec[] modelSpec = new DataColumnSpec[keySet.size()];
        int idx = 0;
        for (String key : keySet) {
            modelSpec[idx] = DataColumnSpec.load(modelInfo.getConfig(key));
            idx++;
        }
        return new DataTableSpec(modelSpec);
    }
    
}
