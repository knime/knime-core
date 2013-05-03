/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
package org.knime.core.node.util;

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;


/**
 * Renderer that checks if the value being renderer is of type
 * <code>DataColumnSpec</code> if so it will renderer the name of the column
 * spec and also the type's icon. If not, the passed value's toString() method
 * is used for rendering.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DataColumnSpecListCellRenderer extends DefaultListCellRenderer {
    private static final long serialVersionUID = 1156595670217009312L;

    /**
     * All {@link DataColumnSpec}s that contain a property whit this name {@value} in their
     * {@link DataColumnProperties} are considered as invalid and special rendered.
     * @since 2.8
     */
    public static final String INVALID_PROPERTY_NAME = "KNIME_invallid_property_flag";

    /**
     * @param spec the {@link DataColumnSpec} to check
     * @return <code>true</code> if the {@link DataColumnSpec} s invalid e.g. its contains
     * {@link DataColumnProperties} contain a property with the name {@value #INVALID_PROPERTY_NAME}.
     * @since 2.8
     */
    public static boolean isInvalid(final DataColumnSpec spec) {
        if (spec == null) {
            throw new NullPointerException("spec must not be null");
        }
        final DataColumnProperties props = spec.getProperties();
        return props != null && props.containsProperty(INVALID_PROPERTY_NAME);
    }

    /**
     * @param colName the name of the invalid column
     * @return the invalid {@link DataColumnSpec} for the given name
     * @since 2.8
     */
    public static DataColumnSpec createInvalidSpec(final String colName) {
        DataColumnSpecCreator creator = new DataColumnSpecCreator(colName, DataType.getType(DataCell.class));
        creator.setProperties(new DataColumnProperties(creaeteInvalidPropertiesMap()));
        return creator.createSpec();
    }

    /**
     * @param originalSpec the original {@link DataColumnSpec} to be marked as invalid
     * @return the given {@link DataColumnSpec} with the added invalid flag
     * @since 2.8
     * @see #INVALID_PROPERTY_NAME
     */
    public static final DataColumnSpec createInvalidSpec(final DataColumnSpec originalSpec) {
        DataColumnSpecCreator creator =
                new DataColumnSpecCreator(originalSpec);
        final DataColumnProperties origProps = originalSpec.getProperties();
        final Map<String, String> map = creaeteInvalidPropertiesMap();
        final DataColumnProperties props;
        if (origProps != null) {
            props = origProps.cloneAndOverwrite(map);
        } else {
            props = new DataColumnProperties(map);
        }
        creator.setProperties(props);
        final DataColumnSpec invalidSpec = creator.createSpec();
        return invalidSpec;
    }

    /**
     * @return
     */
    private static Map<String, String> creaeteInvalidPropertiesMap() {
        final Map<String, String> map = new HashMap<String, String>(1);
        map.put(INVALID_PROPERTY_NAME, INVALID_PROPERTY_NAME);
        return map;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getListCellRendererComponent(
            final JList list, final Object value, final int index,
            final boolean isSelected, final boolean cellHasFocus) {
        // The super method will reset the icon if we call this method
        // last. So we let super do its job first and then we take care
        // that everything is properly set.
        Component c =  super.getListCellRendererComponent(list, value, index,
                isSelected, cellHasFocus);
        assert (c == this);
        String text = null;
        if (value instanceof DataColumnSpec) {
            final DataColumnSpec spec = (DataColumnSpec)value;
            text = spec.getName().toString();
            setText(text);
            setIcon(spec.getType().getIcon());
            if (isInvalid(spec)) {
                //this is an invalid data column
                setBorder(BorderFactory.createLineBorder(Color.red));
            } else {
                setBorder(null);
            }
        }
        list.setToolTipText(text);
        return this;
    }
}
