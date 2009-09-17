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
 *   Sep 9, 2009 (morent): created
 */
package org.knime.base.node.mine.decisiontree2;

import java.util.LinkedList;

import javax.xml.transform.sax.TransformerHandler;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.Config;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public class PMMLCompoundPredicate extends PMMLPredicate {
    /** The node logger for this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(PMMLCompoundPredicate.class);

    /** The string representation of the predicate's XML-element. */
    public static final String NAME = "CompoundPredicate";
    /** The key to store the number of predicates in configurations. */
    private static final String NUM_PREDICATES = "num_predicates";
    /** The key prefix to store the predicates in configurations. */
    private static final String PRED = "predicate";

    private LinkedList<PMMLPredicate> m_predicates =
            new LinkedList<PMMLPredicate>();

    /* Only PMML boolean operators are allowed here. */
    private PMMLBooleanOperator m_op;

    /**
     * Build a new PMMLCompoundPredicate.
     * @param operator the string representation of the operator
     */
    public PMMLCompoundPredicate(final String operator) {
        try {
            m_op = PMMLBooleanOperator.get(operator);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Build a new PMMLCompoundPredicate.
     *
     * @param operator the PMML operator to be set
     */
    public PMMLCompoundPredicate(final PMMLBooleanOperator operator) {
            m_op = operator;
    }

    /**
     * Returns the PMMLPredicate that was most recently added.
     *
     * @return the most recently added predicate
     * @see java.util.LinkedList#getLast()
     */
    public PMMLPredicate getLastPredicate() {
        return m_predicates.getLast();
    }

    /**
     * Adds a PMMLPredicate.
     *
     * @param pred the predicate to be added
     * @return true (as per the general contract of Collection.add)
     * @see java.util.LinkedList#add(java.lang.Object)
     */
    public boolean addPredicate(final PMMLPredicate pred) {
        return m_predicates.add(pred);
    }

    /**
     * Returns all contained predicates.
     *
     * @return the predicates
     */
    public LinkedList<PMMLPredicate> getPredicates() {
        return m_predicates;
    }

    /**
     * Sets the contained predicates.
     *
     * @param predicates the predicates to set
     */
    public void setPredicates(final LinkedList<PMMLPredicate> predicates) {
        m_predicates = predicates;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean evaluate(final DataRow row,
            final DataTableSpec spec) {
        Boolean eval = false;
        int count = 0;

        search:
        for (PMMLPredicate pred : m_predicates) {
            eval = pred.evaluate(row, spec);
            switch (m_op) {
                case AND:
                    if (eval == null || !eval) {
                        // break evaluation on first negative result
                        break search;
                    }
                    break;
                case OR:
                    if (eval == null || eval) {
                        // break evaluation on first positive result
                        break search;
                    }
                    break;
                case XOR:
                    if (eval == null) {
                        break search;
                    } else if (eval) {
                        // count positive results
                        count++;
                    }
                    break;
                case SURROGATE:
                    if (eval == null) {
                        // just continue on unknown result (missing values)
                        continue;
                    } else {
                        // break the loop to return the evaluation result
                        break search;
                    }
            }
        }
        if (m_op == PMMLBooleanOperator.XOR) {
            eval = (count % 2 == 1);
        }
        return eval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSplitAttribute() {
        if (super.getSplitAttribute() != null) {
            return super.getSplitAttribute();
        } else {
            if (m_predicates == null) {
                LOGGER.error("Split attribute cannot be access before "
                        + "predicate is initialized.");
            } else {
                /* Compare the split attributes of all contained predicates. If
                 * they are all the same return the common attribute, otherwise
                 * "". */
                String splitAttribute =
                        m_predicates.getFirst().getSplitAttribute();
                for (PMMLPredicate pred : m_predicates) {
                    String current = pred.getSplitAttribute();
                    if (current == null) {
                        continue; // ignore True or False predicates
                    } else if (!current.equals(splitAttribute)) {
                        splitAttribute = "";
                        break;
                    }
                }
                setSplitAttribute(splitAttribute);
            }
        }
        return super.getSplitAttribute();
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
        try {
            m_op = PMMLBooleanOperator.get(conf.getString(
                    PMMLPredicate.OPERATOR_KEY));
        } catch (InstantiationException e) {
            throw new InvalidSettingsException(e);
        }
        setSplitAttribute(conf.getString(PMMLPredicate.ATTRIBUTE_KEY));
        int numPredicates = conf.getInt(NUM_PREDICATES);
        for (int i = 0; i < numPredicates; i++) {
            Config pconf = conf.getConfig(PRED + i++);
            PMMLPredicate pred = PMMLPredicate.getPredicateForConfig(pconf);
            m_predicates.add(pred);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveToPredParams(final Config conf) {
        conf.addString(PMMLPredicate.TYPE_KEY, NAME);
        conf.addString(PMMLPredicate.OPERATOR_KEY, m_op.toString());
        conf.addString(PMMLPredicate.ATTRIBUTE_KEY, getSplitAttribute());
        conf.addInt(NUM_PREDICATES, m_predicates.size());
        int i = 0;
        for (PMMLPredicate pred : m_predicates) {
            Config pconf = conf.addConfig(PRED + i++);
            pred.saveToPredParams(pconf);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(" " + m_op + "(");
        for (PMMLPredicate pred : m_predicates) {
            sb.append(pred);
            sb.append("; ");
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writePMML(final TransformerHandler handler)
            throws SAXException {
        AttributesImpl predAtts = new AttributesImpl();
        predAtts.addAttribute(null, null, "booleanOperator", CDATA,
                m_op.toString());
        handler.startElement(null, null, "CompoundPredicate", predAtts);
        for (PMMLPredicate pred : m_predicates) {
            pred.writePMML(handler);
        }
        handler.endElement(null, null, "CompoundPredicate");
    }


}
