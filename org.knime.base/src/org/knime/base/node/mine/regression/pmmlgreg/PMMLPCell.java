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
 * Encapsulates a PCell in PMML General Regression.
 *
 * @author Heiko Hofer
 */
public final class PMMLPCell {
    private String m_parameterName;
    private double m_beta;
    private Integer m_df;
    private String m_targetCategory;

    /**
     * @param parameterName the name of the parameter
     * @param beta parameter value
     */
    public PMMLPCell(final String parameterName, final double beta) {
        this(parameterName, beta, null);
    }

    /**
     * @param parameterName the name of the parameter
     * @param beta parameter value
     * @param targetCategory the optional target category
     */
    public PMMLPCell(final String parameterName, final double beta,
            final String targetCategory) {
        m_parameterName = parameterName;
        m_beta = beta;
        m_targetCategory = targetCategory;
    }

    /**
     * @param parameterName the name of the parameter
     * @param beta parameter value
     * @param df degrees of freedom
     */
    public PMMLPCell(final String parameterName, final double beta,
            final int df) {
        this(parameterName, beta, df, null);
    }

    /**
     * @param parameterName the name of the parameter
     * @param beta parameter value
     * @param df degrees of freedom
     * @param targetCategory the optional target category
     */
    public PMMLPCell(final String parameterName, final double beta,
            final int df,
            final String targetCategory) {
        m_parameterName = parameterName;
        m_beta = beta;
        m_df = df;
        m_targetCategory = targetCategory;
    }

    /**
     * @return the parameterName
     */
    public String getParameterName() {
        return m_parameterName;
    }

    /**
     * @return the beta
     */
    public double getBeta() {
        return m_beta;
    }

    /**
     * @return the df
     */
    public Integer getDf() {
        return m_df;
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
    void writePMML(final TransformerHandler handler)
    throws SAXException {
        AttributesImpl a = new AttributesImpl();
        if (null != m_targetCategory) {
            a.addAttribute("",  "", "targetCategory", "CDATA",
                    m_targetCategory);
        }
        a.addAttribute("", "", "parameterName", "CDATA", m_parameterName);
        a.addAttribute("", "", "beta", "CDATA", Double.toString(m_beta));
        if (null != m_df) {
            a.addAttribute("",  "", "df", "CDATA", m_df.toString());
        }
        handler.startElement("", "", "PCell", a);
        handler.endElement("", "", "PCell");
    }

}
