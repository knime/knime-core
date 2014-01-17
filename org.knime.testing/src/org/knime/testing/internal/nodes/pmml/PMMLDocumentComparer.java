/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.testing.internal.nodes.pmml;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.dmg.pmml.PMMLDocument;
import org.knime.core.data.xml.util.XmlDomComparer;
import org.knime.core.data.xml.util.XmlDomComparer.Diff;
import org.knime.core.data.xml.util.XmlDomComparerCustomizer;
import org.knime.core.data.xml.util.XmlDomComparerCustomizer.ChildrenCompareStrategy;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Checks if two PMML documents are equal.
 *
 * @author Alexander Fillbrunn
 *
 */
public class PMMLDocumentComparer {

    /**
     * Name of the root node in the PMML document.
     */
    public static final String PMML_XML_NODE_NAME = "PMML";

    /**
     * Name of the node in the PMML document that contains the data dictionary.
     */
    public static final String DATADICT_XML_NODE_NAME = "DataDictionary";

    /**
     * Name of the node in the PMML document that contains the transformation dictionary.
     */
    public static final String TRANSDICT_XML_NODE_NAME = "TransformationDictionary";

    /**
     * Name of the nodes in the PMML document that contain a model verification.
     */
    public static final String MODEL_VERIFICATION_XML_NODE_NAME = "ModelVerification";

    /**
     * Name of the nodes in the PMML document that contain the header.
     */
    public static final String HEADER_XML_NODE_NAME = "Header";

    /**
     * Name of the nodes in the PMML document that contain extensions.
     */
    public static final String EXTENSION_XML_NODE_NAME = "Extension";

    /**
     * Name of the nodes in the PMML document that contain the mining build task.
     */
    public static final String MINING_BUILD_TASK_XML_NODE_NAME = "MiningBuildTask";

    /**
     * Node type number for an element node.
     */
    public static final int ELEMENT_NODE_TYPE = 1;

    /**
     * Node type number for an attribute node.
     */
    public static final int ATTRIBUTE_NODE_TYPE = 2;

    /**
     * Node type number for a text node.
     */
    public static final int TEXT_NODE_TYPE = 3;

    /**
     * Node type number for a comment node.
     */
    public static final int COMMENT_NODE_TYPE = 8;

    private boolean m_checkDataDictionaries;

    private boolean m_checkTransformationDictionaries;

    private boolean m_checkHeader;

    private boolean m_checkMiningBuildTask;

    private boolean m_checkModelVerification;

    private boolean m_checkExtensions;

    private boolean m_checkSchema;

    /**
     * Constructor for PMMLDocumentComparer.
     *
     * @param checkDataDictionaries Determines whether the PMMLs' data dictionaries are compared
     * @param checkTransformationDictionaries Determines whether the PMMLs' transformation dictionaries are compared
     * @param checkHeader Determines whether the PMMLs' headers are compared
     * @param checkMiningBuildTask Determines whether the PMMLs' mining build tasks are compared
     * @param checkModelVerification Determines whether the PMMLs' model verifications are compared
     * @param checkExtensions Determines whether the PMMLs' extensions are compared
     */
    public PMMLDocumentComparer(final boolean checkDataDictionaries, final boolean checkTransformationDictionaries,
                                final boolean checkHeader, final boolean checkMiningBuildTask,
                                final boolean checkModelVerification, final boolean checkExtensions,
                                final boolean checkSchema) {
        m_checkDataDictionaries = checkDataDictionaries;
        m_checkTransformationDictionaries = checkTransformationDictionaries;
        m_checkHeader = checkHeader;
        m_checkMiningBuildTask = checkMiningBuildTask;
        m_checkModelVerification = checkModelVerification;
        m_checkExtensions = checkExtensions;
        m_checkSchema = checkSchema;
    }

    /**
     * Constructor for a comparer that compares all elements of the PMML document.
     */
    public PMMLDocumentComparer() {
        this(true, true, true, true, true, true, true);
    }

    /**
     * Checks if two PMML Documents are equal.
     *
     * @param doc1 The first document
     * @param doc2 The second document
     * @return A boolean value determining whether the documents are equal with respect to the settings given two this
     *         instance of PMMLDocumentComparer
     * @throws ParserConfigurationException If a DocumentBuilder cannot be created.
     * @throws SAXException If the pmml document cannot be parsed
     * @throws IOException If any IO error occurs
     */
    public Diff areEqual(final PMMLDocument doc1, final PMMLDocument doc2) throws ParserConfigurationException,
            SAXException, IOException {

        XmlDomComparerCustomizer listener = new XmlDomComparerCustomizer(ChildrenCompareStrategy.UNORDERED) {
            @Override
            public boolean include(final Node n) {
                if (n.getNodeType() == ATTRIBUTE_NODE_TYPE) {
                    // Either we are not in the PMML Element, or even schema
                    // attributes are checked
                    // or the node is the version attribute.
                    Element parent = ((Attr)n).getOwnerElement();
                    return parent.getNodeName() != "PMML" || m_checkSchema || n.getNodeName().equals("version");
                } else {
                    return !(n.getNodeType() == COMMENT_NODE_TYPE
                            || (!m_checkModelVerification && n.getNodeName().equals(MODEL_VERIFICATION_XML_NODE_NAME))
                            || (!m_checkDataDictionaries && n.getNodeName().equals(DATADICT_XML_NODE_NAME))
                            || (!m_checkTransformationDictionaries && n.getNodeName().equals(TRANSDICT_XML_NODE_NAME))
                            || (!m_checkHeader && n.getNodeName().equals(HEADER_XML_NODE_NAME))
                            || (!m_checkMiningBuildTask && n.getNodeName().equals(MINING_BUILD_TASK_XML_NODE_NAME))
                            || (!m_checkModelVerification && n.getNodeName().equals(MODEL_VERIFICATION_XML_NODE_NAME))
                            || (!m_checkExtensions && n.getNodeName().equals(EXTENSION_XML_NODE_NAME)) || (n
                            .getNodeType() == TEXT_NODE_TYPE && n.getNodeValue().trim().length() == 0));
                }
            }
        };

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Element pmml1 =
                (Element)builder.parse(new InputSource(new StringReader(doc1.toString())))
                        .getElementsByTagName(PMML_XML_NODE_NAME).item(0);
        Element pmml2 =
                (Element)builder.parse(new InputSource(new StringReader(doc2.toString())))
                        .getElementsByTagName(PMML_XML_NODE_NAME).item(0);

        return XmlDomComparer.compareNodes(pmml1, pmml2, listener);
    }
}
