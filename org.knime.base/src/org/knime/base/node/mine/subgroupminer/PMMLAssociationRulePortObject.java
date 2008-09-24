/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.sax.TransformerHandler;

import org.knime.base.node.mine.subgroupminer.freqitemset.AssociationRule;
import org.knime.base.node.mine.subgroupminer.freqitemset.FrequentItemSet;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLModelType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLAssociationRulePortObject extends PMMLPortObject {

    private double m_minSupport;
    private double m_minConfidence;
    private int m_nrOfTransactions;
    private int m_nrOfItems;
    
    private Set<FrequentItemSet> m_itemsets;
    private Set<AssociationRule> m_associationRules;
    private List<String>m_nameMapping;
    
    public static final PortType TYPE 
        = new PortType(PMMLAssociationRulePortObject.class);
    
    /**
     * 
     */
    public PMMLAssociationRulePortObject() {
        m_itemsets = new LinkedHashSet<FrequentItemSet>();
        m_associationRules = new LinkedHashSet<AssociationRule>();
    }
    
    public PMMLAssociationRulePortObject(
            final PMMLPortObjectSpec spec,
            final double minSupport,
            final double minConfidence, final int nrOfTransactions,
            final int nrOfItems, final Collection<FrequentItemSet>itemsets) {
        super(spec, PMMLModelType.AssociationModel);
        m_minSupport = minSupport;
        m_minConfidence = minConfidence;
        m_nrOfTransactions = nrOfTransactions;
        m_nrOfItems = nrOfItems;
        m_itemsets = new LinkedHashSet<FrequentItemSet>(); 
        m_itemsets.addAll(itemsets);
        m_associationRules = new LinkedHashSet<AssociationRule>();
    }
    
    public PMMLAssociationRulePortObject(
            final PMMLPortObjectSpec spec,
            final Collection<AssociationRule> rules,
            final double minSupport,
            final double minConfidence, 
            final int nrOfTransactions,
            final int nrOfItems) {
        super(spec, PMMLModelType.AssociationModel);
        m_minSupport = minSupport;
        m_minConfidence = minConfidence;
        m_nrOfTransactions = nrOfTransactions;
        m_nrOfItems = nrOfItems;
        m_associationRules = new LinkedHashSet<AssociationRule>();
        m_associationRules.addAll(rules);
        m_itemsets = new LinkedHashSet<FrequentItemSet>();
        for (AssociationRule rule : m_associationRules) {
            m_itemsets.add(rule.getAntecedent());
            m_itemsets.add(rule.getConsequent());
        }
    }
    
    public void setNameMapping(final List<String>nameMapping) {
        m_nameMapping = nameMapping;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getSummary() {
        return "Association Rule Model with "
            + " minimum support = " + m_minSupport
            + ", minimum confidence= " + m_minConfidence + ", "
            + m_itemsets.size() + " itemsets, and "
            + m_associationRules.size() + " association rules.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writePMMLModel(final TransformerHandler handler)
            throws SAXException {
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, null, "functionName", CDATA, 
                "associationRules");
        atts.addAttribute(null, null, "numberOfTransactions", CDATA, 
                "" + m_nrOfTransactions);
        atts.addAttribute(null, null, "minimumSupport", CDATA, 
                "" + m_minSupport);
        atts.addAttribute(null, null, "minimumConfidence", CDATA, 
                "" + m_minConfidence);
        atts.addAttribute(null, null, "numberOfItems", CDATA, "" + m_nrOfItems);
        atts.addAttribute(null, null, "numberOfItemsets", CDATA, 
                "" + m_itemsets.size());
        atts.addAttribute(null, null, "numberOfRules", CDATA, 
                "" + m_associationRules.size());
        handler.startElement(null, null, "AssociationRuleModel", atts);
        PMMLPortObjectSpec.writeMiningSchema(getSpec(), handler);
        writeItems(handler);
        writeItemsets(handler);
        writeRules(handler);
        handler.endElement(null, null, "AssociationRuleModel");
    }
    
    private void writeItems(final TransformerHandler handler) 
        throws SAXException {
        Set<Integer>alreadyWritten = new LinkedHashSet<Integer>();
        for (FrequentItemSet set : m_itemsets) {
            for (Integer item : set) {
                if (!alreadyWritten.contains(item)) {
                    AttributesImpl atts = new AttributesImpl();
                    atts.addAttribute(null, null, "id", CDATA, "" + item);
                    // add the nameMapping here
                    String name = "item_" + item;
                    if (m_nameMapping != null 
                            && m_nameMapping.size() > item
                            && m_nameMapping.get(item) != null) {
                        name = m_nameMapping.get(item);
                    }
                    atts.addAttribute(null, null, "value", CDATA, name);
                    handler.startElement(null, null, "Item", atts);
                    handler.endElement(null, null, "Item");
                }
            }
        }
        
    }
    
    private void writeItemsets(final TransformerHandler handler) 
        throws SAXException {
        for (FrequentItemSet set : m_itemsets) {
            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, null, "id", CDATA, set.getId());
            atts.addAttribute(null, null, "support", CDATA, 
                    "" + set.getSupport());
            atts.addAttribute(null, null, "numberOfItems", CDATA, 
                    "" + set.getItems().size());
            handler.startElement(null, null, "Itemset", atts);
            // add here the ItemRefs to the items (use index of item as id)
            for (Integer item : set) {
                atts = new AttributesImpl();
                atts.addAttribute(null, null, "itemRef", CDATA, "" + item);
                handler.startElement(null, null, "ItemRef", atts);
                handler.endElement(null, null, "ItemRef");
            }
            handler.endElement(null, null, "Itemset");
        }
    }

    private void writeRules(final TransformerHandler handler) 
        throws SAXException {
        for (AssociationRule rule : m_associationRules) {
            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, null, "support", CDATA, 
                    "" + rule.getSupport());
            atts.addAttribute(null, null, "confidence", CDATA, 
                    "" + rule.getConfidence());
            atts.addAttribute(null, null, "antecedent", CDATA,
                    rule.getAntecedent().getId());
            atts.addAttribute(null, null, "consequent", CDATA, 
                    rule.getConsequent().getId());
            handler.startElement(null, null, "AssociationRule", atts);
            handler.endElement(null, null, "AssociationRule");
        }
    }
    
    @Override
    public void loadFrom(final PMMLPortObjectSpec spec, 
            final InputStream in, final String version)
            throws SAXException, ParserConfigurationException, IOException {
        PMMLAssociationRuleHandler hdl = new PMMLAssociationRuleHandler();
        addPMMLContentHandler(PMMLAssociationRuleHandler.ID, hdl);
        super.loadFrom(spec, in, version);
        m_minSupport = hdl.getMinSupport();
        m_minConfidence = hdl.getMinConfidence();
        m_nrOfItems = hdl.getNrOfItems();
        m_nrOfTransactions = hdl.getNrOfTransactions();
        m_associationRules = hdl.getRules();
        m_itemsets = hdl.getItemsets();
    }
}
