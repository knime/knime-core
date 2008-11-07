/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;


public interface NodePersistor {
    
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
    
    LoadResult load(Node node, final ReferencedFile nodeFile, ExecutionMonitor execMon,
            Map<Integer, BufferedDataTable> loadTblRep, HashMap<Integer, ContainerTable> tblRep)
            throws IOException, CanceledExecutionException;
    
    boolean needsResetAfterLoad();
    /** Indicate an error and that this node should better be reset after load.
     */
    public void setNeedsResetAfterLoad();
    
    boolean isConfigured();
    boolean isExecuted();
    boolean hasContent();
    boolean mustWarnOnDataLoadError();
    ReferencedFile getNodeInternDirectory();
    // may return null in which case the node decides what to do.
    LoadNodeModelSettingsFailPolicy getModelSettingsFailPolicy();
    NodeSettingsRO getSettings();
    PortObjectSpec getPortObjectSpec(final int outportIndex);
    PortObject getPortObject(final int outportIndex);
    String getPortObjectSummary(final int outportIndex);
    BufferedDataTable[] getInternalHeldTables();
}
