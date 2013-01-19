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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   Sep 11, 2007 (wiswedel): created
 */
package org.knime.core.node;



public interface NodePersistor extends NodeContentPersistor {
    
    /** Node settings XML file name. */
    static final String SETTINGS_FILE_NAME = "settings.xml";

    /** Directory name to save and load node internals. */
    public static final String INTERN_FILE_DIR = "internal";

    /** Directory name to save and load the data. */
    static final String DATA_FILE_DIR = "data";

    static final String CFG_NAME = "name";

    static final String CFG_ISCONFIGURED = "isConfigured";

    static final String CFG_ISEXECUTED = "isExecuted";
    
    static final String CFG_NODE_MESSAGE = "node_message";
    
    static final String CFG_SPEC_FILES = "spec_files";

    static final String CFG_HAS_SPEC_FILE = "has_output_spec";

    static final String CFG_DATA_FILE = "data_meta_file";

    static final String CFG_DATA_FILE_DIR = "data_files_directory";

    static final String DATA_FILE_PREFIX = "data_";

    static final String CFG_MODEL_FILES = "model_files";

    static final String MODEL_FILE_PREFIX = "model_";

    static final String CFG_OUTPUT_PREFIX = "output_";

    /** Config key: What memory policy to use for a node outport. */
    static final String CFG_MEMORY_POLICY = "memory_policy";

    /** Policy on how to behave if the node model settings fails. */
    public enum LoadNodeModelSettingsFailPolicy {
        /** reset node, force configure (used when node state is yellow). */
        FAIL,
        /** warn using message (used when node state is green). */
        WARN,
        /** ignore (used when node state is idle, e.g. node not connected). */
        IGNORE
    }
    
    boolean isConfigured();
    boolean isExecuted();
    
    /** Whether this node should be marked as dirty after load. This is true
     * if either the {@link #setDirtyAfterLoad()} has been set to true or
     * {@link NodeContentPersistor#needsResetAfterLoad()} returns true. 
     * @return This property.
     */
    boolean isDirtyAfterLoad();
    
    /** Sets the dirty flag on this node. The node will also be dirty if
     * the {@link NodeContentPersistor#setNeedsResetAfterLoad()} is called. */
    void setDirtyAfterLoad();
    
    // may return null in which case the node decides what to do.
    LoadNodeModelSettingsFailPolicy getModelSettingsFailPolicy();
    NodeSettingsRO getSettings();
}
