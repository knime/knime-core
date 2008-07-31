/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 */
package org.knime.core.data.renderer;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.JList;
import javax.swing.JTable;

import org.knime.core.data.DataColumnSpec;

/**
 * Container for <code>DataValueRendererFamily</code> that is by itself a 
 * renderer family. This class is used in <code>DataType</code> when all 
 * available native renderer are gathered and returned as 
 * DataValueRendererFamily. 
 * 
 * <p><strong>Note:</strong>This is a helper class that shouldn't be 
 * any useful for you.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class SetOfRendererFamilies implements DataValueRendererFamily {
    
    private final List<DataValueRendererFamily> m_list;
    private DataValueRenderer m_active;
    
    /**
     * Constructs a new set from a list of renderer families given in a list.
     * The active renderer will be the first one that is available.
     * @param fams All renderer in a list (type DataValueRendererFamily)
     * @throws IllegalArgumentException If list is empty
     * @throws NullPointerException If argument is null
     * @throws ClassCastException If list contains unexpected classes.
     */
    public SetOfRendererFamilies(final List<DataValueRendererFamily> fams) {
        if (fams.isEmpty()) {
            throw new IllegalArgumentException("No renderer available");
        }
        for (DataValueRendererFamily e : fams) {
            if (m_active == null) {
                m_active = e;
            }
        }
        m_list = fams;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getRendererDescriptions() {
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        for (DataValueRendererFamily e : m_list) {
            set.addAll(Arrays.asList(e.getRendererDescriptions()));
        }
        return set.toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     */
    public void setActiveRenderer(final String desc) {
        for (DataValueRendererFamily e : m_list) {
            if (Arrays.asList(e.getRendererDescriptions()).contains(desc)) {
                e.setActiveRenderer(desc);
                m_active = e;
                return;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return m_active.getDescription();
    }

    /**
     * {@inheritDoc}
     */
    public Dimension getPreferredSize() {
        return m_active.getPreferredSize();
    }

    /**
     * {@inheritDoc}
     */
    public Component getTableCellRendererComponent(
            final JTable table, final Object value, final boolean isSelected, 
            final boolean hasFocus, final int row, final int column) {
        return m_active.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
    }

    /**
     * {@inheritDoc}
     */
    public Component getListCellRendererComponent(final JList list,
            final Object value, final int index, 
            final boolean isSelected, final boolean cellHasFocus) {
        return m_active.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);
    }
    
    /**
     * {@inheritDoc}
     */
    public Component getRendererComponent(final Object val) {
        return m_active.getRendererComponent(val);
    }

    /**
     * {@inheritDoc}
     */
    public boolean accepts(final String desc, final DataColumnSpec spec) {
        for (DataValueRendererFamily e : m_list) {
            if (Arrays.asList(e.getRendererDescriptions()).contains(desc)) {
                return e.accepts(desc, spec);
            }
        }
        throw new IllegalArgumentException(
                "Invalid renderer description: " + desc);
    }

    /**
     * {@inheritDoc}
     */
    public boolean accepts(final DataColumnSpec spec) {
        return m_active.accepts(spec);
    }
    
    

}
