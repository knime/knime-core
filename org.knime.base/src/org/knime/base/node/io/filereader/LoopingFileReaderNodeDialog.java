/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   20.06.2008 (mb): created
 */
package org.knime.base.node.io.filereader;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentMultiLineString;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * 
 * @author mb, University of Konstanz
 */
public class LoopingFileReaderNodeDialog extends FileReaderNodeDialog {

    private final DialogComponentString m_varName =
            new DialogComponentString(createVariableNameModel(),
                    "Name of the variable: ");
    
    private final DialogComponentMultiLineString m_fileList = 
        new DialogComponentMultiLineString(createVariableValuesModel(),
        "File names:");

    /**
     * Dialog adding a editable file list to the FileReader's standard dialog.
     */
    LoopingFileReaderNodeDialog() {
        super();
        addTab("additional Files", createFilelistTab());

    }

    private JPanel createFilelistTab() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));

        JPanel varPanel = m_varName.getComponentPanel();
        JPanel listPanel = m_fileList.getComponentPanel();

        Box varBox = Box.createHorizontalBox();
        varBox.add(varPanel);
        varBox.add(Box.createHorizontalGlue());

        Box listBox = Box.createHorizontalBox();
        listBox.add(listPanel);
        listBox.add(Box.createHorizontalGlue());

        result.add(varBox);
        result.add(Box.createVerticalStrut(35));
        result.add(listBox);
        result.add(Box.createVerticalGlue());

        return result;
    }

    /**
     * 
     * @return settings model for the variable values
     */
    static SettingsModelString createVariableValuesModel() {
        return new SettingsModelString("scope.variable.values", "");
    }

    /**
     * 
     * @return settings model for the variable name
     */
    static SettingsModelString createVariableNameModel() {
        return new SettingsModelString("scope.variable.name", "fileName");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        m_varName.loadSettingsFrom(settings, specs);
        m_fileList.loadSettingsFrom(settings, specs);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        super.saveSettingsTo(settings);
        m_varName.saveSettingsTo(settings);
        m_fileList.saveSettingsTo(settings);
    }

}
