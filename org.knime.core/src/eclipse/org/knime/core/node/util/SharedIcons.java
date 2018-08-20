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

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * An enum providing commonly used icons for the use in node dialogs.
 *
 * @author Johannes Schweig
 * @since 3.6
 */
public enum SharedIcons {

    /**
     * Actions
     */
    /** Funnel **/
    FILTER("icons/filter.png"),
    /** Help question mark filled **/
    HELP("icons/help.png"),
    /** Warning sign filled **/
    WARNING("icons/warning.png"),
    /** Info sign filled **/
    INFO("icons/info.png"),
    /** Info sign outline **/
    INFO_OUTLINE("icons/info_outline.png"),
    /** Settings cog wheel **/
    SETTINGS("icons/settings.png"),
    /**
     * Arrows/Dropdowns
     */
    /** Alternative double arrow right **/
    ALT_DOUBLE_ARROW_RIGHT("icons/alt_double_arrow_right.png"),
    /** Alternative arrow right **/
    ALT_ARROW_RIGHT("icons/alt_arrow_right.png"),
    /** Alternative arrow left **/
    ALT_ARROW_LEFT("icons/alt_arrow_left.png"),
    /** Double arrow left**/
    ALT_DOUBLE_ARROW_LEFT("icons/alt_double_arrow_left.png"),
    /** Small arrow down **/
    SMALL_ARROW_DOWN("icons/small_arrow_drop_down.png"),
    /** Small arrow right **/
    SMALL_ARROW_RIGHT("icons/small_arrow_drop_right.png"),
    /** Small arrow down small 8px **/
    SMALL_ARROW_DOWN_8("icons/small_arrow_drop_down@0.5x.png"),
    /** Small arrow right small 8 px**/
    SMALL_ARROW_RIGHT_8("icons/small_arrow_drop_right@0.5x.png"),
    /** Small arrow down dark **/
    SMALL_ARROW_DOWN_DARK("icons/small_arrow_down_dark.png"),
    /** Small arrow down small 8px **/
    SMALL_ARROW_UP_DARK("icons/small_arrow_up_dark.png"),
    /** Small arrow right small 8px**/
    SMALL_ARROW_DOWN_DARK_8("icons/small_arrow_down_dark@0.5x.png"),
    /** Small arrow right small 8 px**/
    SMALL_ARROW_UP_DARK_8("icons/small_arrow_up_dark@0.5x.png"),
    /** Move down arrow **/
    MOVE_DOWN("icons/move_down.png"),
    /** Move to bottom arrow with bar **/
    MOVE_TO_BOTTOM("icons/move_to_bottom.png"),
    /** Move to top arrow with bar **/
    MOVE_TO_TOP("icons/move_to_top.png"),
    /** Move top arrow **/
    MOVE_UP("icons/move_up.png"),
    /**
     * Add, edit, remove, sort, reset
     */
    /** Plus **/
    ADD_PLUS("icons/add_plus.png"),
    /** Add plus filled **/
    ADD_PLUS_FILLED("icons/add_plus_filled.png"),
    /** Add plus filled large 48px**/
    ADD_PLUS_FILLED_LARGE("icons/add_plus_filled@3x.png"),
    /** Edit pencil **/
    EDIT("icons/edit.png"),
    /** Delete cross **/
    DELETE_CROSS("icons/delete_cross.png"),
    /** Delete dash **/
    DELETE_DASH("icons/delete_dash.png"),
    /** Delete trash can **/
    DELETE_TRASH("icons/delete_trash.png"),
    /** Reset round arrow counterclockwise **/
    RESET("icons/reset.png"),
    /** Refresh round arrow clockwise
     * @since 3.7 **/
    REFRESH("icons/refresh.png"),
    /** Sync dual round arrows clockwise
     * @since 3.7 **/
    SYNC("icons/sync.png"),
    /** Search magnifier **/
    SEARCH("icons/search.png"),
    /** Sort a z with arrow **/
    SORT_A_Z("icons/sort_a_z.png"),
    /** Sort z a with arrow **/
    SORT_Z_A("icons/sort_z_a.png"),
    /**
     * Types
     */
    /** Binary large object type icon **/
    TYPE_BLOB("icons/binary_object.png"),
    /** Boolean type icon **/
    TYPE_BOOLEAN("icons/boolean.png"),
    /** Bitvector type icon FIXME currently same icon as boolean **/
    TYPE_BITVECTOR("icons/boolean.png"),
    /** Bytevector type icon **/
    TYPE_BYTEVECTOR("icons/bytevectoricon.png"),
    /** Collection type icon **/
    TYPE_COLLECTION("icons/collection.png"),
    /** Complex number type icon **/
    TYPE_COMPLEX("icons/complex_number.png"),
    /** Default type icon **/
    TYPE_DEFAULT("icons/default_type.png"),
    /** Double type icon **/
    TYPE_DOUBLE("icons/double.png"),
    /** Double vector type icon **/
    TYPE_DOUBLEVECTOR("icons/double_vector.png"),
    /** Fuzzy type icon **/
    TYPE_FUZZY("icons/fuzzy.png"),
    /** Fuzzy interval type icon **/
    TYPE_FUZZY_INTERVAL("icons/fuzzy_interval.png"),
    /** Integer type icon **/
    TYPE_INTEGER("icons/integer.png"),
    /** Interval type icon **/
    TYPE_INTERVAL("icons/interval.png"),
    /** Image type icon **/
    TYPE_IMAGE("icons/image.png"),
    /** Long type icon **/
    TYPE_LONG("icons/long.png"),
    /** Port type icon **/
    TYPE_PORT("icons/port.png"),
    /** String type icon **/
    TYPE_STRING("icons/string.png"),
    /** String vector type icon **/
    TYPE_STRINGVECTOR("icons/string_vector.png"),
    /** Time type icon **/
    TYPE_TIME("icons/time.png"),
    /** Date&Time type icon **/
    TYPE_DATE_TIME("icons/date_time.png"),
    /** PMML type icon **/
    TYPE_PMML("icons/pmml.png"),
    /** XML type icon **/
    TYPE_XML("icons/xml.png"),
    /** PNG image type icon **/
    TYPE_PNG("icons/png.png"),
    /** URI input type icon
     * @since 3.7 **/
    TYPE_URI_INPUT("icons/uri_input.png"),
    /** literal input type icon
     * @since 3.7 **/
    TYPE_LITERAL("icons/literal.png"),
    /** credentials username input type icon
     * @since 3.7 **/
    TYPE_USERNAME("icons/credentials_username.png"),
    /** credentials password input type icon
     * @since 3.7 **/
    TYPE_PASSWORD("icons/credentials_password.png"),
    /**
     * Flow variables
     */
    /** Flow variable boolean
     * @since 3.7 **/
    FLOWVAR_GENERAL("icons/flowvar_general.png"),
    /** Flow variable boolean **/
    FLOWVAR_BOOLEAN("icons/flowvar_boolean.png"),
    /** Flow variable default **/
    FLOWVAR_DEFAULT("icons/flowvar_default.png"),
    /** Flow variable double **/
    FLOWVAR_DOUBLE("icons/flowvar_double.png"),
    /** Flow variable integer **/
    FLOWVAR_INTEGER("icons/flowvar_integer.png"),
    /** Flow variable string **/
    FLOWVAR_STRING("icons/flowvar_string.png"),
    /** Flow variable active **/
    FLOWVAR_ACTIVE("icons/flowvar_active.png"),
    /** Flow variable inactive **/
    FLOWVAR_INACTIVE("icons/flowvar_inactive.png"),
    /** Variable dialog active **/
    FLOWVAR_DIALOG_ACTIVE("icons/flowvar_dialog_active.png"),
    /** Variable dialog inactive **/
    FLOWVAR_DIALOG_INACTIVE("icons/flowvar_dialog_inactive.png");

    private final Icon m_icon;

    private SharedIcons(final String path) {
        URL url = SharedIcons.class.getResource(path);
        // if file does not exist or path is wrong
        if (url == null) {
            System.out.println("Icon at path " + path + " could not be found.");
            m_icon = new ImageIcon();
        } else {
            m_icon = new ImageIcon(url);
        }
    }

    /**
     * @return the actual Icon, not null.
     */
    public Icon get() {
        return m_icon;
    }
}