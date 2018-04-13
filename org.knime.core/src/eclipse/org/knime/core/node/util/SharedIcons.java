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
 *   Apr 3, 2018 (Johannes Schweig): created
 */
package org.knime.core.node.util;

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * An enum providing commonly used icons for the use in node dialogs.
 *
 * @author Johannes Schweig
 * @since 3.6
 */
public enum SharedIcons {

    /** Funnel **/
    FILTER("icons/filter.png"),
    /** Double arrow right **/
    ADD_ALL("icons/add_all.png"),
    /** Arrow right **/
    ADD("icons/add.png"),
    /** Arrow left **/
    REM("icons/rem.png"),
    /** Double arrow left**/
    REM_ALL("icons/rem_all.png"),
    /** Arrow down **/
    ARROW_DOWN("icons/arrow_down.png"),
    /** Arrow up **/
    ARROW_UP("icons/arrow_up.png"),
    /** Small arrow down **/
    SMALL_ARROW_DOWN("icons/down.png"),
    /** Small arrow right **/
    SMALL_ARROW_RIGHT("icons/right.png"),
    /** trash can **/
    TRASH("icons/trash.png");

    private final Icon m_icon;

    private SharedIcons(final String path) {
        m_icon = new ImageIcon(SharedIcons.class.getResource(path));
    }

    /**
     * @return the actual Icon, not null.
     */
    public Icon get() {
        return m_icon;
    }

}