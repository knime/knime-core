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
 * -------------------------------------------------------------------
 * 
 * History
 *   14.07.2006 (Fabian Dill): created
 */
package org.knime.base.node.mine.subgroupminer.freqitemset;

import java.util.Collection;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;


/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class AssociationRuleModel {
    private static final String ASSOCIATION_RULES = "associationRules";

    private static final String ASSOCIATION_RULE = "associationRule";

//    private static final String CONSEQUENT = "consequent";

    private static final String ANTECEDENT = "antecedent";

//    private static final String ANTECEDENT_SIZE = "antecedent size";

    private static final String CONFIDENCE = "confidence";

    private static final String SUPPORT = "support";
    
    private static final String LIFT = "lift";

    private static final String TYPE = "type";

    private static final String ITEM = "item";

    private static final String NR_RULES = "number of rules";

    private static final String NAME_MAPPING = "nameMapping";

    private Collection<AssociationRule> m_rules;

    private List<String> m_nameMapping;

    /**
     * 
     * @param model model containing the association rules.
     * @throws InvalidSettingsException if a
     */
    public void loadFromModelContent(final ModelContentRO model)
            throws InvalidSettingsException {
        /*
        String type = model.getString(TYPE, "");
        if (!type.equals(ASSOCIATION_RULES)) {
            throw new InvalidSettingsException("Model is not of type "
                    + ASSOCIATION_RULES);
        }
        m_nameMapping = Arrays.asList(model.getStringArray(NAME_MAPPING));
        int nrRules = model.getInt(NR_RULES);
        for (int i = 0; i < nrRules; i++) {
            ModelContentRO ruleModel = model.getModelContent(ASSOCIATION_RULE
                    + i);
            double support = ruleModel.getDouble(SUPPORT);
            double confidence = ruleModel.getDouble(CONFIDENCE);
            String consequentName = ruleModel.getString(CONSEQUENT);
            int consequent;
            if (m_nameMapping != null) {
                consequent = m_nameMapping.indexOf(consequentName);
            } else {
                consequent = Integer.parseInt(consequentName.substring(4,
                        consequentName.length()));
            }
            List<Integer> antecedent = new ArrayList<Integer>();
            ModelContentRO antecedentModel = ruleModel
                    .getModelContent(ANTECEDENT);
            int antecedentSize = antecedentModel.getInt(ANTECEDENT_SIZE);
            for (int j = 0; j < antecedentSize; j++) {
                String itemName = ruleModel.getString(CONSEQUENT);
                int item;
                if (m_nameMapping != null) {
                    item = m_nameMapping.indexOf(itemName);
                } else {
                    item = Integer.parseInt(itemName.substring(4, itemName
                            .length()));
                }
                antecedent.add(item);
            }
            AssociationRule rule = new AssociationRule(consequent, antecedent,
                    confidence, support);
            m_rules.add(rule);
        }
    */
    }

    /**
     * @param model the model the association rules are saved to
     */
    public void saveToModelContent(final ModelContentWO model) {
        ModelContentWO associationRulesModel = model
                .addModelContent(ASSOCIATION_RULES);
        associationRulesModel.addString(TYPE, ASSOCIATION_RULES);
        if (m_nameMapping != null) {
            String[] mappingArray = new String[m_nameMapping.size()];
            m_nameMapping.toArray(mappingArray);
            associationRulesModel.addStringArray(NAME_MAPPING, mappingArray);
        }
        int counter = 0;
        associationRulesModel.addInt(NR_RULES, m_rules.size());
        for (AssociationRule rule : m_rules) {
            ModelContentWO ruleModel = associationRulesModel
                    .addModelContent(ASSOCIATION_RULE + counter++);
            ruleModel.addDouble(SUPPORT, rule.getSupport());
            ruleModel.addDouble(CONFIDENCE, rule.getConfidence());
            ruleModel.addDouble(LIFT, rule.getLift());
            String name;
            /*
            if (m_nameMapping != null 
                    && m_nameMapping.size() > rule.getConsequent()) {
                name = m_nameMapping.get(rule.getConsequent());
            } else {
                name = "item" + rule.getConsequent();
            }
            */
//            ruleModel.addString(CONSEQUENT, name);
//            int antecedentSize = rule.getAntecedent().size();
            ModelContentWO antecedentModel = ruleModel
                    .addModelContent(ANTECEDENT);
//            antecedentModel.addInt(ANTECEDENT_SIZE, antecedentSize);
            int itemCounter = 0;
            for (Integer item : rule.getAntecedent()) {
                if (m_nameMapping != null && m_nameMapping.size() > item) {
                    name = m_nameMapping.get(item);
                } else {
                    name = "item" + item;
                }
                antecedentModel.addString(ITEM + itemCounter++, name);
            }
        }
    }

    /**
     * @param nameMapping the index to name mapping for the items
     */
    public void setNameMapping(final List<String> nameMapping) {
        m_nameMapping = nameMapping;
    }

    /**
     * @param rules sets the rule for the model
     */
    public void setAssociationRules(final Collection<AssociationRule> rules) {
        m_rules = rules;
    }

    /**
     * @return the loaded association rules
     */
    public Collection<AssociationRule> getAssociationRules() {
        return m_rules;
    }
}
