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
    
    private double m_lift;
    
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
            final double confidence, final double lift) {
        m_antecedent = antecedent;
        m_consequent = consequent;
        m_support = support;
        m_confidence = confidence;
        m_lift = lift;
    }
    
    
    /**
     * @return the support of the rule.
     */
    public double getSupport() {
        return m_support;
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
     * @return lift value
     */
    public double getLift() {
    	return m_lift;
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
                + " lift: " + m_lift + " antecedent: " + m_antecedent.getId() 
                + " consequent: " + m_consequent.getId();
    }
}
