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
package org.knime.core.node.port.pmml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.knime.core.data.xml.PMMLCellFactory;
import org.knime.core.data.xml.PMMLValue;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.preproc.PMMLPreprocOperation;
import org.knime.core.node.port.pmml.preproc.PMMLPreprocOperation.PMMLWriteElement;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLPortObject implements PortObject {
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(PMMLPortObject.class);

    /** Convenience accessor for the port type. */
    public static final PortType TYPE = new PortType(PMMLPortObject.class);

    /** Constant for CDATA. */
    public static final String CDATA = "CDATA";
    /** Constant for DataDictionary. */
    public static final String DATA_DICT = "DataDictionary";
    /** Constant for DataField. */
    public static final String DATA_FIELD = "DataField";
    /** Constant for PMML Element. */
    public static final String PMML_ELEMENT = "PMML";

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

    private PMMLValue m_content;


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


    private PMMLPortObjectSpec m_spec;

    private final PMMLMasterContentHandler m_masterHandler;

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
     * Calling this constructor is discouraged. It is only available for
     * internal calls.
     */
    public PMMLPortObject() {
        m_masterHandler = new PMMLMasterContentHandler();
    }

    /**
     * Creates a new PMML port object. Models can be added later by calling
     * {@link #addPMMLModel(Node)}.
     * @param spec the referring {@link PMMLPortObjectSpec}
     */
    public PMMLPortObject(final PMMLPortObjectSpec spec) {
        m_spec = spec;
        m_masterHandler = new PMMLMasterContentHandler();
        initializePMMLDocument();
    }

    /**
     * Creates a new PMML port object. Models can be added later by calling
     * {@link #addPMMLModel(Node)}.
     * @param spec the referring {@link PMMLPortObjectSpec}
     * @param dom the pmml document
     */
    public PMMLPortObject(final PMMLPortObjectSpec spec, final Document dom) {
        m_spec = spec;
        m_masterHandler = new PMMLMasterContentHandler();
        m_content = (PMMLValue)PMMLCellFactory.create(dom);
    }

    /**
     * @param spec the port object spec
     * @param handler the pmml content handler that adds the model content
     * @throws SAXException if the pmml model could not be added
     */
    public PMMLPortObject(final PMMLPortObjectSpec spec,
            final PMMLContentHandler handler) throws SAXException {
        this(spec);
        DocumentFragment fragment
                = m_content.getDocument().createDocumentFragment();
        handler.addPMMLModel(fragment, spec);
        addPMMLModel(fragment);
    }

    /**
     *
     * @return a list with all pmml models
     * @see PMMLModelType
     */
    public Set<PMMLModelType> getModelTypes() {
        return m_content.getModelTypes();
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

    /**
     * Writes the port object to valid PMML. Subclasses should not override this
     * method.
     *
     *
     * @param out zipped stream which reads the PMML file
     * @throws TransformerFactoryConfigurationError if something goes wrong with
     *          the transformation handler
     * @throws TransformerException if something goes wrong in the
     *      transformation process
     * @throws IOException if the file cannot be written to the directory
     */
    public final void save(final OutputStream out)
            throws TransformerFactoryConfigurationError, TransformerException,
            IOException {
//        try {
//            validate();
//        } catch (SAXException e) {
//            throw new IOException(e);
//        }
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        Source source = new DOMSource(m_content.getDocument());
        t.transform(source,  new StreamResult(out));
        out.close();
    }

   /**
     * Creates a pmml document from scratch (still without a model) and stores
     * it as the PMMLValue of this class.
     */
    private void initializePMMLDocument() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            String version = getWriteVersion();
            SAXTransformerFactory fac =
                    (SAXTransformerFactory)TransformerFactory.newInstance();
            TransformerHandler handler;
            handler = fac.newTransformerHandler();
            Transformer t = handler.getTransformer();
            t.setOutputProperty(OutputKeys.METHOD, "xml");
            t.setOutputProperty(OutputKeys.INDENT, "yes");

            handler.setResult(new StreamResult(out));

            // PMML root element, namespace declaration, etc.
            handler.startDocument();
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute(null, null, "version", CDATA, version);
            attr.addAttribute(null, null, "xmlns", CDATA,
                    VERSION_NAMESPACE_MAP.get(version));
            attr.addAttribute(null, null, "xmlns:xsi", CDATA,
                    "http://www.w3.org/2001/XMLSchema-instance");
            handler.startElement(null, null, "PMML", attr);
            PMMLPortObjectSpec.writeHeader(handler, version);
            PMMLPortObjectSpec.writeDataDictionary(
                    getSpec().getDataTableSpec(), handler, version);
            /*
             * No model is written yet. It has to be added by calling
             * addPMMLModel.
             */
            handler.endElement(null, null, "PMML");
            handler.endDocument();
            m_content = (PMMLValue)PMMLCellFactory.create(
                    new ByteArrayInputStream(out.toByteArray()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                // ignore if closing the stream fails
            }
        }
    }


    /** {@inheritDoc} */
    @Override
    public String getSummary() {
        return  "PMML document with version " + m_content.getPMMLVersion()
            + "and models: " + m_content.getModelTypes();
    }



    /**
     * This method should no longer be used. The version parameter is ignored.
     * Use {@link #loadFrom(PMMLPortObjectSpec, InputStream)} instead.
    *
    * @param spec the referring spec of this object
    * @param in the input stream to write to
    * @param version the version (3.0 - 3.2)
    * @throws SAXException if something goes wrong during writing
    * @throws ParserConfigurationException if the parser cannot be instantiated
    * @throws IOException if the file cannot be found
    */
    @Deprecated
    public void loadFrom(final PMMLPortObjectSpec spec, final InputStream in,
            @SuppressWarnings("unused") final String version)
            throws IOException, ParserConfigurationException, SAXException {
        // Version is ignored and only maintained due to compatibility reasons
        loadFrom(spec, in);
    }

    /**
     * Initializes the pmml port object based on the xml input stream.
     * @param spec the referring spec of this object
     * @param is the pmml input stream
     * @throws SAXException if something goes wrong during writing
     * @throws ParserConfigurationException if the parser cannot be instantiated
     * @throws IOException if the file cannot be found
     */
    public void loadFrom(final PMMLPortObjectSpec spec, final InputStream is)
            throws IOException, ParserConfigurationException, SAXException {
        try {
            m_content = (PMMLValue)PMMLCellFactory.create(is);
        } catch (XMLStreamException e) {
            throw new SAXException(e);
        }
        m_spec = spec;
    }



    private InputStream getSchemaInputStream(final String location) {
        ClassLoader loader = PMMLPortObject.class.getClassLoader();
        String packagePath =
                PMMLPortObject.class.getPackage().getName().replace('.', '/');
        return loader.getResourceAsStream(
                packagePath + location);
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
     * Adds a local transformation element to the document fragment if some
     * are defined.
     * @param localTrans the local transformation document fragment
     * @throws SAXException if something goes wrong during writing the PMML
     */
    private void addLocalTransformations(final DocumentFragment localTrans)
            throws SAXException {
        if (m_operations == null) {
            return;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SAXTransformerFactory fac =
                (SAXTransformerFactory)TransformerFactory.newInstance();
        TransformerHandler handler;
        try {
            handler = fac.newTransformerHandler();
        } catch (TransformerConfigurationException e) {
            throw new SAXException(e);
        }
        Transformer t = handler.getTransformer();
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        handler.setResult(new StreamResult(out));
        handler.startDocument();

        handler.startElement(null, null, LOCAL_TRANS, null);
        for (PMMLPreprocOperation op : m_operations) {
            if (op.getWriteElement() == PMMLWriteElement.LOCALTRANS) {
                op.save(handler, null);
            }
        }
        handler.endElement(null, null, LOCAL_TRANS);

        handler.endDocument();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        SAXSource s = new SAXSource(new InputSource(in));
        DOMResult r = new DOMResult(localTrans);
        try {
            t.transform(s, r);
            in.close();
            out.close();
        } catch (Exception e) {
            throw new SAXException(e);
        }
    }




    /**
     * Appends the pmml model to the pmml document. Only fragments that have
     * been retrieved by the {@link #getDocFragment()} method can be added.
     * Only {@link PMMLModelType} elements can be added.
     *
     * @param model the model fragment to add
     * @throws SAXException if the pmml model could not be added
     */
    private void addPMMLModel(final DocumentFragment model)
            throws SAXException{
        String modelName = model.getFirstChild().getNodeName();
        if (!PMMLModelType.contains(modelName)) {
            throw new IllegalArgumentException(modelName + " cannot be added "
                    + "as pmml model. Only " + PMMLModelType.TYPESTRING
                    + " are supported.");
        }
        Node pmmlNode = m_content.getDocument()
                .getElementsByTagName(PMML_ELEMENT).item(0);

        // add the local transformations
        DocumentFragment localTrans = m_content.getDocument()
                .createDocumentFragment();
        addLocalTransformations(localTrans);

        /* Find the next sibling of the mining scheme and insert the local
         * transformations in between. */
        NodeList childNodes = model.getFirstChild().getChildNodes();
        Node node = null;
        for (int i = 0; i < childNodes.getLength(); i++) {
            node = childNodes.item(i);
            if (PMMLPortObjectSpec.MINING_SCHEMA.equalsIgnoreCase(
                    node.getNodeName())) {
                node = childNodes.item(i + 1);
                break;
            }
        }
        model.insertBefore(localTrans, node);
        pmmlNode.appendChild(model);
    }

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

    /**
     * Returns the PMML value.
     *
     * @return the pmml value
     */
    public PMMLValue getPMMLValue() {
        return m_content;
    }

    private void validate() throws SAXException {
        try {
            String pmmlVersion = m_content.getPMMLVersion();
            LOGGER.debug("Validating PMML output. Version = " + pmmlVersion);
            SchemaFactory schemaFac =
                    SchemaFactory
                            .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            SAXParserFactory fac = SAXParserFactory.newInstance();
            fac.setNamespaceAware(true);

            Schema schema = null;
            if (pmmlVersion == null) {
                throw new SAXException("Input file is not a valid PMML file. "
                        + "Attribute \"version\" is missing");
            }
            String schemaLocation =
                    PMMLPortObject.getLocalSchemaLocation(pmmlVersion);
            if (schemaLocation == null) {
                throw new SAXException("Version " + pmmlVersion
                        + " is not supported!");
            }
            InputStream stream = getSchemaInputStream(schemaLocation);
            InputSource inputSource = new InputSource(
                            stream);
            SAXSource saxSource = new SAXSource(
                    inputSource);
            schema = schemaFac.newSchema(saxSource);
            XFilter filter = new XFilter(pmmlVersion);
            filter.setParent(fac.newSAXParser().getXMLReader());
            // use validator here
            Validator validator = schema.newValidator();
            // register error handler
//            validator.setErrorHandler(m_errorHandler);
            validator.validate(new DOMSource(m_content.getDocument()));
        } catch (IOException io) {
            throw new SAXException(io);
        } catch (ParserConfigurationException pce) {
            throw new SAXException(pce);
        }
    }

    /**
     * @param file the pmml file to parse the spec from
     * @return the pmml port object spec
     * @throws SAXException
     */
    public static PMMLPortObjectSpec parseSpec(final File file)
            throws SAXException {
        try {
            return parseSpec(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new SAXException(e);
        }
    }

    /**
     * @param doc the document to parse the spec from
     * @return the pmml port object spec
     * @throws SAXException
     */
    public static PMMLPortObjectSpec parseSpec(final Document doc)
            throws SAXException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Source in = new DOMSource(doc);
        Result out = new StreamResult(outputStream);
        try {
            TransformerFactory.newInstance().newTransformer().transform(
                    in, out);
        } catch (Exception e) {
            throw new SAXException(e);
        }
        InputStream is = new ByteArrayInputStream(outputStream.toByteArray());
        return parseSpec(is);
    }

    private static PMMLPortObjectSpec parseSpec(final InputStream in)
            throws SAXException {
        try {
            SAXParserFactory fac = SAXParserFactory.newInstance();
            SAXParser parser = fac.newSAXParser();
            PMMLMasterContentHandler masterHandler =
                    new PMMLMasterContentHandler();
            DataDictionaryContentHandler ddHandler =
                    new DataDictionaryContentHandler();
            masterHandler.addContentHandler(DataDictionaryContentHandler.ID,
                    ddHandler);
            MiningSchemaContentHandler miningSchemaHdl =
                    new MiningSchemaContentHandler();
            masterHandler.addContentHandler(MiningSchemaContentHandler.ID,
                    miningSchemaHdl);
            parser.parse(in, masterHandler);
            PMMLPortObjectSpecCreator creator =
                    new PMMLPortObjectSpecCreator(ddHandler.getDataTableSpec());
            creator.setLearningColsNames(miningSchemaHdl.getLearningFields());
            creator.setTargetColsNames(miningSchemaHdl.getTargetFields());
            return creator.createSpec();
        } catch (IOException e) {
            throw new SAXException(e);
        } catch (ParserConfigurationException pce) {
            throw new SAXException(pce);
        }
    }
}
