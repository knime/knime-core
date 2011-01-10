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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.mine.subgroupminer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

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
 * This class is not sufficently tested yet and the API of this class might be
 * still subject to changes.
 *

*
 * @author Fabian Dill, University of Konstanz
 */
 @Deprecated
public class PMMLAssociationRulePortObject extends PMMLPortObject {

    private double m_minSupport;
    private double m_minConfidence;
    private int m_nrOfTransactions;
    private int m_nrOfItems;

    private Map<String, String> m_items;

    private Set<FrequentItemSet> m_itemsets;
    private Set<AssociationRule> m_associationRules;

    /** PortType for association rules. */
    public static final PortType TYPE
        = new PortType(PMMLAssociationRulePortObject.class);

    /**
     * Creates a new PMML association rule port object.
     */
    public PMMLAssociationRulePortObject() {
        m_itemsets = new LinkedHashSet<FrequentItemSet>();
        m_associationRules = new LinkedHashSet<AssociationRule>();
    }

    /**
     * Creates a new PMML association rule port object.
     * @param spec PMML spec
     * @param minSupport minimum support
     * @param minConfidence minimum confidence
     * @param nrOfTransactions number of transactions
     * @param nrOfItems number of items
     * @param items mapping of item id to item name
     * @param itemsets collection of frequent item sets
     */
    public PMMLAssociationRulePortObject(
            final PMMLPortObjectSpec spec,
            final double minSupport,
            final double minConfidence, final int nrOfTransactions,
            final int nrOfItems, final  Map<String, String> items,
            final Collection<FrequentItemSet>itemsets) {
        super(spec, PMMLModelType.AssociationModel);
        m_minSupport = minSupport;
        m_minConfidence = minConfidence;
        m_nrOfTransactions = nrOfTransactions;
        m_nrOfItems = nrOfItems;
        m_items =  new LinkedHashMap<String, String>();
        m_items.putAll(items);
        m_itemsets = new LinkedHashSet<FrequentItemSet>();
        m_itemsets.addAll(itemsets);
        m_associationRules = new LinkedHashSet<AssociationRule>();
    }

    /**
     * Creates a new PMML association rule port object. This constructor is
     * deprecated and should no longer be used. It relies on setting the
     * mapping with the deprecated method <code>setNameMapping</code>.
     *
     * @param spec PMML spec
     * @param rules collection of association rules
     * @param minSupport minimum support
     * @param minConfidence minimum confidence
     * @param nrOfTransactions number of transactions
     * @param nrOfItems number of items
     */
     @Deprecated
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
        m_items =  new LinkedHashMap<String, String>();
        m_itemsets = new LinkedHashSet<FrequentItemSet>();
        for (AssociationRule rule : m_associationRules) {
            m_itemsets.add(rule.getAntecedent());
            m_itemsets.add(rule.getConsequent());
        }
    }

    /**
     * Sets a new name mapping. This method should no longer be used. The name
     * mapping (list of strings) is replaced by a mapping of item_id to value.
     * Set the items in the constructor instead.
     *
     * @param nameMapping list of names
     */
     @Deprecated
    public void setNameMapping(final List<String>nameMapping) {
         int i = 0;
         for (String value : nameMapping) {
             m_items.put(String.valueOf(i++), value);
        }
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
        handler.startElement(null, null, "AssociationModel", atts);
        PMMLPortObjectSpec.writeMiningSchema(getSpec(), handler,
                getWriteVersion());
        writeLocalTransformations(handler);
        writeItems(handler);
        writeItemsets(handler);
        writeRules(handler);
        handler.endElement(null, null, "AssociationModel");
    }

    private void writeItems(final TransformerHandler handler)
        throws SAXException {
        for (Entry<String, String> itemEntry : m_items.entrySet()) {
            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, null, "id", CDATA, itemEntry.getKey());
            atts.addAttribute(null, null, "value", CDATA, itemEntry.getValue());
            handler.startElement(null, null, "Item", atts);
            handler.endElement(null, null, "Item");
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
            atts.addAttribute(null, null, "lift", CDATA,
                    "" + rule.getLift());
            atts.addAttribute(null, null, "antecedent", CDATA,
                    rule.getAntecedent().getId());
            atts.addAttribute(null, null, "consequent", CDATA,
                    rule.getConsequent().getId());
            handler.startElement(null, null, "AssociationRule", atts);
            handler.endElement(null, null, "AssociationRule");
        }
    }

    /**
     * {@inheritDoc}
     */
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
        m_items = hdl.getItems();
        m_associationRules = hdl.getRules();
        m_itemsets = hdl.getItemsets();
    }
}
