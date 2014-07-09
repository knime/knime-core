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
 *   05.07.2014 (koetter): created
 */
package org.knime.timeseries.node.movagg;

import org.knime.core.node.util.ButtonGroupEnumInterface;

/**
 * Enumeration of window types in time series analysis.
 *
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 */
public enum WindowType implements ButtonGroupEnumInterface {
    /**Forward.*/
    FORWARD("Forward", "Looks window length rows forward from the current point."),
    /**Central.*/
    CENTER("Central", "Looks half the window length backward from the current point and half forward."),
    /**Backward.*/
    BACKWARD("Backward", "Looks window length rows back from the current point.");

    private final String m_name;
    private final String m_description;

    private WindowType(final String name, final String description) {
        m_name = name;
        m_description = description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return m_name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getActionCommand() {
        return name();
    }

    /**
     * @param actionCommand to get the type for
     * @return the {@link WindowType} for the given action command
     */
    public static WindowType getType(final String actionCommand) {
        return WindowType.valueOf(actionCommand);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTip() {
        return m_description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDefault() {
        return getDefault().equals(this);
    }

    /**
     * @return the default {@link WindowType} to use
     */
    public static WindowType getDefault() {
        return BACKWARD;
    }

}
