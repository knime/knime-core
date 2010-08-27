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
 *   27.04.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.pmmlgreg;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Encapsulates a PCovCell in PMML General Regression.
 *
 * @author Heiko Hofer
 */
public final class PMMLPCovCell {
    private String m_pRow;
    private String m_pCol;
    private String m_tRow;
    private String m_tCol;
    private double m_value;
    private String m_targetCategory;

    /**
     * @param pRow row for parameter name
     * @param pCol column for parameter name
     * @param value the value
     */
    public PMMLPCovCell(final String pRow, final String pCol,
            final double value) {
        this(pRow, pCol, value, null);
    }

    /**
     * @param pRow row for parameter name
     * @param pCol column for parameter name
     * @param value the value
     * @param targetCategory the optional target category
     */
    public PMMLPCovCell(final String pRow, final String pCol,
            final double value, final String targetCategory) {
        this(pRow, pCol, null, null, value, targetCategory);
    }

    /**
     * @param pRow row for parameter name
     * @param pCol column for parameter name
     * @param tRow row for target name
     * @param tCol column for target name
     * @param value the value
     */
    public PMMLPCovCell(final String pRow, final String pCol,
            final String tRow, final String tCol,
            final double value) {
        this(pRow, pCol, tRow, tCol, value, null);
    }


    /**
     * @param pRow row for parameter name
     * @param pCol column for parameter name
     * @param tRow row for target name
     * @param tCol column for target name
     * @param value the value
     * @param targetCategory the optional target category
     */
    public PMMLPCovCell(final String pRow, final String pCol,
            final String tRow, final String tCol,
            final double value, final String targetCategory) {
        m_pRow = pRow;
        m_pCol = pCol;
        m_tRow = tRow;
        m_tCol = tCol;
        m_value = value;
        m_targetCategory = targetCategory;
    }

    /**
     * @return the pRow
     */
    public String getPRow() {
        return m_pRow;
    }

    /**
     * @return the pCol
     */
    public String getPCol() {
        return m_pCol;
    }

    /**
     * @return the tRow
     */
    public String getTRow() {
        return m_tRow;
    }

    /**
     * @return the tCol
     */
    public String getTCol() {
        return m_tCol;
    }

    /**
     * @return the value
     */
    public double getValue() {
        return m_value;
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
        a.addAttribute("", "", "pRow", "CDATA", m_pRow);
        a.addAttribute("", "", "pCol", "CDATA", m_pCol);
        if (null != m_tRow || null != m_tCol) {
            a.addAttribute("", "", "tRow", "CDATA", m_tRow);
            a.addAttribute("", "", "tCol", "CDATA", m_tCol);
        }
        a.addAttribute("", "", "value", "CDATA", Double.toString(m_value));
        if (null != m_targetCategory) {
            a.addAttribute("",  "", "df", "CDATA", m_targetCategory);
        }
        handler.startElement("", "", "PCovCell", a);
        handler.endElement("", "", "PCovCell");
    }
}
