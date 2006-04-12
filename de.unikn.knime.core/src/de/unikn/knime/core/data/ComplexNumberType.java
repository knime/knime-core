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
 *   23.03.2006 (cebron): created
 */
package de.unikn.knime.core.data;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import de.unikn.knime.core.data.def.DefaultComplexNumberCell;
import de.unikn.knime.core.data.renderer.ComplexNumberCellRenderer;
import de.unikn.knime.core.data.renderer.DataCellRendererFamily;
import de.unikn.knime.core.data.renderer.DefaultDataCellRendererFamily;

/**
 * The data type for datacells storing a complex number value.
 * 
 * @author ciobaca, cebron, University of Konstanz
 */
public final class ComplexNumberType extends DataType 
    implements DataCellSerializer {
    
    /** Singleton of this type. */
    public static final ComplexNumberType COMPLEX_NUMBER_TYPE = 
                            new ComplexNumberType();

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
                    loader.getResource(path + "/icon/complexnumbericon.png"));
        } catch (Exception e) {
            icon = null;
        }
        ICON = icon;
    }

    private static final ComplexNumberCellComparator COMPARATOR =
        new ComplexNumberCellComparator();
    
    /**
     * creates a new instance of the ComplexNumberType. Don't use it. 
     * Rather use the static singleton COMPLEX_NUMBER_TYPE of this class.  
     */
    private ComplexNumberType() {
        addCompatibleType(DoubleType.DOUBLE_TYPE);
    }
    
    /**
     * @see DataType#getNativeComparator()
     */
    public DataCellComparator getNativeComparator() {
        return COMPARATOR;
    }
    
    /**
     * Returns ComplexNumberValue.class.
     * @see DataType#getNativeValue()
     */
    protected Class<? extends DataValue> getNativeValue() {
        return ComplexNumberValue.class;
    }
    
    /**
     * @see de.unikn.knime.core.data.DataType#getNativeRenderer(DataColumnSpec)
     */
    @Override
    protected DataCellRendererFamily getNativeRenderer(
            final DataColumnSpec spec) {
        
        return new DefaultDataCellRendererFamily(
                ComplexNumberCellRenderer.STANDARD_RENDERER 
                    ); 
    }
    
    /**
     * @see de.unikn.knime.core.data.DataType#getIcon()
     */
    public Icon getIcon() {
        return ICON;
    }

    /**
     * Returns "Complex number DataType".
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "Complex number DataType";
    }
    
    /**
     * @see DataCellSerializer#serialize(DataCell, DataOutput)
     */
    public void serialize(final DataCell cell, 
            final DataOutput output) throws IOException {
        if (!isOneSuperTypeOf(cell.getType())) {
            throw new IOException("ComplexNumberType can't save cells of type "
                    +  cell.getType());
        }
        ComplexNumberValue value = (ComplexNumberValue)cell;
        output.writeDouble(value.getRealValue());
        output.writeDouble(value.getImaginaryValue());
    }
    
    /**
     * @see DataCellSerializer#deserialize(DataInput)
     */
    public DataCell deserialize(final DataInput input) throws IOException {
        double real = input.readDouble();
        double imag = input.readDouble();
        return new DefaultComplexNumberCell(real, imag);
    }
}
