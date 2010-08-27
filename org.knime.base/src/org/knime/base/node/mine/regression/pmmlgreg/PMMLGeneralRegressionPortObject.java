/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.sax.TransformerHandler;

import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLModelType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.xml.sax.SAXException;

/**
 * This class wraps a PMML general regression model that can then be
 * transferred from one node to the other.
 *
 * @author Heiko Hofer
 */
public class PMMLGeneralRegressionPortObject extends PMMLPortObject {
    private PMMLGeneralRegressionContent m_content;

    /** The port object's type. */
    @SuppressWarnings("hiding")
    public static final PortType TYPE =
            new PortType(PMMLGeneralRegressionPortObject.class);

    /**
     * This class must have an empty constructor, see documentation of
     * default constructor of <code>PMMLPortObject</code>.
     */
    public PMMLGeneralRegressionPortObject() {
        // do nothing.
    }

    /**
     * Creates a new PMML port object for logistic regression.
     *
     * @param spec the objects spec
     * @param content the content handler that receives SAX parsing events upon
     * reading a PMML model
     */
    public PMMLGeneralRegressionPortObject(final PMMLPortObjectSpec spec,
            final PMMLGeneralRegressionContent content) {
        super(spec, PMMLModelType.GeneralRegressionModel);
        m_content = content;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSummary() {
        StringBuilder b = new StringBuilder();
        b.append("Logistic Regression on \"");
        b.append(getTargetVariableName());
        b.append("\"");
        return b.toString();
    }


    /**
     * The content which stores information about the general regression model.
     *
     * @return the content
     */
    public PMMLGeneralRegressionContent getContent() {
        return m_content;
    }

    /**
     * @return the targetVariableName
     */
    public String getTargetVariableName() {
        for (String s : getSpec().getTargetFields()) {
            return s;
        }
        return "Response";
    }


    /** {@inheritDoc} */
    @Override
    public void loadFrom(final PMMLPortObjectSpec spec,
            final InputStream stream, final String version)
            throws ParserConfigurationException, SAXException, IOException {

        PMMLGeneralRegressionContentHandler hdl =
                new PMMLGeneralRegressionContentHandler(spec);
        super.addPMMLContentHandler("GeneralRegressionModel", hdl);
        super.loadFrom(spec, stream, version);
        try {
            hdl.checkValidity();
        } catch (IllegalStateException e) {
            throw new SAXException("Incomplete general regression model: "
                    + e.getMessage());
        }
        m_content = hdl.getContent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writePMMLModel(final TransformerHandler handler)
            throws SAXException {
        PMMLGeneralRegressionWriter writer =
            new PMMLGeneralRegressionWriter(this, m_content,
                    getWriteVersion());
        writer.writePMMLGeneralRegressionModel(handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeLocalTransformations(final TransformerHandler handler)
            throws SAXException {
        super.writeLocalTransformations(handler);
    }

}
