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
 *   Sep 4, 2009 (morent): created
 */
package org.knime.base.node.mine.decisiontree2;

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
 * Implements a SimplePredicate as specified in PMML
 * (<a>http://www.dmg.org/v4-0/TreeModel.html</a>).
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public class PMMLSimplePredicate extends PMMLPredicate {
    /** The string representation of the predicate's XML-element. */
    public static final String NAME = "SimplePredicate";
    /** The key to store the threshold in configurations. */
    protected static final String THRESHOLD_KEY = "threshold";



    /** The threshold to compare against. */
    private String m_threshold;
    /* Used for storing numerical thresholds to avoid unnecessary casts. */
    private Double m_thresholdNumerical;

    /**
     * Build a new simple predicate.
     *
     * @param attribute the field the predicate is applied on
     * @param operator the string representation of the operator
     * @param value the value to be compared with (the threshold)
     */
    public PMMLSimplePredicate(final String attribute, final String operator,
            final String value) {
        setSplitAttribute(attribute);
        setOperator(operator);
        m_threshold = value;
    }

    /**
     * Build a new simple predicate.
     *
     * @param attribute the field the predicate is applied on
     * @param operator the PMML operator to be set
     * @param value the value to be compared with (the threshold)
     */
    public PMMLSimplePredicate(final String attribute,
            final PMMLOperator operator, final String value) {
        setSplitAttribute(attribute);
        setOperator(operator);
        setThreshold(value);
    }


    /**
     * @param threshold the threshold to set
     */
    public void setThreshold(final String threshold) {
        m_threshold = threshold;
        try {
            m_thresholdNumerical = Double.valueOf(threshold);
        } catch (final NumberFormatException e) {
            // no numerical threshold
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean evaluate(final DataRow row, final DataTableSpec spec) {
        cacheSpec(spec);
        assert getPreviousIndex() != -1;
        DataCell cell = row.getCell(getPreviousIndex());
        if (cell.isMissing()) {
            return null;
        }
        if (cell.getType().isCompatible(DoubleValue.class)) {
            if (m_thresholdNumerical == null) {
                m_thresholdNumerical = Double.valueOf(m_threshold);
            }

            Double value = ((DoubleValue)cell).getDoubleValue();
            return getOperator().evaluate(value, m_thresholdNumerical);
        } else {
            String s = cell.toString();
            return getOperator().evaluate(s, m_threshold);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (m_thresholdNumerical != null) {
            return getSplitAttribute() + " " + getOperator().getSymbol() + " "
            + NUMBERFORMAT.format(m_thresholdNumerical);
        } else {
            return getSplitAttribute() + " " + getOperator().getSymbol() + " "
            + m_threshold;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writePMML(final TransformerHandler handler)
            throws SAXException {
        AttributesImpl predAtts = new AttributesImpl();
        predAtts.addAttribute(null, null, "field", CDATA, getSplitAttribute());
        predAtts.addAttribute(null, null, "operator", CDATA,
                getOperator().toString());
        predAtts.addAttribute(null, null, "value", CDATA, m_threshold);
        handler.startElement(null, null, "SimplePredicate", predAtts);
        handler.endElement(null, null, "SimplePredicate");
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
    public void loadFromPredParams(final Config conf)
            throws InvalidSettingsException {
        assert conf.getString(PMMLPredicate.TYPE_KEY).equals(NAME);
        setSplitAttribute(conf.getString(PMMLPredicate.ATTRIBUTE_KEY));
        setOperator(conf.getString(PMMLPredicate.OPERATOR_KEY));
        setThreshold(conf.getString(THRESHOLD_KEY));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveToPredParams(final Config conf) {
        conf.addString(PMMLPredicate.TYPE_KEY, NAME);
        conf.addString(PMMLPredicate.ATTRIBUTE_KEY, getSplitAttribute());
        conf.addString(PMMLPredicate.OPERATOR_KEY, getOperator().toString());
        conf.addString(THRESHOLD_KEY, m_threshold);
    }

   }
