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

import static org.knime.core.node.port.pmml.preproc.PMMLTransformation.NormContinuous;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.xml.transform.sax.TransformerHandler;

import org.knime.base.node.preproc.normalize.NormalizerNodeModel;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLContentHandler;
import org.knime.core.node.port.pmml.preproc.PMMLPreprocOperation;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            PMMLPreprocNormalization.class);

    /** Name of the summary Extension element. */
    private static final String SUMMARY = "summary";
    private AffineTransConfiguration m_configuration;
    private static final int MAX_NUM_SEGMENTS = 2;

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

        atts = new AttributesImpl();
        atts.addAttribute(null, null, NAME, CDATA, SUMMARY);
        atts.addAttribute(null, null, VALUE, CDATA,
                m_configuration.getSummary());
        handler.startElement(null, null, EXTENSION, atts);
        handler.endElement(null, null, EXTENSION);

        int n = m_configuration.getNames().length;
        double execPart = 1.0 / n;
        for (int i = 0; i < n; i++) {
            if (exec != null) {
                exec.setProgress(execPart * i);
            }
            String field = m_configuration.getNames()[i];
            double scale = m_configuration.getScales()[i];
            double trans = m_configuration.getTranslations()[i];
         // TODO handle min, max extension for linear norm
//            double min = m_configuration.getMin()[i];
//            double max = m_configuration.getMax()[i];

            atts = new AttributesImpl();
            atts.addAttribute(null, null, OPTYPE, CDATA, OP_TYPE_CONTINOUS);
            atts.addAttribute(null, null, DATATYPE, CDATA, "double");
            handler.startElement(null, null, DERIVED_FIELD, atts);

            atts = new AttributesImpl();
            atts.addAttribute(null, null, FIELD, CDATA, field);
            handler.startElement(null, null, NormContinuous.toString() , atts);

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

            handler.endElement(null, null, NormContinuous.toString());
            handler.endElement(null, null, DERIVED_FIELD);
        }
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
     * @return the configuration
     */
    public AffineTransConfiguration getConfiguration() {
        return m_configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getColumnNames() {
        return Arrays.asList(m_configuration.getNames());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void parse(final Element transformElement) {
       PMMLNormalizationHandler handler
               = new PMMLNormalizationHandler(transformElement);
       m_configuration = handler.getAffineTransConfig();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLTransformElement getTransformElement() {
        return PMMLTransformElement.LOCALTRANS;
    }


    /**
     * Parses normalization transformation elements.
     * @author Dominik Morent, KNIME.com, Zurich, Switzerland
     *
     */
    class PMMLNormalizationHandler {
        private final List<String> m_fields;
        private final List<Double> m_scales;
        private final List<Double> m_translations;
        private String m_summary;
        private AffineTransConfiguration m_affineTrans;

        /**
         * Builds a new PMMLNormalizationHandler based on the transformation
         * element.
         *
         * @param transform an xml element containing a
         *      {@link PMMLTransformElement} to be parsed.
         */
        public PMMLNormalizationHandler(final Element transform) {
            m_fields = new ArrayList<String>();
            m_scales = new ArrayList<Double>();
            m_translations = new ArrayList<Double>();
            init(transform);
        }

        /**
         * @param transform
         */
        private void init(final Element transform) {
            /*
             * Try to read the summary which might be stored as first extension.
             */
            NodeList ext = transform.getElementsByTagName(EXTENSION);
            if (ext.getLength() > 0) {
                Attr summary =
                        (Attr)ext.item(0).getAttributes().getNamedItem(SUMMARY);
                if (summary != null) {
                    m_summary = summary.getValue();
                }
            }

           NodeList normalizations = transform.getElementsByTagName(
                   NormContinuous.toString());
           for (int i = 0; i < normalizations.getLength(); i++) {
               Node normalization = null;
               try {
                   normalization = normalizations.item(i);
                   String field = ((Attr)normalization.getAttributes()
                           .getNamedItem(FIELD)).getValue();
                   double[] orig = new double[MAX_NUM_SEGMENTS];
                   double[] norm = new double[MAX_NUM_SEGMENTS];

                   NodeList normChildren = normalization.getChildNodes();
                   int numSegments = 0;
                    for (int j = 0; j < normChildren.getLength(); j++) {
                        if (numSegments > 2) {
                            throw new IllegalArgumentException("Unexpected "
                                    + LINEAR_NORM + " element encountered. "
                                    + "Only two elements per "
                                    + NormContinuous + " are allowed.");
                        }
                        Node child = normChildren.item(j);
                        if (LINEAR_NORM.equalsIgnoreCase(child.getNodeName())) {
                            NamedNodeMap attr = child.getAttributes();
                            orig[j] =
                                    Double.parseDouble(((Attr)attr
                                            .getNamedItem(ORIG)).getValue());
                            norm[j] =
                                    Double.parseDouble(((Attr)attr
                                            .getNamedItem(NORM)).getValue());
                            // remove the operation from the document
                            normalization.removeChild(child);
                            numSegments++;
                        } else if (EXTENSION.equalsIgnoreCase(
                                child.getNodeName())) {
                            // TODO handle min, max extension for linear norm
                        }
                    }

                    double scale = (norm[1] - norm[0])/(orig[1] - orig[0]);
                    m_scales.add(scale);
                    m_translations.add(norm[0] - scale * orig[0]);
                    m_fields.add(field);
               } catch (Exception e) {
                   LOGGER.warn("Invalid input. Could not parse element "
                           + normalization.getNodeName() + ".", e);
               }
           }
        }

        /**
         * Builds a configuration object for a {@link AffineTransTable}.
         * @return the affine trans configuration
         */
        public AffineTransConfiguration getAffineTransConfig() {
            if (m_affineTrans  == null) {
                double[] nanArray = new double[m_fields.size()];
                Arrays.fill(nanArray, Double.NaN);
                double[] s = new double[m_scales.size()];
                double[] t = new double[m_translations.size()];
                for (int i = 0; i < t.length; i++) {
                    s[i] = m_scales.get(i);
                    t[i] = m_translations.get(i);
                }
                m_configuration = new AffineTransConfiguration(
                        m_fields.toArray(new String[m_fields.size()]), s, t,
                        nanArray, nanArray, m_summary);
                m_affineTrans =  new AffineTransConfiguration(
                        m_fields.toArray(new String[m_fields.size()]), s, t,
                        nanArray, nanArray, m_summary);
            }
            return m_affineTrans;
        }
    }


    /**
     * Content handler for parsing linear normalization PMML fragments.
     * TODO: Replace this one with a more general content handler in
     * org.knime.core that is also responsible for parsing imported PMML.
     *
     * This class is deprecated and will be replaced by a DOM parser.
     *
     * @author Dominik Morent, KNIME.com, Zurich, Switzerland
     */
    @Deprecated
    class PMMLPreprocessingContentHandler extends PMMLContentHandler {
        // y = s * x + t
        private final List<String> m_fields = new LinkedList<String>();
        private final List<Double> m_scale = new LinkedList<Double>();
        private final List<Double> m_trans = new LinkedList<Double>();
        private String m_summary;

        private int m_linNormCnt = 0;
        private final double[] m_orig = new double[2];
        private final double[] m_norm = new double[2];

        /**
         * {@inheritDoc}
         */
        @Override
        public void startElement(final String uri, final String localName,
                final String name, final Attributes atts) throws SAXException {
            if (NormContinuous.toString().equalsIgnoreCase(name)) {
                m_fields.add(atts.getValue(FIELD));
                m_linNormCnt = 0;
            } else if (name.equalsIgnoreCase(LINEAR_NORM)) {
                if (m_linNormCnt > 1) {
                    throw new SAXException("Unexpected " + LINEAR_NORM
                            + " element encountered. Only two elements per "
                            + NormContinuous + " are allowed.");
                }
                m_orig[m_linNormCnt] = Double.parseDouble(atts.getValue(ORIG));
                m_norm[m_linNormCnt] = Double.parseDouble(atts.getValue(NORM));
                m_linNormCnt++;
            } else if (name.equalsIgnoreCase(EXTENSION) && atts.getValue(NAME) != null
                    && atts.getValue(NAME).equalsIgnoreCase(SUMMARY)) {
                m_summary = atts.getValue(VALUE);
            }

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void endElement(final String uri, final String localName,
                final String name) throws SAXException {
            if (NormContinuous.toString().equalsIgnoreCase(name)) {
                double scale = (m_norm[1] - m_norm[0])/(m_orig[1] - m_orig[0]);
                m_scale.add(scale);
                m_trans.add(m_norm[0] - scale * m_orig[0]);
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
            // TODO handle min, max extension for linear norm
        }
    }
}
