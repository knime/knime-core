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
 * ---------------------------------------------------------------------
 *
 * Created on 28.05.2013 by thor
 */
package org.knime.core.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.knime.core.def.node.NodeType;
import org.knime.node.v31.InPort;
import org.knime.node.v31.KnimeNodeDocument;
import org.knime.node.v31.OutPort;
import org.knime.node.v31.View;
import org.knime.node.v31.Views;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of {@link NodeDescription} for node descriptions introduced with 3.1. It uses XMLBeans to extract the
 * information from the XML file.<br>
 * If assertions are enabled (see {@link KNIMEConstants#ASSERTIONS_ENABLED} it also checks the contents of the XML for
 * against the XML schema and reports errors via the logger.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 3.1
 */
public final class NodeDescription31Proxy extends NodeDescription {
    private static final XmlOptions OPTIONS = new XmlOptions();

    private static final NodeLogger logger = NodeLogger.getLogger(NodeDescription31Proxy.class);

    static {
        Map<String, String> namespaceMap = new HashMap<String, String>(1);
        namespaceMap.put("", KnimeNodeDocument.type.getContentModel().getName().getNamespaceURI());
        OPTIONS.setLoadSubstituteNamespaces(namespaceMap);
    }

    private final KnimeNodeDocument m_document;

    /**
     * Creates a new proxy object using the given XML document. If assertions are enabled (see
     * {@link KNIMEConstants#ASSERTIONS_ENABLED} it also checks the contents of the XML for against the XML schema and
     * reports errors via the logger.
     *
     * @param doc the XML document of the node description XML file
     * @throws XmlException if something goes wrong while analyzing the XML structure
     */
    public NodeDescription31Proxy(final Document doc) throws XmlException {
        m_document = KnimeNodeDocument.Factory.parse(doc.getDocumentElement(), OPTIONS);
        setIsDeprecated(m_document.getKnimeNode().getDeprecated());
        validate();
    }

    /**
     * Creates a new proxy object using the given knime node document. If assertions are enabled (see
     * {@link KNIMEConstants#ASSERTIONS_ENABLED} it also checks the contents of the XML for against the XML schema and
     * reports errors via the logger.
     *
     * @param doc a knime node document
     */
    public NodeDescription31Proxy(final KnimeNodeDocument doc) {
        m_document = doc;
        setIsDeprecated(m_document.getKnimeNode().getDeprecated());
        if (KNIMEConstants.ASSERTIONS_ENABLED) {
            validate();
        }
    }

    /**
     * Validate against the XML Schema. If violations are found they are reported via the logger as coding problems.
     *
     * @return <code>true</code> if the document is valid, <code>false</code> otherwise
     */
    protected final boolean validate() {
        // this method has default visibility so that we can use it in testcases
        XmlOptions options = new XmlOptions(OPTIONS);
        List<XmlError> errors = new ArrayList<XmlError>();
        options.setErrorListener(errors);
        boolean valid = m_document.validate(options);
        if (!valid) {
            logger.coding("Node description of '" + m_document.getKnimeNode().getName()
                + "' does not conform to the Schema. Violations follow.");
            for (XmlError err : errors) {
                logger.coding(err.toString());
            }
        }
        return valid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIconPath() {
        return m_document.getKnimeNode().getIcon();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInportDescription(final int index) {
        if (m_document.getKnimeNode().getPorts() == null) {
            return null;
        }

        for (InPort inPort : m_document.getKnimeNode().getPorts().getInPortList()) {
            if (inPort.getIndex().intValue() == index) {
                return stripXmlFragment(inPort);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInportName(final int index) {
        if (m_document.getKnimeNode().getPorts() == null) {
            return null;
        }

        for (InPort inPort : m_document.getKnimeNode().getPorts().getInPortList()) {
            if (inPort.getIndex().intValue() == index) {
                return inPort.getName();
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInteractiveViewName() {
        if (m_document.getKnimeNode().getInteractiveView() != null) {
            return m_document.getKnimeNode().getInteractiveView().getName();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNodeName() {
        String nodeName = m_document.getKnimeNode().getName();
        if (m_document.getKnimeNode().getDeprecated() && !nodeName.matches("^.+\\s+\\(?[dD]eprecated\\)?$")) {
            return nodeName + " (deprecated)";
        } else {
            return nodeName;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOutportDescription(final int index) {
        if (m_document.getKnimeNode().getPorts() == null) {
            return null;
        }

        for (OutPort outPort : m_document.getKnimeNode().getPorts().getOutPortList()) {
            if (outPort.getIndex().intValue() == index) {
                return stripXmlFragment(outPort);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOutportName(final int index) {
        if (m_document.getKnimeNode().getPorts() == null) {
            return null;
        }

        for (OutPort outPort : m_document.getKnimeNode().getPorts().getOutPortList()) {
            if (outPort.getIndex().intValue() == index) {
                return outPort.getName();
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeType getType() {
        try {
            return NodeType.valueOf(m_document.getKnimeNode().getType().toString());
        } catch (IllegalArgumentException ex) {
            logger.error("Unknown node type for " + m_document.getKnimeNode().getName() + ": "
                + m_document.getKnimeNode().getDomNode().getAttributes().getNamedItem("type").getNodeValue(), ex);
            return NodeType.Unknown;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getViewCount() {
        Views views = m_document.getKnimeNode().getViews();
        return (views == null) ? 0 : views.sizeOfViewArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getViewDescription(final int index) {
        if (m_document.getKnimeNode().getViews() == null) {
            return null;
        }

        for (View view : m_document.getKnimeNode().getViews().getViewList()) {
            if (view.getIndex().intValue() == index) {
                return stripXmlFragment(view);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getViewName(final int index) {
        if (m_document.getKnimeNode().getViews() == null) {
            return null;
        }

        for (View view : m_document.getKnimeNode().getViews().getViewList()) {
            if (view.getIndex().intValue() == index) {
                return view.getName();
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setIsDeprecated(final boolean b) {
        super.setIsDeprecated(b);
        m_document.getKnimeNode().setDeprecated(b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element getXMLDescription() {
        return (Element)m_document.getKnimeNode().getDomNode();
    }
}
