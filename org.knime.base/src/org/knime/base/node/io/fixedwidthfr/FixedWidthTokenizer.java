/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   15.10.2014 (tibuch): created
 */
package org.knime.base.node.io.fixedwidthfr;

import java.io.IOException;

import org.knime.base.node.util.BufferedFileReader;
import org.knime.core.node.NodeLogger;

/**
 *
 * @author Tim-Oliver Buchholz
 */
public class FixedWidthTokenizer {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FixedWidthTokenizer.class);

    private BufferedFileReader m_inputStream;

    private FixedWidthFRSettings m_nodeSettings;

    private int[] m_colWidths;

    private int m_numberOfColumns;

    private int m_currentCol;

    private boolean[] m_includes;

    private boolean m_pushedBack;

    private String m_lastToken;

    private boolean m_reachedEndOfLine;

    private String m_line;

    private int m_currentLine;

    /**
     * @param inStream a BufferedFileReader
     * @param nodeSettings the current node settings
     */
    public FixedWidthTokenizer(final BufferedFileReader inStream, final FixedWidthFRSettings nodeSettings) {

        m_inputStream = inStream;

        m_nodeSettings = nodeSettings;

        m_colWidths = m_nodeSettings.getColWidths();

        m_currentCol = 0;

        m_pushedBack = false;

        m_numberOfColumns = m_colWidths.length;

        m_reachedEndOfLine = false;

        m_includes = m_nodeSettings.getIncludes();
        m_includes[0] = m_nodeSettings.getHasRowHeader() ? true : m_includes[0];

        try {
            m_currentLine = 1;
            m_line = m_inputStream.readLine();
            if (m_line == null) {
                m_lastToken = null;
                // return EOF asap
                m_pushedBack = true;
            }
        } catch (IOException e) {
            LOGGER.error("Can't read from file '" + m_nodeSettings.getFileLocation().toString() + "'.", e);
        }
    }

    /**
     * @return next Token
     */
    public String nextToken() {

        if (m_pushedBack) {
            m_pushedBack = false;
            return m_lastToken;
        }

        try {
            if (m_currentLine == 1 && m_nodeSettings.getHasColHeaders()) {
                // skip first row if we have column headers
                m_line = m_inputStream.readLine();
                m_currentLine++;
                if (m_line == null) {
                    // EOF
                    return null;
                }
            }
        } catch (IOException e1) {
            LOGGER.error("Can't read from file '" + m_nodeSettings.getFileLocation().toString() + "'.", e1);
        }

        boolean include;
        int tokenLength;

        do {
            m_lastToken = null;

            if (m_currentCol == m_numberOfColumns) {
                m_currentCol = 0;
                try {
                    m_currentLine++;
                    m_line = m_inputStream.readLine();
                    m_reachedEndOfLine = false;
                    if (m_line == null) {
                        // EOF
                        return null;
                    }
                } catch (IOException e) {
                    LOGGER.error("Can't read from file '" + m_nodeSettings.getFileLocation().toString() + "'.", e);
                }
            }

            include = m_includes[m_currentCol];
            tokenLength = m_colWidths[m_currentCol++];

            if (tokenLength < m_line.length()) {
                m_lastToken = m_line.substring(0, tokenLength);
                m_line = m_line.substring(tokenLength, m_line.length());
            } else if (m_line.length() > 0) {
                m_lastToken = m_line;
                m_line = "";
            } else {
                m_lastToken = m_line;
                m_reachedEndOfLine = true;
            }

        } while (!include);
        return m_lastToken;
    }

    /**
     * @return reached end of current line
     */
    public boolean getReachedEndOfLine() {
        return m_reachedEndOfLine;
    }

    /**
     * push the last read token back.
     */
    public void pushBack() {
        m_pushedBack = true;

    }

    /**
     * @return number of the current line. (-1 if the stream has been closed)
     */
    public int getLineNumber() {
            return m_currentLine;
    }
}
