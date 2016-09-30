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
 *   30.09.2016 (andisadewi): created
 */
package org.knime.base.node.flowvariable.createfilename;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButton;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentMultiLineString;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
*
* @author Andisa Dewi, KNIME.com, Berlin, Germany
*/
public class CreateFilenameNodeDialog extends DefaultNodeSettingsPane {

   static SettingsModelString getPathModel() {
       return new SettingsModelString(CreateFilenameConfigKeys.CFGKEY_BASE_DIR, CreateFilenameNodeModel.DEFAULT_PATH);
   }

   static SettingsModelString getFilenameModel() {
       return new SettingsModelString(CreateFilenameConfigKeys.CFGKEY_FILENAME, "");
   }

   static SettingsModelString getFileExtModel() {
       return new SettingsModelString(CreateFilenameConfigKeys.CFGKEY_FILE_EXT,
           CreateFilenameNodeModel.DEFAULT_FILE_EXT);
   }

   static SettingsModelString getAreaModel() {
       return new SettingsModelString("previewArea", "");
   }

   static SettingsModelString getOutputModel() {
       return new SettingsModelString(CreateFilenameConfigKeys.CFGKEY_OUTPUT_VAR,
           CreateFilenameNodeModel.DEFAULT_OUTPUT_NAME);
   }

   private final SettingsModelString m_pathModel = getPathModel();

   private final SettingsModelString m_nameModel = getFilenameModel();

   private final SettingsModelString m_extModel = getFileExtModel();

   private final SettingsModelString m_areaModel = getAreaModel();

   private final DialogComponentStringSelection m_extSelection;

   private WarningLabel m_warningLabel;

   private String m_baseDir;

   private String m_name;

   private String m_ext;

   private FlowVariableModel m_flowVarName;

   private FlowVariableModel m_flowVarBaseDir;

   private FlowVariableModel m_flowVarExt;

   /**
    * Creates s dialog for the FilenameCreator node. There are a file chooser to input the base directory and string
    * components to input the file name, file extension and the output variable name. A preview box is available to
    * show a preview of the output by clicking the preview button.
    */
   public CreateFilenameNodeDialog() {
       m_flowVarBaseDir = createFlowVariableModel(getPathModel());
       m_flowVarBaseDir.addChangeListener(new FlowVarListener());
       addDialogComponent(new DialogComponentFileChooser(m_pathModel, CreateFilenameNodeDialog.class.toString(),
           JFileChooser.OPEN_DIALOG, true, m_flowVarBaseDir));

       createNewGroup("Filename settings");
       setHorizontalPlacement(true);
       m_flowVarName = createFlowVariableModel(getFilenameModel());
       m_flowVarName.addChangeListener(new FlowVarListener());
       addDialogComponent(new DialogComponentString(m_nameModel, "Base file name", false, 20, m_flowVarName));

       m_flowVarExt = createFlowVariableModel(getFileExtModel());
       m_flowVarExt.addChangeListener(new FlowVarListener());
       m_extSelection = new DialogComponentStringSelection(m_extModel, "File extension",
           CreateFilenameNodeModel.FILE_EXTS, true, m_flowVarExt);
       addDialogComponent(m_extSelection);

       setHorizontalPlacement(false);
       addDialogComponent(new DialogComponentString(getOutputModel(), "Output flow variable name", false, 20));

       closeCurrentGroup();

       createNewGroup("Output Preview");
       DialogComponentButton previewButton = new DialogComponentButton("Filename Preview");
       previewButton.addActionListener(new PreviewListener());
       addDialogComponent(previewButton);

       setHorizontalPlacement(false);

       addDialogComponent(new PreviewTextArea(m_areaModel, ""));

       m_warningLabel = new WarningLabel("");
       addDialogComponent(m_warningLabel);

       closeCurrentGroup();
   }

   /**
    *
    * {@inheritDoc}
    */
   @Override
   public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
       throws NotConfigurableException {
       super.loadAdditionalSettingsFrom(settings, specs);
       m_extSelection.replaceListItems(CreateFilenameNodeModel.createPredefinedExtensions(),
           m_extModel.getStringValue());
   }

   private void update() {
       if (m_flowVarName.isVariableReplacementEnabled()) {
           m_name = getFlowVarFromStack(m_flowVarName.getInputVariableName());
       } else {
           m_name = m_nameModel.getStringValue();
       }
       if (m_flowVarBaseDir.isVariableReplacementEnabled()) {
           m_baseDir = getFlowVarFromStack(m_flowVarBaseDir.getInputVariableName());
       } else {
           m_baseDir = m_pathModel.getStringValue();
       }
       if (m_flowVarExt.isVariableReplacementEnabled()) {
           m_ext = getFlowVarFromStack(m_flowVarExt.getInputVariableName());
       } else {
           m_ext = m_extModel.getStringValue();
       }
   }

   private String getFlowVarFromStack(final String str) {
       return getAvailableFlowVariables().get(str).getStringValue();
   }

   private class FlowVarListener implements ChangeListener {

       /**
        * {@inheritDoc}
        */
       @Override
       public void stateChanged(final ChangeEvent e) {
           update();
       }
   }

   private class PreviewListener implements ActionListener {

       /**
        * {@inheritDoc}
        */
       @Override
       public void actionPerformed(final ActionEvent arg0) {
           update();

           if (!CreateFilenameNodeModel.verifyBaseDir(m_baseDir)) {
               m_warningLabel.setText("Error: Invalid base directory name.");
               m_areaModel.setStringValue("");
               return;
           }

           if (m_name.isEmpty()) {
               m_warningLabel.setText("Error: Filename cannot be empty.");
               m_areaModel.setStringValue("");
               return;
           }

           int invalidCharIdx = CreateFilenameNodeModel.findInvalidChar(m_name);
           if (invalidCharIdx != -1) {
               m_warningLabel.setText("Error: Filename contains invalid characters ( /, \\, ?, *, :, <, >, \", |).");
               m_areaModel.setStringValue("");
               return;
           }

           if (CreateFilenameNodeModel.IS_WINDOWS && CreateFilenameNodeModel.checkForbiddenWindowsName(m_name)) {
               m_warningLabel
                   .setText("Error: Filename might contain names that are forbidden in Windows platform.");
               m_areaModel.setStringValue("");
               return;
           }

           if (!CreateFilenameNodeModel.checkDotsAndSpaces(m_name)) {
               m_warningLabel.setText("Error: Filename cannot contain only dot(s) or space(s).");
               m_areaModel.setStringValue("");
               return;
           }

           if (!CreateFilenameNodeModel.checkLeadingWhitespaces(m_name)) {
               m_name = m_name.replaceAll("^\\s+", "");
               m_warningLabel.setText("Warning: Filename contains leading whitespace(s). It will be removed.");
               m_areaModel.setStringValue("");
           }

           m_ext = CreateFilenameNodeModel.verifyExtension(m_ext);
           if (m_ext == "-1") {
               m_warningLabel.setText("Error: Only alphanumeric characters are allowed for extensions.");
               m_areaModel.setStringValue("");
               return;
           }

           String output = CreateFilenameNodeModel.handleSlash(m_baseDir, m_name, m_ext);
           if (output == "-1") {
               m_warningLabel.setText("Error: Invalid file name!");
               m_areaModel.setStringValue("");
               return;
           }
           m_areaModel.setStringValue(output);
           m_warningLabel.setText("");
       }
   }

   private class WarningLabel extends DialogComponentButton {

       private JButton m_button;

       /**
        * @param label the label name
        */
       public WarningLabel(final String label) {
           super(label);

           getComponentPanel().setLayout(new FlowLayout());
           Component[] comps = getComponentPanel().getComponents();
           for (Component comp : comps) {
               if (comp instanceof JButton) {
                   m_button = (JButton)comp;
                   getComponentPanel().remove(comp);
                   break;
               }
           }
           formatButton();
           getComponentPanel().add(m_button);
       }

       private void formatButton() {
           m_button.setFocusPainted(false);
           m_button.setMargin(new Insets(0, 0, 0, 0));
           m_button.setContentAreaFilled(false);
           m_button.setBorderPainted(true);
           m_button.setOpaque(false);
           m_button.setForeground(Color.RED);
           m_button.setPreferredSize((new Dimension(500, 15)));
       }
   }

   private class PreviewTextArea extends DialogComponentMultiLineString {

       private JTextArea m_area;

       /**
        * @param stringModel the setting model of the string
        * @param label the label name
        */
       public PreviewTextArea(final SettingsModelString stringModel, final String label) {
           super(stringModel, label);
           getComponentPanel().setLayout(new FlowLayout());
           Component[] comps = getComponentPanel().getComponents();
           for (Component comp : comps) {
               if (comp instanceof JScrollPane) {
                   JScrollPane scrollPane = (JScrollPane)comp;
                   for (Component elem : scrollPane.getViewport().getComponents()) {
                       if (elem instanceof JTextArea) {
                           m_area = (JTextArea)elem;
                           getComponentPanel().remove(comp);
                           break;
                       }
                   }
               }
           }
           m_area.setEditable(false);
           m_area.setBorder(javax.swing.BorderFactory.createEmptyBorder());
           m_area.setOpaque(false);
           m_area.setLineWrap(true);
           getComponentPanel().add(m_area);
       }
   }
}
