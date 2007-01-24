/* 
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 */
package org.knime.base.node.viz.histogram;

import java.util.ArrayList;
import java.util.List;

/**
 * Enumerates all possible aggregation method of the Histogram visualisation.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public enum HistogramLayout implements HistogramProperty {
    /** The colored rows are displayed next to each other. */
    SIDE_BY_SIDE("side_by_side", "Side by side", false),
    /** The colored rows are displayed vertical. */
    STACKED("stacked", "Stacked", true);
    
    private final String m_id;
    private final String m_label;
    private final boolean m_default;
    
    private HistogramLayout(final String id, final String label, 
            final boolean isDefault) {
        m_id = id;
        m_label = label;
        m_default = isDefault;
    }
    
    /**
     * @return the id of the layout option
     */
    public String getID() {
        return m_id;
    }
    
    /**
     * @return the name of the layout option
     */
    public String getLabel() {
        return m_label;
    }
    

    /**
     * Returns the enumeration fields as a String list of their names.
     * 
     * @return the enumeration fields as a String list of their names
     */
    public static List<String> asStringList() {
        Enum[] values = values();
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < values.length; i++) {
            list.add(values[i].name());
        }
        return list;
    }

    /**
     * Returns the histogram layout for the given name. If the name is
     * <code>null</code> or has length zero the method returns the default
     * layout.
     * 
     * @param name the name to check
     * @return the layout with the given name
     */
    public static HistogramLayout getLayout4String(final String name) {
        if (name == null || name.length() < 1) {
            return getDefaultLayout();
        }
        for (HistogramLayout value : HistogramLayout.values()) {
            if (value.name().equals(name)) {
                return value;
            }
        }
        return getDefaultLayout();
    }

    /**
     * @return the default aggregation method
     */
    public static HistogramLayout getDefaultLayout() {
        for (HistogramLayout value : HistogramLayout.values()) {
            if (value.isDefault()) {
                return value;
            }
        }
        throw new IllegalStateException("No default layout defined");
    }

   /**
     * @see org.knime.base.node.viz.histogram.HistogramProperty#isDefault()
     */
    public boolean isDefault() {
       return m_default;
   }
    
    /**
     * @param layout the name of the layout to check
     * @return <code>true</code> if it's a valid histogram layout otherwise
     *         it returns <code>false</code>.
     */
    public static boolean valid(final String layout) {
        for (HistogramLayout value : HistogramLayout.values()) {
            if (value.name().equals(layout)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param id the id to search for
     * @return the <code>HistogramLayout</code> object with the given id
     */
    public static HistogramLayout getLayout4ID(final String id) {
        for (HistogramLayout type : values()) {
            if (type.getID().equals(id)) {
                return type;
            }
        }
        return getDefaultLayout();
    }
}
