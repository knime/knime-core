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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.io.listfiles2;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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

import org.knime.base.node.io.listfiles2.ListFiles.Filter;
import org.knime.base.util.WildcardMatcher;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.util.ConvenientComboBoxRenderer;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.util.FilesHistoryPanel.LocationValidation;
import org.knime.core.node.workflow.FlowVariable;

/**
 * <code>NodeDialog</code> for the "List Files" Node.
 *
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows creation of a simple dialog with standard
 * components. If you need a more complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 *
 * @author Peter
 * @author Tim-Oliver Buchholz (replaced locationpanel in 2.11)
 */
public class ListFilesNodeDialog extends NodeDialogPane implements ItemListener {

    private static final int HORIZ_SPACE = 10;

    private JComboBox<String> m_extensionField;

    private JCheckBox m_caseSensitive;

    private JCheckBox m_recursive;

    private JRadioButton m_filterALLRadio;

    private JRadioButton m_filterExtensionsRadio;

    private JRadioButton m_filterRegExpRadio;

    private JRadioButton m_filterWildCardsRadio;

    private FilesHistoryPanel m_localdirectory;

    /**
     * Creates a new List FilesNodeDialog.
     */
    protected ListFilesNodeDialog() {
        super.removeTab("Options");
        super.addTabAt(0, "Options", createPanel());
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
        panel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Filter:"));

        // Get the same height for Location and extension field.
        int buttonHeight = new JButton("Browse...").getPreferredSize().height;

        m_extensionField = new JComboBox<String>();
        m_extensionField.setEditable(true);
        m_extensionField.setRenderer(new ConvenientComboBoxRenderer());
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

        panel2.add(extBox);
        panel2.add(filterBox2);

        return panel2;
    }

    /**
     * This Methods build the Location Panel.
     *
     * @return Location Panel
     */
    private JPanel createLocationPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Location:"));

        // Creating the browse button to get its preferred height
        FlowVariableModel fvm =
            createFlowVariableModel("file_location", FlowVariable.Type.STRING);
        // Directory (local location)
        m_localdirectory =
            new FilesHistoryPanel(fvm, "filereader_history", LocationValidation.DirectoryInput, new String[]{});
        m_localdirectory.setSelectMode(JFileChooser.DIRECTORIES_ONLY);
        m_localdirectory.setAllowRemoteURLs(false);

        m_recursive = new JCheckBox();
        m_recursive.setText("include sub folders");

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;

        panel.add(m_localdirectory, c);

        c.gridy++;
        panel.add(m_recursive, c);

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
     *
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

        // check if all entered Locations are valid
        String location = m_localdirectory.getSelectedFile();
        if (location.trim().isEmpty()) {
            throw new InvalidSettingsException("Please select a file!");
        }

        ListFilesSettings set = new ListFilesSettings();
        set.setLocationString(location);
        set.setRecursive(m_recursive.isSelected());
        set.setCaseSensitive(m_caseSensitive.isSelected());
        String extensions = m_extensionField.getEditor().getItem().toString();
        set.setExtensionsString(extensions);

        // save the selected radio-Button
        Filter filter;
        if (m_filterALLRadio.isSelected()) {
            filter = Filter.None;
        } else if (m_filterExtensionsRadio.isSelected()) {
            filter = Filter.Extensions;
        } else if (m_filterRegExpRadio.isSelected()) {
            if (extensions.trim().isEmpty()) {
                throw new InvalidSettingsException("Enter valid regular expressin pattern");
            }
            try {
                String pattern = extensions;
                Pattern.compile(pattern);
            } catch (PatternSyntaxException pse) {
                throw new InvalidSettingsException("Error in pattern: ('" + pse.getMessage(), pse);
            }
            filter = Filter.RegExp;
        } else if (m_filterWildCardsRadio.isSelected()) {

            if ((extensions).length() <= 0) {
                throw new InvalidSettingsException("Enter valid wildcard pattern");
            }
            try {
                String pattern = extensions;
                pattern = WildcardMatcher.wildcardToRegex(pattern);
                Pattern.compile(pattern);
            } catch (PatternSyntaxException pse) {
                throw new InvalidSettingsException("Error in pattern: '" + pse.getMessage(), pse);
            }
            filter = Filter.Wildcards;
        } else { // one button must be selected though
            filter = Filter.None;
        }
        set.setFilter(filter);
        set.saveSettingsTo(settings);
        m_localdirectory.addToHistory();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {

        ListFilesSettings set = new ListFilesSettings();
        set.loadSettingsInDialog(settings);

        // add previous selections to the extension textfield
        String[] history = ListFilesSettings.getExtensionHistory();
        m_extensionField.removeAllItems();
        for (String str : history) {
            m_extensionField.addItem(str);
        }

        m_caseSensitive.setSelected(set.isCaseSensitive());
        String loc = set.getLocationString();
        m_localdirectory.setSelectedFile(loc);
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
