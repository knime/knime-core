/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   07.07.2005 (mb): created
 */
package de.unikn.knime.core.data;

import javax.swing.Icon;
import javax.swing.ImageIcon;


/**
 * The data type for datacells storing a fuzzy number and also acting as a fuzzy
 * interval.
 * 
 * @author M. Berthold, University of Konstanz
 */
public final class FuzzyNumberType extends DataType {

    /** Singleton of this type. */
    public static final FuzzyNumberType FUZZY_NUMBER_TYPE =
        new FuzzyNumberType();

    /** Singleton icon to be used to display this cell type. */
    private static final Icon ICON;

    /** Load fuzzy icon, use <code>null</code> if not available. */
    static {
        ImageIcon icon;
        try {
            ClassLoader loader = DataCell.class.getClassLoader();
            String path = 
                DataCell.class.getPackage().getName().replace('.', '/');
            icon = new ImageIcon(
                    loader.getResource(path + "/icon/fuzzyicon.png"));
        } catch (Exception e) {
            icon = null;
        }
        ICON = icon;
    }

    private static final FuzzyNumberCellComparator COMPARATOR = 
        new FuzzyNumberCellComparator();

    private FuzzyNumberType() {
        addCompatibleType(FuzzyIntervalType.FUZZY_INTERVAL_TYPE);
    }

    /**
     * @see DataType#getNativeComparator()
     */
    public DataCellComparator getNativeComparator() {
        return COMPARATOR;
    }

    /**
     * @return <code>FuzzyNumberValue.class</code>
     */
    protected Class<? extends DataValue> getNativeValue() {
        return FuzzyNumberValue.class;
    }
    
    /**
     * @see de.unikn.knime.core.data.DataType#getIcon()
     */
    public Icon getIcon() {
        return ICON;
    }
    
    /**
     * Returns "Fuzzy-Number DataType".
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "Fuzzy-Number DataType";
    }

}
