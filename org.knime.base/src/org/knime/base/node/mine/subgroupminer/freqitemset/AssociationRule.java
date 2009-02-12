/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   22.02.2006 (dill): created
 */
package org.knime.base.node.mine.subgroupminer.freqitemset;


/**
 * A data structure to encapsulate an association rule.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class AssociationRule {
//    private Integer m_consequent;
//
//    private List<Integer> m_antecedent;

    private double m_confidence;

    private double m_support;
    
    private final FrequentItemSet m_antecedent;
    
    private final FrequentItemSet m_consequent;

//    public AssociationRule(final Integer consequent,
//            final List<Integer> antecendent, final double confidence,
//            final double support) {
//        m_consequent = consequent;
//        m_antecedent = antecendent;
//        m_confidence = confidence;
//        m_support = support;
//    }

    // TODO: rewrite to have a FrequentItem antecedent
    // and a FrequentItemSet as consequent
    
    /**
     * Creates an association rule with the list of ids of the antecedent and an
     * id as the consequent of this rule.
     * 
     * @param consequent the consequent of the rule
     * @param antecedent the antecedent of the rule
     * @param confidence the confidence of the rule
     * @param support the support of the rule
     */
    public AssociationRule(final FrequentItemSet antecedent, 
            final FrequentItemSet consequent, final double support,
            final double confidence) {
        m_antecedent = antecedent;
        m_consequent = consequent;
        m_support = support;
        m_confidence = confidence;
    }
    
    
    /**
     * @return the support of the rule.
     */
    public double getSupport() {
        return m_support;
    }

    /**
     * @param confidence the confidence to set
     */
    public void setConfidence(final double confidence) {
        m_confidence = confidence;
    }

    /**
     * @return the confidence.
     */
    public double getConfidence() {
        return m_confidence;
    }

    /**
     * @return the antecedent
     */
    public FrequentItemSet getAntecedent() {
        return m_antecedent;
    }


    /**
     * @return the consequent
     */
    public FrequentItemSet getConsequent() {
        return m_consequent;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "support: " + m_support + " confidence: " + m_confidence
                + " antecedent: " + m_antecedent.getId() + " consequent: "
                + m_consequent.getId();
    }
}
