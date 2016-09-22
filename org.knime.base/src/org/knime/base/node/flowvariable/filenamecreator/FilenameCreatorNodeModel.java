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
 *   22.09.2016 (andisadewi): created
 */
package org.knime.base.node.flowvariable.filenamecreator;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.util.FileUtil;

/**
*
* @author Andisa Dewi, KNIME.com, Berlin, Germany
*/
public class FilenameCreatorNodeModel extends NodeModel {

   /**
    * The default path to the base directory.
    */
   public static final String DEFAULT_PATH = System.getProperty("user.home");

   private static final NodeLogger LOGGER = NodeLogger.getLogger(FilenameCreatorNodeModel.class);

   private SettingsModelString m_pathModel = FilenameCreatorNodeDialog.getPathModel();

   private SettingsModelString m_nameModel = FilenameCreatorNodeDialog.getFilenameModel();

   private SettingsModelString m_extModel = FilenameCreatorNodeDialog.getFileExtModel();

   /**
    * Creates a new instance of the FilenameCreator model.
    */
   public FilenameCreatorNodeModel() {
       super(new PortType[]{FlowVariablePortObject.TYPE_OPTIONAL}, new PortType[]{FlowVariablePortObject.TYPE});
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
       getDir(m_pathModel.getStringValue());

       if (m_nameModel.getStringValue().isEmpty()) {
           throw new InvalidSettingsException("Please provide a file name");
       }
       if (m_extModel.getStringValue().isEmpty()) {
           throw new InvalidSettingsException("Please provide a file extension");
       }

       return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
       final File dir = getDir(m_pathModel.getStringValue());
       String outputFlowVar = "filePath";
       String ext = m_extModel.getStringValue();
       String name = m_nameModel.getStringValue();

       if (ext.indexOf(".") == -1) {
           ext = "." + ext;
       }
       if (!Pattern.matches("^\\.[a-zA-Z0-9]+", ext)) {
           throw new Exception("Invalid file extension: Only alphanumeric characters are allowed.");
       }
       Pattern pattern = Pattern.compile("[/:?<>*\"|\\\\]");
       Matcher matcher = pattern.matcher(name);
       if(matcher.find()){
           throw new Exception("Invalid file name: " + name.charAt(matcher.start()) + " at position " + matcher.start() + ".");
       }

       if(Pattern.matches("[.\\s]+", name)){
           throw new Exception("Invalid file name: Filename cannot contain only dot(s) or space(s).");
       }

       if(Pattern.matches("^\\s+.*", name)){
           name = name.replaceAll("^\\s+", "");
           LOGGER.warn("Filename contains leading whitespace(s). It will be removed.");
       }

       try{
           new File(name).getCanonicalPath();
       } catch (IOException | NullPointerException e) {
           throw new Exception("Invalid file name!");
       }

       File filepath;
       try {
           filepath = new File(Paths.get(dir.getCanonicalPath(), name + ext).toString());
           filepath.getCanonicalPath();
       } catch (IOException | InvalidPathException e) {
           throw new Exception("Invalid file name!");
       }
       if (filepath.isDirectory()) {
           throw new Exception("File path is a directory!");
       }
       int i = 0;
       Set<String> flowVars = getAvailableFlowVariables().keySet();
       while(flowVars.contains(outputFlowVar)){
           LOGGER.info("Flow variable " + outputFlowVar + " already exists. Using " + outputFlowVar + i + " as output variable.");
           outputFlowVar += i;
           i++;
       }
       pushFlowVariableString(outputFlowVar, filepath.getCanonicalPath());

       return new PortObject[]{FlowVariablePortObject.INSTANCE};
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
       throws IOException, CanceledExecutionException {

   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
       throws IOException, CanceledExecutionException {

   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void saveSettingsTo(final NodeSettingsWO settings) {
       m_extModel.saveSettingsTo(settings);
       m_nameModel.saveSettingsTo(settings);
       m_pathModel.saveSettingsTo(settings);

   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
       m_extModel.validateSettings(settings);
       m_nameModel.validateSettings(settings);
       m_pathModel.validateSettings(settings);

   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
       m_extModel.loadSettingsFrom(settings);
       m_nameModel.loadSettingsFrom(settings);
       m_pathModel.loadSettingsFrom(settings);

   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void reset() {

   }

   private static File getDir(final String file) throws InvalidSettingsException {
       File f = null;
       try {
           // first try if file string is an URL (files in drop dir come as URLs)
           final URL url = new URL(file);
           f = FileUtil.getFileFromURL(url);
       } catch (MalformedURLException e) {
           // if no URL try string as path to file
           f = new File(file);
       }

       if (!f.isDirectory() || !f.exists() || !f.canRead()) {
           throw new InvalidSettingsException("Selected dir: " + file + " cannot be accessed!");
       }

       return f;
   }
}
