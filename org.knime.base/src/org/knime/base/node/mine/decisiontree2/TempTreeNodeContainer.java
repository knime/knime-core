/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
