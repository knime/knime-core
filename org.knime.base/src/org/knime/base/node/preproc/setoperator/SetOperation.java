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
 * History
 *    22.11.2007 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.setoperator;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.util.StringIconOption;


/**
 * This enumeration contains all valid set operations.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public enum SetOperation implements StringIconOption {

    /**And operator.*/
//    "\u22c2"
    AND("Intersection", "&", "and.png"),
    /**or operator.*/
//    "\u22c3"
    OR("Union", "|", "or.png"),
    /**Minus operator.*/
    MINUS("Complement", "-", "minus.png"),
    /**Xor operator.*/
    XOR("Exclusive-or", "X|", "xor.png");

    private static final String ICON_PATH =
        "org/knime/base/node/preproc/setoperator/icons/";

    private final String m_name;

    private final String m_colName;

    private final Icon m_icon;

    private SetOperation(final String name, final String colName,
            final String iconName) {
        m_name = name;
        m_colName = colName;
        final URL iconUrl =
            this.getClass().getClassLoader().getResource(ICON_PATH + iconName);
        if (iconUrl == null) {
            m_icon = null;
        } else {
            m_icon = new ImageIcon(iconUrl);
        }
    }


    /**
     * @return the name of the operation
     */
    public String getName() {
        return m_name;
    }

    /**
     * @return the default set operation
     */
    public static SetOperation getDefault() {
        return AND;
    }

    /**
     * @param name the name to get the operation for
     * @return the set operation with the given name
     */
    public static SetOperation getOperation4Name(final String name) {
        if (name == null) {
            throw new NullPointerException("Name must not be null");
        }
        for (final SetOperation op : values()) {
            if (op.getName().equals(name)) {
                return op;
            }
        }
        throw new IllegalArgumentException("No valid set operation name: "
                + name);
    }

    /**
     * @return a array with the name of all set operations
     */
    public static String[] getNameList() {
        final SetOperation[] values = values();
        final String[] names = new String[values.length];
        for (int i = 0, length = values.length; i < length; i++) {
            names[i] = values[i].getName();
        }
        return names;
    }

    /**
     * @param col1Spec the column spec of the first set
     * @param col2Spec the column spec of the second set
     * @return the result column spec
     */
    public DataColumnSpec createResultColSpec(final DataColumnSpec col1Spec,
            final DataColumnSpec col2Spec) {
        if (col1Spec == null) {
            throw new NullPointerException("col1Spec must not be null");
        }
        if (col2Spec == null) {
            throw new NullPointerException("col2Spec must not be null");
        }
        final DataType resultType;
        if (col1Spec.getType().equals(col2Spec.getType())) {
            resultType = col1Spec.getType();
        } else {
            resultType = StringCell.TYPE;
        }
        final StringBuilder buf = new StringBuilder();
        buf.append(col1Spec.getName());
        buf.append(' ');
        buf.append(m_colName);
        buf.append(' ');
        buf.append(col2Spec.getName());
        final DataColumnSpecCreator specCreator = new DataColumnSpecCreator(
                buf.toString(), resultType);
        return specCreator.createSpec();
    }


    /**
     * @param spec1 the {@link DataColumnSpec} of the first set or
     * <code>null</code> if the RowID is used
     * @param spec2 the {@link DataColumnSpec} of the second set or
     * <code>null</code> if the RowID is used
     * @return the {@link DataValueComparator} to use
     */
    public DataValueComparator getComparator(final DataColumnSpec spec1,
            final DataColumnSpec spec2) {
        final DataValueComparator comp;
        if (spec1.getType().equals(spec2.getType())) {
            comp = spec1.getType().getComparator();
        } else {
            comp = GeneralDataValueComparator.getInstance();
        }
        return comp;
    }


    /**
     * @param cell1 the member of the first set or <code>null</code> if it
     * is a duplicate.
     * @param cell2 the member of the second set or <code>null</code> if it
     * is a duplicate.
     * @param differentTypes <code>true</code> if both sets are of a
     * different type
     * @return the result of the combination. Could be <code>null</code>.
     */
    public DataCell compute(final DataCell cell1, final DataCell cell2,
            final boolean differentTypes) {
        final DataCell c1;
        final DataCell c2;
        if (differentTypes) {
            if (cell1 != null) {
                c1 = new StringCell(cell1.toString());
            } else {
                c1 = null;
            }
            if (cell2 != null) {
                c2 = new StringCell(cell2.toString());
            } else {
                c2 = null;
            }
        } else {
            c1 = cell1;
            c2 = cell2;
        }
        switch (this) {
        case AND:
            if (c1 != null && c1.equals(c2)) {
                return c1;
            }
            return null;
        case OR:
            if (c1 != null) {
                return c1;
            } else if (c2 != null) {
                return c2;
            }
            return null;
        case MINUS:
            if (c1 != null && !c1.equals(c2)) {
                return c1;
            }
            return null;
        case XOR:
            if (c1 != null && !c1.equals(c2)) {
                return c1;
            } else if (c2 != null && !c2.equals(c1)) {
                return c2;
            }
            return null;
        default:
            throw new IllegalStateException("Operation " + this.getName()
                    + " not implemented");
        }
    }


    /**
     * {@inheritDoc}
     */
    public Icon getIcon() {
        return m_icon;
    }


    /**
     * {@inheritDoc}
     */
    public String getText() {
        return getName();
    }
}
