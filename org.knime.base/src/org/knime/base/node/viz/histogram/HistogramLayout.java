/*
 *
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 */
package org.knime.base.node.viz.histogram;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.node.util.ButtonGroupEnumInterface;

/**
 * Enumerates all possible layouts of the Histogram visualisation.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public enum HistogramLayout implements ButtonGroupEnumInterface {
    /** The BarElements are displayed next to each other. */
    SIDE_BY_SIDE("side_by_side", "Side by side", false),
    /** The BarElements are displayed on top of each other. */
    STACKED("stacked", "Stacked", true);

    private final String m_id;
    private final String m_label;
    private final boolean m_default;

    private HistogramLayout(final String id, final String label,
            final boolean isDefault) {
        m_id = id;
        m_label = label;
        m_default = isDefault;
    }

    /**
     * @return the id of the layout option
     */
    public String getActionCommand() {
        return m_id;
    }

    /**
     * @return the name of the layout option
     */
    public String getText() {
        return m_label;
    }

    /**
     * {@inheritDoc}
     */
    public String getToolTip() {
        return null;
    }


    /**
     * Returns the enumeration fields as a String list of their names.
     *
     * @return the enumeration fields as a String list of their names
     */
    public static List<String> asStringList() {
        final Enum<HistogramLayout>[] values = values();
        final List<String> list = new ArrayList<String>();
        for (int i = 0; i < values.length; i++) {
            list.add(values[i].name());
        }
        return list;
    }

    /**
     * Returns the histogram layout for the given name. If the name is
     * <code>null</code> or has length zero the method returns the default
     * layout.
     *
     * @param name the name to check
     * @return the layout with the given name
     */
    public static HistogramLayout getLayout4String(final String name) {
        if (name == null || name.length() < 1) {
            return getDefaultLayout();
        }
        for (final HistogramLayout value : HistogramLayout.values()) {
            if (value.name().equals(name)) {
                return value;
            }
        }
        return getDefaultLayout();
    }

    /**
     * @return the default aggregation method
     */
    public static HistogramLayout getDefaultLayout() {
        for (final HistogramLayout value : HistogramLayout.values()) {
            if (value.isDefault()) {
                return value;
            }
        }
        throw new IllegalStateException("No default layout defined");
    }

   /**
    * {@inheritDoc}
    */
    public boolean isDefault() {
       return m_default;
   }

    /**
     * @param layout the name of the layout to check
     * @return <code>true</code> if it's a valid histogram layout otherwise
     *         it returns <code>false</code>.
     */
    public static boolean valid(final String layout) {
        for (final HistogramLayout value : HistogramLayout.values()) {
            if (value.name().equals(layout)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param id the id to search for
     * @return the <code>HistogramLayout</code> object with the given id
     */
    public static HistogramLayout getLayout4ID(final String id) {
        for (final HistogramLayout type : values()) {
            if (type.getActionCommand().equals(id)) {
                return type;
            }
        }
        return getDefaultLayout();
    }
}
