/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
package org.knime.core.node.port.pmml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.preproc.PMMLPreprocOperation;
import org.knime.core.node.port.pmml.preproc.PMMLPreprocOperation.PMMLWriteElement;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public abstract class PMMLPortObject implements PortObject {
    /** Convenience accessor for the port type. */
    public static final PortType TYPE = new PortType(PMMLPortObject.class);

    /** Constant for CDATA. */
    protected static final String CDATA = "CDATA";
    /** Constant for DataDictionary. */
    protected static final String DATA_DICT = "DataDictionary";
    /** Constant for DataField. */
    protected static final String DATA_FIELD = "DataField";
    /** Constant for Value. */
    protected static final String VALUE = "Value";
    /** Constant for the LocalTransformations tag. */
    protected static final String LOCAL_TRANS = "LocalTransformations";

    private static final String PMML_3_0 = "/schemata/pmml-3-0.xsd";

    private static final String PMML_3_1 = "/schemata/pmml-3-1.xsd";

    private static final String PMML_3_2 = "/schemata/pmml-3-2.xsd";

    /** Constant for version 3.0. Can be used as argument for the load method.*/
    public static final String PMML_V3_0 = "3.0";
    /** Constant for version 3.1. Can be used as argument for the load method.*/
    public static final String PMML_V3_1 = "3.1";
    /** Constant for version 3.2. Can be used as argument for the load method.*/
    public static final String PMML_V3_2 = "3.2";

    private static final Map<String, String> VERSION_SCHEMA_MAP
        = new HashMap<String, String>();

    private static final Map<String, String> VERSION_NAMESPACE_MAP
            = new HashMap<String, String>();

    private List<PMMLPreprocOperation> m_operations;


    /**
     * Based on the version number the local schema location is returned.
     * @param version version 3.0 - 3.2
     * @return the location of the local schema
     */
    public static String getLocalSchemaLocation(final String version) {
        return VERSION_SCHEMA_MAP.get(version);
    }

    static {
        VERSION_SCHEMA_MAP.put(PMML_V3_0, PMML_3_0);
        VERSION_SCHEMA_MAP.put(PMML_V3_1, PMML_3_1);
        VERSION_SCHEMA_MAP.put(PMML_V3_2, PMML_3_2);
        VERSION_NAMESPACE_MAP.put(PMML_V3_0, "http://www.dmg.org/PMML-3_0");
        VERSION_NAMESPACE_MAP.put(PMML_V3_1, "http://www.dmg.org/PMML-3_1");
        VERSION_NAMESPACE_MAP.put(PMML_V3_2, "http://www.dmg.org/PMML-3_2");
    }

    /** Ensures that the load method is called once at most. */
    private boolean m_isLoaded;

    private PMMLPortObjectSpec m_spec;
    private PMMLModelType m_modelType;

    private PMMLMasterContentHandler m_masterHandler;

    private static PMMLPortObjectSerializer serializer;

    /**
     * Static serializer as demanded from {@link PortObject} framework.
     * @return serializer for PMML (reads and writes PMML files)
     */
    public static final PortObjectSerializer<PMMLPortObject>
            getPortObjectSerializer() {
        if (serializer == null) {
            serializer = new PMMLPortObjectSerializer();
        }
        return serializer;
    }


    /**
     * Default constructor necessary for loading. Derived classes also
     * <em>must</em> provide a default constructor, otherwise loading will fail.
     */
    public PMMLPortObject() {
        m_masterHandler = new PMMLMasterContentHandler();
        m_isLoaded = false;
    }

    /**
     * @param spec the referring {@link PMMLPortObjectSpec}
     * @param type the type of the PMML model
     */
    public PMMLPortObject(final PMMLPortObjectSpec spec,
            final PMMLModelType type) {
        m_spec = spec;
        m_masterHandler = new PMMLMasterContentHandler();
        m_modelType = type;
        m_isLoaded = true;
    }


    /**
     * Adds a content handler to the master content handler. The master content
     * handler forwards all relevant events from PMML file parsing to all
     * registered content handlers.
     *
     * @param id to later on retrieve the registered content handler
     * @param defaultHandler specialized content handler interested in certain
     * parts of the PMML file (ClusteringModel, TreeModel, etc.)
     * @return true if the handler was added, false if it is already registered
     */
    public boolean addPMMLContentHandler(final String id,
            final PMMLContentHandler defaultHandler) {
        return m_masterHandler.addContentHandler(id, defaultHandler);
    }

    /**
     *
     * @param id the id which was used for registration of the handler
     * @return the handler registered with this id or null if no handler with
     *  this id can be found
     */
    public PMMLContentHandler getPMMLContentHandler(final String id) {
        return m_masterHandler.getDefaultHandler(id);
    }

    /**
     *
     * @return the type of model
     * @see PMMLModelType
     */
    public PMMLModelType getModelType() {
        return m_modelType;
    }

    /**
     * The PMML version this port object will write. Sub-classes will overwrite
     * this method and return their desired version. The default implementation
     * returns {@link #PMML_V3_1}.
     * @return the version string written by the save method.
     */
    protected String getWriteVersion() {
        return PMML_V3_1;
    }


    private TransformerHandler createTransformerHandlerForSave(
            final OutputStream out)
            throws TransformerConfigurationException, SAXException {
        String version = getWriteVersion();
        SAXTransformerFactory fac =
            (SAXTransformerFactory)TransformerFactory.newInstance();
        TransformerHandler handler = fac.newTransformerHandler();

        Transformer t = handler.getTransformer();
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.setOutputProperty(OutputKeys.INDENT, "yes");

        handler.setResult(new StreamResult(out));

        // PMML root element, namespace declaration, etc.
        handler.startDocument();
        AttributesImpl attr = new AttributesImpl();
        if (!VERSION_NAMESPACE_MAP.containsKey(version)) {
            throw new SAXException("PMML model seems to be of an "
                    + "unsupported version. Only PMML versions "
                    + VERSION_NAMESPACE_MAP.keySet()
                    + " are supported. Found " + version);
        }
        attr.addAttribute(null, null, "version", CDATA, version);
        attr.addAttribute(null, null, "xmlns", CDATA,
                VERSION_NAMESPACE_MAP.get(version));
        attr.addAttribute(null, null, "xmlns:xsi", CDATA,
            "http://www.w3.org/2001/XMLSchema-instance");
        handler.startElement(null, null, "PMML", attr);
        return handler;
    }

    /**
     * Writes the port object to valid PMML. Subclasses should not override this
     * method but the {@link #writePMMLModel(TransformerHandler)} instead.
     *
     *
     * @param out zipped stream which reads the PMML file
     * @throws SAXException if something goes wrong during writing of PMML
     * @throws IOException if the file cannot be written to the directory
     * @throws TransformerConfigurationException if something goes wrong with
     *  the transformation handler
     */
    public void save(final OutputStream out)
        throws SAXException, IOException, TransformerConfigurationException {
        String version = getWriteVersion();
        TransformerHandler handler = createTransformerHandlerForSave(out);
        PMMLPortObjectSpec.writeHeader(handler, version);
        PMMLPortObjectSpec.writeDataDictionary(getSpec().getDataTableSpec(),
                handler, version);
        writePMMLModel(handler);
        handler.endElement(null, null, "PMML");
        handler.endDocument();
        out.close();
    }



    /** {@inheritDoc} */
    @Override
    public abstract String getSummary();

    /**
     *
     * @param spec the referring spec of this object
     * @param in the input stream to write to
     * @param version the version (3.0 - 3.2)
     * @throws SAXException if something goes wrong during writing
     * @throws ParserConfigurationException if the parser cannot be instantiated
     * @throws IOException if the file cannot be found
     */
    public void loadFrom(final PMMLPortObjectSpec spec, final InputStream in,
            final String version)
            throws SAXException, ParserConfigurationException, IOException {
        if (m_isLoaded) {
            throw new IOException(getClass().getSimpleName()
                    + " is not loadable or has already been loaded");
        }
        m_isLoaded = true;
        SAXParserFactory fac = SAXParserFactory.newInstance();

        SchemaFactory schemaFac = SchemaFactory.newInstance(
                XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFac.newSchema(new StreamSource(
                getSchemaInputStream(version)));
        fac.setSchema(schema);
        fac.setNamespaceAware(true);

        SAXParser parser = fac.newSAXParser();
        // removes all X- elements!!!
        // and inserts namespace
        XFilter filter = new XFilter(version);
        filter.setParent(parser.getXMLReader());

        ExtractModelTypeHandler modelTypeHdl = new ExtractModelTypeHandler();

        m_masterHandler.addContentHandler(ExtractModelTypeHandler.ID,
                modelTypeHdl);
        filter.setContentHandler(m_masterHandler);
        filter.setErrorHandler(m_masterHandler);
        filter.parse(new InputSource(in));
        ExtractModelTypeHandler hdl = (ExtractModelTypeHandler)m_masterHandler
            .getDefaultHandler(ExtractModelTypeHandler.ID);
        m_modelType = hdl.getModelType();
        if (m_modelType == null) {
            m_modelType = PMMLModelType.None;
        }
        m_spec = spec;

        /*
         * The PMML version parsed by the master handler is not used for the
         * PMML port object as we are interpreting the parsed PMML. It is only
         * used to validate if we support the parsed version. The port
         * object has to decide itself which version it writes. This can be set
         * in the constructor (3.1 is the default) or with the provided setter.
         */
    }


    private InputStream getSchemaInputStream(final String version) {
        ClassLoader loader = PMMLPortObject.class.getClassLoader();
        String packagePath =
                PMMLPortObject.class.getPackage().getName().replace('.', '/');
        return loader.getResourceAsStream(
                packagePath + VERSION_SCHEMA_MAP.get(version));
    }


    /**
     *
     * {@inheritDoc}
     */
    @Override
    public PMMLPortObjectSpec getSpec() {
        return m_spec;
    }


    /**
     * @param handler the handler responsible for writing the PMML
     * @throws SAXException if something goes wrong during writing the PMML
     * */

    protected void writeLocalTransformations(final TransformerHandler handler)
            throws SAXException {
        if (m_operations == null)
            return;

        handler.startElement(null, null, LOCAL_TRANS, null);
        for (PMMLPreprocOperation op : m_operations) {
            if (op.getWriteElement() == PMMLWriteElement.LOCALTRANS) {
                op.save(handler, null);
            }
        }
        handler.endElement(null, null, LOCAL_TRANS);
    }

    /**
     *
     * @param handler the handler responsible for writing the PMML
     * @throws SAXException if something goes wrong during writing the PMML
     */
    protected abstract void writePMMLModel(final TransformerHandler handler)
        throws SAXException;


    /**
     *
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        return new JComponent[] {new PMMLPortObjectView(this)};
    }


    /**
     * @return the operations
     */
    protected List<PMMLPreprocOperation> getOperations() {
        return m_operations;
    }


    /**
     * Note that this method can only be called once on each PMMLPortObject.
     *
     * @param operations the preprocessing operations to set
     * @throws IllegalStateException if the operations have already been set
     */
    public void setOperations(final List<PMMLPreprocOperation> operations)
            throws IllegalStateException {
        if (m_operations != null) {
            throw new IllegalStateException("Preprocessing operations are "
                    + "already set. This can be done only once. Please combine "
                    + "all operations before integrating them.");
        }
        m_operations = new ArrayList<PMMLPreprocOperation>(operations);
    }
}
