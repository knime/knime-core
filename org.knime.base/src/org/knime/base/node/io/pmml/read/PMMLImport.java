/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.dmg.pmml.PMMLDocument;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLDataDictionaryTranslator;
import org.knime.core.node.port.pmml.PMMLMiningSchemaTranslator;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.knime.core.pmml.PMMLUtils;
import org.knime.core.pmml.PMMLValidator;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * @author Dominik Morent, KNIME AG, Zurich, Switzerland
 */
public class PMMLImport {
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(PMMLImport.class);

    private PMMLPortObject m_portObject;

    /**
     * Reads and validates the passed file and creates the
     * {@link PMMLPortObjectSpec} from the content of the file.
     *
     * @param file containing the PMML model
     * @param update try to update the PMML to version 4.1 if an older version
     *      is imported
     * @throws IOException if something goes wrong reading the file
     * @throws XmlException if an invalid PMML file is passed
     * @throws IllegalArgumentException if the input file is invalid or has
     *      invalid content
     */
    public PMMLImport(final File file, final boolean update)
            throws IOException, XmlException, IllegalArgumentException {
        this(checkFileAndGetURL(file), update);
    }

    /** Checks file existence (throws exception if not exists) and returns corresponding URL. */
    private static URL checkFileAndGetURL(final File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File must not be null!");
        }
        if (!file.exists()) {
            throw new IllegalArgumentException("File " + file.getName()
                    + " does not exists.");
        }
        return file.toURI().toURL();
    }

    /**
     * Reads and validates the passed file and creates the
     * {@link PMMLPortObjectSpec} from the content of the file.
     *
     * @param url url of PMML file
     * @param update try to update the PMML to version 4.1 if an older version
     *      is imported
     * @throws IOException if something goes wrong reading the file
     * @throws XmlException if an invalid PMML file is passed
     * @throws IllegalArgumentException if the input file is invalid or has invalid content
     */
    PMMLImport(final URL url, final boolean update)
            throws IOException, XmlException, IllegalArgumentException {

        if (url == null) {
            throw new IllegalArgumentException("URL argument must not be null.");
        }
        String urlToString;
        if ("file".equals(url.getProtocol())) {
            try {
                urlToString = new File(url.toURI()).getAbsolutePath();
            } catch (Exception e) {
                urlToString = url.toString();
            }
        } else {
            urlToString = url.toString();
        }
        PMMLDocument pmmlDoc = null;
        XmlObject xmlDoc;
        try (InputStream inStream = url.openStream()) {
            // see bug 6306 - XML 1.1 features not parsed 'correctly'
            XmlOptions o = new XmlOptions();
            try {
                o.setLoadUseXMLReader( SAXParserFactory.newInstance().newSAXParser().getXMLReader() );
            } catch (ParserConfigurationException | SAXException e) {
                LOGGER.error("Unable to create SAX parser, will use default: " + e.getMessage(), e);
            }
            xmlDoc = XmlObject.Factory.parse(inStream, o);
        }
        if (xmlDoc instanceof PMMLDocument) {
            pmmlDoc = (PMMLDocument)xmlDoc;
        } else {
            /* Try to recover when reading a PMML 3.x document that
             * was produced by KNIME by just replacing the PMML version and
             * namespace or when the recover flag is set. */
            if (update || PMMLUtils.isOldKNIMEPMML(xmlDoc) || PMMLUtils.is4_1PMML(xmlDoc)) {
                try {
                    String updatedPMML
                            = PMMLUtils.getUpdatedVersionAndNamespace(xmlDoc);
                    /* Parse the modified document and assign it to a
                     * PMMLDocument.*/
                    pmmlDoc = PMMLDocument.Factory.parse(updatedPMML);
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Parsing of PMML v 3.x/4.0/4.1 document failed.", e);
                }
                if (!update) {
                    LOGGER.info(
                            "KNIME produced PMML 3.x/4.0/4.1 updated to PMML 4.2.");
                } else {
                    LOGGER.info("Older PMML version updated to PMML 4.2.");
                }
            }
        }
        if (pmmlDoc == null) {
            throw new IllegalArgumentException("File \"" + urlToString + "\" is not a valid PMML 4.2 file.");
        }
        Map<String, String> msg = PMMLValidator.validatePMML(pmmlDoc);
        if (!msg.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            sb.append("File \"" + urlToString + "\" is not a valid PMML 4.2 file. Errors:\n");
            for (Map.Entry<String, String> entry : msg.entrySet()) {
                String location = entry.getKey();
                String message = entry.getValue();
                sb.append(location);
                sb.append(": ");
                sb.append(message);
                sb.append("\n");
            }
            throw new IllegalArgumentException(sb.toString());
        }
        init(pmmlDoc);
    }

    /**
     * Reads and validates the passed file and creates the
     * {@link PMMLPortObjectSpec} from the content of the file.
     *
     * @param file containing the PMML model
     * @throws IOException if something goes wrong reading the file
     * @throws XmlException if an invalid PMML file is passed
     * @throws IllegalArgumentException if the input file is invalid or has
     *      invalid content
     */
    public PMMLImport(final File file) throws IOException, XmlException,
            IllegalArgumentException {
        this(file, false);
    }

    /**
     * @param pmmlDoc
     */
    private void init(final PMMLDocument pmmlDoc) {
        PMMLDataDictionaryTranslator dictTrans
                = new PMMLDataDictionaryTranslator();
        dictTrans.initializeFrom(pmmlDoc);
        DataTableSpec tableSpec = dictTrans.getDataTableSpec();
        List<String> activeDerivedFields = dictTrans.getActiveDerivedFields();
        PMMLPortObjectSpecCreator specCreator
                = new PMMLPortObjectSpecCreator(tableSpec);

        PMMLMiningSchemaTranslator miningTrans
                = new PMMLMiningSchemaTranslator();
        miningTrans.initializeFrom(pmmlDoc);

        Set<String> activeFields = new LinkedHashSet<String>();
        List<String> miningFields = miningTrans.getActiveFields();
        /* If we have a model all active fields of the data dictionary
         * are passed through the mining schema. */
        activeFields.addAll(miningFields);
        activeFields.addAll(activeDerivedFields);
        specCreator.setLearningColsNames(new LinkedList<String>(activeFields));
        specCreator.addPreprocColNames(activeDerivedFields);
        specCreator.setTargetColsNames(miningTrans.getTargetFields());

        PMMLPortObjectSpec portObjectSpec = specCreator.createSpec();
        m_portObject = new PMMLPortObject(portObjectSpec, pmmlDoc);
    }

    /**
     * Reads and validates the passed file and creates the
     * {@link PMMLPortObjectSpec} from the content of the file.
     *
     * @param doc the document containing the PMML model
     * @throws IOException if something goes wrong reading the file
     * @throws XmlException if an invalid PMML file is passed
     * @throws IllegalArgumentException if the input file is invalid or has
     *      invalid content
     */
    public PMMLImport(final Document doc) throws IOException, XmlException,
            IllegalArgumentException {
        if (doc == null) {
            throw new IllegalArgumentException("Document must not be null!");
        }

        PMMLDocument pmmlDoc = PMMLDocument.Factory.parse(doc);
        if (!pmmlDoc.validate()) {
            throw new IllegalArgumentException("Document \"" + doc
                    + "\" is not a valid PMML 4.1 file.");
        }
        init(pmmlDoc);
    }


    /**
     *
     * @return the parsed port object spec (data dictionary and mining schema)
     */
    public PMMLPortObjectSpec getPortObjectSpec() {
        return m_portObject.getSpec();
    }

    /**
     *
     * @return the parsed PMML model
     */
    public PMMLPortObject getPortObject() {
        return m_portObject;
    }



}
