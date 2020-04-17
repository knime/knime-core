/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.core.node.util;

import java.awt.Component;

import javax.swing.JFileChooser;

/**
 * Represents a file system browser used by the {@link FilesHistoryPanel}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @since 4.0
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noreference This interface is not intended to be referenced by clients.
 */
public interface FileSystemBrowser {

    /**
     * Specifies what is allowed to be selected in the file browser.
     */
    enum FileSelectionMode {
            FILES_ONLY(JFileChooser.FILES_ONLY), DIRECTORIES_ONLY(JFileChooser.DIRECTORIES_ONLY),
            FILES_AND_DIRECTORIES(JFileChooser.FILES_AND_DIRECTORIES);

        private int m_jFileChooserCode;

        private FileSelectionMode(final int jFileChooserCode) {
            m_jFileChooserCode = jFileChooserCode;

        }

        /**
         * Returns the JFileChooserCode corresponding to this FileSelectionMode.
         *
         * @return the corresponding JFileChooserCode
         * @since 4.2
         */
        public int getJFileChooserCode() {
            return m_jFileChooserCode;
        }

        public static FileSelectionMode fromJFileChooserCode(final int code) {
            switch (code) {
                case JFileChooser.FILES_ONLY:
                    return FILES_ONLY;
                case JFileChooser.DIRECTORIES_ONLY:
                    return DIRECTORIES_ONLY;
                case JFileChooser.FILES_AND_DIRECTORIES:
                    return FILES_AND_DIRECTORIES;
                default:
                    throw new IllegalArgumentException("Invalid code");
            }
        }
    }

    /**
     * Browse files to save or load them?
     */
    enum DialogType {
            OPEN_DIALOG(JFileChooser.OPEN_DIALOG), SAVE_DIALOG(JFileChooser.SAVE_DIALOG);

        private int m_jFileChooserCode;

        private DialogType(final int jFileChooserCode) {
            m_jFileChooserCode = jFileChooserCode;

        }

        /**
         * Returns the JFileChooserCode corresponding to this DialogType.
         *
         * @return the corresponding JFileChooserCode
         * @since 4.2
         */
        public int getJFileChooserCode() {
            return m_jFileChooserCode;
        }

        public static DialogType fromJFileChooserCode(final int code) {
            switch (code) {
                case JFileChooser.OPEN_DIALOG:
                    return OPEN_DIALOG;
                case JFileChooser.SAVE_DIALOG:
                    return SAVE_DIALOG;
                default:
                    throw new IllegalArgumentException("Invalid code");
            }
        }
    }

    /**
     * Normal/default priority is 0.
     */
    static final int NORMAL_PRIORITY = 0;

    /**
     * The priority to use a file system browser if multiple are registered.
     *
     * @return the priority
     */
    default int getPriority() {
        return NORMAL_PRIORITY;
    }

    /**
     * @return <code>true</code> if this file browser can be used, e.g., given the current node- and workflow context
     */
    boolean isCompatible();

    /**
     * Opens a dialog to select a file and returns the selected path.
     *
     * Method is called from awt's event dispatch thread!
     *
     * @param fileSelectionMode what is allowed to be selected
     * @param dialogType load or save dialog
     * @param parent parent for a modal dialog
     * @param forcedFileExtensionOnSave if non-null, the provided file extension is added on save (i.e. only for
     *            'save'-dialog types)
     * @param selectedFile a selected file by default or <code>null</code>
     * @param suffixes suffixes to filter for
     * @return path of the selected file
     */
    String openDialogAndGetSelectedFileName(FileSelectionMode fileSelectionMode, DialogType dialogType,
        Component parent, final String forcedFileExtensionOnSave, String selectedFile, String[] suffixes);

}
