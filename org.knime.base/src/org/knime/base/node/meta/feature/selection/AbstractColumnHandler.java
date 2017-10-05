/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   31.05.2016 (Adrian Nembach): created
 */
package org.knime.base.node.meta.feature.selection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides an abstract implementation of a {@link ColumnHandler}.<br>
 * It stores the constant columns because the handling of the constant columns should be the same for all subclasses. <br>
 * It also provides the logic for saving and loading AbstractColumnHanlders.
 *
 * @author Adrian Nembach, KNIME.com
 */
abstract class AbstractColumnHandler implements ColumnHandler {

    private final List<String> m_constantColumns;

    /**
     * Loads an AbstractColumnHandler from the provided <b>inputStream</b>
     *
     * @param inputStream
     * @return the ColumnHandler loaded from <b>inputStream</b>
     * @throws IOException
     */
    public static AbstractColumnHandler loadColumnHandler(final DataInputStream inputStream) throws IOException {
        // might be important in later versions
        @SuppressWarnings("unused")
        int version = inputStream.readInt();
        int constantColumnsSize = inputStream.readInt();
        List<String> constantColumns = new ArrayList<>(constantColumnsSize);
        for (int i = 0; i < constantColumnsSize; i++) {
            constantColumns.add(inputStream.readUTF());
        }
        char type = inputStream.readChar();
        switch (type) {
            case 'd':
                return new DefaultColumnHandler(constantColumns, inputStream);
            default:
                throw new IllegalStateException("Unknown column handler identifier \"" + type + "\".");
        }
    }

    /**
     * @param constantColumns the names of the columns that should be constant during the feature selection process
     */
    protected AbstractColumnHandler(final List<String> constantColumns) {
        m_constantColumns = constantColumns;
    }


    @Override
    public List<String> getConstantColumns() {
        return m_constantColumns;
    }

    /**
     * Saves an AbstractColumnHandler to <b>outputStream</b>. <br>
     * Along with the names of the constant columns also an identifier for the extending class is stored.
     *
     * @param outputStream
     * @throws IOException
     */
    public final void save(final DataOutputStream outputStream) throws IOException {
        outputStream.writeInt(20160601); // version number
        outputStream.writeInt(m_constantColumns.size());
        for (final String col : m_constantColumns) {
            outputStream.writeUTF(col);
        }
        if (this instanceof DefaultColumnHandler) {
            outputStream.writeChar('d');
        }
        saveData(outputStream);
        outputStream.flush();
    }


    /**
     * Subclasses should use this method to save additional data.
     *
     * @param outStream
     * @throws IOException
     */
    protected abstract void saveData(final DataOutputStream outStream) throws IOException;

}
