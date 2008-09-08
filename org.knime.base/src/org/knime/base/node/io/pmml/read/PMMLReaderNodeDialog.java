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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.pmml.ExtractModelTypeHandler;
import org.knime.core.node.port.pmml.PMMLMasterContentHandler;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.xml.sax.SAXException;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLReaderNodeDialog extends DefaultNodeSettingsPane {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            PMMLReaderNodeDialog.class);
    
    private static final 
        Map<String, Set<Class<? extends PMMLPortObject>>>REGISTRY 
            = new HashMap<String, Set<Class<? extends PMMLPortObject>>>();
    
    private static List<String>EMPTY_LIST = new ArrayList<String>();
    
    
    // TODO: only one PortObject per ModelType!!!
    
    static {
        EMPTY_LIST.add("No valid PortObject found for this selected model!");
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        for (IConfigurationElement element : registry
                    .getConfigurationElementsFor("org.knime.base.pmmlports")) {
            String modelType = element.getAttribute("modeltype");
            try {
                Class<? extends PMMLPortObject> clazz 
                    = (Class<?extends PMMLPortObject>)Class.forName(element
                            .getAttribute("PMMLPortObject"));
                if (REGISTRY.get(modelType) == null) {
                    // add set
                    Set<Class<? extends PMMLPortObject>>ports 
                    = new HashSet<Class<? extends PMMLPortObject>>();
                    ports.add(clazz);
                    REGISTRY.put(modelType, ports);
                } else {
                    // add to set
                    REGISTRY.get(modelType).add(clazz);
                }
            } catch (InvalidRegistryObjectException e) {
                throw new IllegalArgumentException(e);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
            for (String key : REGISTRY.keySet()) {
                LOGGER.debug("model type: " + key);
                for (Class<? extends PMMLPortObject>clazz : REGISTRY.get(key)) {
                    LOGGER.debug("port object: " + clazz.getSimpleName());
                }
            }
        }
    }
    
    
    
    /**
     * 
     */
    public PMMLReaderNodeDialog() {
        final SettingsModelString fileNameModel = createFileChooserModel();
        final SettingsModelString portObjectSelectionModel 
            = createPortObjectSelectionModel();
        final DialogComponentStringSelection portObjSelection = 
            new DialogComponentStringSelection(
                    portObjectSelectionModel, "Select desired target port", 
                    EMPTY_LIST);
        
        fileNameModel.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent arg0) {
                if (fileNameModel.getStringValue() != "") {
                    try {
                        String modelType = extractType(
                                fileNameModel.getStringValue());
                        // go into registry
                        Set<Class<? extends PMMLPortObject>>portObjects 
                            = REGISTRY.get(modelType); 
                        if (portObjects == null) {
                            portObjSelection.replaceListItems(
                                    EMPTY_LIST, null);
                        } else {
                            // set values to port tpye select box model
                            List<String>classNames = new ArrayList<String>();
                            for (Class<? extends PMMLPortObject>clazz 
                                        : portObjects) {
                                classNames.add(clazz.getName());
                            }
                            portObjSelection.replaceListItems(classNames, null);
                        }
                    } catch (ParserConfigurationException e) {
                        LOGGER.error("Error parsing file " 
                                + fileNameModel.getStringValue(), e);
                    } catch (SAXException e) {
                        LOGGER.error("Error parsing file " 
                                + fileNameModel.getStringValue(), e);
                    }
                }
            }

            
        });
        
        addDialogComponent(new DialogComponentFileChooser(
                fileNameModel, "pmml.reader", ".xml", ".pmml"));
        addDialogComponent(portObjSelection);
    }
    
    private String extractType(final String fileName) 
        throws ParserConfigurationException, SAXException {
        File f = new File(fileName);
        SAXParserFactory fac = SAXParserFactory.newInstance();
        SAXParser parser = fac.newSAXParser();
        PMMLMasterContentHandler masterHandler = new PMMLMasterContentHandler();
        ExtractModelTypeHandler modelTypeHdl = new ExtractModelTypeHandler();
        masterHandler.addContentHandler(ExtractModelTypeHandler.ID, 
                modelTypeHdl);
        try {
            parser.parse(f, masterHandler);
        } catch (Exception e) {
            LOGGER.error("Error parsing file" + fileName, e);
        } 
        return modelTypeHdl.getModelType().name();
    }
    
    
    /**
     * 
     * @return model for the port type selection
     */
    static SettingsModelString createPortObjectSelectionModel() {
        return new SettingsModelString("pmml.reader.porttype_selection", "");
    }

    /**
     * 
     * @return model for PMML file
     */
    static SettingsModelString createFileChooserModel() {
        return new SettingsModelString("pmml.reader.file", "");

    }
    
    
    // TODO:
    /*
     * if file selected 
     * - parse it
     * - detect model type
     * - scan extension points whether one or more PMMLPortObject are registered
     * if one is registered -> select it
     * if several are selected 
     *  -> set WarningMessage and provide them to be chosen 
     */

}
