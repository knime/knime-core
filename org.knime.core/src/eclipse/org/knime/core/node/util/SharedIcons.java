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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

/**
 * A class providing commonly used icons for the use in node dialogs.
 * @author Johannes Schweig
 */
public class SharedIcons {

    /**
     * Constants for the icon name and path
     */
    public final static String FILTER_ICON = "FILTER";
    private final static String FILTER_PATH = "icons/filter.png";
    public final static String ADD_ALL_ICON = "ADD_ALL";
    private final static String ADD_ALL_PATH = "icons/add_all.png";
    public final static String ADD_ICON = "ADD";
    private final static String ADD_PATH = "icons/add.png";
    public final static String REM_ALL_ICON = "REM_ALL";
    private final static String REM_ALL_PATH = "icons/rem_all.png";
    public final static String REM_ICON = "REM";
    private final static String REM_PATH = "icons/rem.png";
    public final static String ARROW_DOWN_ICON = "ARROW_DOWN";
    private final static String ARROW_DOWN_PATH = "icons/arrow_down.png";
    public final static String ARROW_UP_ICON = "ARROW_UP";
    private final static String ARROW_UP_PATH = "icons/arrow_up.png";
    public final static String SMALL_ARROW_DOWN_ICON = "DOWN";
    private final static String SMALL_ARROW_DOWN_PATH = "icons/down.png";
    public final static String SMALL_ARROW_RIGHT_ICON = "RIGHT";
    private final static String SMALL_ARROW_RIGHT_PATH = "icons/right.png";
    public final static String TRASH_ICON = "TRASH";
    private final static String TRASH_PATH ="icons/trash.png";

    // icon register to retrieve icons' paths
    private static final Map<String, String> m_iconRegister;
    static {
        Map<String, String> map = new HashMap<String, String>();
        map.put(FILTER_ICON, FILTER_PATH);
        map.put(ADD_ALL_ICON, ADD_ALL_PATH);
        map.put(ADD_ICON, ADD_PATH);
        map.put(REM_ALL_ICON, REM_ALL_PATH);
        map.put(REM_ICON, REM_PATH);
        map.put(ARROW_DOWN_ICON, ARROW_DOWN_PATH);
        map.put(ARROW_UP_ICON, ARROW_UP_PATH);
        map.put(SMALL_ARROW_DOWN_ICON, SMALL_ARROW_DOWN_PATH);
        map.put(SMALL_ARROW_RIGHT_ICON, SMALL_ARROW_RIGHT_PATH);
        map.put(TRASH_ICON, TRASH_PATH);
        m_iconRegister = map;
    }


    /**
     * Returns the ImageIcon to an icon
     * @param icon the requested icon
     * @return the ImageIcon
     */
    public static ImageIcon getImageIcon(final String icon){
        String path = m_iconRegister.get(icon);
        if (path != null) {
            return new ImageIcon(getImageUrl(path));
        } else {
            return null;
        }

    }

    /**
     * Returns the Image URL for the specified path
     * @param path
     * @return the Image URL
     */
    private static URL getImageUrl(final String path){
        return SharedIcons.class.getResource(path);
    }
}
