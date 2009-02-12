/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 *
 * History
 *   01.08.2007 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.learner;

import org.knime.core.data.DataCell;

/**
 * Super class for all nominal split variants.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public abstract class SplitNominal extends Split {

    /**
     * Constructs the best split for the given attribute list and the class
     * distribution. The results can be retrieved from getter methods. This is a
     * nominal split.
     *
     * @param table the table for which to create the split
     * @param attributeIndex the index specifying the attribute for which to
     *            calculate the split
     * @param splitQualityMeasure the quality measure to determine the best
     *            split (e.g. gini or gain ratio)
     */
    public SplitNominal(final InMemoryTable table, final int attributeIndex,
            final SplitQualityMeasure splitQualityMeasure) {
        super(table, attributeIndex, splitQualityMeasure);
    }

    /**
     * Returns the possible values of this splits attribute. Those values are
     * used for the split criteria.
     *
     * @return the possible values of this splits attribute
     */
    public DataCell[] getSplitValues() {
        return getTable().getNominalValuesInMappingOrder(getAttributeIndex());
    }
}
