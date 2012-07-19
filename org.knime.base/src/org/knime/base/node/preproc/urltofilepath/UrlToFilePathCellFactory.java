/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2012
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
 *   16.05.2012 (kilian): created
 */
package org.knime.base.node.preproc.urltofilepath;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.util.FileUtil;

/**
 * Cell factory that creates cells with converted string (url to file path).
 *
 * @author Kilian Thiel, KNIME.com, Berlin, Germany
 */
class UrlToFilePathCellFactory extends AbstractCellFactory {

    private int m_colIndex;

    private boolean m_failOnInvalidURL;

    private boolean m_failOnNonExistsingFile;

    /**
     * Creates new instance of <code>UrlToFilePathCellFactory</code>.
     *
     * @param colIndex The index of the column containing url strings.
     * @param failOnInvalidURL if set <code>true</code> an exception will be
     * thrown if an invalid url string occurs, otherwise missing values will
     * be inserted as file path.
     * @param failOnNonExistsingFile if set <code>true</code> an exception
     * will be thrown if file location does not exist, otherwise missing
     * values will be inserted as file path.
     * @param newColSpecs The specs of the new columns to append.
     */
    UrlToFilePathCellFactory(final int colIndex,
            final boolean failOnInvalidURL,
            final boolean failOnNonExistsingFile,
            final DataColumnSpec[] newColSpecs) {
        super(newColSpecs);

        if (colIndex < 0) {
            throw new IllegalArgumentException("Invalid column index: "
                    + colIndex);
        }

        m_colIndex = colIndex;
        m_failOnInvalidURL = failOnInvalidURL;
        m_failOnNonExistsingFile = failOnNonExistsingFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell[] getCells(final DataRow row) {
        DataCell[] newCells = new DataCell[4];
        DataCell cell = row.getCell(m_colIndex);

        // check for missing
        if (cell.isMissing()) {
            newCells = getMissingCells();

        // if not missing try to convert url string
        } else {

            URL url = null;
            try {
                url = new URL(cell.toString());
            } catch (MalformedURLException e) {

                // if url string is not valid fail if specified
                if (m_failOnInvalidURL) {
                    throw new IllegalArgumentException("URL "
                            + cell.toString() + " is not valid!");

                // or insert missing value
                } else {
                    // if fail on file does not exist is check, throw exception
                    if (m_failOnNonExistsingFile) {
                        throw new IllegalArgumentException("File at URL "
                                + cell.toString() + " does not exist!");
                    }
                    newCells = getMissingCells();
                    return newCells;
                }
            }

            File file = FileUtil.getFileFromURL(url);
            if (!file.exists()) {

                // if file does not exists fail if specified
                if (m_failOnNonExistsingFile) {
                    throw new IllegalArgumentException("File "
                            + file.getAbsolutePath() + " does not exist!");
                }
            }

            // get parent folder, file name, file extension, and complete
            // file path
            newCells[0] = new StringCell(UrlToFileUtil.getParentFolder(file));
            newCells[1] = new StringCell(UrlToFileUtil.getFileName(file));
            newCells[2] = new StringCell(UrlToFileUtil.getFileExtension(file));
            newCells[3] = new StringCell(file.getAbsolutePath());
        }

        return newCells;
    }

    private DataCell[] getMissingCells() {
        DataCell[] newCells = new DataCell[4];
        newCells[0] = DataType.getMissingCell();
        newCells[1] = DataType.getMissingCell();
        newCells[2] = DataType.getMissingCell();
        newCells[3] = DataType.getMissingCell();
        return newCells;
    }
}
