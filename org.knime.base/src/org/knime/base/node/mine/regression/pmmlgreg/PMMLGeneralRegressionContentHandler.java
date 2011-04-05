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
 *   04.02.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.pmmlgreg;

import java.util.Stack;

import javax.xml.transform.sax.TransformerHandler;

import org.knime.base.node.mine.regression.PMMLRegressionContentHandler;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionContent.FunctionName;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionContent.ModelType;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLContentHandler;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 *
 * @author Heiko Hofer
 */
public final class PMMLGeneralRegressionContentHandler extends PMMLContentHandler {
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(PMMLRegressionContentHandler.class);

    private final Stack<PMMLContentHandler> m_contentHandlerStack =
        new Stack<PMMLContentHandler>();

    private final PMMLPortObjectSpec m_spec;

    private final PMMLGeneralRegressionContent m_content;

    /**
     * Creates a new PMML content handler for general regression models.
     *
     * @param spec the spec for the regression model
     */
    public PMMLGeneralRegressionContentHandler(final PMMLPortObjectSpec spec) {
       this(spec, new PMMLGeneralRegressionContent());
    }

    /**
     * Creates a new PMML content handler for general regression models.
     *
     * @param spec the spec for the regression model
     * @param content the regression content for the model
     */
    public PMMLGeneralRegressionContentHandler(final PMMLPortObjectSpec spec,
            final PMMLGeneralRegressionContent content) {
        m_spec = spec;
        m_content = content;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(final char[] ch, final int start, final int length)
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
            final String name, final Attributes atts) throws SAXException {
        if (!m_contentHandlerStack.isEmpty()) {
            m_contentHandlerStack.peek().startElement(
                    uri, localName, name, atts);
        } else if ("MiningField".equals(name)) {
            // Test here for correct target field which is the one with
            // attribute: usageType="predicted"
            String usageType = atts.getValue("usageType");
            if (usageType.equals("predicted")) {
                String targetFieldName = atts.getValue("name");
                checkTargetField(targetFieldName);
            }
        } else if ("GeneralRegressionModel".equals(name)) {
            assert m_contentHandlerStack.isEmpty();
            // PMML 3.1: The attribute targetVariableName is now optional,
            // usage is deprecated. It is for information only, anyway.
            // Use usageType="predicted" in MiningField instead
            String targetFieldName = atts.getValue("targetVariableName");
            checkTargetField(targetFieldName);

            // PMML 3.1: One of: regression, gerneralLinear, multinomialLogistic
            // or ordinalMultinomial
            m_content.setModelType(
                    ModelType.valueOf(atts.getValue("modelType")));
            if (!m_content.getModelType().equals(
                    ModelType.multinomialLogistic)) {
                throw new SAXException("The ModelType: "
                        + m_content.getModelType()
                        + " is currently not supported.");
            }

            // PMML 3.1: Either regression or classification
            m_content.setFunctionName(
                    FunctionName.valueOf(atts.getValue("functionName")));
            if (!m_content.getFunctionName().equals(
                    FunctionName.classification)) {
                throw new SAXException("The FunctionName: "
                        + m_content.getFunctionName()
                        + " is currently not supported.");
            }
            m_content.setAlgorithmName(atts.getValue("algorithmName"));
            m_content.setModelName(atts.getValue("modelName"));
            if (null != atts.getValue("cumulativeLink")) {
                throw new SAXException("The attribute \"cumulativeLink\""
                        + " is currently not supported.");
            }
        } else if ("MiningSchema".equals(name)) {
            m_contentHandlerStack.push(new PMMLMiningSchemaHandler(
                            m_contentHandlerStack));
        } else if ("ParameterList".equals(name)) {
            m_contentHandlerStack.push(new PMMLParameterListHandler(
                    m_contentHandlerStack, m_content));
        } else if ("FactorList".equals(name)) {
            m_contentHandlerStack.push(new PMMLFactorListHandler(
                    m_contentHandlerStack, m_content));
        } else if ("CovariateList".equals(name)) {
            m_contentHandlerStack.push(new PMMLCovariateListHandler(
                    m_contentHandlerStack, m_content));
        } else if ("PPMatrix".equals(name)) {
            m_contentHandlerStack.push(new PMMLPPMatrixHandler(
                    m_contentHandlerStack, m_content));
        } else if ("PCovMatrix".equals(name)) {
            m_contentHandlerStack.push(new PMMLPCovMatrixHandler(
                    m_contentHandlerStack, m_content));
        } else if ("ParamMatrix".equals(name)) {
            m_contentHandlerStack.push(new PMMLParamMatrixHandler(
                    m_contentHandlerStack, m_content));
        } else {
            // ignored
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String localName,
            final String name) throws SAXException {
        if (!m_contentHandlerStack.isEmpty()) {
            m_contentHandlerStack.peek().endElement(uri, localName, name);
        } else if ("GeneralRegressionModel".equals(name)) {
            assert m_contentHandlerStack.isEmpty();
        } else if ("MiningSchema".equals(name)) {
            assert false : "MiningSchema must be terminated elsewhere";
        } else if ("ParmeterList".equals(name)) {
            assert false : "ParmeterList must be terminated elsewhere";
        } else if ("FactorList".equals(name)) {
            assert false : "FactorList must be terminated elsewhere";
        } else if ("CovariateList".equals(name)) {
            assert false : "CovariateList must be terminated elsewhere";
        } else if ("PPMatrix".equals(name)) {
            assert false : "PPMatrix must be terminated elsewhere";
        } else if ("PPCovMatrix".equals(name)) {
            assert false : "PPCovMatrix must be terminated elsewhere";
        } else if ("ParamMatrix".equals(name)) {
            assert false : "RegressionTable must be terminated elsewhere";
        } else {
            // ignored
        }
    }


    /**
     * Checks if the given target field name exists in this model.
     *
     * @param targetFieldName a target field name
     */
    private void checkTargetField(final String targetFieldName) {
        if (!getTargetField().equals(targetFieldName)) {
            LOGGER.warn("Non matching target field name or no target "
                    + "field in mining schema");
        }
    }


    /**
     * Get the response column name as said in the spec or "Response" if none is
     * set.
     *
     * @return target field name.
     */
    private String getTargetField() {
        for (String s : m_spec.getTargetFields()) {
            return s;
        }
        return "Response";
    }

    /**
     * @return the content
     */
    public PMMLGeneralRegressionContent getContent() {
        return m_content;
    }

    /**
     * Checks if read content is valid.
     * If not an {@link IllegalStateException} is thrown.
     *
     * @throws IllegalStateException if required information is missing
     */
    void checkValidity() {
        if (m_content == null) {
            throw new IllegalStateException("No content set.");
        }
        // TODO: Add more validation checks.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addPMMLModelContent(final TransformerHandler handler,
            final PMMLPortObjectSpec spec)
            throws SAXException {
      // TODO Null pointer checks

        PMMLGeneralRegressionWriter writer =
            new PMMLGeneralRegressionWriter(spec, this);
        writer.writePMMLGeneralRegressionModel(handler);
    }

    /**
     * @return the targetVariableName
     */
    public String getTargetVariableName() {
        for (String s : m_spec.getTargetFields()) {
            return s;
        }
        return "Response";
    }

    /**
     * @return the spec
     */
    public PMMLPortObjectSpec getSpec() {
        return m_spec;
    }
}
