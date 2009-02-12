/* This source code, its documentation and all appendant files
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
 */
package org.knime.base.node.mine.subgroupminer;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.knime.base.node.mine.subgroupminer.freqitemset.AssociationRule;
import org.knime.base.node.mine.subgroupminer.freqitemset.FrequentItemSet;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */

// TODO: add support for the name mapping!!! no list of strings 
// but Map<String, String> 
public class PMMLAssociationRuleHandler extends PMMLContentHandler {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            PMMLAssociationRuleHandler.class);
    
    /** Id of this handler. */ 
    public static final String ID = "association.rule.handler";
    
    private double m_minSupport;
    private double m_minConfidence;
    private int m_nrOfTransactions;
    private int m_nrOfItems;
    private int m_nrOfItemsets;
    private int m_nrOfRules;
    
    private final Set<FrequentItemSet>m_itemsets 
        = new LinkedHashSet<FrequentItemSet>();
    private final Set<AssociationRule>m_rules 
        = new LinkedHashSet<AssociationRule>();
    
    private Map<String, String>m_items = new LinkedHashMap<String, String>();

    private FrequentItemSet m_currentItemSet;
    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(final char[] ch, final int start, 
            final int length) throws SAXException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endDocument() throws SAXException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, 
            final String localName, final String name) throws SAXException {
        if (name.equals("Itemset")) {
            // last itemset must explicitely added here
            if (!m_itemsets.contains(m_currentItemSet)) {
                m_itemsets.add(m_currentItemSet);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri, final String localName, 
            final String name, final Attributes atts) throws SAXException {
        // start element -> extract minimum support, confidence, nr of items
        if (name.equals("AssociationRuleModel")) {
            // all required attributes
            m_nrOfTransactions = Integer.parseInt(
                    atts.getValue("numberOfTransactions"));
            m_minSupport = Double.parseDouble(atts.getValue("minimumSupport"));
            m_minConfidence = Double.parseDouble(atts.getValue(
                    "minimumConfidence"));
            m_nrOfItems = Integer.parseInt(atts.getValue("numberOfItems"));
            m_nrOfItemsets = Integer.parseInt(atts.getValue(
                    "numberOfItemsets"));
            m_nrOfRules = Integer.parseInt(atts.getValue("numberOfRules"));
        } else if (name.equals("Item")) {
            // get the id and the value
            String id = atts.getValue("id");
            String value = atts.getValue("value");
            if (!m_items.containsKey(id)) {
                m_items.put(id, value);
            }
            // ignore the mapped value!
            if (atts.getValue("mappedValue") != null) {
                LOGGER.warn("Ignoring mapped value: " 
                        + atts.getValue("mappedValue"));
            }
            // and the weight
            if (atts.getValue("weight") != null) {
                LOGGER.warn("Ignoring weight of item " + id + "/" + value);
            }
        } else if (name.equals("Itemset")) {
            String id = atts.getValue("id");
            if (m_currentItemSet == null) {
                m_currentItemSet = new FrequentItemSet(id);
            }
            if (!id.equals(m_currentItemSet.getId())) {
                m_itemsets.add(m_currentItemSet);
                m_currentItemSet = new FrequentItemSet(id);
            }
            if (atts.getValue("support") != null) {
                m_currentItemSet.setSupport(Double.parseDouble(
                        atts.getValue("support")));
            }
        } else if (name.equals("ItemRef")) {
            // get the referenced item id
            String itemId = atts.getValue("itemRef");
            // find the item:
            if (!m_items.containsKey(itemId)) {
                throw new SAXException(
                        "Referenced item " + itemId + " in itemset " 
                        + m_currentItemSet.getId() 
                        + " cannot be found in items!");
            }
            // TODO: also support String ids
            m_currentItemSet.add(Integer.parseInt(itemId));
        } else if (name.equals("AssociationRule")) {
            double support = Double.parseDouble(atts.getValue("support"));
            double confidence = Double.parseDouble(atts.getValue("confidence"));
            String antecedentId = atts.getValue("antecedent");
            String consequentId = atts.getValue("consequent");
            FrequentItemSet antecedent = null;
            FrequentItemSet consequent = null;
            for (FrequentItemSet set : m_itemsets) {
                if (set.getId().equals(antecedentId)) {
                    antecedent = set;
                } else if (set.getId().equals(consequentId)) {
                    consequent = set;
                }
            }
            if (consequent == null || antecedent == null) {
                throw new SAXException(
                        "One of the referenced itemsets " 
                        + antecedentId + " or " + consequentId
                        + " in association rule could not be found.");
            }
            m_rules.add(new AssociationRule(
                    antecedent, consequent, support, confidence));
        }
    }

    /**
     * @return the minSupport
     */
    public double getMinSupport() {
        return m_minSupport;
    }

    /**
     * @return the minConfidence
     */
    public double getMinConfidence() {
        return m_minConfidence;
    }

    /**
     * @return the nrOfTransactions
     */
    public int getNrOfTransactions() {
        return m_nrOfTransactions;
    }

    /**
     * @return the nrOfItems
     */
    public int getNrOfItems() {
        return m_nrOfItems;
    }

    /**
     * @return the nrOfItemsets
     */
    public int getNrOfItemsets() {
        return m_nrOfItemsets;
    }

    /**
     * @return the nrOfRules
     */
    public int getNrOfRules() {
        return m_nrOfRules;
    }

    /**
     * @return the itemsets
     */
    public Set<FrequentItemSet> getItemsets() {
        return m_itemsets;
    }

    /**
     * @return the rules
     */
    public Set<AssociationRule> getRules() {
        return m_rules;
    }
    

}
