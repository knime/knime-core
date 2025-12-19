/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 15, 2023 (wiswedel): created
 */
package org.knime.core.data.property;

import java.awt.Color;

import org.knime.core.data.DataCell;
import org.knime.core.node.config.ConfigWO;

/**
 * Interface allowing requests for {@link ColorAttr} by {@link DataCell}. This interface is not meant to be extended
 * by 3rd party code. There are two implementations available:
 * <ol>
 * <li> {@link ColorModelNominal} for nominal assignment based on column values
 * <li> {@link ColorModelRange} numeric range based coloring
 * </old>
 *
 */
public sealed interface ColorModel permits ColorModelNominal, ColorModelRange, ColorModelRange2 {

    /**
     * Returns a <code>ColorAttr</code> for the given <code>DataCell</code>.
     * @param dc the <code>DataCell</code> to get the color for
     * @return a <code>ColorAttr</code> object, but not <code>null</code>
     */
    ColorAttr getColorAttr(DataCell dc);
    /**
     * Saves this <code>ColorModel</code> to the given
     * <code>ConfigWO</code>.
     * @param config used to save this <code>ColorModel</code> to
     */
    void save(ConfigWO config);

    /**
     * Opaque color as a 24-bit integer - as per java.awt.Color.decode(String), e.g. RED -> "#FF0000"
     *
     * @param color Non-null color object
     * @return The hex string
     */
    static String colorToHexString(final Color color) {
        return String.format("#%06X", color.getRGB() & 0x00FFFFFF);
    }
}
