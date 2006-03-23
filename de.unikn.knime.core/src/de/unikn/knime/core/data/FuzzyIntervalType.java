/* -------------------------------------------------------------------
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import de.unikn.knime.core.data.def.DefaultFuzzyIntervalCell;


/**
 * The data type for datacells storing a fuzzy interval.
 * 
 * @author mb, University of Konstanz
 */
public final class FuzzyIntervalType extends DataType 
    implements DataCellSerializer {

    /** Singleton icon to be used to display this cell type. */
    private static final Icon ICON;

    /** Load fuzzy interval icon, use <code>null</code> if not available. */
    static {
        ImageIcon icon;
        try {
            ClassLoader loader = DataCell.class.getClassLoader();
            String path = 
                DataCell.class.getPackage().getName().replace('.', '/');
            icon = new ImageIcon(
                    loader.getResource(path + "/icon/fuzzyintervalicon.png"));
        } catch (Exception e) {
            icon = null;
        }
        ICON = icon;
    }

    /** Singleton of this type. */
    public static final FuzzyIntervalType FUZZY_INTERVAL_TYPE 
                                                = new FuzzyIntervalType();

    private static final FuzzyIntervalCellComparator COMPARATOR = 
        new FuzzyIntervalCellComparator(); 

    private FuzzyIntervalType() {
        
    }

    /**
     * @return Comparator which compares two <code>FuzzyIntervalValue</code>
     *         objects.
     */
    public DataCellComparator getNativeComparator() {
        return COMPARATOR;
    }

    /**
     * @see DataType#getNativeValue()
     */
    protected Class<? extends DataValue> getNativeValue() {
        return FuzzyIntervalValue.class;
    }

    /**
     * @see de.unikn.knime.core.data.DataType#getIcon()
     */
    public Icon getIcon() {
        return ICON;
    }
    
    /**
     * Returns "Fuzzy-Interval DataType".
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "Fuzzy-Interval DataType";
    }
    
    /**
     * @see DataCellSerializer#serialize(DataCell, DataOutput)
     */
    public void serialize(final DataCell cell, 
            final DataOutput output) throws IOException {
        if (!isOneSuperTypeOf(cell.getType())) {
            throw new IOException("FuzzIntervalType can't save cells of type "
                    +  cell.getType());
        }
        FuzzyIntervalValue value = (FuzzyIntervalValue)cell;
        output.writeDouble(value.getMinSupport());
        output.writeDouble(value.getMinCore());
        output.writeDouble(value.getMaxCore());
        output.writeDouble(value.getMaxSupport());
    }
    
    /**
     * @see DataCellSerializer#deserialize(DataInput)
     */
    public DataCell deserialize(final DataInput input) throws IOException {
        double minSupp = input.readDouble();
        double minCore = input.readDouble();
        double maxCore = input.readDouble();
        double maxSupp = input.readDouble();
        return new DefaultFuzzyIntervalCell(minSupp, minCore, maxCore, maxSupp);
    }
}
