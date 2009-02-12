/*
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *    20.11.2008 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.groupby;

import org.knime.core.data.DataColumnSpec;

import org.knime.base.node.preproc.groupby.aggregation.AggregationMethod;


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
     * @param origSpec the {@link DataColumnSpec} of the original column
     * @param aggrMethod the {@link AggregationMethod} to use
     * @return the column name of the aggregation column
     */
    public String createColumName(final DataColumnSpec origSpec,
            final AggregationMethod aggrMethod) {
        if (origSpec == null) {
            throw new NullPointerException("origSpec must not be null");
        }
        if (aggrMethod == null) {
            throw new NullPointerException("aggrMethod must not be null");
        }
        final String aggrLabel = aggrMethod.getShortLabel();
        final String colName;
        switch (this) {
        case KEEP_ORIGINAL_NAME:
            colName = origSpec.getName();
            break;

        case AGGREGATION_METHOD_COLUMN_NAME:
            colName = aggrLabel + "(" + origSpec.getName() + ")";
            break;

        case COLUMN_NAME_AGGREGATION_METHOD:
            colName = origSpec.getName() + " (" + aggrLabel + ")";
            break;

        default:
            colName = origSpec.getName();
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
