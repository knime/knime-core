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
 *   Jun 01, 2018 (Mor Kalla): created
 */
package org.knime.core.util.binning.auto;

import org.knime.core.node.util.ButtonGroupEnumInterface;

/**
 * This enum holds the binning naming options.
 * <ul>
 * <li>{@link #NUMBERED}</li>
 * <li>{@link #EDGES}</li>
 * <li>{@link #MIDPOINTS}</li>
 * </ul>
 *
 * @author Mor Kalla
 * @since 3.6
 *
 * @deprecated Extends the now-old {@link ButtonGroupEnumInterface}.
 */
@Deprecated
public enum BinNaming implements ButtonGroupEnumInterface {

        /**
         * Numbered starting from one: Bin 1, Bin2, ...
         */
        NUMBERED("Numbered", "e.g.: Bin 1, Bin 2, Bin 3"),

        /**
         * Uses edges for defining bins: (-,0] (0,1], ...
         */
        EDGES("Borders", "e.g.: [-10,0], (0,10], (10,20]"),

        /**
         * Uses midpoints for bins: 0.25, 0.75, ...
         */
        MIDPOINTS("Midpoints", "e.g.: -5, 5, 15");

    private String m_label;

    private String m_desc;

    /**
     * Constructs a {@link BinNaming} with a label and description.
     *
     * @param label the label of the binning naming
     * @param desc the description of the binning naming
     */
    BinNaming(final String label, final String desc) {
        m_label = label;
        m_desc = desc;
    }

    @Override
    public String getText() {
        return m_label;
    }

    @Override
    public String getActionCommand() {
        return name();
    }

    @Override
    public String getToolTip() {
        return m_desc;
    }

    @Override
    public boolean isDefault() {
        return NUMBERED.equals(this);
    }

}
