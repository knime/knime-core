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
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class should no longer be used. PMML support is handled now with the
 * help of XMLBeans. A {@link PMMLTranslator} should be implemented to provide
 * the translation from PMML to KNIME and vice versa.
 *
 * @author Fabian Dill, University of Konstanz
 */
@Deprecated
public abstract class PMMLContentHandler extends DefaultHandler
        implements ContentHandler {
    /**
     *
     * {@inheritDoc}
     */
    @Override
    public abstract void characters(final char[] ch,
            final int start, final int length) throws SAXException;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public abstract void endDocument() throws SAXException;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public abstract void endElement(final String uri, final String localName,
            final String name)throws SAXException;


    /**
     *
     * {@inheritDoc}
     */
    @Override
    public abstract void startElement(final String uri, final String localName,
            final String name, final Attributes atts) throws SAXException;


    /* For convenience implement the following methods empty...*/

    /**
     * {@inheritDoc}
     */
    @Override
    public void endPrefixMapping(final String prefix) throws SAXException {
        // TODO Auto-generated method stub

    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void ignorableWhitespace(final char[] ch, final int start,
            final int length) throws SAXException {
        // TODO Auto-generated method stub

    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void processingInstruction(final String target, final String data)
            throws SAXException {
        // TODO Auto-generated method stub

    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void setDocumentLocator(final Locator locator) {
        // TODO Auto-generated method stub

    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void skippedEntity(final String name) throws SAXException {
        // TODO Auto-generated method stub

    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void startDocument() throws SAXException {
        // TODO Auto-generated method stub

    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void startPrefixMapping(final String prefix, final String uri)
            throws SAXException {
        // TODO Auto-generated method stub

    }

    /**
     * Checks whether the PMML version is supported.
     *
     * @param version The PMML version to be tested.
     * @return True if the handler can read the specified version, false
     *      otherwise
     */
    public boolean canReadPMMLVersion(final String version) {
        return getSupportedVersions().contains(version);
    }

    /**
     * Returns the supported PMML versions. Override this method in a derived
     * class to change the set of supported versions. Versions are e.g.
     * {@link PMMLPortObject#PMML_V3_1} or {@link PMMLPortObject#PMML_V3_2}.
     *
     * @return A set of the PMML version supported by this PMML port object.
     */
    protected Set<String> getSupportedVersions() {
        TreeSet<String> versions = new TreeSet<String>();
        versions.add(PMMLPortObject.PMML_V3_0);
        versions.add(PMMLPortObject.PMML_V3_1);
        versions.add(PMMLPortObject.PMML_V3_2);
        versions.add(PMMLPortObject.PMML_V4_0);
        return versions;
    }

    /**
     * Returns the PMML version that this content handler prefers to write.
     * Override this method in a derived class to change the set preferred
     * version. Valid versions are defined in
     * {@link PMMLPortObject#getSupportedPMMLVersions()}.
     *
     * @return the preferred PMML version
     */
    protected String getPreferredWriteVersion() {
        return PMMLPortObject.PMML_V4_0;
    }

    /**
     * Parses the given node.
     *
     * @param node the node to be parsed
     * @throws SAXException if something with the parsing goes wrong
     */
    public void parse(final Node node) throws SAXException {
        SAXParserFactory fac = SAXParserFactory.newInstance();
        SAXParser parser;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = null;
        try {
            parser = fac.newSAXParser();

            Transformer t = TransformerFactory
                .newInstance().newTransformer();
            Source source = new DOMSource(node);
            t.transform(source, new StreamResult(out));
            in = new ByteArrayInputStream(out.toByteArray());
            parser.parse(new InputSource(in), this);
        } catch (ParserConfigurationException e) {
            throw new SAXException(e);
        } catch (TransformerConfigurationException e) {
            throw new SAXException(e);
        } catch (TransformerException e) {
            throw new SAXException(e);
        } catch (IOException e) {
            throw new SAXException(e);
        } finally {
            try {
                out.close();
            if (in != null) {
                in.close();
            }
            } catch (IOException e) {
               // ignore if closing the streams fail
            }
        }
    }

    /**
     * @param fragment the document fragment to add the model to
     * @param spec the pmml port object spec
     * @throws SAXException if the model cannot be added
     */
    public final void addPMMLModel(
            final DocumentFragment fragment, final PMMLPortObjectSpec spec)
            throws SAXException {
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

        /* Here the subclasses can insert the content by overriding the
         * addModelContent method.*/
        addPMMLModelContent(handler, spec);

        handler.endDocument();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        SAXSource s = new SAXSource(new InputSource(in));
        DOMResult r = new DOMResult(fragment);
        try {
            t.transform(s, r);
            in.close();
            out.close();
        } catch (Exception e) {
            throw new SAXException(e);
        }
    }

        /**
         * @param spec the pmml port object spec
         * @return an input stream containing the model fragment
         * @throws SAXException if the model cannot be added
         */
        public final ByteArrayInputStream getPMMLModelFragment(
                final PMMLPortObjectSpec spec)
                throws SAXException {
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

            /* Here the subclasses can insert the content by overriding the
             * addModelContent method.*/
            addPMMLModelContent(handler, spec);

            handler.endDocument();
            return new ByteArrayInputStream(out.toByteArray());
        }

    /**
     * Derived classes should override this method to add the model content.
     * They can assume that the document is started and will be ended and should
     * only provided the content starting with the mining scheme.
     * If they want to support the addition of LocalTransformations they have to
     * provide an empty <LocalTransformation></LocalTransformations> element
     * that can be filled later with preprocessing operations.
     *
     *
     * @param handler the transformer handler
     * @param spec the port object spec
     * @throws SAXException if the model cannot be added
     */
    @SuppressWarnings("unused")
    protected void addPMMLModelContent(final TransformerHandler handler,
            final PMMLPortObjectSpec spec) throws SAXException {
        // empty in the base class
    }

}
