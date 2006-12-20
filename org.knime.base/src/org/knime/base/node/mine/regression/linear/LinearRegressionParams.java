/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Feb 23, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.regression.linear;

import java.util.LinkedHashMap;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;


/**
 * Utility class that carries out the loading and saving of linear regression
 * models. It is used by the learner node model and the predictor node model.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class LinearRegressionParams {

    private static final String CFG_SUB_PARAMS = "sub_params";

    private static final String CFG_PARA_NAME = "cell_name";

    private static final String CFG_PARA_VALUE = "para_value";

    private static final String CFG_PARA_MEAN = "para_mean";

    private static final String CFG_OFFSET_NAME = "offset_name";

    private final Map<String, Double> m_map;

    private final Map<String, Double> m_means;

    /**
     * Create new object with the given parameters.
     * 
     * @param map the map input col name -> value
     * @param means the map input col name -> mean
     */
    public LinearRegressionParams(final Map<String, Double> map,
            final Map<String, Double> means) {
        if (map == null || means == null) {
            throw new NullPointerException();
        }
        boolean first = true;
        for (String c : map.keySet()) {
            if (first) {
                first = false;
                continue;
            }
            if (!means.containsKey(c)) {
                throw new IllegalArgumentException(
                        "No mean value for variable " + c);
            }
        }
        if (first) {
            throw new IllegalArgumentException("No reponse column set.");
        }
        m_map = map;
        m_means = means;
    }

    /**
     * Get reference to the means map, i.e. input column name -> mean value. The
     * mean values of all included columns are used for visualization. (The view
     * shows the 2D-regression line on one input variable. We use the mean
     * values of the remaining variables to determine the two points that define
     * the regression line.)
     * 
     * @return the reference
     */
    public Map<String, Double> getMeans() {
        return m_means;
    }

    /**
     * Get reference to the parameter map, i.e. input column name -> value. The
     * first entry contains the offset: The (column-)name of the offset will be
     * the target name - this allows the predictor to append a column with the
     * "correct" name.
     * 
     * @return the reference
     */
    public Map<String, Double> getMap() {
        return m_map;
    }

    /**
     * Get the name of the response column, i.e. the prediction column.
     * 
     * @return the name of the response column
     */
    public String getTargetColumnName() {
        // map must contain at least one column.
        return m_map.keySet().iterator().next();
    }

    /**
     * Does a prediction when the given variable has the value v and all other
     * variables have their mean value. Used to determine the line in a 2D plot.
     * 
     * @param variable the variable currently shown on x
     * @param v its value
     * @return the value of the linear regression line
     */
    public double getApproximationFor(final String variable, final double v) {
        Map<String, Double> map = m_map;
        Map<String, Double> means = m_means;
        double sum = 0.0;
        boolean isFirst = true;
        boolean isFound = false;
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            if (isFirst) {
                sum += entry.getValue();
                isFirst = false;
            } else {
                String cur = entry.getKey();
                double multiplier;
                if (cur.equals(variable)) {
                    multiplier = v;
                    isFirst = true;
                    isFound = true;
                } else {
                    multiplier = means.get(cur);
                }
                sum += multiplier * entry.getValue();
            }
        }
        if (!isFound) {
            throw new IllegalArgumentException("No such column: " + variable);
        }
        return sum;
    }

    /**
     * Writes the current parameters to <code>par</code>.
     * 
     * @param par the object to save to
     */
    public void saveParams(final ModelContentWO par) {
        ModelContentWO sub = par.addModelContent(CFG_SUB_PARAMS);
        // the first element is the offset value, must put that separately
        boolean isFirst = true;
        for (Map.Entry<String, Double> entry : m_map.entrySet()) {
            String cell = entry.getKey();
            double val = entry.getValue();
            ModelContentWO subsub;
            if (isFirst) {
                subsub = par.addModelContent(CFG_OFFSET_NAME);
                isFirst = false;
            } else {
                subsub = sub.addModelContent(cell);
                double mean = m_means.get(cell);
                subsub.addDouble(CFG_PARA_MEAN, mean);
            }
            subsub.addString(CFG_PARA_NAME, cell);
            subsub.addDouble(CFG_PARA_VALUE, val);
        }
    }

    /**
     * Reads the model from the <code>par</code> argument.
     * 
     * @param par the object to read from
     * @return the map containing the values, or weights, for each column
     * @throws InvalidSettingsException if that fails
     */
    public static LinearRegressionParams loadParams(final ModelContentRO par)
            throws InvalidSettingsException {
        Map<String, Double> map = new LinkedHashMap<String, Double>();
        Map<String, Double> means = new LinkedHashMap<String, Double>();
        ModelContentRO offsetParam = par.getModelContent(CFG_OFFSET_NAME);
        String cell = offsetParam.getString(CFG_PARA_NAME);
        double val = offsetParam.getDouble(CFG_PARA_VALUE);
        map.put(cell, val);
        ModelContentRO sub = par.getModelContent(CFG_SUB_PARAMS);
        for (String id : sub) {
            ModelContentRO subsub = sub.getModelContent(id);
            cell = subsub.getString(CFG_PARA_NAME);
            val = subsub.getDouble(CFG_PARA_VALUE);
            double mean = subsub.getDouble(CFG_PARA_MEAN);
            map.put(cell, val);
            means.put(cell, mean);
        }
        return new LinearRegressionParams(map, means);
    }
}
