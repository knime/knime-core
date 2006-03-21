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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import de.unikn.knime.core.data.def.DefaultIntCell;



/**
 * The data type for datacells storing an int value and also acting as a 
 * double value, and with that as fuzzy number and fuzzy interval.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public final class IntType extends DataType implements DataCellSerializer {
    
    /** Singleton for int types. */
    public static final IntType INT_TYPE = new IntType();
    
    /** Singleton icon to be used to display this cell type. */
    private static final Icon ICON;

    /** Load integer icon, use <code>null</code> if not available. */
    static {
        ImageIcon icon;
        try {
            ClassLoader loader = DataCell.class.getClassLoader();
            String path = 
                DataCell.class.getPackage().getName().replace('.', '/');
            icon = new ImageIcon(
                    loader.getResource(path + "/icon/integericon.png"));
        } catch (Exception e) {
            icon = null;
        }
        ICON = icon;
    }

    private static final IntCellComparator INT_COMPARATOR = 
        new IntCellComparator();
    
    /**
     * use the public singleton instead of creating a new instance.
     */
    private IntType() {
        addCompatibleType(DoubleType.DOUBLE_TYPE);
        addCompatibleType(FuzzyNumberType.FUZZY_NUMBER_TYPE);
        addCompatibleType(FuzzyIntervalType.FUZZY_INTERVAL_TYPE);
    }
    
    /**
     * @return Compares two <code>IntValue</code> objects.
     */
    public DataCellComparator getNativeComparator() {
        return INT_COMPARATOR;
    }

    /**
     * @see DataType#getNativeValue()
     */
    protected Class<? extends DataValue> getNativeValue() {
        return IntValue.class;
    }
    
    /**
     * @see de.unikn.knime.core.data.DataType#getIcon()
     */
    public Icon getIcon() {
        return ICON;
    }
    
    /**
     * Returns "Int DataType".
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "Int DataType";
    }
    
    /**
     * @see DataCellSerializer#serialize(DataCell, DataOutput)
     */
    public void serialize(final DataCell cell, 
            final DataOutput output) throws IOException {
        if (!isOneSuperTypeOf(cell.getType())) {
            throw new IOException("IntType can't save cells of type "
                    +  cell.getType());
        }
        IntValue value = (IntValue)cell;
        output.writeInt(value.getIntValue());
    }
    
    /**
     * @see DataCellSerializer#deserialize(DataInput)
     */
    public DataCell deserialize(final DataInput input) throws IOException {
        int i = input.readInt();
        return new DefaultIntCell(i);
    }
}
