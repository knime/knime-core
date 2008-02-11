/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   Jan 25, 2008 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodePersistor;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class SingleNodeContainerPersistorVersion200 extends
        SingleNodeContainerPersistorVersion1xx {
    
    private static final String NODE_FILE = "node.xml";

    public SingleNodeContainerPersistorVersion200(
            final HashMap<Integer, ContainerTable> tableRep) {
        super(tableRep);
    }
    
    @Override
    protected String loadNodeFile(NodeSettingsRO settings) 
        throws InvalidSettingsException {
        return settings.getString("node_file");
    }

    protected String save(final SingleNodeContainer snc, final File nodeDir, 
            final ExecutionMonitor exec, final boolean isSaveData) 
                throws CanceledExecutionException, IOException {
        nodeDir.mkdirs();
        if (!nodeDir.isDirectory()) {
                throw new IOException("Unable to read or create directory \""
                        + nodeDir.getAbsolutePath() + "\"");
        }
        NodeSettings settings = new NodeSettings(SETTINGS_FILE_NAME);
        saveNodeFactoryClassName(settings, snc);
        NodeContainerMetaPersistorVersion200 metaPersistor =
            createNodeContainerMetaPersistor();
        metaPersistor.save(snc, settings, exec, isSaveData);
        File nodeXMLFile = saveNodeFileName(settings, nodeDir);
        NodePersistor persistor = createNodePersistor();
        persistor.save(snc.getNode(), nodeXMLFile, exec, isSaveData);
        String fileName = SETTINGS_FILE_NAME;
        File nodeSettingsXMLFile = new File(nodeDir, fileName);
        settings.saveToXML(new FileOutputStream(nodeSettingsXMLFile));
        return fileName;
    }
    
    protected void saveNodeFactoryClassName(final NodeSettingsWO settings,
            final SingleNodeContainer nc) {
        String cl = nc.getNode().getFactory().getClass().getName();
        settings.addString(KEY_FACTORY_NAME, cl);
    }
    
    protected File saveNodeFileName(
            final NodeSettingsWO settings, final File nodeDirectory) {
        String fileName = NODE_FILE;
        settings.addString("node_file", fileName);
        return new File(nodeDirectory, fileName);
    }
    
    @Override
    protected NodeContainerMetaPersistorVersion200 
            createNodeContainerMetaPersistor() {
        return new NodeContainerMetaPersistorVersion200();
    }

}
