/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jan 21, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public final class TreeMetaData {

    private final TreeAttributeColumnMetaData[] m_attributesMetaData;

    private final TreeTargetColumnMetaData m_targetMetaData;

    /**
     * @param attributesMetaData
     * @param targetMetaData
     */
    TreeMetaData(final TreeAttributeColumnMetaData[] attributesMetaData, final TreeTargetColumnMetaData targetMetaData) {
        m_attributesMetaData = attributesMetaData;
        for (int i = 0; i < attributesMetaData.length; i++) {
            final TreeAttributeColumnMetaData t = attributesMetaData[i];
            final int tIndex = t.getAttributeIndex();
            if (tIndex != i) {
                throw new IllegalArgumentException("Attribute \"" + t.getAttributeName()
                    + "\" does not have correct index " + i + " but " + tIndex);
            }
        }
        m_targetMetaData = targetMetaData;
    }

    /** @param index
     * @return the attributesMetaData */
    public TreeAttributeColumnMetaData getAttributeMetaData(final int index) {
        return m_attributesMetaData[index];
    }

    /**
     * @return number of attributes
     */
    public int getNrAttributes() {
        return m_attributesMetaData.length;
    }

    /** @return the targetMetaData */
    public TreeTargetColumnMetaData getTargetMetaData() {
        return m_targetMetaData;
    }

    /**
     * @return true if regression model else false
     */
    public boolean isRegression() {
        return m_targetMetaData instanceof TreeTargetNumericColumnMetaData;
    }

    /**
     * @param output
     * @throws IOException
     */
    public void save(final DataOutputStream output) throws IOException {
        output.writeInt(m_attributesMetaData.length);
        for (TreeAttributeColumnMetaData a : m_attributesMetaData) {
            try {
                a.save(output);
            } catch (IOException ioe) {
                throw new IOException("Failed saving meta data to column \"" + a.getAttributeName() + "\": "
                    + ioe.getMessage(), ioe);
            }
        }
        try {
            m_targetMetaData.save(output);
        } catch (IOException ioe) {
            throw new IOException("Failed saving meta data to target column \"" + m_targetMetaData.getAttributeName()
                + "\": " + ioe.getMessage(), ioe);
        }
    }

    /**
     * @param input
     * @return the TreeMetaData loaded from <b>input</b>
     * @throws IOException
     *  */
    public static TreeMetaData load(final DataInputStream input) throws IOException {
        int length = input.readInt();
        TreeAttributeColumnMetaData[] attributesMetaData = new TreeAttributeColumnMetaData[length];
        for (int i = 0; i < length; i++) {
            try {
                attributesMetaData[i] = TreeAttributeColumnMetaData.load(input);
            } catch (IOException ioe) {
                throw new IOException("Failed to load attribute meta data " + i + ": " + ioe.getMessage(), ioe);
            }
        }
        TreeTargetColumnMetaData targetMetaData;
        try {
            targetMetaData = TreeTargetColumnMetaData.load(input);
        } catch (IOException ioe) {
            throw new IOException("Faild to load target attribute: " + ioe.getMessage(), ioe);
        }
        return new TreeMetaData(attributesMetaData, targetMetaData);
    }

}
