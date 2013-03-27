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
package org.knime.workbench.ui.preferences;

/**
 * Constant definitions for plug-in preferences. Values are stored under these
 * keys in the <code>PreferenceStore</code> of the UI plugin.
 *
 * @author Florian Georg, University of Konstanz
 */
public interface PreferenceConstants {
    /** Preference constant: whether the tips&amp;tricks window should be hidden.
     */
    public static final String P_HIDE_TIPS_AND_TRICKS = "knime.hidetipsandtricks";

    /** Preference constant: whether user needs to confirm reset actions. */
    public static final String P_CONFIRM_RESET = "knime.confirm.reset";

    /** Preference constant: whether user needs to confirm delete actions. */
    public static final String P_CONFIRM_DELETE = "knime.confirm.delete";

    /** Preference constant to confirm reconnecting a node. */
    public static final String P_CONFIRM_RECONNECT = "knime.confirm.reconnect";

    /** Preference constant to confirm executing nodes not saved on close. */
    public static final String P_CONFIRM_EXEC_NODES_NOT_SAVED =
        "knime.confirm.exec_nodes_not_saved";

    /** Preference constant to confirm executing nodes not saved on close. */
    public static final String P_CONFIRM_EXEC_NODES_DATA_AWARE_DIALOGS =
        "knime.confirm.exec_nodes_for_data_aware_dialogs";

    /** Preference constant for the size of the favorite nodes frequency
     * history size.
     */
    public static final String P_FAV_FREQUENCY_HISTORY_SIZE =
        "knime.favorites.frequency";

    /** Preference constant for the size of the favorite nodes last used
     * history size.
     */
    public static final String P_FAV_LAST_USED_SIZE =
        "knime.favorites.lastused";

    /** Preference constant to allow setting empty node label. */
    public static final String P_SET_NODE_LABEL = "knime.set.node_label";

    /** Preference constant for default node label prefix. */
    public static final String P_DEFAULT_NODE_LABEL =
        "knime.default.node_label";

    /** Preference constant for node name and node label to change font size. */
    public static final String P_NODE_LABEL_FONT_SIZE =
        "knime.node.font_size";

    /** Preference constant for whether meta node links should be updated on
     * workflow load. */
    public static final String P_META_NODE_LINK_UPDATE_ON_LOAD =
        "knime.metanode.updateOnLoad";

    /** Preference constant for mount points for the Explorer. */
    public static final String P_EXPLORER_MOUNT_POINT =
        "knime.explorer.mountpoint";

    /** Pref constant to link the original meta node to a newly
     * defined template. */
    public static final String P_EXPLORER_LINK_ON_NEW_TEMPLATE =
        "knime.explorer.link_on_new_template";

    /** Pref constant whether to show grid in workflow editor (boolean). */
    public static final String P_GRID_SHOW = "knime.showgrid";

    /** Pref constant whether to snap to grid. */
    public static final String P_GRID_SNAP_TO = "knime.snaptogrid";

    /** Pref constant for grid size (number of pixels). */
    public static final String P_GRID_SIZE_X = "knime.gridsize.x";
    /** Pref constant for grid size (number of pixels). */
    public static final String P_GRID_SIZE_Y = "knime.gridsize.y";
    /** default grid distance */
    public static final int P_GRID_DEFAULT_SIZE_X = 90;
    /** default grid distance */
    public static final int P_GRID_DEFAULT_SIZE_Y = 120;

}
