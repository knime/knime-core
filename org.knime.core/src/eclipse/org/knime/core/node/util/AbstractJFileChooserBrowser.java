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
 * History
 *   Dec 30, 2018 (hornm): created
 */
package org.knime.core.node.util;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.util.SimpleFileFilter;

/**
 * An abstract file system browser that uses the {@link JFileChooser} but allows sub-classes to provide, e.g., custom
 * implementations of the JFileChooser's {@link FileSystemView}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 4.0
 */
public abstract class AbstractJFileChooserBrowser implements FileSystemBrowser {

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract boolean isCompatible();

    /**
     * {@inheritDoc}
     */
    @Override
    public String openDialogAndGetSelectedFileName(final FileSelectionMode fileSelectionMode,
        final DialogType dialogType, final Component parent, final String forcedFileExtensionOnSave,
        final String selectedFile, final String[] suffixes) {
        final JFileChooser fileChooser = new JFileChooser(getFileSystemView());
        setFileView(fileChooser);
        fileChooser.setAcceptAllFileFilterUsed(true);
        List<SimpleFileFilter> filters = createFiltersFromSuffixes(suffixes);
        for (SimpleFileFilter filter : filters) {
            fileChooser.addChoosableFileFilter(filter);
        }
        if (filters.size() > 0) {
            fileChooser.setFileFilter(filters.get(0));
        }
        fileChooser.setFileSelectionMode(fileSelectionMode.getJFileChooserCode());
        fileChooser.setDialogType(dialogType.getJFileChooserCode());

        // AP-2562
        // It seems only resized event is happening when showing the dialog
        // Grabbing the focus then makes two clicks to single click selection.
        fileChooser.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent e) {
                fileChooser.grabFocus();
            }
        });

        final File selected = createFileFromPath(selectedFile);
        if (selected != null) {
            fileChooser.setSelectedFile(selected.getAbsoluteFile());
        } else {
            fileChooser.setSelectedFile(null);
        }

        /* This if construct is result of a fix for bug 5841.
        * showDialog does not resolve localized folder names correctly under Mac OS,
        * so we use the methods showSaveDialog and showOpenDialog if possible.
        */
        int r;
        if (dialogType == DialogType.SAVE_DIALOG) {
            r = fileChooser.showSaveDialog(parent);
        } else if (dialogType == DialogType.OPEN_DIALOG) {
            r = fileChooser.showOpenDialog(parent);
        } else {
            r = fileChooser.showDialog(parent, "OK");
        }
        if (r == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (dialogType == DialogType.SAVE_DIALOG) {
                String forceFileExtension = StringUtils.defaultString(forcedFileExtensionOnSave);
                final String fileName = file.getName();
                if (!(StringUtils.endsWithAny(fileName, suffixes)
                    || StringUtils.endsWithIgnoreCase(fileName, forcedFileExtensionOnSave))) {
                    file = new File(file.getParentFile(), fileName.concat(forceFileExtension));
                }
            }
            if (file.exists() && (fileSelectionMode == FileSelectionMode.FILES_ONLY) && file.isDirectory()) {
                JOptionPane.showMessageDialog(parent, "Error: Please select a file, not a directory.");
                return null;
            }
            return postprocessSelectedFilePath(file.getAbsolutePath());
        }
        return null;
    }

    private static List<SimpleFileFilter> createFiltersFromSuffixes(final String... extensions) {
        List<SimpleFileFilter> filters = new ArrayList<>(extensions.length);

        for (final String extension : extensions) {
            if (extension.indexOf('|') > 0) {
                filters.add(new SimpleFileFilter(extension.split("\\|")));
            } else {
                filters.add(new SimpleFileFilter(extension));
            }
        }
        return filters;
    }

    /**
     * @return <code>null</code> if the default should be selected
     */
    protected abstract FileSystemView getFileSystemView();

    /**
     * @return <code>null</code> if the default should be used
     */
    protected abstract FileView getFileView();

    /**
     * Turns a path into a file.
     *
     * @param filePath
     * @return a file representing the given path
     */
    protected abstract File createFileFromPath(String filePath);

    /**
     * Allows one to modify the selected file path as returned from the file chooser.
     *
     * @param selectedFile the file path as output by the file chooser
     * @return a potentially modified file path/url
     */
    protected String postprocessSelectedFilePath(final String selectedFile) {
        return selectedFile;
    }

    private void setFileView(final JFileChooser jfc) {
        FileView fileView = getFileView();
        if (fileView != null) {
            jfc.setFileView(fileView);
        }
    }
}
