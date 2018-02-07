package org.knime.expressions;

import java.util.Map;

import org.knime.ext.sun.nodes.script.expression.Expression;

/**
 * Interface for a ParsedScript, i.e., a script that has been parsed and which
 * stores all necessary information to provide input to the script.
 * 
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 *
 */
public interface ParsedScript extends Script{

	/**
	 * 
	 * @return The field name in the script representing {@link Expression#ROWID}.
	 */
	public String getRowIdField();

	/**
	 * 
	 * @return The field name in the script representing
	 *         {@link Expression#ROWINDEX}.
	 */
	public String getRowIndexField();

	/**
	 * 
	 * @return The field name in the script representing
	 *         {@link Expression#ROWCOUNT}.
	 */
	public String getRowCountField();

	/**
	 * 
	 * @return {@code true} if the expression uses specified columns, otherwise
	 *         {@code false}
	 */
	public boolean usesColumns();

	/**
	 * @return Underlying expression script.
	 */
	public String getScript();

	/**
	 * @return Java type that is returned by the script.
	 */
	public Class<?> getReturnType();

	/**
	 * @return Mapping from the original column names to their field names in the
	 *         script.
	 */
	public Map<String, String> getColumnNameMap();

	/**
	 * @return Mapping from the original column names to the original column index
	 *         of the input table.
	 */
	public Map<String, Integer> getColumnTableMap();

	/**
	 * @return Types of the fields used in the script.
	 */
	public Class<?>[] getFieldTypes();

	/**
	 * @return Mapping from the field names of the script to the index of the
	 *         provided input.
	 */
	public Map<String, Integer> getFieldInputMap();
}
