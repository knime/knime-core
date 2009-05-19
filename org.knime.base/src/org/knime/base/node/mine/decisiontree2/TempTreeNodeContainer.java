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
 *   Aug 28, 2008 (albrecht): created
 */
package org.knime.base.node.mine.decisiontree2;

import java.util.HashMap;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.def.StringCell;

/**
 *
 * @author albrecht, University of Konstanz
 */
class TempTreeNodeContainer {

    private int m_index;

    private int m_level;

    private DataCell m_class;

    private double m_allClassFrequency;

    private ParentNodeType m_parentType;

    private HashMap<DataCell, Double> m_classCounts;

    private String m_splitAttribute = null;

    private double m_ownClassFrequency;

    //if parent is continuous split node
    private Double m_threshold = Double.NaN;

    //if parent is normal nominal split node
    private String m_splitValue = null;

    //if parent is binary nominal split node
    private List<String> m_splitValues = null;

    /**
     * @param ownIndex
     * @param majorityClass
     * @param allClassFrequency
     * @param level
     */
    TempTreeNodeContainer(final int ownIndex,
            final String majorityClass, final double allClassFrequency,
            final int level) {
        m_index = ownIndex;
        m_class = new StringCell(majorityClass);
        m_allClassFrequency = allClassFrequency;
        m_level = level;
        m_classCounts = new HashMap<DataCell, Double>();
        m_parentType = ParentNodeType.UNKNOWN;
    }

    /**
     * @param className
     * @param value
     */
    void addClassCount(final DataCell className, final double value){
        m_classCounts.put(className, value);
        if(className.equals(m_class)){
            m_ownClassFrequency = value;
        }
    }

    /**
     * @param splitAttribute
     */
    void addSplitAttribute(final String splitAttribute){
        if(m_splitAttribute != null){
            return;
        }
        m_splitAttribute = splitAttribute;
    }

    /**
     * @param value
     */
    void addSplitValue(final String value){
        if(m_splitValue != null || !Double.isNaN(m_threshold)){
            return;
        }
        try{
            m_threshold = Double.parseDouble(value);
            m_parentType = ParentNodeType.CONTINUOUS_SPLIT_NODE;
        }catch(NumberFormatException e){
            m_parentType = ParentNodeType.NOMINAL_SPLIT_NODE_NORMAL;
            m_splitValue = value;
        }
    }

    /**
     * @param values
     */
    void addSplitValues(final List<String> values){
        m_parentType = ParentNodeType.NOMINAL_SPLIT_NODE_BINARY;
        m_splitValues = values;
    }

    /**
     * @return index
     */
    int getOwnIndex() {
        return m_index;
    }

    /**
     * @return value (score)
     */
    DataCell getMajorityClass() {
        return m_class;
    }

    /**
     * @return amount (record count)
     */
    double getAllClassFrequency() {
        return m_allClassFrequency;
    }

    /**
     * @return frequency of majority class in this node
     */
    double getOwnClassFrequency(){
        return m_ownClassFrequency;
    }

    /**
     * @return depth in tree
     */
    int getLevel() {
        return m_level;
    }

    /**
     * @return the type of node to create
     */
    ParentNodeType getParentNodeType() {
        return m_parentType;
    }

    /**
     * @return all class counts
     */
    HashMap<DataCell, Double> getClassCounts(){
        return m_classCounts;
    }

    /**
     * @return the split attribute
     */
    String getSplitAttribute(){
        return m_splitAttribute;
    }

    /**
     * @return threshold for continuous parent split node
     */
    Double getThreshold(){
        return m_threshold;
    }

    /**
     * @return split value for normal nominal parent split node
     */
    String getSplitValue(){
        return m_splitValue;
    }

    /**
     * @return split values for binary nominal parent split node
     */
    List<String> getSplitValues() {
        return m_splitValues;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("Class: " + m_class);
        b.append("; attribute: " + m_splitAttribute);
        if (m_splitValue != null) {
            b.append("; split value: " + m_splitValue);
        } else if (m_splitValues != null) {
            b.append("; split values: " + m_splitValues);
        } else if (m_threshold != null) {
            b.append("; threshold: " + m_threshold);
        }
        return b.toString();
    }

}
