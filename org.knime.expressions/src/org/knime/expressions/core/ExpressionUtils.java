/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.expressions.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.control.CompilationFailedException;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;
import org.knime.expressions.base.node.ExpressionCompletionProvider;
import org.knime.expressions.core.exceptions.ScriptCompilationException;
import org.knime.expressions.core.exceptions.ScriptParseException;
import org.knime.ext.sun.nodes.script.expression.Expression;

import groovy.lang.GroovyClassLoader;

/**
 * Class which provides methods to parse a script, compile it to a Java class
 * and to wrap it in such a way that it can be easily invoked with the given
 * data.
 * 
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 *
 */
final class ExpressionUtils {

	private final static String METHOD_NAME = ParsedScript.METHOD_NAME;

	/**
	 * Parses the script, replaces column, flow variable names, and special
	 * expressions (rowid, rowindex, rowcount). After replacing these names, setters
	 * for column and special expression inputs are appended and the original
	 * expression is wrapped within a method, which is again wrapped in a class.
	 * 
	 * @param script
	 *            The original expression that shall be parsed.
	 * @param columns
	 *            {@link DataColumnSpec} of the columns that are available, i.e.
	 *            accessible, in the script.
	 * @param variables
	 *            {@link FlowVariable}s that are accissble in the script.
	 * @param returnType
	 *            {@link DataType} that shall be returned by the script.
	 * @return {@link ParsedScript} that contains all necessary information to
	 *         provide the script with input.
	 * @throws ScriptParseException
	 *             Throws exception when an error during parsing occurs, e.g. a
	 *             column used in the script is not known etc..
	 */
	public static ParsedScript parseScript(String script, DataColumnSpec[] columns, FlowVariable[] variables,
			final Class<?> returnType) throws ScriptParseException {
		/*
		 * Escape characters used to mark column names and flow variables in the
		 * expression
		 */
		String escapeColumnStart = ExpressionCompletionProvider.getEscapeColumnStartSymbol();
		String escapeColumnEnd = ExpressionCompletionProvider.getEscapeColumnEndSymbol();
		String escapeFlowVariableStart = ExpressionCompletionProvider.getEscapeFlowVariableStartSymbol();
		String escapeFlowVariableEnd = ExpressionCompletionProvider.getEscapeFlowVariableEndSymbol();

		String escapeSpecialExpressionStart = ExpressionCompletionProvider.getEscapeExpressionStartSymbol();
		String escapeSepecialExpressionEnd = ExpressionCompletionProvider.getEscapeExpressionEndSymbol();

		/*
		 * HashMap used to check if a column/flow variable found in the script actually
		 * exists. Furthermore it stores the DataType/Type so that we can easily access
		 * it with only the original name, i.e. without delimiters.
		 */
		HashMap<String, DataType> columnNameDataTypeMap = new HashMap<>();
		HashMap<String, Type> flowVariableTypeMap = new HashMap<>();

		/* Mapping the original names to the original indexes. */
		HashMap<String, Integer> originalColumnNameIndexMap = new HashMap<>();
		HashMap<String, Integer> originalFlowVariableNameIndexMap = new HashMap<>();

		for (int i = 0; columns != null && i < columns.length; i++) {
			DataColumnSpec column = columns[i];
			columnNameDataTypeMap.put(column.getName(), column.getType());
			originalColumnNameIndexMap.put(column.getName(), i);
		}

		for (int i = 0; variables != null && i < variables.length; i++) {
			FlowVariable variable = variables[i];
			flowVariableTypeMap.put(variable.getName(), variable.getType());
			originalFlowVariableNameIndexMap.put(variable.getName(), i);
		}

		/*
		 * List to store the found column and flow variable names.
		 */
		HashMap<String, String> foundColumnList = new HashMap<>();
		HashMap<String, String> foundFlowVariableList = new HashMap<>();

		/*
		 * booleans to determine if ROWID, ROWINDEX, or ROWCOUNT is used in the
		 * expression.
		 */
		boolean containsROWID = false;
		boolean containsROWINDEX = false;
		boolean containsROWCOUNT = false;

		/*
		 * Split expression into lines, so that we are able to parse line by line. This
		 * makes it easier concerning the handling of the end delimiter (e.g. if its not
		 * in the same line).
		 */
		String[] lines = StringUtils.split(script, "\n");

		/* Search for used column names using the escape characters. */
		for (int i = 0; i < lines.length; i++) {
			int startIndex = 0;
			String line = lines[i];

			/*
			 * Check if ROWID, ROWINDEX, or ROWCOUNT is used in the expression.
			 */
			while ((startIndex = StringUtils.indexOf(line, escapeSpecialExpressionStart, startIndex)) >= 0) {
				int endIndex = StringUtils.indexOf(line, escapeSepecialExpressionEnd, startIndex + 1);

				if (endIndex < 0) {
					throw new ScriptParseException("No ending for '" + escapeSpecialExpressionStart + "' found: "
							+ StringUtils.substring(line, startIndex + 1) + " (at line " + (i + 1)
							+ ") \n\n expression: \n" + script, script, i + 1, line, null);
				}

				String foundExpression = StringUtils.substring(line, startIndex + escapeSpecialExpressionStart.length(),
						endIndex);

				if (foundExpression.equals(Expression.ROWID)) {
					containsROWID = true;
				} else if (foundExpression.equals(Expression.ROWINDEX)) {
					containsROWINDEX = true;
				} else if (foundExpression.equals(Expression.ROWCOUNT)) {
					containsROWCOUNT = true;
				} else {
					throw new ScriptParseException("Special expression '" + foundExpression + "' in line " + i
							+ " is not known. \n\nexpression:\n" + script, script, i + 1, line, null);
				}

				startIndex = endIndex + escapeSepecialExpressionEnd.length();
			}

			startIndex = 0;

			/* Columns */
			/* Continue until we've read all start delimiters. */
			while ((startIndex = StringUtils.indexOf(line, escapeColumnStart, startIndex)) >= 0) {
				int endIndex = StringUtils.indexOf(line, escapeColumnEnd, startIndex + 1);

				if (endIndex < 0) {
					throw new ScriptParseException(
							"No column ending found: " + StringUtils.substring(line, startIndex + 1) + " (at line "
									+ (i + 1) + ") \n\n expression: \n" + script,
							script, i + 1, line, null);
				}

				/* Found a column name. */
				String foundColumn = StringUtils.substring(line, startIndex, endIndex + escapeColumnEnd.length());
				String column = StringUtils.substring(foundColumn, escapeColumnStart.length(),
						foundColumn.length() - escapeColumnEnd.length());

				if (!columnNameDataTypeMap.containsKey(column)) {
					throw new ScriptParseException(
							"Column '" + column + "' in line " + i + " is not known. \n\nexpression:\n" + script,
							script, i + 1, line, null);
				}

				foundColumnList.put(foundColumn, foundColumn);

				/*
				 * Update startIndex in case the end escape is the same as the start escape
				 */
				startIndex = endIndex + escapeColumnEnd.length();
			}

			startIndex = 0;

			/* Flow variables */
			/* Continue until we've read all start delimiters. */
			while ((startIndex = StringUtils.indexOf(line, escapeFlowVariableStart, startIndex)) >= 0) {
				int endIndex = StringUtils.indexOf(line, escapeFlowVariableEnd, startIndex + 1);

				if (endIndex < 0) {
					throw new ScriptParseException(
							"No flow variable ending found: " + StringUtils.substring(line, startIndex + 1)
									+ " (at line " + i + ") \n\n expression: \n" + script,
							script, i + 1, line, null);
				}

				String foundVariable = StringUtils.substring(line, startIndex,
						endIndex + escapeFlowVariableEnd.length());
				String flowVariable = StringUtils.substring(foundVariable, escapeColumnStart.length(),
						foundVariable.length() - escapeFlowVariableEnd.length());

				if (!flowVariableTypeMap.containsKey(flowVariable)) {
					throw new ScriptParseException("Flow variable '" + flowVariable + "' in line " + (i + 1)
							+ " is not known. \n\nexpression:\n" + script, script, i + 1, line, null);
				}

				foundFlowVariableList.put(foundVariable, foundVariable);

				/*
				 * Update startIndex in case the end escape is the same as the start escape
				 */
				startIndex = endIndex + escapeFlowVariableEnd.length();
			}
		}

		/*-- Parse the names of the found columns and flow variables. --*/

		String[] columnNames = foundColumnList.values().toArray(new String[foundColumnList.size()]);
		String[] flowVariableNames = foundFlowVariableList.values().toArray(new String[foundFlowVariableList.size()]);

		/* Mapping from original names to parsed names. */
		Map<String, String> originalToParsedColumnMap = parseNames(script, columnNames);
		Map<String, String> originalToParsedFlowVariableMap = parseNames(script, flowVariableNames);

		/* Special expressions are row id, row index, and row count. */
		String specialStart = ExpressionCompletionProvider.getEscapeExpressionStartSymbol();
		String specialEnd = ExpressionCompletionProvider.getEscapeExpressionEndSymbol();
		ArrayList<String> specialExpressionsList = new ArrayList<>();

		if (containsROWCOUNT) {
			specialExpressionsList.add(specialStart + Expression.ROWCOUNT + specialEnd);
		}
		if (containsROWID) {
			specialExpressionsList.add(specialStart + Expression.ROWID + specialEnd);
		}
		if (containsROWINDEX) {
			specialExpressionsList.add(specialStart + Expression.ROWINDEX + specialEnd);
		}

		String[] specialExpressions = specialExpressionsList.toArray(new String[specialExpressionsList.size()]);

		Map<String, String> originalToParsedSpecialExpressionMap = parseNames(script, specialExpressions);

		/* Mapping from the parsed name to its DataType/Type. Used to create fields. */
		HashMap<String, DataType> parsedColumnDataTypeMap = new HashMap<>();

		/*
		 * -- Create and fill arrays which are used (i.e. names with delimiter) to
		 * replace the found names with their parsed names. --
		 */

		String[] parsedNames = new String[columnNames.length + flowVariableNames.length + specialExpressions.length];
		String[] scriptNames = new String[parsedNames.length];

		HashMap<String, Integer> parsedNameFieldMap = new HashMap<>();

		for (int i = 0; i < parsedNames.length; i++) {
			if (i < columnNames.length) {
				/* Column names */
				scriptNames[i] = columnNames[i];

				/* Add the DataType of each parsed name to the map. */
				String originalName = StringUtils.substring(scriptNames[i], escapeColumnStart.length(),
						scriptNames[i].length() - escapeColumnEnd.length());

				parsedNames[i] = originalToParsedColumnMap.get(originalName);
				parsedColumnDataTypeMap.put(parsedNames[i], columnNameDataTypeMap.get(originalName));

				parsedNameFieldMap.put(parsedNames[i], i);
			} else if (i < columnNames.length + flowVariableNames.length) {
				/* Flow variable names. */
				scriptNames[i] = flowVariableNames[i - columnNames.length];

				String originalName = StringUtils.substring(scriptNames[i], escapeFlowVariableStart.length(),
						scriptNames[i].length() - escapeFlowVariableEnd.length());

				parsedNames[i] = originalToParsedFlowVariableMap.get(originalName);
			} else {
				/* Special expressions. */
				scriptNames[i] = specialExpressions[i - columnNames.length - flowVariableNames.length];

				String originalName = StringUtils.substring(scriptNames[i], escapeSpecialExpressionStart.length(),
						scriptNames[i].length() - escapeSepecialExpressionEnd.length());

				parsedNames[i] = originalToParsedSpecialExpressionMap.get(originalName);
			}
		}

		/* Replace the names with their parsed names. */
		String replacedScript = StringUtils.replaceEach(script, scriptNames, parsedNames);

		/* Creates the fields together with their getters and setters. */
		String[] columnFields = createColumnFields(parsedColumnDataTypeMap);
		String variableFields = createVariableFields(variables, originalToParsedFlowVariableMap,
				originalFlowVariableNameIndexMap);

		String[] specialExpressionFields = createSpecialExpressionFields(originalToParsedSpecialExpressionMap);

		/* Appends needed methods provided by ExpressionSetRegistry. */
		String[] methods = createMethods(script);

		/*-- Creates the parsed script, wrapped in a method wrapped in a class. --*/

		StringBuilder classBuilder = new StringBuilder();

		/*
		 * Note: class is omitted as otherwise the user has to use 'def' to define
		 * variables in the expression. Without a surrounding class 'def' can be
		 * omitted. However, we need now setters for flow variables as their fields are
		 * now unknown, which isn't the case if we would have a surrounding class. Thus,
		 * we place the variable fields simply into the 'main' method which wraps the
		 * original expression. For more complex scripts, i.e., scripts that don't have
		 * a 'main' method to invoke the execution but rather several methods which are
		 * invoked from somewhere else, variable fields have to be set similar to column
		 * fields.
		 */
		/* Imports */
		classBuilder.append(methods[1]);
		classBuilder.append("\n\n");
		/* Fields */
		classBuilder.append(columnFields[0]);
		classBuilder.append("\n");
		classBuilder.append(specialExpressionFields[0]);
		classBuilder.append("\n");
		/* Main method */
		String methodReturn = returnType != null ? returnType.getName() : "def";
		classBuilder.append(methodReturn);
		classBuilder.append(" ");
		classBuilder.append(METHOD_NAME);
		classBuilder.append("() {\n");
		classBuilder.append(variableFields);
		classBuilder.append("\n");
		/* Parsed script */
		classBuilder.append(replacedScript);
		classBuilder.append("\n}\n");
		/* Setters */
		classBuilder.append(columnFields[1]);
		classBuilder.append(specialExpressionFields[1]);
		/* Getters */
		classBuilder.append(columnFields[2]);
		classBuilder.append(specialExpressionFields[2]);
		/* Methods */
		classBuilder.append(methods[0]);

		/* Types of the used fields. */
		Class<?>[] columnFieldTypes = getUsedColumnTypes(columnNameDataTypeMap, parsedNameFieldMap,
				originalToParsedColumnMap);

		return new DefaultParsedScript(classBuilder.toString(), returnType, parsedNameFieldMap,
				originalToParsedColumnMap, originalColumnNameIndexMap, columnFieldTypes,
				originalToParsedSpecialExpressionMap.get(Expression.ROWID),
				originalToParsedSpecialExpressionMap.get(Expression.ROWINDEX),
				originalToParsedSpecialExpressionMap.get(Expression.ROWCOUNT));
	}

	/**
	 * Creates the fields together with their getters and setters, which are used to
	 * create the class containing the script.
	 * 
	 * @param columnDataTypeMap
	 *            Mapping from original names to their parsed names.
	 * @return Array containing the field declarations at position 0, the setters at
	 *         position 1, and the getters at position 2.
	 */
	private static String[] createSpecialExpressionFields(Map<String, String> originalToParsedSpecialExpressionMap) {
		StringBuilder fieldBuilder = new StringBuilder();
		StringBuilder getterBuilder = new StringBuilder();
		StringBuilder setterBuilder = new StringBuilder();

		for (String field : originalToParsedSpecialExpressionMap.keySet()) {
			String javaType = "";

			if (field.equals(Expression.ROWINDEX) || field.equals(Expression.ROWCOUNT)) {
				javaType = "Long";
			} else {
				javaType = "String";
			}

			field = originalToParsedSpecialExpressionMap.get(field);

			fieldBuilder.append(javaType + " " + field + ";\n");

			String setter = "\n\n void set" + field + "(" + javaType + " arg) {" + field + " = arg;}";
			setterBuilder.append(setter);

			String getter = "\n\n" + javaType + " get" + field + "(){ return " + field + ";}";
			getterBuilder.append(getter);
		}

		return new String[] { fieldBuilder.toString(), setterBuilder.toString(), getterBuilder.toString() };
	}

	/**
	 * Appends needed methods provided by {@link ExpressionSetRegistry}
	 * 
	 * @param script
	 *            The script to be compiled.
	 * @return Array containing already defined methods at position 0 and needed
	 *         imports at position 1.
	 */
	private static String[] createMethods(String script) {
		List<ExpressionSet> expressionSets = ExpressionSetRegistry.getExpressionSets();

		StringBuilder methodBuilder = new StringBuilder();
		StringBuilder importBuilder = new StringBuilder();

		LinkedList<org.knime.expressions.core.Expression> notAppended = new LinkedList<>();

		for (ExpressionSet set : expressionSets) {
			outer: for (org.knime.expressions.core.Expression exp : set.getExpressions()) {
				String name = exp.getName() + "(";
				int idx = StringUtils.indexOf(script, name);

				/*
				 * This guarantees that we don't accidentally append methods whose names are a
				 * sub-part of another method. One example would be or(...) and for(...).
				 */
				while (idx >= 0) {
					if (idx == 0 || !Character.isJavaIdentifierPart(script.charAt(idx - 1))) {
						methodBuilder.append("\n\n");
						methodBuilder.append(exp.getScript());

						importBuilder.append(exp.getImports());
						importBuilder.append("\n");

						continue outer;
					}

					idx = StringUtils.indexOf(script, name, idx + name.length());
				}
				notAppended.add(exp);
			}
		}

		/*
		 * Append methods recursively in case a method uses another predefined method,
		 * until no further methods have been appended.
		 */
		String appendedMethods = methodBuilder.toString();

		while (!appendedMethods.equals("")) {
			StringBuilder additionalMethods = new StringBuilder();
			LinkedList<org.knime.expressions.core.Expression> tempList = new LinkedList<>();

			outer: for (org.knime.expressions.core.Expression exp : notAppended) {
				String name = exp.getName() + "(";
				int idx = StringUtils.indexOf(script, name);

				/*
				 * This guarantees that we don't accidentally append methods whose names are a
				 * sub-part of another method. One example would be or(...) and for(...).
				 */
				while (idx >= 0) {
					if (idx == 0 || !Character.isJavaIdentifierPart(script.charAt(idx - 1))) {
						additionalMethods.append("\n\n");
						additionalMethods.append(exp.getScript());

						importBuilder.append(exp.getImports());
						importBuilder.append("\n");

						continue outer;
					}

					idx = StringUtils.indexOf(script, name, idx + name.length());
				}
				tempList.add(exp);
			}

			notAppended = tempList;
			appendedMethods = additionalMethods.toString();
			methodBuilder.append(appendedMethods);
		}

		return new String[] { methodBuilder.toString(), importBuilder.toString() };
	}

	/**
	 * Creates initialized fields with the values of the {@link FlowVariable}s.
	 * 
	 * @param variables
	 *            Available {@link FlowVariables}.
	 * @param originalToParsedNameMap
	 *            Mapping from original to parsed names.
	 * @param originalFlowVariableNameIndexMap
	 *            Mapping from original names to the indexes in the provided array.
	 * @return
	 */
	private static String createVariableFields(FlowVariable[] variables, Map<String, String> originalToParsedNameMap,
			Map<String, Integer> originalFlowVariableNameIndexMap) {
		StringBuilder fieldBuilder = new StringBuilder();

		for (String originalName : originalToParsedNameMap.keySet()) {
			FlowVariable variable = variables[originalFlowVariableNameIndexMap.get(originalName)];
			String field = originalToParsedNameMap.get(originalName);

			String javaType = ExpressionConverterUtils.extractJavaTypeString(variable.getType());
			Object value = ExpressionConverterUtils.extractFlowVariable(variable);

			String valueString = value.toString();

			if (variable.getType() == Type.STRING) {
				valueString = "\"" + StringUtils.replace(valueString, "\\", "\\\\") + "\"";
			}

			fieldBuilder.append(javaType + " " + field + " = " + valueString + ";\n");

		}

		return fieldBuilder.toString();
	}

	/**
	 * Gets the Java types used by the fields in the script. The types are in the
	 * same order as the fields occur.
	 * 
	 * @param columnTypeMap
	 *            Mapping from original column names to their types.
	 * @param columnIndexMap
	 *            Mapping from parsed column names to their field index in the
	 *            script.
	 * @param columnNameMap
	 *            Mapping from original column names to parsed column names.
	 * @return Array containing the field's {@link Class}es.
	 */
	private static Class<?>[] getUsedColumnTypes(Map<String, DataType> columnTypeMap,
			Map<String, Integer> columnIndexMap, Map<String, String> columnNameMap) {
		Class<?>[] types = new Class<?>[columnNameMap.size()];

		for (String originalName : columnNameMap.keySet()) {
			String parsedName = columnNameMap.get(originalName);

			types[columnIndexMap.get(parsedName)] = ExpressionConverterUtils
					.getDestinationType(columnTypeMap.get(originalName));
		}

		return types;
	}

	/**
	 * Creates the fields together with their getters and setters, which are used to
	 * create the class containing the script.
	 * 
	 * @param columnDataTypeMap
	 *            Map containing the {@link DataType} for each column that will be
	 *            represented as a field.
	 * @return Array containing the field declarations at position 0, the setters at
	 *         position 1, and the getters at position 2.
	 */
	private static String[] createColumnFields(Map<String, DataType> columnDataTypeMap) {
		StringBuilder fieldBuilder = new StringBuilder();
		StringBuilder getterBuilder = new StringBuilder();
		StringBuilder setterBuilder = new StringBuilder();

		for (String field : columnDataTypeMap.keySet()) {
			String javaType = "";

			javaType = ExpressionConverterUtils.extractJavaTypeString(columnDataTypeMap.get(field));

			fieldBuilder.append(javaType + " " + field + ";\n");

			String setter = "\n\n void set" + field + "(" + javaType + " arg) {" + field + " = arg;}";
			setterBuilder.append(setter);

			String getter = "\n\n" + javaType + " get" + field + "(){ return " + field + ";}";
			getterBuilder.append(getter);
		}

		return new String[] { fieldBuilder.toString(), setterBuilder.toString(), getterBuilder.toString() };
	}

	/**
	 * Compiles the script into a Java {@link Class}, which can be used to
	 * instantiate objects of the {@link ParsedScript}.
	 * 
	 * @param info
	 *            {@link ParsedScript} containing the parsed script with all
	 *            necessary information.
	 * @return {@link FunctionScript} containing the compiled class and necessary
	 *         information.
	 * @throws ScriptCompilationException
	 *             Thrown when an error occurs during the compiling, e.g. through
	 *             syntax mistakes.
	 */
	// this is actually just an intermediate step which might be useful later... (or
	// not)
	public static Class<?> compile(ParsedScript info) throws ScriptCompilationException {
		try (GroovyClassLoader loader = new GroovyClassLoader();) {
			Class<?> parsedClass = loader.parseClass(info.getScript());

			return parsedClass;
		} catch (CompilationFailedException ex) {
			throw new ScriptCompilationException(ex);
		} catch (IOException ex) {
			// Should not happen as we parse the class from a String and not a File.
			throw new IllegalStateException(
					"Groovy class loader could not be closed. This should not happen as we only parse classes from Strings and not from files.",
					ex);
		}
	}

	/**
	 * Parses the used column and flow variable names in such a way that special
	 * characters are replaced. Additionally, column names will get the prefix "c_"
	 * whereas flow variables will get the prefix "f_". This ensures that flow
	 * variables and column variables with the same name can be distinguished.
	 * Furthermore the script will be checked if the parsed name is already in use
	 * and may alternate it depending on the given result. This method should be
	 * called separately for column names and flow variable names.
	 * 
	 * @param script
	 *            The original script that is being parsed.
	 * @param names
	 *            The used names together with their delimiters.
	 * 
	 * @return A mapping from the original column/flow variable names to their
	 *         parsed names.
	 */
	private static Map<String, String> parseNames(final String script, final String[] names) {
		HashMap<String, String> nameMap = new HashMap<>(names.length);

		if (names.length == 0) {
			return nameMap;
		}

		HashSet<String> usedNames = new HashSet<>(names.length);
		Random random = new Random();

		/* Check if the given names are flow variables or column variables. */
		boolean isFlowVariable = names[0].startsWith(ExpressionCompletionProvider.getEscapeFlowVariableStartSymbol());
		boolean isSpecialRow = names[0].startsWith(ExpressionCompletionProvider.getEscapeExpressionStartSymbol());

		for (String name : names) {
			/* Strip the found name down to the actual column name. */
			if (isFlowVariable) {
				name = name.substring(ExpressionCompletionProvider.getEscapeFlowVariableStartSymbol().length(),
						name.length() - ExpressionCompletionProvider.getEscapeFlowVariableEndSymbol().length());
			} else if (isSpecialRow) {
				name = name.substring(ExpressionCompletionProvider.getEscapeExpressionStartSymbol().length(),
						name.length() - ExpressionCompletionProvider.getEscapeExpressionEndSymbol().length());
			} else {
				name = name.substring(ExpressionCompletionProvider.getEscapeColumnStartSymbol().length(),
						name.length() - ExpressionCompletionProvider.getEscapeColumnEndSymbol().length());
			}

			/* Remove all characters that aren't letters or numbers or '_'. */
			String parsedName = name.replaceAll("[^A-Za-z0-9_]", "");

			/*
			 * Add the specific prefix (also ensures that a variable doesn't start with a
			 * number).
			 */
			if (isFlowVariable) {
				parsedName = "f_" + parsedName;
			} else if (isSpecialRow) {
				parsedName = "se_" + parsedName;
			} else {
				parsedName = "c_" + parsedName;
			}

			/*
			 * If the name is already contained in the script (i.e. a variable with the same
			 * name exist), append a random number until we have a free variable name.
			 */
			while (script.contains(parsedName) || usedNames.contains(parsedName)) {
				parsedName += random.nextInt(100);
			}

			usedNames.add(parsedName);
			nameMap.put(name, parsedName);
		}

		return nameMap;
	}

	static String getInvokeMethodName() {
		return METHOD_NAME;
	}
}
