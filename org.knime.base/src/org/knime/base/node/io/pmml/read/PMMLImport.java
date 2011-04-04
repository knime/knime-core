/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.io.pmml.read;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.ExtractModelTypeHandler;
import org.knime.core.node.port.pmml.PMMLMasterContentHandler;
import org.knime.core.node.port.pmml.PMMLModelType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 *
 * @author Fabian Dill, University of Konstanz
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public class PMMLImport {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(PMMLImport.class);

    private static final Map<String, String> NS_MAP =
            new HashMap<String, String>();

    private static final Map<String, Class<? extends PMMLPortObject>> REGISTRY =
            new HashMap<String, Class<? extends PMMLPortObject>>();

    static {
        NS_MAP.put("3.0", "http://www.dmg.org/PMML-3_0");
        NS_MAP.put("3.1", "http://www.dmg.org/PMML-3_1");
        NS_MAP.put("3.2", "http://www.dmg.org/PMML-3_2");

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        for (IConfigurationElement element : registry
                .getConfigurationElementsFor("org.knime.base.pmmlports")) {
            String modelType = element.getAttribute("modeltype");
            try {
                /*
                 * Use the configuration element method to load an object of the
                 * given class name. This method ensures the correct classpath
                 * is used providing access to all extension points.
                 */
                final Object o =
                        element.createExecutableExtension("PMMLPortObject");
                @SuppressWarnings("unchecked")
                Class<? extends PMMLPortObject> clazz =
                        (Class<? extends PMMLPortObject>)o.getClass();
                if (REGISTRY.get(modelType) == null) {
                    // add class
                    REGISTRY.put(modelType, clazz);
                } // else already registered -> first come first serve
            } catch (InvalidRegistryObjectException e) {
                throw new IllegalArgumentException(e);
            } catch (CoreException e) {
                throw new IllegalArgumentException(e);
            }
            for (String key : REGISTRY.keySet()) {
                LOGGER.debug("model type: " + key);
                LOGGER.debug("port object: "
                        + REGISTRY.get(key).getSimpleName());
            }
        }
    }

    private File m_file;

    private ErrorHandler m_errorHandler = new LoggingErrorHandler();

    private PMMLModelType m_modelType = PMMLModelType.None;

    private String m_version;

    private boolean m_hasNamespace;

    private PMMLPortObjectSpec m_portObjectSpec;

    private PMMLPortObject m_portObject;

    private PMMLImport() {
        //
    }

    /**
     * Reads the passed file, checks the PMML version, the PMML model type, and
     * namespace and also validates the file against the schema, then creates
     * the {@link PMMLPortObjectSpec} and {@link PMMLPortObject} from the
     * content of the file.
     *
     * @param file containing the PMML model
     * @param errorHandler error handler used during parsing
     * @throws SAXException if something goes wrong (wrong version, unsupported
     *             model type or invalid file)
     */
    public PMMLImport(final File file, final ErrorHandler errorHandler)
            throws SAXException {
        if (file == null) {
            throw new IllegalArgumentException("File must not be null!");
        }
        if (!file.exists()) {
            throw new IllegalArgumentException("File " + file.getName()
                    + " does not exists.");
        }
        m_file = file;
        m_errorHandler = errorHandler;
        // initial file parsing:
        // extract model type, version, and if namespace is available
        preCollectInformation();
        initializePortObjectClass();
        if (!m_hasNamespace) {
            // add namespace in order to have the parser set the default values
            try {
                m_file = addNamespace(m_file);
            } catch (IOException e) {
                throw new SAXException(e);
            }
        }
        m_portObjectSpec = PMMLPortObject.parseSpec(m_file);
        m_portObject = parseModel(m_portObjectSpec);
        if (!m_hasNamespace) {
            // if the file had no namespace a new file with namespace added was
            // created in temp dir -> now we can delete it;
            m_file.delete();
        }

    }

    /**
     * @see PMMLImport#PMMLImport(File, ErrorHandler) with default error
     *      handler: {@link LoggingErrorHandler}.
     *
     * @param file containing the PMML model
     * @throws SAXException if something goes wrong
     */
    public PMMLImport(final File file) throws SAXException {
        this(file, new LoggingErrorHandler());
    }

    /**
     *
     * @param file the file containing the PMML model
     * @return true if the type of model is supported by the current platform
     *         configuration
     * @throws SAXException if something goes wrong
     */
    public static boolean isModelSupported(final File file)
            throws SAXException {
        PMMLImport importer = new PMMLImport();
        importer.m_file = file;
        importer.preCollectInformation();
        return REGISTRY.get(importer.m_modelType.name()) != null;
    }

    /**
     *
     * @return the parsed port object spec (data dictionary and mining schema)
     */
    public PMMLPortObjectSpec getPortObjectSpec() {
        return m_portObjectSpec;
    }

    /**
     *
     * @return the parsed PMML model
     */
    public PMMLPortObject getPortObject() {
        return m_portObject;
    }

    private void preCollectInformation() throws SAXException {
        try {
            SAXParserFactory fac = SAXParserFactory.newInstance();
            SAXParser parser = fac.newSAXParser();
            PMMLMasterContentHandler masterHandler =
                    new PMMLMasterContentHandler();
            ExtractModelTypeHandler modelTypeHdl =
                    new ExtractModelTypeHandler();
            masterHandler.addContentHandler(ExtractModelTypeHandler.ID,
                    modelTypeHdl);
            parser.parse(m_file, masterHandler);
            m_version = masterHandler.getVersion();
            m_modelType = modelTypeHdl.getModelType();
            m_hasNamespace = modelTypeHdl.hasNamespace();
        } catch (IOException io) {
            throw new SAXException(io);
        } catch (ParserConfigurationException pce) {
            throw new SAXException(pce);
        }
    }

    private void initializePortObjectClass() {
        Class<? extends PMMLPortObject> portClass =
                REGISTRY.get(m_modelType.name());
        if (portClass == null) {
            m_portObject = new PMMLPortObject();
        } else {
            try {
                m_portObject = portClass.newInstance();
            } catch (IllegalAccessException iae) {
                throw new IllegalArgumentException("PortObject for model "
                        + m_modelType + " could not be initialized!", iae);
            } catch (InstantiationException e) {
                throw new IllegalArgumentException("PortObject for model "
                        + m_modelType + " could not be initialized!", e);
            }
        }
    }

    private File addNamespace(final File file) throws IOException {
        LOGGER.debug("adding namespace");
        File f = File.createTempFile("ns_added", ".xml");
        f.deleteOnExit();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        BufferedWriter writer = new BufferedWriter(new FileWriter(f));
        String line = reader.readLine();
        boolean tagOpen = false;
        while (line != null) {
            if (line.startsWith("<PMML")) {
                tagOpen = true;
            }
            if (tagOpen && line.endsWith(">")) {
                tagOpen = false;
                // add namespace declaration
                line =
                       "<PMML version=\"" + m_version + "\" xmlns=\""
                           + NS_MAP.get(m_version) + "\" xmlns:xsi="
                           + "\"http://www.w3.org/2001/XMLSchema-instance\">";
                LOGGER.debug(line);
            }
            if (!tagOpen) {
                writer.write(line + "\n");
            }
            line = reader.readLine();
        }
        reader.close();
        writer.close();
        return f;
    }

    private PMMLPortObject parseModel(final PMMLPortObjectSpec portObjectSpec)
            throws SAXException {
        try {
            m_portObject.loadFrom(portObjectSpec, new FileInputStream(m_file));
        } catch (FileNotFoundException e) {
            throw new SAXException(e);
        } catch (ParserConfigurationException e) {
            throw new SAXException(e);
        } catch (IOException e) {
            throw new SAXException(e);
        }
        return m_portObject;
    }

    private InputStream getSchemaInputStream(final String path) {
        ClassLoader loader = PMMLPortObject.class.getClassLoader();
        String packagePath =
                PMMLPortObject.class.getPackage().getName().replace('.', '/');
        return loader.getResourceAsStream(packagePath + path);
    }

}
