/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   25.06.2014 (thor): created
 */
package org.knime.core.data.renderer;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValue.UtilityFactory;
import org.knime.core.data.ExtensibleUtilityFactory;
import org.knime.core.data.image.ImageValue;

/**
 * Renderer for {@link ImageValue}s that tries to get the concrete renderer for every image (e.g. PNG or SVG).
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.10
 */
public class ImageValueRenderer implements DataValueRenderer {
    /**
     * Factory for percentage renderers.
     *
     * @since 2.8
     */
    public static final class Factory extends AbstractDataValueRendererFactory {
        /**
         * {@inheritDoc}
         */
        @Override
        public String getDescription() {
            return DESCRIPTION;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataValueRenderer createRenderer(final DataColumnSpec colSpec) {
            return new ImageValueRenderer();
        }
    }

    static final String DESCRIPTION = "Default";

    private final JLabel m_fallbackRendererComponent = new JLabel();

    private final DefaultTableCellRenderer m_fallbackTableCellRenderer = new DefaultTableCellRenderer();

    private final DefaultListCellRenderer m_fallbackListCellRenderer = new DefaultListCellRenderer();

    private static final Map<DataType, DataValueRendererFactory> TYPE_RENDERER_MAP = new ConcurrentHashMap<>();

    private ImageValueRenderer() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
        final boolean hasFocus, final int row, final int column) {
        if (!(value instanceof DataCell)) {
            return m_fallbackTableCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                column);
        } else {
            DataCell cell = (DataCell)value;
            if (cell.isMissing()) {
                return m_fallbackTableCellRenderer.getTableCellRendererComponent(table, "?", isSelected, hasFocus, row,
                    column);
            } else {
                DataValueRenderer renderer = findMatchingRenderer(cell);
                if (renderer != null) {
                    return renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                } else {
                    return m_fallbackTableCellRenderer.getTableCellRendererComponent(table, value, isSelected,
                        hasFocus, row, column);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getListCellRendererComponent(final JList list, final Object value, final int index,
        final boolean isSelected, final boolean cellHasFocus) {
        if (!(value instanceof DataCell)) {
            return m_fallbackListCellRenderer
                .getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        } else {
            DataCell cell = (DataCell)value;
            if (cell.isMissing()) {
                return m_fallbackListCellRenderer.getListCellRendererComponent(list, "?", index, isSelected,
                    cellHasFocus);
            } else {
                DataValueRenderer renderer = findMatchingRenderer(cell);
                if (renderer != null) {
                    return renderer.getListCellRendererComponent(list, cell, index, isSelected, cellHasFocus);
                } else {
                    return m_fallbackListCellRenderer.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(90, 90);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getRendererComponent(final Object val) {
        if (!(val instanceof DataCell)) {
            m_fallbackRendererComponent.setText(val.toString());
            return m_fallbackRendererComponent;
        } else {
            DataCell cell = (DataCell)val;
            if (cell.isMissing()) {
                m_fallbackRendererComponent.setText("?");
                return m_fallbackRendererComponent;
            } else {
                DataValueRenderer renderer = findMatchingRenderer(cell);
                if (renderer != null) {
                    return renderer.getRendererComponent(cell);
                } else {
                    m_fallbackRendererComponent.setText(val.toString());
                    return m_fallbackRendererComponent;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean accepts(final DataColumnSpec spec) {
        return true;
    }

    private static DataValueRenderer findMatchingRenderer(final DataCell cell) {
        DataType type = cell.getType();
        DataColumnSpec dummySpec = new DataColumnSpecCreator("dummy", type).createSpec();

        DataValueRendererFactory rFac;
        if (cell instanceof AdapterValue) {
            rFac = findMatchingRendererFactory(((AdapterValue)cell).getAdapterMap().keySet());
        } else {
            rFac = TYPE_RENDERER_MAP.get(type);
            if (rFac == null) {
                rFac = findMatchingRendererFactory(type.getValueClasses());
                TYPE_RENDERER_MAP.put(type, rFac);
            }
        }
        if (rFac != null) {
            return rFac.createRenderer(dummySpec);
        } else {
            return null;
        }
    }

    private static DataValueRendererFactory findMatchingRendererFactory(final Collection<Class<? extends DataValue>> valueClasses) {
        for (Class<? extends DataValue> dvc : valueClasses) {
            if (dvc == DataValue.class) {
                continue;
            }

            UtilityFactory fac = DataType.getUtilityFor(dvc);
            if (fac instanceof ExtensibleUtilityFactory) {
                DataValueRendererFactory rFac = ((ExtensibleUtilityFactory)fac).getPreferredRenderer();
                if (rFac == null) {
                    rFac = ((ExtensibleUtilityFactory)fac).getDefaultRenderer();
                }
                return rFac;
            }
        }
        return null;
    }
}
