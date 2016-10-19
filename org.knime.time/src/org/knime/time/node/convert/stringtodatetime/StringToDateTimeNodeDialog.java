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
 *   Oct 19, 2016 (simon): created
 */
package org.knime.time.node.convert.stringtodatetime;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.StringHistory;
import org.knime.time.node.convert.oldtonew.DateTimeTypes;

/**
 * The node dialog of the node which converts strings to the new date&time types.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
public class StringToDateTimeNodeDialog extends NodeDialogPane {

    private final DialogComponentColumnFilter2 m_dialogCompColFilter;

    private final DialogComponentBoolean m_dialogCompReplaceBool;

    private DialogComponentString m_dialogCompSuffix;

    private final JComboBox<DateTimeTypes> m_typeCombobox;

    private final DialogComponentStringSelection m_dialogCompFormatSelect;

    private final DialogComponentBoolean m_dialogCompCancelOnFail;

    private final DialogComponentNumber m_dialogCompFailNumber;

    /**
     * Predefined date formats.
     */
    public static final Collection<String> PREDEFINED_FORMATS = createPredefinedFormats();

    /**
     * Key for the string history to re-use user entered date formats.
     */
    public static final String FORMAT_HISTORY_KEY = "string2date-formats";

    /** Setting up all DialogComponents. */
    StringToDateTimeNodeDialog() {

        m_dialogCompColFilter = new DialogComponentColumnFilter2(StringToDateTimeNodeModel.createColSelectModel(), 0);

        final SettingsModelBoolean replaceModelBool = StringToDateTimeNodeModel.createReplaceModelBool();
        m_dialogCompReplaceBool = new DialogComponentBoolean(replaceModelBool, "Replace selected column");

        final SettingsModelString suffixModel = StringToDateTimeNodeModel.createSuffixModel();
        m_dialogCompSuffix = new DialogComponentString(suffixModel, "Suffix of appended columns: ");

        final SettingsModelString formatModel = StringToDateTimeNodeModel.createFormatModel();
        m_dialogCompFormatSelect =
            new DialogComponentStringSelection(formatModel, "Date format: ", PREDEFINED_FORMATS, true);

        final SettingsModelBoolean cancelOnFailModel = StringToDateTimeNodeModel.createCancelOnFailModel();
        m_dialogCompCancelOnFail = new DialogComponentBoolean(cancelOnFailModel, "Abort execution...");

        final SettingsModelInteger failNumberModel = StringToDateTimeNodeModel.createFailNumberModel();
        m_dialogCompFailNumber =
            new DialogComponentNumber(failNumberModel, "...after this number of unresolved rows", 10);

        /*
         * create panel with gbc
         */
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;

        /*
         * add column filter
         */
        panel.add(m_dialogCompColFilter.getComponentPanel(), gbc);

        /*
         * add replace/append selection
         */
        gbc.gridy++;
        gbc.weighty = 0;
        final JPanel panelReplace = new JPanel(new GridBagLayout());
        panelReplace.setBorder(BorderFactory.createTitledBorder("Replace/Append Selection"));
        final GridBagConstraints gbcReplaceAppend = new GridBagConstraints();
        // add check box
        gbcReplaceAppend.fill = GridBagConstraints.BOTH;
        gbcReplaceAppend.gridx = 0;
        gbcReplaceAppend.gridy = 0;
        gbcReplaceAppend.weightx = 1;
        gbcReplaceAppend.weighty = 1;
        panelReplace.add(m_dialogCompReplaceBool.getComponentPanel(), gbcReplaceAppend);

        // add suffix text field
        gbcReplaceAppend.gridx++;
        panelReplace.add(m_dialogCompSuffix.getComponentPanel(), gbcReplaceAppend);

        panel.add(panelReplace, gbc);

        /*
         * add type/format selection
         */
        gbc.gridy++;
        final JPanel panelTypeFormat = new JPanel(new GridBagLayout());
        panelTypeFormat.setBorder(BorderFactory.createTitledBorder("Type and Format Selection"));
        final GridBagConstraints gbcTypeFormat = new GridBagConstraints();
        // add label and combo box for type selection
        gbcTypeFormat.fill = GridBagConstraints.BOTH;
        gbcTypeFormat.gridx = 0;
        gbcTypeFormat.gridy = 0;
        gbcTypeFormat.weightx = 1;
        gbcTypeFormat.weighty = 1;
        final JPanel panelTypeList = new JPanel(new FlowLayout());
        final JLabel label = new JLabel("New type: ");
        panelTypeList.add(label);
        m_typeCombobox = new JComboBox<DateTimeTypes>(DateTimeTypes.values());
        panelTypeList.add(m_typeCombobox);
        panelTypeFormat.add(panelTypeList, gbcTypeFormat);
        // add format selection
        gbcTypeFormat.gridx++;
        panelTypeFormat.add(m_dialogCompFormatSelect.getComponentPanel(), gbcTypeFormat);

        panel.add(panelTypeFormat, gbc);

        /*
         * add cancel on fail selection
         */
        gbc.gridy++;
        final JPanel panelCancelOnFail = new JPanel(new GridBagLayout());
        panelCancelOnFail.setBorder(BorderFactory.createTitledBorder("Abort Execution"));
        final GridBagConstraints gbcCancelOnFail = new GridBagConstraints();
        // add check box
        gbcCancelOnFail.fill = GridBagConstraints.BOTH;
        gbcCancelOnFail.gridx = 0;
        gbcCancelOnFail.gridy = 0;
        gbcCancelOnFail.weightx = 1;
        gbcCancelOnFail.weighty = 1;
        panelCancelOnFail.add(m_dialogCompCancelOnFail.getComponentPanel(), gbcCancelOnFail);
        gbcCancelOnFail.gridx++;
        panelCancelOnFail.add(m_dialogCompFailNumber.getComponentPanel(), gbcCancelOnFail);

        panel.add(panelCancelOnFail, gbc);

        /*
         * add tab
         */
        addTab("Options", panel);

        /*
         * Change and action listeners
         */
        replaceModelBool.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                suffixModel.setEnabled(!replaceModelBool.getBooleanValue());
            }
        });

        cancelOnFailModel.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                failNumberModel.setEnabled(cancelOnFailModel.getBooleanValue());
            }
        });

        m_typeCombobox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                // TODO: check if format is compatible with type
                String stringValue = formatModel.getStringValue();
                ((JComponent)m_dialogCompFormatSelect.getComponentPanel().getComponent(1))
                    .setBorder(BorderFactory.createLineBorder(new Color(255, 0, 0)));

            }
        });

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_dialogCompColFilter.saveSettingsTo(settings);
        m_dialogCompReplaceBool.saveSettingsTo(settings);
        m_dialogCompSuffix.saveSettingsTo(settings);
        settings.addString("typeEnum", ((DateTimeTypes)m_typeCombobox.getModel().getSelectedItem()).name());
        m_dialogCompFormatSelect.saveSettingsTo(settings);
        m_dialogCompCancelOnFail.saveSettingsTo(settings);
        m_dialogCompFailNumber.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_dialogCompColFilter.loadSettingsFrom(settings, specs);
        m_dialogCompReplaceBool.loadSettingsFrom(settings, specs);
        m_dialogCompSuffix.loadSettingsFrom(settings, specs);
        m_typeCombobox
            .setSelectedItem(DateTimeTypes.valueOf(settings.getString("typeEnum", DateTimeTypes.LOCAL_DATE.name())));
        m_dialogCompFormatSelect.loadSettingsFrom(settings, specs);
        m_dialogCompCancelOnFail.loadSettingsFrom(settings, specs);
        m_dialogCompFailNumber.loadSettingsFrom(settings, specs);
        // retrieve potential new values from the StringHistory and add them
        // (if not already present) to the combo box...
        //        m_dialogCompFormatSelect.replaceListItems(createPredefinedFormats(),
        //            StringToDateTimeNodeModel.createFormatModel().getStringValue());
    }

    private static Collection<String> createPredefinedFormats() {
        // unique values
        Set<String> formats = new LinkedHashSet<String>();
        formats.add("yyyy-MM-dd;HH:mm:ss.S");
        formats.add("dd.MM.yyyy;HH:mm:ss.S");
        formats.add("yyyy-MM-dd'T'HH:mm:ss.SSS");
        formats.add("yyyy/dd/MM");
        formats.add("dd.MM.yyyy");
        formats.add("yyyy-MM-dd");
        formats.add("HH:mm:ss");
        // check also the StringHistory....
        String[] userFormats = StringHistory.getInstance(FORMAT_HISTORY_KEY).getHistory();
        for (String userFormat : userFormats) {
            formats.add(userFormat);
        }
        return formats;
    }

}
