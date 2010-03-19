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
 *    20.11.2008 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.groupby;


import org.knime.base.node.preproc.groupby.aggregation.AggregationMeth;


/**
 *  This enum defines the different aggregation column name versions.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public enum ColumnNamePolicy {

    /**Keeps the original column name.*/
    KEEP_ORIGINAL_NAME("Keep original name(s)"),
    /**Combines the aggregation method with the column name e.g. Avg(col1).*/
    AGGREGATION_METHOD_COLUMN_NAME("Aggregation method (column name)"),
    /**Combines the column name with the aggregation method e.g. col1(Avg).*/
    COLUMN_NAME_AGGREGATION_METHOD("Column name (aggregation method)");

    private final String m_label;

    private ColumnNamePolicy(final String label) {
        m_label = label;
    }

    /**
     * @return the label of this column name policy
     */
    public String getLabel() {
        return m_label;
    }

    /**
     * @param origName the original name of the column
     * @param aggregation the {@link AggregationMeth} to use
     * @return the column name of the aggregation column
     */
    public String createColumName(final String origName,
            final AggregationMeth aggregation) {
        if (origName == null) {
            throw new NullPointerException("origSpec must not be null");
        }
        if (aggregation == null) {
            throw new NullPointerException("aggrMethod must not be null");
        }
        final String aggrLabel = aggregation.getShortLabel();
        final String colName;
        switch (this) {
        case KEEP_ORIGINAL_NAME:
            colName = origName;
            break;

        case AGGREGATION_METHOD_COLUMN_NAME:
            colName = aggrLabel + "(" + origName + ")";
            break;

        case COLUMN_NAME_AGGREGATION_METHOD:
            colName = origName + " (" + aggrLabel + ")";
            break;

        default:
            colName = origName;
            break;
        }
        return colName;
    }

    /**
     * @param label the label of the {@link ColumnNamePolicy}
     * @return the {@link ColumnNamePolicy} with the given label
     */
    public static ColumnNamePolicy getPolicy4Label(final String label) {
        for (final ColumnNamePolicy namePolicy : values()) {
            if (namePolicy.getLabel().equals(label)) {
                return namePolicy;
            }
        }
        throw new IllegalArgumentException("Invalid ColumnNamePolicy label");
    }

    /**
     * @return the labels of all available {@link ColumnNamePolicy} options
     */
    public static String[] getPolicyLabels() {
        final ColumnNamePolicy[] values = values();
        final String[] labels = new String[values.length];
        for (int i = 0, length = values.length; i < length; i++) {
            labels[i] = values[i].getLabel();
        }
        return labels;
    }

    /**
     * @return the default {@link ColumnNamePolicy}
     */
    public static ColumnNamePolicy getDefault() {
        return AGGREGATION_METHOD_COLUMN_NAME;
    }
}
