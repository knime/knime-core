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
 *    19.10.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.aggregation.util;

import java.util.Comparator;

import org.knime.base.node.viz.aggregation.AggregationValModel;

/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
@SuppressWarnings("unchecked")
public class AggrValModelComparator 
       implements Comparator<AggregationValModel> {

    private boolean m_sortNumerical = true;

    private int m_lower = -1;

    private int m_upper = 1;

    /**
     * Constructor for class AggrValModelComparator.
     * @param sortNumerical if numeric sorting enabled
     * @param ascending <code>true</code> if the sections should be sorted in
     * ascending order
     */
    public AggrValModelComparator(final boolean sortNumerical,
            final boolean ascending) {
        m_sortNumerical = sortNumerical;
        setSortAscending(ascending);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public int compare(
            final AggregationValModel o1,
            final AggregationValModel o2) {
        if (m_sortNumerical) {
            double v1 = 0;
            double v2 = 0;
            try {
                v1 = Double.parseDouble(o1.getName());
                v2 = Double.parseDouble(o2.getName());
                final int result = Double.compare(v1, v2);
                if (result < 0) {
                    return m_lower;
                }
                if (result > 0) {
                    return m_upper;
                }
                return 0;
            } catch (final NumberFormatException e) {
                //if the number conversion failed sort it by name
            }
        }
        if (o1 == null && o2 != null) {
            return m_lower;
        }
        if (o1 != null && o2 == null) {
            return m_upper;
        }
        if (o1 == null && o2 == null) {
            return 0;
        }
        final int result = o1.getName().compareTo(o2.getName());
        if (result < 0) {
            return m_lower;
        }
        if (result > 0) {
            return m_upper;
        }
        return 0;
    }

    /**
     * @param ascending <code>true</code> if the sections should be sorted in
     * ascending order
     */
    public void setSortAscending(final boolean ascending) {
        if (ascending) {
            m_lower = -1;
            m_upper = 1;
        } else {
            m_lower = 1;
            m_upper = -1;
        }
    }

    /**
     * @param sortNumerical <code>true</code> if the name is a number
     */
    public void setBinNumerical(final boolean sortNumerical) {
        m_sortNumerical = sortNumerical;
    }
}
