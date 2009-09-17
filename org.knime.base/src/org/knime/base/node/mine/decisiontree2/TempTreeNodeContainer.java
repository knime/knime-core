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

import java.util.LinkedHashMap;

import org.knime.core.data.DataCell;
import org.knime.core.data.def.StringCell;

/**
 * @author albrecht, University of Konstanz
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
class TempTreeNodeContainer {

    private int m_index;

    private int m_level;

    private DataCell m_class;

    private double m_ownClassFrequency;

    private double m_allClassFrequency;

    private LinkedHashMap<DataCell, Double> m_classCounts;

    private int m_defaultChild = -1;

    private PMMLPredicate m_predicate;

    /**
     * @param ownIndex index of this node
     * @param majorityClass of this node
     * @param allClassFrequency sum of class frequencies
     * @param level depth of tree for this node
     * @param defaultChild the index of the default child
     */
    TempTreeNodeContainer(final int ownIndex, final String majorityClass,
            final double allClassFrequency, final int level,
            final String defaultChild) {
        m_index = ownIndex;
        m_class = new StringCell(majorityClass);
        m_allClassFrequency = allClassFrequency;
        m_level = level;
        m_classCounts = new LinkedHashMap<DataCell, Double>();
        m_predicate = null;
        if (defaultChild != null) {
            m_defaultChild = Integer.parseInt(defaultChild);
        }
    }

    /**
     * @param className to be added to
     * @param value can be >1 in case of missing values
     */
    void addClassCount(final DataCell className, final double value) {
        m_classCounts.put(className, value);
        if (className.equals(m_class)) {
            m_ownClassFrequency = value;
        }
    }

    /**
     * @param pred the child predicate to be set
     */
    void setPredicate(final PMMLPredicate pred) {
        m_predicate = pred;
    }

    /**
     * @return the predicate
     */
    public PMMLPredicate getPredicate() {
        return m_predicate;
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
    double getOwnClassFrequency() {
        return m_ownClassFrequency;
    }

    /**
     * @return depth in tree
     */
    int getLevel() {
        return m_level;
    }

    /**
     * @return the defaultChild
     */
    public int getDefaultChild() {
        return m_defaultChild;
    }

    /**
     * @return all class counts
     */
    LinkedHashMap<DataCell, Double> getClassCounts() {
        return m_classCounts;
    }


    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Class: " + m_class + "; predicate: " + m_predicate;
    }
}
