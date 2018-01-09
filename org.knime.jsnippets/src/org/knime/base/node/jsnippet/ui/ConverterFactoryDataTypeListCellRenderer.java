/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
package org.knime.base.node.jsnippet.ui;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;

/**
 * Renderer that checks if the value being renderer is a {@link JavaToDataCellConverterFactory} to render the name of
 * the destination type and its icon aswell as the name of the factory. If not, the passed value's toString() method is
 * used for rendering.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @since 3.2
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public class ConverterFactoryDataTypeListCellRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = -3238164216976500254L;

    @Override
    public Component getListCellRendererComponent(final JList list, final Object value, final int index,
        final boolean isSelected, final boolean cellHasFocus) {
        // The super method will reset the icon if we call this method
        // last. So we let super do its job first and then we take care
        // that everything is properly set.
        final Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        assert c == this;

        if (value instanceof JavaToDataCellConverterFactory<?>) {
            final JavaToDataCellConverterFactory<?> factory = (JavaToDataCellConverterFactory<?>)value;
            setText(factory.getName());
            setIcon(factory.getDestinationType().getIcon());
        } else if (value instanceof DataCellToJavaConverterFactory) {
            final DataCellToJavaConverterFactory<?, ?> factory = (DataCellToJavaConverterFactory<?, ?>)value;
            setText(factory.getName());
        }
        setToolTipText(getText());
        return this;
    }
}
