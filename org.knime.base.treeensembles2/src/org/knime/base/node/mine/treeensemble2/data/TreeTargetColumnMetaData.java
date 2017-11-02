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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jun 10, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public abstract class TreeTargetColumnMetaData extends TreeColumnMetaData {

    /**
     * @param attributeName
     */
    public TreeTargetColumnMetaData(final String attributeName) {
        super(attributeName);
    }

    /**
     * @param input
     * @throws IOException
     */
    public TreeTargetColumnMetaData(final DataInputStream input) throws IOException {
        super(input);
    }

    /** {@inheritDoc} */
    @Override
    void save(final DataOutputStream output) throws IOException {
        byte colTypeByte;
        if (this instanceof TreeTargetNominalColumnMetaData) {
            colTypeByte = 'c'; // categorical
        } else if (this instanceof TreeTargetNumericColumnMetaData) {
            colTypeByte = 'n';
        } else {
            throw new IOException("Unsupported column type (not implemented): " + getClass().getSimpleName());
        }
        output.writeByte(colTypeByte);
        super.save(output);
        saveContent(output);
    }

    /**
     *  */
    static TreeTargetColumnMetaData load(final DataInputStream input) throws IOException {
        byte colTypeByte = input.readByte();
        switch (colTypeByte) {
            case 'c':
                return new TreeTargetNominalColumnMetaData(input);
            case 'n':
                return new TreeTargetNumericColumnMetaData(input);
            default:
                throw new IOException("Unknown column type identifier '" + (char)colTypeByte + "'");
        }
    }

    /**
     * @param output
     * @throws IOException
     */
    abstract void saveContent(DataOutputStream output) throws IOException;

}
