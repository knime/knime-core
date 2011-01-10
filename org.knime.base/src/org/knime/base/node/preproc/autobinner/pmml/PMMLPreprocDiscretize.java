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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   22.06.2010 (hofer): created
 */
package org.knime.base.node.preproc.autobinner.pmml;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.xml.transform.sax.TransformerHandler;

import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.pmml.PMMLContentHandler;
import org.knime.core.node.port.pmml.preproc.PMMLPreprocOperation;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author Heiko Hofer
 */
public final class PMMLPreprocDiscretize extends PMMLPreprocOperation {
    /** Name of the summary Extension element. */
    private static final String SUMMARY = "summary";
    private DisretizeConfiguration m_configuration;

    /** Used in load method. */
    public PMMLPreprocDiscretize() {
        // necessary for initialization with the load method
        m_configuration = new DisretizeConfiguration();
    }

    /**
     * @param config The discretize configuration.
     */
    public PMMLPreprocDiscretize(
            final DisretizeConfiguration config) {
        super();
        m_configuration = config;
    }

    /**
     * @return the configuration
     */
    public DisretizeConfiguration getConfiguration() {
        return m_configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLContentHandler getHandlerForLoad() {
        return new PMMLPreprocDiscretizeContentHandler(m_configuration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "Discretization";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSummary() {
        return m_configuration.getSummary();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLWriteElement getWriteElement() {
        return PMMLWriteElement.LOCALTRANS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final TransformerHandler handler,
            final ExecutionMonitor executionMonitor) throws SAXException {
        List<String> names = m_configuration.getNames();
        for (String name : names) {
            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, null, NAME, CDATA, name);
            atts.addAttribute(null, null, OPTYPE, CDATA, "categorical");
            atts.addAttribute(null, null, DATATYPE, CDATA, "string");
            handler.startElement(null, null, DERIVED_FIELD, atts);
            PMMLDiscretize discretize = m_configuration.getDiscretize(name);
            discretize.writePMML(handler);
            handler.endElement(null, null, DERIVED_FIELD);
        }
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, null, NAME, CDATA, SUMMARY);
        atts.addAttribute(null, null, VALUE, CDATA,
                m_configuration.getSummary());
        handler.startElement(null, null, EXTENSION, atts);
        handler.endElement(null, null, EXTENSION);
    }

    /**
    *
    * @author Heiko Hofer
    */
   final static class PMMLPreprocDiscretizeContentHandler
           extends PMMLContentHandler {
       private DisretizeConfiguration m_configuration;
       private List<PMMLDiscretize> m_list;
       private Stack<PMMLContentHandler> m_contentHandlerStack =
           new Stack<PMMLContentHandler>();
       private String m_currName;

       /**
        * @param configuration The configuration to store data
        */
       public PMMLPreprocDiscretizeContentHandler(
               final DisretizeConfiguration configuration) {
           m_configuration = configuration;
       }
       /**
        * {@inheritDoc}
        */
       @Override
       public void characters(final char[] ch, final int start,
               final int length)
               throws SAXException {
           // Not needed.
       }

       /**
        * {@inheritDoc}
        */
       @Override
       public void endDocument() throws SAXException {
           // Not needed.
       }

       /**
        * {@inheritDoc}
        */
       @Override
       public void startElement(final String uri, final String localName,
               final String name,
               final Attributes atts) throws SAXException {
           if (!m_contentHandlerStack.isEmpty()) {
               m_contentHandlerStack.peek().startElement(
                       uri, localName, name, atts);
           } else if ("DerivedField".equals(name)) {
               assert m_contentHandlerStack.isEmpty();
               m_currName = atts.getValue(NAME);
               m_configuration.addName(m_currName);
           } else if ("Discretize".equals(name)) {
               m_list = new ArrayList<PMMLDiscretize>();
               m_contentHandlerStack.push(new PMMLDiscretizeContentHandler(
                       m_contentHandlerStack, atts, m_list));
           } else if (name.equals(EXTENSION) && atts.getValue(NAME) != null
                   && atts.getValue(NAME).equals(SUMMARY)) {
               m_configuration.setSummary(atts.getValue(VALUE));
           }
       }

       /**
        * {@inheritDoc}
        */
       @Override
       public void endElement(final String uri, final String localName,
               final String name)
               throws SAXException {
           if (!m_contentHandlerStack.isEmpty()) {
               m_contentHandlerStack.peek().endElement(uri, localName, name);
           } else if ("DerivedField".equals(name)) {
               assert m_contentHandlerStack.isEmpty();
               assert m_list.size() <= 1 : "More than one \"Discretize\" "
                   + "is not supported";
               // end of this element (and its life cycle)
               m_configuration.setDiscretize(m_currName, m_list.get(0));
           }
       }

   }

}
