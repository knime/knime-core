/*
 *
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
 * -------------------------------------------------------------------
 *
 * History
 *   Mar 16, 2010 (morent): created
 */
package org.knime.base.data.normalize;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.xml.transform.sax.TransformerHandler;

import org.knime.base.node.preproc.normalize.NormalizerNodeModel;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.pmml.PMMLContentHandler;
import org.knime.core.node.port.pmml.preproc.PMMLPreprocOperation;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Loads and saves the PMML preprocessing elements that are required by the 
 * {@link NormalizerNodeModel}.
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public class PMMLPreprocNormalization extends PMMLPreprocOperation {

    /** Name of the summary Extension element. */
    private static final String SUMMARY = "summary";
    private AffineTransConfiguration m_configuration;

    /**
     *
     */
    public PMMLPreprocNormalization() {
        // necessary for initialization with the load method
    }

    /**
     * @param config The affine configuration.
     */
    public PMMLPreprocNormalization(
            final AffineTransConfiguration config) {
        super();
        m_configuration = config;
    }

    /**
     * {@inheritDoc}
     *
     * @throws SAXException
     */
    @Override
    public void save(final TransformerHandler handler,
            final ExecutionMonitor exec) throws SAXException {
        AttributesImpl atts;
        int n = m_configuration.getNames().length;
        double execPart = 1.0 / n;
        for (int i = 0; i < n; i++) {
            if (exec != null) {
                exec.setProgress(execPart * i);
            }
            String field = m_configuration.getNames()[i];
            double scale = m_configuration.getScales()[i];
            double trans = m_configuration.getTranslations()[i];

            atts = new AttributesImpl();
            atts.addAttribute(null, null, OPTYPE, CDATA, OP_TYPE_CONTINOUS);
            atts.addAttribute(null, null, DATATYPE, CDATA, "double");
            handler.startElement(null, null, DERIVED_FIELD, atts);

            atts = new AttributesImpl();
            atts.addAttribute(null, null, FIELD, CDATA, field);
            handler.startElement(null, null, NORM_CONT, atts);

            atts = new AttributesImpl();
            atts.addAttribute(null, null, ORIG, CDATA, "0.0");
            atts.addAttribute(null, null, NORM, CDATA, Double.toString(trans));
            handler.startElement(null, null, LINEAR_NORM, atts);
            handler.endElement(null, null, LINEAR_NORM);

            atts = new AttributesImpl();
            atts.addAttribute(null, null, ORIG, CDATA, "1.0");
            atts.addAttribute(null, null, NORM, CDATA, Double.toString(scale
                    + trans));
            handler.startElement(null, null, LINEAR_NORM, atts);
            handler.endElement(null, null, LINEAR_NORM);

            handler.endElement(null, null, NORM_CONT);
            handler.endElement(null, null, DERIVED_FIELD);
        }
        atts = new AttributesImpl();
        atts.addAttribute(null, null, NAME, CDATA, SUMMARY);
        atts.addAttribute(null, null, VALUE, CDATA,
                m_configuration.getSummary());
        handler.startElement(null, null, EXTENSION, atts);
        handler.endElement(null, null, EXTENSION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "Linear Normalization";
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
    public PMMLContentHandler getHandlerForLoad() {
       return new PMMLPreprocessingContentHandler();
    }

    /**
     * Content handler for parsing linear normalization PMML fragments.
     * TODO: Replace this one with a more general content handler in 
     * org.knime.core that is also responsible for parsing imported PMML.
     *
     * @author Dominik Morent, KNIME.com, Zurich, Switzerland
     */
    class PMMLPreprocessingContentHandler extends PMMLContentHandler {
        // y = s * x + t
        private List<String> m_fields = new LinkedList<String>();
        private List<Double> m_scale = new LinkedList<Double>();
        private List<Double> m_trans = new LinkedList<Double>();
        private String m_summary;

        private int m_linNormCnt = 0;
        private double[] m_orig = new double[2];
        private double[] m_norm = new double[2];

        /**
         * {@inheritDoc}
         */
        @Override
        public void startElement(final String uri, final String localName,
                final String name, final Attributes atts) throws SAXException {
            if (name.equals(NORM_CONT)) {
                m_fields.add(atts.getValue(FIELD));
                m_linNormCnt = 0;
            } else if (name.equals(LINEAR_NORM)) {
                m_orig[m_linNormCnt] = Double.parseDouble(atts.getValue(ORIG));
                m_norm[m_linNormCnt] = Double.parseDouble(atts.getValue(NORM));
                m_linNormCnt++;
                if (m_linNormCnt > 2) {
                    throw new SAXException("Unexpected " + LINEAR_NORM
                            + " element encountered. Only two elements per "
                            + NORM_CONT + " are allowed.");
                }
            } else if (name.equals(EXTENSION) && atts.getValue(NAME) != null
                    && atts.getValue(NAME).equals(SUMMARY)) {
                m_summary = atts.getValue(VALUE);
            }

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void endElement(final String uri, final String localName,
                final String name) throws SAXException {
            if (name.equals(NORM_CONT)) {
                m_scale.add(m_norm[0]);
                m_trans.add(m_norm[1] - m_norm[0]);
                m_linNormCnt = 0;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void characters(final char[] ch, final int start,
                final int length) throws SAXException {
            // Ignore. No content is expected.
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void endDocument() throws SAXException {
            double[] nanArray = new double[m_fields.size()];
            Arrays.fill(nanArray, Double.NaN);
            double[] s = new double[m_scale.size()];
            double[] t = new double[m_trans.size()];
            for (int i = 0; i < t.length; i++) {
                s[i] = m_scale.get(i);
                t[i] = m_trans.get(i);
            }
            m_configuration = new AffineTransConfiguration(
                    m_fields.toArray(new String[m_fields.size()]), s, t,
                    nanArray, nanArray, m_summary);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLWriteElement getWriteElement() {
        return PMMLWriteElement.LOCALTRANS;
    }
}
