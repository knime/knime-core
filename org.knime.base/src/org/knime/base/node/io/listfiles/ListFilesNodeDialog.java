/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 */
package org.knime.base.node.io.listfiles;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.knime.base.node.io.listfiles.ListFiles.Filter;
import org.knime.base.util.WildcardMatcher;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.util.ConvenientComboBoxRenderer;

/**
 * <code>NodeDialog</code> for the "List Files" Node.
 *
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 *
 * @author Peter
 */
public class ListFilesNodeDialog extends NodeDialogPane implements ItemListener {

    private static final int HORIZ_SPACE = 10;

    private static final int PANEL_WIDTH = 585;

    private final JPanel m_outerpanel;

    private JComboBox m_locations;

    private JComboBox m_extensionField;

    private JCheckBox m_caseSensitive;

    private JCheckBox m_recursive;

    private JRadioButton m_filterALLRadio;

    private JRadioButton m_filterExtensionsRadio;

    private JRadioButton m_filterRegExpRadio;

    private JRadioButton m_filterWildCardsRadio;

    /**
     * Creates a new List FilesNodeDialog.
     */
    protected ListFilesNodeDialog() {
        super();
        m_outerpanel = createPanel();
        super.removeTab("Options");
        super.addTabAt(0, "Options", m_outerpanel);
    }

    private JPanel createPanel() {

        createFiltersModels();
        JPanel panel = createLocationPanel();
        Box panel2 = createFilterBox();

        JPanel outer = new JPanel();
        outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));
        outer.setAlignmentX(Component.LEFT_ALIGNMENT);
        outer.add(panel);
        outer.add(panel2);
        return outer;
    }

    /**
     * This method create the Filter-Box.
     *
     * @return Filter-Box
     */
    private Box createFilterBox() {
        Box panel2 = Box.createVerticalBox();
        panel2.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Filter:"));

        // Get the same height for Location and extension field.
        int buttonHeight = new JButton("Browse...").getPreferredSize().height;

        m_extensionField = new JComboBox();
        m_extensionField.setEditable(true);
        m_extensionField.setRenderer(new ConvenientComboBoxRenderer());
        m_extensionField
                .setMaximumSize(new Dimension(PANEL_WIDTH, buttonHeight));
        m_extensionField.setMinimumSize(new Dimension(250, buttonHeight));
        m_extensionField.setPreferredSize(new Dimension(250, buttonHeight));

        Box extBox = Box.createHorizontalBox();
        extBox.add(Box.createHorizontalStrut(HORIZ_SPACE));
        extBox.add(new JLabel("Extension(s) / Expression:"));
        extBox.add(Box.createHorizontalStrut(HORIZ_SPACE));
        extBox.add(m_extensionField);
        extBox.add(Box.createHorizontalStrut(HORIZ_SPACE));

        m_caseSensitive = new JCheckBox();
        m_caseSensitive.setText("case sensitive");

        JPanel filterBox = new JPanel(new GridLayout(2, 3));
        filterBox.add(m_filterALLRadio);
        filterBox.add(m_filterExtensionsRadio);
        filterBox.add(m_caseSensitive);
        filterBox.add(m_filterRegExpRadio);
        filterBox.add(m_filterWildCardsRadio);

        Box filterBox2 = Box.createHorizontalBox();
        filterBox2.add(Box.createHorizontalStrut(HORIZ_SPACE));
        filterBox2.add(filterBox);
        filterBox2.add(Box.createHorizontalStrut(PANEL_WIDTH / 4));

        panel2.add(extBox);
        panel2.add(filterBox2);

        panel2.setMaximumSize(new Dimension(PANEL_WIDTH, 120));
        panel2.setMinimumSize(new Dimension(PANEL_WIDTH, 120));

        return panel2;
    }

    /**
     * This Methods build the Location Panel.
     *
     * @return Location Panel
     */
    private JPanel createLocationPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Location:"));

        Box fileBox = Box.createHorizontalBox();

        // Creating the browse button to get its preferred height
        JButton browse = new JButton("Browse...");
        int buttonHeight = browse.getPreferredSize().height;

        m_locations = new JComboBox();
        m_locations.setToolTipText("Enter Location(s) here");
        m_locations.setEditable(true);
        m_locations.setRenderer(new ConvenientComboBoxRenderer());
        m_locations.setMaximumSize(new Dimension(350, buttonHeight));
        m_locations.setMinimumSize(new Dimension(350, buttonHeight));
        m_locations.setPreferredSize(new Dimension(350, buttonHeight));

        m_recursive = new JCheckBox();
        m_recursive.setText("include sub folders");
        Box rec = Box.createHorizontalBox();
        rec.add(Box.createHorizontalStrut(HORIZ_SPACE));
        rec.add(m_recursive);
        rec.add(Box.createHorizontalStrut(PANEL_WIDTH - m_recursive.getWidth()
                - HORIZ_SPACE));

        fileBox.add(Box.createHorizontalStrut(HORIZ_SPACE));
        fileBox.add(new JLabel("Location(s):"));
        fileBox.add(Box.createHorizontalStrut(HORIZ_SPACE));
        fileBox.add(m_locations);
        fileBox.add(Box.createHorizontalStrut(HORIZ_SPACE));
        fileBox.add(browse);
        fileBox.add(Box.createHorizontalStrut(45));

        panel.add(fileBox);
        panel.add(rec);
        panel.setMaximumSize(new Dimension(PANEL_WIDTH, 80));
        panel.setMinimumSize(new Dimension(PANEL_WIDTH, 80));

        browse.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                // sets the path in the file text field.
                String[] newFile = popupFileChooser();
                if (newFile != null) {
                    m_locations.getEditor().setItem(getStringForBox(newFile));

                }
            }
        });

        return panel;
    }

    /** creates the filter radio buttons. */
    private void createFiltersModels() {
        m_filterALLRadio = new JRadioButton();
        m_filterALLRadio.setText("none");
        m_filterALLRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                m_extensionField.setEnabled(false);
            }
        });

        m_filterExtensionsRadio = new JRadioButton();
        m_filterExtensionsRadio.setText("file extension(s)");
        m_filterExtensionsRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                m_extensionField.setEnabled(true);
            }
        });

        m_filterRegExpRadio = new JRadioButton();
        m_filterRegExpRadio.setText("regular expression");
        m_filterRegExpRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                m_extensionField.setEnabled(true);
            }
        });

        m_filterWildCardsRadio = new JRadioButton();
        m_filterWildCardsRadio.setText("wildcard pattern");
        m_filterWildCardsRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                m_extensionField.setEnabled(true);
            }
        });

        ButtonGroup group = new ButtonGroup();
        group.add(m_filterALLRadio);
        group.add(m_filterExtensionsRadio);
        group.add(m_filterRegExpRadio);
        group.add(m_filterWildCardsRadio);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void itemStateChanged(final ItemEvent e) {
        // nothing to do
    }

    /**
     * Pops up the file selection dialog and returns the path(s) to the selected
     * file(s) - or <code>null</code> if the user canceled.
     *
     * @return Array containing the File locations
     **/
    protected String[] popupFileChooser() {
        String startingDir = "";
        JFileChooser chooser;
        chooser = new JFileChooser(startingDir);
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        // make dialog modal
        int returnVal = chooser.showOpenDialog(getPanel().getParent());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String[] path = new String[chooser.getSelectedFiles().length];
            for (int i = 0; i < path.length; i++) {
                try {
                    path[i] =
                            chooser.getSelectedFiles()[i].getAbsoluteFile()
                                    .toString();
                } catch (Exception e) {
                    path[i] = "<Error: Couldn't create URL for Directory>";
                }
            }
            return path;
        }
        // user canceled - return null
        return null;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {

        // check if all entered Locations are valid
        String location = m_locations.getEditor().getItem().toString();
        if (location.trim().isEmpty()) {
            throw new InvalidSettingsException("Please select a file!");
        }

        String[] files = location.split(";");
        for (int i = 0; i < files.length; i++) {
            File currentFile = new File(files[i]);
            if (!currentFile.isDirectory()) {
                // check if it was an URL;
                String s = files[i];
                try {

                    if (s.startsWith("file:")) {
                        s = s.substring(5);
                    }
                    currentFile = new File(URIUtil.decode(s));
                } catch (URIException ex) {
                    throw new InvalidSettingsException("\"" + s
                            + "\" does not exist or is not a directory");
                }
                if (!currentFile.isDirectory()) {
                    throw new InvalidSettingsException("\"" + s
                            + "\" does not exist or is not a directory");
                }
            }

        }

        ListFilesSettings set = new ListFilesSettings();
        set.setLocationString(location);
        set.setRecursive(m_recursive.isSelected());
        set.setCaseSensitive(m_caseSensitive.isSelected());
        String extensions = m_extensionField.getEditor().getItem().toString();
        set.setExtensionsString(extensions.toString());

        // save the selected radio-Button
        Filter filter;
        if (m_filterALLRadio.isSelected()) {
            filter = Filter.None;
        } else if (m_filterExtensionsRadio.isSelected()) {
            filter = Filter.Extensions;
        } else if (m_filterRegExpRadio.isSelected()) {
            if (extensions.trim().isEmpty()) {
                throw new InvalidSettingsException(
                        "Enter valid regular expressin pattern");
            }
            try {
                String pattern = extensions;
                Pattern.compile(pattern);
            } catch (PatternSyntaxException pse) {
                throw new InvalidSettingsException("Error in pattern: ('"
                        + pse.getMessage());
            }
            filter = Filter.RegExp;
        } else if (m_filterWildCardsRadio.isSelected()) {

            if ((extensions).length() <= 0) {
                throw new InvalidSettingsException(
                        "Enter valid wildcard pattern");
            }
            try {
                String pattern = extensions;
                pattern = WildcardMatcher.wildcardToRegex(pattern);
                Pattern.compile(pattern);
            } catch (PatternSyntaxException pse) {
                throw new InvalidSettingsException("Error in pattern: '"
                        + pse.getMessage());
            }
            filter = Filter.Wildcards;
        } else { // one button must be selected though
            filter = Filter.None;
        }
        set.setFilter(filter);
        set.saveSettingsTo(settings);
    }

    /**
     * This Method creates the String for the Location textfield from the given
     * fileurls.
     *
     * @param fileurls
     * @return
     */
    private String getStringForBox(final String[] fileurls) {
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < fileurls.length; i++) {
            buff.append(fileurls[i] + ";");
        }
        return buff.toString();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {

        ListFilesSettings set = new ListFilesSettings();
        set.loadSettingsInDialog(settings);

        // add previous selections to the Location textfield
        String[] history = ListFilesSettings.getLocationHistory();
        m_locations.removeAllItems();
        for (String str : history) {
            m_locations.addItem(str);
        }

        // add previous selections to the extension textfield
        history = ListFilesSettings.getExtensionHistory();
        m_extensionField.removeAllItems();
        for (String str : history) {
            m_extensionField.addItem(str);
        }

        m_caseSensitive.setSelected(set.isCaseSensitive());
        String loc = set.getLocationString();
        m_locations.getEditor().setItem(loc == null ? "" : loc);
        m_recursive.setSelected(set.isRecursive());
        String ext = set.getExtensionsString();
        m_extensionField.getEditor().setItem(ext == null ? "" : ext);
        switch (set.getFilter()) {
            case Extensions:
                m_filterExtensionsRadio.doClick(); // trigger event
                break;
            case RegExp:
                m_filterRegExpRadio.doClick();
                break;
            case Wildcards:
                m_filterWildCardsRadio.doClick();
                break;
            default:
                m_filterALLRadio.doClick();
        }

    }
}
