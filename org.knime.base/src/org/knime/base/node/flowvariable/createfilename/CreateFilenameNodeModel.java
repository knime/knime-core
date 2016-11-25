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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.SystemUtils;
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
import org.knime.core.node.util.StringHistory;
import org.knime.core.util.FileUtil;

/**
*
* @author Andisa Dewi, KNIME.com, Berlin, Germany
*/
public class CreateFilenameNodeModel extends NodeModel {

   /**
    * The default path to the base directory.
    */
   public static final String DEFAULT_PATH = System.getProperty("user.home");

   /**
    * The default value for the output.
    */
   public static final String DEFAULT_OUTPUT_NAME = "filePath";

   /**
    * The default value for the file extension.
    */
   public static final String DEFAULT_FILE_EXT = ".txt";

   /**
    * Predefined file extensions.
    */
   public static final Collection<String> FILE_EXTS = createPredefinedExtensions();

   /**
    * Key for the string history to re-use user-entered file extension.
    */
   public static final String EXT_HISTORY_KEY = "createFileNameHistoryKey";

   /**
    * Boolean to check whether the system is windows.
    */
   public static final boolean IS_WINDOWS = SystemUtils.IS_OS_WINDOWS;

   private static final NodeLogger LOGGER = NodeLogger.getLogger(CreateFilenameNodeModel.class);

   private SettingsModelString m_pathModel = CreateFilenameNodeDialog.getPathModel();

   private SettingsModelString m_nameModel = CreateFilenameNodeDialog.getFilenameModel();

   private SettingsModelString m_extModel = CreateFilenameNodeDialog.getFileExtModel();

   private SettingsModelString m_areaModel = CreateFilenameNodeDialog.getAreaModel();

   private SettingsModelString m_outputModel = CreateFilenameNodeDialog.getOutputModel();

   /**
    * Creates a new instance of the FilenameCreator model.
    */
   public CreateFilenameNodeModel() {
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

       return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
       String outputFlowVar;
       if (!m_outputModel.getStringValue().isEmpty()) {
           outputFlowVar = m_outputModel.getStringValue();
       } else {
           outputFlowVar = DEFAULT_OUTPUT_NAME;
       }

       String baseDir = m_pathModel.getStringValue();
       String ext = m_extModel.getStringValue();
       String name = m_nameModel.getStringValue();

       if (!verifyBaseDir(baseDir)) {
           throw new Exception("Invalid base directory name!");
       }

       if (name.isEmpty()) {
           throw new InvalidSettingsException("Invalid file name: Filename cannot be empty.");
       }

       int invalidCharIdx = findInvalidChar(name);
       if (invalidCharIdx != -1) {
           throw new Exception(
               "Invalid file name: " + name.charAt(invalidCharIdx) + " at position " + invalidCharIdx + ".");
       }

       if (IS_WINDOWS && checkForbiddenWindowsName(name)) {
           throw new InvalidSettingsException(
               "Invalid file name: Filename might contain names that are forbidden in Windows platform.");
       }

       if (!checkDotsAndSpaces(name)) {
           throw new Exception("Invalid file name: Filename cannot contain only dot(s) or space(s).");
       }

       if (!checkLeadingWhitespaces(name)) {
           name = name.replaceAll("^\\s+", "");
           LOGGER.warn("Filename contains leading whitespace(s). It will be removed.");
       }

       ext = verifyExtension(ext);
       if (ext == "-1") {
           throw new Exception("Invalid file extension: Only alphanumeric characters are allowed.");
       }

       try {
           new File(name).getCanonicalPath();
       } catch (IOException | NullPointerException e) {
           throw new Exception("Invalid file name!");
       }

       String output = handleSlash(baseDir, name, ext);
       if (output == "-1") {
           throw new Exception("Invalid file name!");
       }

       int i = 0;
       Set<String> flowVars = getAvailableFlowVariables().keySet();
       while (flowVars.contains(outputFlowVar)) {
           LOGGER.info("Flow variable " + outputFlowVar + " already exists. Using " + outputFlowVar + i
               + " as output variable.");
           outputFlowVar += i;
           i++;
       }
       pushFlowVariableString(outputFlowVar, output);

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
       m_areaModel.saveSettingsTo(settings);
       m_outputModel.saveSettingsTo(settings);

   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
       m_extModel.validateSettings(settings);
       m_nameModel.validateSettings(settings);
       m_pathModel.validateSettings(settings);
       m_areaModel.validateSettings(settings);
       m_outputModel.validateSettings(settings);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
       m_extModel.loadSettingsFrom(settings);
       m_nameModel.loadSettingsFrom(settings);
       m_pathModel.loadSettingsFrom(settings);
       m_areaModel.loadSettingsFrom(settings);
       m_outputModel.loadSettingsFrom(settings);
       String extName = m_extModel.getStringValue();
       if (!FILE_EXTS.contains(extName)) {
           StringHistory.getInstance(EXT_HISTORY_KEY).add(extName);
       }
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

   /**
    * @return string collection of the file extensions. Beside the predefined ones, user-entered extensions will also
    *         be loaded.
    */
   protected static Collection<String> createPredefinedExtensions() {
       Set<String> formats = new LinkedHashSet<String>();
       formats.add(".txt");
       formats.add(".csv");
       formats.add(".table");
       formats.add(".pmml");
       formats.add(".xml");
       formats.add(".arff");
       formats.add(".xls");
       formats.add(".xlsx");
       formats.add(".zip");
       formats.add(".png");
       String[] userFormats = StringHistory.getInstance(EXT_HISTORY_KEY).getHistory();
       for (String userFormat : userFormats) {
           formats.add(userFormat);
       }
       return formats;
   }

   /**
    * @param ext the string extension to be verified
    * @return the verified extension (dot will be added if didn't exist beforehand), or -1 if it contains any invalid
    *         chars.
    */
   protected static String verifyExtension(String ext) {
       if (ext.isEmpty()) {
           return ext;
       }
       if (ext.indexOf(".") == -1) {
           ext = "." + ext;
       }
       if (!Pattern.matches("^\\.[a-zA-Z0-9]+", ext)) {
           return "-1";
       }
       return ext;
   }

   /**
    * @param name the file name string
    * @return the index of the first invalid char found. If nothing is found, -1 will be returned.
    */
   protected static int findInvalidChar(final String name) {
       Pattern pattern = Pattern.compile("[/:?<>*\"|\\\\]");
       Matcher matcher = pattern.matcher(name);
       if (matcher.find()) {
           return matcher.start();
       }
       return -1;
   }

   private static Pattern FORBIDDEN_WINDOWS_NAMES =
       Pattern.compile("^(?:(?:CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\.[^.]*)?|[ \\.])$");

   /**
    * @param name the file name string
    * @return boolean value, true if the input matches any forbidden windows name, false otherwise.
    */
   protected static boolean checkForbiddenWindowsName(final String name) {
       if (IS_WINDOWS) {
           Matcher matcher = FORBIDDEN_WINDOWS_NAMES.matcher(name);
           if (matcher.find()) {
               return true;
           }
       }
       return false;
   }

   /**
    * @param name the file name string
    * @return boolean value, true if the input contains only dot(s) or white space(s), false otherwise.
    */
   protected static boolean checkDotsAndSpaces(final String name) {
       if (Pattern.matches("[.\\s]+", name)) {
           return false;
       }
       return true;
   }

   /**
    * @param name the file name string
    * @return boolean value, true if file name contains leading white spaces, false otherwise.
    */
   protected static boolean checkLeadingWhitespaces(final String name) {
       if (Pattern.matches("^\\s+.*", name)) {
           return false;
       }
       return true;
   }

   /**
    * @param dir the directory path
    * @return boolean value, true if the path exists or can be resolved, false otherwise.
    */
   protected static boolean verifyBaseDir(final String dir) {
       try {
           getDir(dir);
       } catch (Exception e) {
           return false;
       }
       return true;
   }

   /**
    * This method handles the slash between the base dir path and the file name.
    *
    * @param baseDir the directory path
    * @param name the file name
    * @param ext the file extension
    * @return the concatenated string of all three input, or -1 if exception occurred.
    */
   protected static String handleSlash(String baseDir, final String name, final String ext) {
       String output = "";
       try {
           if (baseDir.endsWith("?")) {
               baseDir = Pattern.compile("[?]+$").matcher(baseDir).replaceAll("");
           }
           if (!(baseDir.endsWith("/") || baseDir.endsWith("\\"))) {
               if (!baseDir.toLowerCase().startsWith("knime") && IS_WINDOWS) {
                   output = baseDir + "\\" + name + ext;
               } else {
                   output = baseDir + "/" + name + ext;
               }
           } else {
               Matcher slash = Pattern.compile("[/]+$").matcher(baseDir);
               if (slash.find()) {
                   baseDir = slash.replaceAll("") + "/";
               }
               slash = Pattern.compile("[\\\\]+$").matcher(baseDir);
               if (slash.find()) {
                   baseDir = slash.replaceAll("") + "\\";
               }
               output = baseDir + name + ext;
           }
       } catch (InvalidPathException e) {
           return "-1";
       }
       return output;
   }

}
