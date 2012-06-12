/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
