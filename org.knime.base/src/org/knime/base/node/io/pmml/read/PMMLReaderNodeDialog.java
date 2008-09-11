/* This source code, its documentation and all appendant files
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
 */
package org.knime.base.node.io.pmml.read;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.pmml.ExtractModelTypeHandler;
import org.knime.core.node.port.pmml.PMMLMasterContentHandler;
import org.knime.core.node.port.pmml.PMMLPortObject;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLReaderNodeDialog extends DefaultNodeSettingsPane {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            PMMLReaderNodeDialog.class);
    
    /** Config key for the port object implementation class name. */
    static final String PORT_OBJECT_KEY = "pmml.reader.porttype_selection";
    
    private static final 
        Map<String, Class<? extends PMMLPortObject>>REGISTRY 
            = new HashMap<String, Class<? extends PMMLPortObject>>();
    
    private String m_selectedPortType;
    
    private final SettingsModelString m_fileNameModel;
    
    // TODO: only one PortObject per ModelType!!!
    
    static {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        for (IConfigurationElement element : registry
                    .getConfigurationElementsFor("org.knime.base.pmmlports")) {
            String modelType = element.getAttribute("modeltype");
            try {
                Class<? extends PMMLPortObject> clazz 
                    = (Class<?extends PMMLPortObject>)Class.forName(element
                            .getAttribute("PMMLPortObject"));
                if (REGISTRY.get(modelType) == null) {
                    // add class
                    REGISTRY.put(modelType, clazz);
                } // else already registered -> first come first serve
            } catch (InvalidRegistryObjectException e) {
                throw new IllegalArgumentException(e);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
            for (String key : REGISTRY.keySet()) {
                LOGGER.debug("model type: " + key);
                    LOGGER.debug("port object: " 
                            + REGISTRY.get(key).getSimpleName());
                
            }
        }
    }
    
    
    
    /**
     * 
     */
    public PMMLReaderNodeDialog() {
        m_fileNameModel = createFileChooserModel();
        addDialogComponent(new DialogComponentFileChooser(
                m_fileNameModel, "pmml.reader", ".xml", ".pmml"));
    }
    
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        // adding the specific port object class name. 
        preParseFile(m_fileNameModel);
        settings.addString(PORT_OBJECT_KEY, m_selectedPortType);
        super.saveAdditionalSettingsTo(settings);
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        // loading the specific port obejct class name
        m_selectedPortType = settings.getString(PORT_OBJECT_KEY, "");
        super.loadAdditionalSettingsFrom(settings, specs);
    }
    
    

    /**
     * 
     * @return model for PMML file
     */
    static SettingsModelString createFileChooserModel() {
        return new SettingsModelString("pmml.reader.file", "");

    }
    
    private void preParseFile(final SettingsModelString fileNameModel)
            throws InvalidSettingsException {
        if (fileNameModel.getStringValue() != "") {
            String modelType = extractType(fileNameModel.getStringValue());
            // go into registry
            Class<? extends PMMLPortObject> portObject =
                    REGISTRY.get(modelType);
            if (portObject == null) {
                m_selectedPortType = null;
                throw new InvalidSettingsException(
                        "Model type: " + modelType + " not supported yet.");
            } else {
                m_selectedPortType = portObject.getName();
            }
        }
    }

    private String extractType(final String fileName)
            throws InvalidSettingsException {
        try {
            File f = new File(fileName);
            SAXParserFactory fac = SAXParserFactory.newInstance();
            SAXParser parser = fac.newSAXParser();
            PMMLMasterContentHandler masterHandler =
                    new PMMLMasterContentHandler();
            ExtractModelTypeHandler modelTypeHdl =
                    new ExtractModelTypeHandler();
            masterHandler.addContentHandler(ExtractModelTypeHandler.ID,
                    modelTypeHdl);
            parser.parse(f, masterHandler);
            return modelTypeHdl.getModelType().name();
        } catch (IOException io) {
            throw new InvalidSettingsException("File name " + fileName
                    + " is not valid. " + "Please enter a valid file name");
        } catch (Exception e) {
            throw new InvalidSettingsException(e);
        }
    }

}
