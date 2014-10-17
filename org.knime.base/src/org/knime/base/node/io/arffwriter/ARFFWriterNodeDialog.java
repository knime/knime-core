/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   11.02.2005 (ohl): created
 */
package org.knime.base.node.io.arffwriter;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.util.FileUtil;

/**
 * Contains the dialog for the ARFF file writer.
 *
 * @author Peter Ohl, University of Konstanz
 */
public class ARFFWriterNodeDialog extends NodeDialogPane {

    private static final int VERT_SPACE = 5;

    private static final int HORIZ_SPACE = 5;

    private static final int COMPONENT_HEIGHT = 25;

    private FilesHistoryPanel m_url;

    private JLabel m_urlError;

    private JCheckBox m_overwriteOKChecker;

    private static final int TABWIDTH = 600;

    /**
     * Creates a new ARFF file reader dialog.
     */
    public ARFFWriterNodeDialog() {
        super();

        addTab("Specify ARFF file", createFilePanel());
    }

    private JPanel createFilePanel() {
        int sumOfCompHeigth = 0;

        JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.Y_AXIS));
        filePanel.add(Box.createGlue());

        JPanel innerBox = new JPanel();
        innerBox.setLayout(new BoxLayout(innerBox, BoxLayout.Y_AXIS));
        innerBox.add(Box.createVerticalStrut(COMPONENT_HEIGHT + VERT_SPACE));
        sumOfCompHeigth += COMPONENT_HEIGHT + VERT_SPACE;

        JPanel fPanel = new JPanel();
        fPanel.setLayout(new BoxLayout(fPanel, BoxLayout.X_AXIS));
        fPanel
                .setBorder(BorderFactory.createTitledBorder(BorderFactory
                        .createEtchedBorder(),
                        "Enter location to write ARFF file to:"));
        Container fileBox = Box.createHorizontalBox();
        fileBox.add(Box.createHorizontalStrut(20));
        fileBox.add(new JLabel("valid location:"));
        fileBox.add(Box.createHorizontalStrut(HORIZ_SPACE));

        sumOfCompHeigth += COMPONENT_HEIGHT;

        m_url = new FilesHistoryPanel("org.knime.base.node.io.arffwriter", ".arff");
        m_url.setBorder(null);
        fileBox.add(m_url);
        fileBox.add(Box.createHorizontalStrut(HORIZ_SPACE));

        sumOfCompHeigth += COMPONENT_HEIGHT + VERT_SPACE;

        fPanel.add(fileBox);
        innerBox.add(fPanel);
        innerBox.add(Box.createVerticalStrut(VERT_SPACE));
        sumOfCompHeigth += VERT_SPACE;

        Container errBox = Box.createHorizontalBox();
        errBox.add(Box.createHorizontalGlue());
        // force certain height even if textfield is empty
        errBox.add(Box.createVerticalStrut(COMPONENT_HEIGHT));
        m_urlError = new JLabel("");
        m_urlError.setForeground(Color.RED);
        errBox.add(m_urlError);
        errBox.add(Box.createHorizontalGlue());
        errBox.add(Box.createHorizontalGlue());
        innerBox.add(errBox);
        innerBox.add(Box.createVerticalStrut(VERT_SPACE));
        sumOfCompHeigth += COMPONENT_HEIGHT + VERT_SPACE;

        m_overwriteOKChecker = new JCheckBox("Overwrite OK");
        Container overwriteOKBox = Box.createHorizontalBox();
        overwriteOKBox.add(Box.createHorizontalGlue());
        overwriteOKBox.add(m_overwriteOKChecker);
        overwriteOKBox.add(Box.createHorizontalStrut(20));
        innerBox.add(overwriteOKBox);
        innerBox.add(Box.createVerticalStrut(VERT_SPACE));
        sumOfCompHeigth += VERT_SPACE;


        innerBox.add(Box.createVerticalStrut(VERT_SPACE));
        sumOfCompHeigth += COMPONENT_HEIGHT + (2 * VERT_SPACE);

        innerBox.setMaximumSize(new Dimension(TABWIDTH, sumOfCompHeigth));

        filePanel.add(innerBox);
        filePanel.add(Box.createGlue());


        m_url.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateFileError();
            }
        });

        return filePanel;
    }

    /**
     * Updates the file error label whenever the fileURL entered changed.
     */
    protected void updateFileError() {
        String selFile = m_url.getSelectedFile();
        String errorText = null;
        if ((selFile != null) && !selFile.isEmpty()) {
            try {
                URL newUrl = FileUtil.toURL(selFile);
                Path path = FileUtil.resolveToPath(newUrl);
                m_overwriteOKChecker.setEnabled(path != null);
                if (path != null) {
                    if (Files.isDirectory(path)) {
                        errorText = "Location is a directory: \"" + path + "\"";
                    }
                    if (Files.exists(path) && !Files.isWritable(path)) {
                        errorText = "Cannot write to file \"" + path + "\".";
                    }
                }
            } catch (IOException | URISyntaxException ex) {
                // ignore
            }
        }

        if (errorText == null) {
            m_urlError.setText("");
        } else {
            m_urlError.setText(errorText);
            m_urlError.setVisible(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        updateFileError();
        if (!m_urlError.getText().equals("")) {
            throw new InvalidSettingsException("Specify valid file location. "
                    + "Or press 'Cancel'.");
        }
        settings.addString(ARFFWriterNodeModel.CFGKEY_FILENAME, m_url.getSelectedFile());
        settings.addBoolean(ARFFWriterNodeModel.CFGKEY_OVERWRITE_OK,
                m_overwriteOKChecker.isSelected());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        // set the selected file
        m_url.setSelectedFile(settings.getString(ARFFWriterNodeModel.CFGKEY_FILENAME, ""));

        m_overwriteOKChecker.setSelected(settings.getBoolean(
                ARFFWriterNodeModel.CFGKEY_OVERWRITE_OK, false));
        updateFileError();
    }
}
