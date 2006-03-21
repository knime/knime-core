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

import de.unikn.knime.core.data.def.DefaultDoubleCell;
import de.unikn.knime.core.data.renderer.DataCellRendererFamily;
import de.unikn.knime.core.data.renderer.DefaultDataCellRendererFamily;
import de.unikn.knime.core.data.renderer.DoubleBarRenderer;
import de.unikn.knime.core.data.renderer.DoubleCellRenderer;
import de.unikn.knime.core.data.renderer.DoubleGrayValueRenderer;


/**
 * The data type for datacells storing a double value and also acting as a 
 * fuzzy number, and with that as fuzzy interval.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public final class DoubleType extends DataType 
    implements DataCellSerializer {
    
    /** Singleton of this type. */
    public static final DoubleType DOUBLE_TYPE = new DoubleType();

    /** Singleton icon to be used to display this cell type. */
    private static final Icon ICON;

    /** Load double icon, use <code>null</code> if not available. */
    static {
        ImageIcon icon;
        try {
            ClassLoader loader = DataCell.class.getClassLoader();
            String path = 
                DataCell.class.getPackage().getName().replace('.', '/');
            icon = new ImageIcon(
                    loader.getResource(path + "/icon/doubleicon.png"));
        } catch (Exception e) {
            icon = null;
        }
        ICON = icon;
    }

    private static final DoubleCellComparator COMPARATOR =
        new DoubleCellComparator();
    
    /**
     * creates a new instance of the DoubleType. Don't use it. Rather use
     * the static singleton DOUBLE_TYPE of this class.  
     */
    private DoubleType() {
        addCompatibleType(FuzzyNumberType.FUZZY_NUMBER_TYPE);
        addCompatibleType(FuzzyIntervalType.FUZZY_INTERVAL_TYPE);
    }
    
    /**
     * @see DataType#getNativeComparator()
     */
    public DataCellComparator getNativeComparator() {
        return COMPARATOR;
    }
    
    /**
     * Returns DoubleValue.class.
     * @see DataType#getNativeValue()
     */
    protected Class<? extends DataValue> getNativeValue() {
        return DoubleValue.class;
    }
    
    /**
     * @see de.unikn.knime.core.data.DataType#getNativeRenderer(DataColumnSpec)
     */
    @Override
    protected DataCellRendererFamily getNativeRenderer(
            final DataColumnSpec spec) {
        
        return new DefaultDataCellRendererFamily(
                DoubleCellRenderer.STANDARD_RENDERER, 
                DoubleCellRenderer.PERCENT_RENDERER,
                new DoubleGrayValueRenderer(spec),
                new DoubleBarRenderer(spec)); 
    }
    
    /**
     * @see de.unikn.knime.core.data.DataType#getIcon()
     */
    public Icon getIcon() {
        return ICON;
    }

    /**
     * Returns "Double DataType".
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "Double DataType";
    }
    
    
    /**
     * @see DataCellSerializer#serialize(DataCell, DataOutput)
     */
    public void serialize(final DataCell cell, final DataOutput out) 
        throws IOException {
        if (!isOneSuperTypeOf(cell.getType())) {
            throw new IOException("DoubleType can't save cells of type "
                    +  cell.getType());
        }
        DoubleValue value = (DoubleValue)cell;
        out.writeDouble(value.getDoubleValue());
    }
    
    /**
     * @see DataCellSerializer#deserialize(DataInput)
     */
    public DataCell deserialize(final DataInput input) throws IOException {
        double d = input.readDouble();
        return new DefaultDoubleCell(d);
    }
}
