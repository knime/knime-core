/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.mine.svm.util;

import java.util.ArrayList;

import org.knime.core.data.RowKey;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;


/**
 * This class is used to represent a vector (in the sense of
 * input data sample). A vector contains double values and the class
 * value.
 *
 * @author Stefan, University of Konstanz
 * @author Nicolas Cebron, University of Konstanz
 */
public class DoubleVector {

    /* keys for memorizing a vector into a ModelContent. */
    private static final String KEY_CATEGORY = "Result";
    private static final String KEY_POINTS = "Points";


    // Values
    private double[] m_values;

    // the class value
    private String m_classValue;

    // RowKey
    private RowKey m_key;

    /**
     * Default constructor.
     *
     * @param key the RowKey of the row associated with this vector.
     * @param values the double values of the vector.
     * @param classvalue the class value.
     */
    public DoubleVector(final RowKey key, final ArrayList<Double> values,
            final String classvalue) {
        m_key = key;
        m_values = new double[values.size()];
        for (int i = 0; i < values.size(); ++i) {
            m_values[i] = values.get(i);
        }
        m_classValue = classvalue;
    }

    /**
     * Default constructor with no associated {@link RowKey}.
     *
     * @param values the double values of the vector.
     * @param classvalue the class value.
     */
    public DoubleVector(final ArrayList<Double> values,
            final String classvalue) {
        this(null, values, classvalue);
    }

    /**
     * @return the RowKey of the row associated with this vector.
     * Can be <code>null</code>.
     */
    public RowKey getKey() {
        return m_key;
    }

    /**
     * @return the class value
     */
    public String getClassValue() {
        return m_classValue;
    }

    /**
     * return the i'th value in the vector.
     *
     * @param i the index of the value to return
     * @return the value at this index
     */
    public double getValue(final int i) {
        return m_values[i];
    }

    /**
     * @return the number of values.
     */
    public int getNumberValues() {
        return m_values.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String result = "";
        for (int i = 0; i < getNumberValues(); ++i) {
            result = result + getValue(i);
            if (i != getNumberValues() - 1) {
                result += ", ";
            } else {
                result += ": ";
            }
        }
        result += getClassValue();
        return result;
    }


    /**
     * Save the vector to a ModelContent object.
     * @param predParams where to save the vector
     * @param id used to identify this vector uniquely
     */
    public void saveTo(final ModelContentWO predParams, final String id) {
        predParams.addString(id + KEY_CATEGORY, m_classValue);
        predParams.addDoubleArray(id + KEY_POINTS, m_values);
    }

    /**
     * Loads a vector from a predParams object.
     * @param predParams from where to load
     * @param id used to identify this vector uniquely
     * @throws InvalidSettingsException if a key is not found
     */
    public DoubleVector(final ModelContentRO predParams, final String id)
            throws InvalidSettingsException {
        m_classValue = predParams.getString(id + KEY_CATEGORY);
        m_values = predParams.getDoubleArray(id + KEY_POINTS);
    }

    /**
     * Sets the class value of the {@link DoubleVector}.
     * @param value the new class value.
     */
    public void setClassValue(final String value) {
        m_classValue = value;
    }
}
