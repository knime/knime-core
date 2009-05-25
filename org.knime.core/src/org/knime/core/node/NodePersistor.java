/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 * 
 * History
 *   Sep 11, 2007 (wiswedel): created
 */
package org.knime.core.node;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;


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
    
    /** Loads content into node instance. 
     * @param node The target node, used for meta info (#ports, e.g) and to
     *  invoke the 
     *  {@link Node#load(NodePersistor, ExecutionMonitor, LoadResult)} on
     * @param nodeFile The configuration file for the node.
     * @param execMon For progress/cancelation
     * @param loadTblRep The table repository used during load
     * @param tblRep The table repository for blob handling
     * @param loadResult where to add errors to
     * @throws IOException If files can't be read 
     * @throws CanceledExecutionException If canceled
     */
    void load(final Node node, final ReferencedFile nodeFile, 
            final ExecutionMonitor execMon, 
            final Map<Integer, BufferedDataTable> loadTblRep, 
            final HashMap<Integer, ContainerTable> tblRep,
            final LoadResult loadResult)
            throws IOException, CanceledExecutionException;
    
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
