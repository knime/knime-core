/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
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
 *   Sep 8, 2009 (morent): created
 */
package org.knime.base.node.mine.decisiontree2;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.transform.sax.TransformerHandler;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 *  Implements a SimpleSetPredicate as specified in PMML
 * (<a>http://www.dmg.org/v4-0/TreeModel.html</a>).
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public class PMMLSimpleSetPredicate extends PMMLPredicate {
    /** The string representation of the predicate's XML-element. */
    public static final String NAME = "SimpleSetPredicate";
    /** The key to store the array's values  in configurations. */
    private static final String VALUES = "values";
    /** The key to store the array type  in configurations. */
    private static final String ARRAY_TYPE = "arrayType";

    /** The data type of the contained array. */
    private PMMLArrayType m_arrayType;
    /** Depending on the array type the values are either stored as strings. */
    private LinkedHashSet<String> m_values;
    /** ... or doubles. */
    private LinkedHashSet<Double> m_doubleValues;
    /** Only PMML set operators are allowed here. */
    private PMMLSetOperator m_op;

    /**
     * Returns the data type of the contained array.
     *
     * @return the arrayType
     */
    public PMMLArrayType getArrayType() {
        return m_arrayType;
    }

    /**
     * Sets the data type of the contained array.
     *
     * @param arrayType the arrayType to set
     */
    public void setArrayType(final String arrayType) {
        try {
            m_arrayType = PMMLArrayType.get(arrayType);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Sets the data type of the contained array.
     *
     * @param arrayType the arrayType to set
     */
    public void setArrayType(final PMMLArrayType arrayType) {
        m_arrayType = arrayType;
    }

    /**
     * Build a new simple set predicate without values.
     *
     * @param attribute the field the predicate is applied on
     * @param operator the string representation of the set operator
     */
    public PMMLSimpleSetPredicate(final String attribute,
            final String operator) {
        setSplitAttribute(attribute);
        setOperator(operator);
    }

    /**
     * Build a new simple set predicate without values.
     *
     * @param attribute the field the predicate is applied on
     * @param operator the PMML set operator
     */
    public PMMLSimpleSetPredicate(final String attribute,
            final PMMLSetOperator operator) {
        setSplitAttribute(attribute);
        m_op = operator;
    }


    /**
     * @return the operator used for this predicate
     */
    public PMMLSetOperator getSetOperator() {
        return m_op;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOperator(final String op) {
        try {
            m_op = PMMLSetOperator.get(op);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns the array values.
     *
     * @return the values
     */
    public Set<String> getValues() {
        return m_values;
    }

    /**
     * Sets the array values.
     *
     * @param values the values to set
     */
    public void setValues(final List<String> values) {
        m_values = new LinkedHashSet<String>(values);
        if (m_arrayType == PMMLArrayType.INT
                || m_arrayType == PMMLArrayType.REAL) {
            m_doubleValues = new LinkedHashSet<Double>(m_values.size());
            for (String value : m_values) {
                m_doubleValues.add(Double.valueOf(value));
            }
        }
    }

    /**
     * Sets the array values.
     *
     * @param values the values to set
     */
    public void setValues(final LinkedHashSet<String> values) {
        m_values = values;
        if (m_arrayType == PMMLArrayType.INT
                || m_arrayType == PMMLArrayType.REAL) {
            m_doubleValues = new LinkedHashSet<Double>(m_values.size());
            for (String value : m_values) {
                m_doubleValues.add(Double.valueOf(value));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean evaluate(final DataRow row,
            final DataTableSpec spec) {
        cacheSpec(spec);
        assert getPreviousIndex() != -1;
        DataCell cell = row.getCell(getPreviousIndex());
        if (cell.isMissing()) {
            return null;
        }
        if (m_arrayType == PMMLArrayType.STRING) {
            return m_op.evaluate(cell.toString(), m_values);
        } else {
            Double a = ((DoubleValue)cell).getDoubleValue();
            return m_op.evaluate(a, m_doubleValues);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getSplitAttribute() + " " + m_op + " " + m_values + " ";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writePMML(final TransformerHandler handler)
            throws SAXException {
        AttributesImpl predAtts = new AttributesImpl();
        predAtts.addAttribute(null, null, "field", CDATA, getSplitAttribute());
        predAtts.addAttribute(null, null, "booleanOperator", CDATA,
                m_op.toString());
        handler.startElement(null, null, "SimpleSetPredicate", predAtts);

        // write the array
        AttributesImpl arrayAtts = new AttributesImpl();
        arrayAtts.addAttribute(null, null, "n", CDATA, String.valueOf(
                m_values.size()));
        arrayAtts.addAttribute(null, null, "type", CDATA,
                m_arrayType.toString());
        handler.startElement(null, null, "Array", arrayAtts);
        StringBuffer sb = new StringBuffer();
        for (String value : m_values) {
            sb.append('"');
            sb.append(value);
            sb.append('"');
            sb.append(' ');
        }
        handler.characters(sb.toString().toCharArray(), 0, sb.length());
        handler.endElement(null, null, "Array");

        handler.endElement(null, null, "SimpleSetPredicate");

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadFromPredParams(final Config conf)
            throws InvalidSettingsException {
        assert conf.getString(PMMLPredicate.TYPE_KEY).equals(NAME);
        setSplitAttribute(conf.getString(PMMLPredicate.ATTRIBUTE_KEY));
        try {
            m_op = PMMLSetOperator.get(conf.getString(
                    PMMLPredicate.OPERATOR_KEY));
            m_arrayType = PMMLArrayType.get(conf.getString(ARRAY_TYPE));
        } catch (InstantiationException e) {
            throw new InvalidSettingsException(e);
        }
        m_values = new LinkedHashSet<String>(Arrays.asList(
                conf.getStringArray(VALUES)));

        // create the double set if applicable
        if (m_arrayType == PMMLArrayType.INT
                || m_arrayType == PMMLArrayType.REAL) {
            m_doubleValues = new LinkedHashSet<Double>(m_values.size());
            for (String value : m_values) {
                m_doubleValues.add(Double.valueOf(value));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveToPredParams(final Config conf) {
        conf.addString(PMMLPredicate.TYPE_KEY, NAME);
        conf.addString(PMMLPredicate.ATTRIBUTE_KEY, getSplitAttribute());
        conf.addString(PMMLPredicate.OPERATOR_KEY, m_op.toString());
        conf.addString(ARRAY_TYPE, m_arrayType.toString());
        conf.addStringArray(VALUES, m_values.toArray(
                new String[m_values.size()]));
    }
}
