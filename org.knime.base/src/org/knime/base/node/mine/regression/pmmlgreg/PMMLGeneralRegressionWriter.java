/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   28.04.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.pmmlgreg;

import javax.xml.transform.sax.TransformerHandler;

import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author Heiko Hofer
 */
final class PMMLGeneralRegressionWriter {
    private PMMLGeneralRegressionPortObject m_port;
    private PMMLPortObjectSpec m_spec;


    private String m_writeVersion;

    private PMMLGeneralRegressionContent m_content;

    /**
     * Creates a new PMML content handler for general regression models based on
     * an existing port object.
     *
     * @param port a PMML general regression port object
     * @param content the content to write
     * @param writeVersion the PMML version to write
     */
    PMMLGeneralRegressionWriter(
            final PMMLGeneralRegressionPortObject port,
            final PMMLGeneralRegressionContent content,
            final String writeVersion) {
        m_writeVersion = writeVersion;
        m_port = port;
        m_spec = port.getSpec();
        m_content = content;
    }

    /**
     * Writes the PMML general regression model to the given handler.
     *
     * @param handler a transform handler
     * @throws SAXException if anything goes wrong while serializing the model
     */
    void writePMMLGeneralRegressionModel(
            final TransformerHandler handler)
            throws SAXException {
        // Basic attributes of the GeneralRegressionModel
        AttributesImpl a = new AttributesImpl();
        // PMML 3.1: The attribute targetVariableName is now optional, usage is
        // deprecated. It is for information only, anyway.
        // Use usageType="predicted" in MiningField instead
        a.addAttribute("", "", "targetVariableName", "CDATA", getTargetField());
        // PMML 3.1: One of: regression, gerneralLinear, multinomialLogistic or
        // ordinalMultinomial
        a.addAttribute("", "", "modelType", "CDATA",
                m_content.getModelType().toString());
        // PMML 3.1: Either regression or classification
        a.addAttribute("", "", "functionName", "CDATA",
                m_content.getFunctionName().toString());

        handler.startElement("", "", "GeneralRegressionModel", a);
        PMMLPortObjectSpec.writeMiningSchema(m_spec, handler, m_writeVersion);
        m_port.writeLocalTransformations(handler);
        addParameterList(handler);
        addFactorList(handler);
        addCovariateList(handler);
        addPPMatrix(handler);
        addPCovMatrix(handler);
        addParamMatrix(handler);
        handler.endElement("", "", "GeneralRegressionModel");
    }

    /**
     * Writes the PMML general regression model to the given handler.
     *
     * @param handler a transform handler
     * @throws SAXException if anything goes wrong while serializing the model
     */
    private void addParameterList(final TransformerHandler handler)
            throws SAXException {
        if (null == m_content.getParameterList()) {
            return;
        }
        // PMML 3.1: Lists all Parameters. Each Parameter contains a
        // required name, and optional label. Parameter names should be unique
        // within the model and as brief as possible (since Parameter names
        // appear frequently in the document). The label, if present, is meant
        // to give a hint on a Parameter's correlation with the Predictors.
        handler.startElement("", "", "ParameterList", new AttributesImpl());
        for (PMMLParameter p : m_content.getParameterList()) {
            p.writePMML(handler);
        }
        handler.endElement("", "", "ParameterList");
    }

    /**
     * Writes the FactorList PMML general regression model to the given handler.
     * Optional in PMML 3.1
     *
     * @param handler a transform handler
     * @throws SAXException if anything goes wrong while serializing the model
     */
    private void addFactorList(final TransformerHandler handler)
            throws SAXException {
        if (null == m_content.getFactorList()) {
            return;
        }
        // PMML 3.1: List of factor names. Not present if this particular
        // regression flavor does not support factors (ex. linear regression).
        // If present, the list may or may not be empty. Each name in the list
        // must match a DataField name or a DerivedField name. The factors are
        // assumed to be categorical variables.
        handler.startElement("", "", "FactorList", new AttributesImpl());
        for (PMMLPredictor p : m_content.getFactorList()) {
            p.writePMML(handler);
        }
        handler.endElement("", "", "FactorList");
    }

    /**
     * Writes the CovariateList PMML general regression model to the given
     * handler.
     * Optional in PMML 3.1
     *
     * @param handler a transform handler
     * @throws SAXException if anything goes wrong while serializing the model
     */
    private void addCovariateList(final TransformerHandler handler)
            throws SAXException {
        if (null == m_content.getCovariateList()) {
            return;
        }
        // PMML 3.1: List of covariate names. Will not be present when there is
        // no covariate. Each name in the list must match a DataField name or a
        // DerivedField name. The covariates will be treated as continuous
        // variables.
        handler.startElement("", "", "CovariateList", new AttributesImpl());
        for (PMMLPredictor p : m_content.getCovariateList()) {
            p.writePMML(handler);
        }
        handler.endElement("", "", "CovariateList");
    }

    /**
     * Writes the PPMatrix of aPMML general regression model to the given
     * handler.
     * Required since PMML 3.1
     *
     * @param handler a transform handler
     * @throws SAXException if anything goes wrong while serializing the model
     */
    private void addPPMatrix(final TransformerHandler handler)
            throws SAXException {
        if (null == m_content.getPPMatrix()) {
            return;
        }
        // PMML 3.1: Predictor-to-Parameter correlation matrix. It is a
        // rectangular matrix having a column for each Predictor (factor or
        // covariate) and a row for each Parameter. The matrix is represented as
        // a sequence of cells, each cell containing a number representing the
        // correlation between the Predictor and the Parameter.
        handler.startElement("", "", "PPMatrix", new AttributesImpl());
        for (PMMLPPCell p : m_content.getPPMatrix()) {
            p.writePMML(handler);
        }
        handler.endElement("", "", "PPMatrix");
    }

    /**
     * Writes the PCovMatrix of a PMML general regression model to the given
     * handler.
     * Required in PMML 3.1
     *
     * @param handler a transform handler
     * @throws SAXException if anything goes wrong while serializing the model
     */
    private void addPCovMatrix(final TransformerHandler handler)
            throws SAXException {
        if (null == m_content.getPCovMatrix()) {
            return;
        }
        // PMML 3.1: matrix of Parameter estimate covariances. Made up of
        // PCovCells, each of them being located via row information for
        // Parameter name (pRow), row information for target variable value
        // (tRow), column information for Parameter name (pCol) and column
        // information for target variable value (tCol). Note that the matrix
        // is symmetric with respect to the main diagonal (interchanging tRow
        // and tCol together with pRow and pCol will not change the value).
        // Therefore it is sufficient that only half of the matrix be exported.
        // Attributes tRow and tCol are optional since they are not needed for
        // linear regression models.
        handler.startElement("", "", "PCovMatrix", new AttributesImpl());
        for (PMMLPCovCell p : m_content.getPCovMatrix()) {
            p.writePMML(handler);
        }
        handler.endElement("", "", "PCovMatrix");
    }

    /**
     * Writes the ParamMatrix of a PMML general regression model to the given
     * handler.
     * Required in PMML 3.1
     *
     * @param handler a transform handler
     * @throws SAXException if anything goes wrong while serializing the model
     */
    private void addParamMatrix(final TransformerHandler handler)
            throws SAXException {
        if (null == m_content.getParamMatrix()) {
            return;
        }
        // PMML 3.1: Parameter matrix. A table containing the Parameter values
        // along with associated statistics (degrees of freedom). One dimension
        // has the target variable's categories, the other has the Parameter
        // names. The table is represented by specifying each cell. There is no
        // requirement for Parameter names other than that each name should
        // uniquely identify one Parameter.
        handler.startElement("", "", "ParamMatrix", new AttributesImpl());
        for (PMMLPCell p : m_content.getParamMatrix()) {
            p.writePMML(handler);
        }
        handler.endElement("", "", "ParamMatrix");
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
}
