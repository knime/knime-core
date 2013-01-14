/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   27.04.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.pmmlgreg;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Encapsulates a PPCell in PMML General Regression.
 *
 * @author Heiko Hofer
 */
public final class PMMLPPCell {
    private String m_value;
    private String m_predictorName;
    private String m_parameterName;
    private String m_targetCategory;

    /**
     * @param value The value
     * @param predictorName The name of the predictor
     * @param parameterName The name of the parameter
     */
    public PMMLPPCell(final String value, final String predictorName,
            final String parameterName) {
        this(value, predictorName, parameterName, null);
    }

    /**
     * @param value The value
     * @param predictorName The name of the predictor
     * @param parameterName The name of the parameter
     * @param targetCategory The optional target category
     */
    public PMMLPPCell(final String value, final String predictorName,
            final String parameterName, final String targetCategory) {
        m_value = value;
        m_predictorName = predictorName;
        m_parameterName = parameterName;
        m_targetCategory = targetCategory;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return m_value;
    }

    /**
     * @return the predictorName
     */
    public String getPredictorName() {
        return m_predictorName;
    }

    /**
     * @return the parameterName
     */
    public String getParameterName() {
        return m_parameterName;
    }

    /**
     * @return the targetCategory
     */
    public String getTargetCategory() {
        return m_targetCategory;
    }

    /**
     * Writes the PMML to the given handler.
     *
     * @param handler the transformation handler
     * @throws SAXException if anything goes wrong while serializing the model
     */
    void writePMML(final TransformerHandler handler) throws SAXException {
        AttributesImpl a = new AttributesImpl();
        a.addAttribute("", "", "value", "CDATA", m_value);
        a.addAttribute("", "", "predictorName", "CDATA", m_predictorName);
        a.addAttribute("", "", "parameterName", "CDATA", m_parameterName);
        if (null != m_targetCategory) {
            a.addAttribute("",  "", "targetCategory", "CDATA",
                    m_targetCategory);
        }
        handler.startElement("", "", "PPCell", a);
        handler.endElement("", "", "PPCell");
    }

}
