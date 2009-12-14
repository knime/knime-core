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
