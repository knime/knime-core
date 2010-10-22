/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
     */
    public PMMLSimpleSetPredicate() {
		super();
		// for usage with loadFromPredParams(Config)
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
        if (m_arrayType == PMMLArrayType.STRING) {
            for (String value : m_values) {
                sb.append('"');
                sb.append(value.replace("\"", "\\\""));
                sb.append('"');
                sb.append(' ');
            }
        } else {
            for (String value : m_values) {
                sb.append(value);
                sb.append(' ');
            }
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
