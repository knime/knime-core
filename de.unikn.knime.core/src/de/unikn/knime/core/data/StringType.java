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

import de.unikn.knime.core.data.def.DefaultStringCell;
import de.unikn.knime.core.data.renderer.DataCellRendererFamily;
import de.unikn.knime.core.data.renderer.DefaultDataCellRendererFamily;
import de.unikn.knime.core.data.renderer.StringCellRenderer;

/**
 * The data type for datacells storing a string value.
 * 
 * @author mb, University of Konstanz
 */
public final class StringType extends DataType 
    implements DataCellSerializer {

    /** Singelton of <code>StringType</code>. */
    public static final StringType STRING_TYPE = new StringType();

    /** Singleton icon to be used to display this cell type. */
    private static final Icon ICON;

    /** Load string icon, use <code>null</code> if not available. */
    static {
        ImageIcon icon;
        try {
            ClassLoader loader = DataCell.class.getClassLoader();
            String path = 
                DataCell.class.getPackage().getName().replace('.', '/');
            icon = new ImageIcon(
                    loader.getResource(path + "/icon/stringicon.png"));
        } catch (Exception e) {
            icon = null;
        }
        ICON = icon;
    }

    private static final StringCellComparator STRING_COMPARATOR = 
        new StringCellComparator();

    /**
     * Don't use it, rather take the static singleton STRING_TYPE.
     */
    private StringType() {

    }

    /**
     * @return Compares two <code>StringValue</code> objects.
     */
    public DataCellComparator getNativeComparator() {
        return STRING_COMPARATOR;
    }
    
    /**
     * @see de.unikn.knime.core.data.DataType#getNativeRenderer(DataColumnSpec)
     */
    @Override
    protected DataCellRendererFamily getNativeRenderer(
            final DataColumnSpec spec) {
        return new DefaultDataCellRendererFamily(StringCellRenderer.INSTANCE);
     
    }

    /**
     * @see DataType#getNativeValue()
     */
    protected Class<? extends DataValue> getNativeValue() {
        return StringValue.class;
    }
    
    /**
     * @see de.unikn.knime.core.data.DataType#getIcon()
     */
    public Icon getIcon() {
        return ICON;
    }
    
    /**
     * Returns "String DataType".
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "String DataType";
    }
    
    /**
     * @see DataCellSerializer#serialize(DataCell, DataOutput)
     */
    public void serialize(final DataCell cell, 
            final DataOutput output) throws IOException {
        if (!isOneSuperTypeOf(cell.getType())) {
            throw new IOException("StringType can't save cells of type "
                    +  cell.getType());
        }
        StringValue value = (StringValue)cell;
        output.writeUTF(value.getStringValue());
    }
    
    /**
     * @see DataCellSerializer#deserialize(DataInput)
     */
    public DataCell deserialize(final DataInput input) throws IOException {
        String s = input.readUTF();
        return new DefaultStringCell(s);
    }

}
