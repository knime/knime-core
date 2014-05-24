/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
public class PMMLCellContent extends XMLCellContent implements PMMLValue {

    /**
     * Creates a {@link Document} by parsing the passed string. It must
     * contain a valid XML document.
     *
     * @param xmlString an XML document
     * @throws IOException If any IO errors occur.
     * @throws ParserConfigurationException If {@link DocumentBuilder} cannot
     *          be created.
     * @throws SAXException If xmlString cannot be parsed
     * @throws XMLStreamException no valid PMML/XML stream
     */
    PMMLCellContent(final String xmlString) throws IOException,
            ParserConfigurationException, SAXException, XMLStreamException {
       this(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
    }

    /**
     * Creates a {@link Document} by parsing the contents of the passed
     * {@link InputStream}. It must contain a valid XML document.
     *
     * @param is an XML document
     * @throws IOException If any IO errors occur.
     * @throws ParserConfigurationException If {@link DocumentBuilder} cannot
     *          be created.
     * @throws SAXException If xmlString cannot be parsed.
     * @throws XMLStreamException no valid PMML/XML stream
     */
    PMMLCellContent(final InputStream is) throws IOException,
            ParserConfigurationException, SAXException, XMLStreamException {
        super(is);
    }

    /**
     * Creates a new instance which encapsulates the passed XML document.
     *
     * @param doc an XML document
     */
    PMMLCellContent(final Document doc) {
       super(doc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPMMLVersion() {
        Node pmml = getDocument().getElementsByTagName(
                PMMLPortObject.PMML_ELEMENT).item(0);
        return pmml.getAttributes().getNamedItem("version").getNodeValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<PMMLModelType> getModelTypes() {
        Set<PMMLModelType> types = new TreeSet<PMMLModelType>();
        for (Node node : getModels()) {
            types.add(PMMLModelType.getType(node.getNodeName()));
        }
        return types;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Node> getModels(final PMMLModelType type) {
        List<Node> nodes = new LinkedList<Node>();
        Node pmml = getDocument().getElementsByTagName("PMML").item(0);
        NodeList children = pmml.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeName().equals(type.toString())) {
                nodes.add(children.item(i));
            }
        }
        return nodes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Node> getModels() {
        List<Node> nodes = new LinkedList<Node>();
        for (PMMLModelType type : PMMLModelType.values()) {
            nodes.addAll(getModels(type));
        }
        return nodes;
    }


    /**
     * @return The XML Document as a string.
     */
    @Override
    String getStringValue() {
        return super.getStringValue();
    }

}
