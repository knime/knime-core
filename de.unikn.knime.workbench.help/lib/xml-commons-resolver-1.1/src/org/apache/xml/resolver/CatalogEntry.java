// CatalogEntry.java - Represents Catalog entries

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.xml.resolver;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Represents a Catalog entry.
 *
 * <p>Instances of this class represent individual entries
 * in a Catalog.</p>
 *
 * <p>Each catalog entry has a unique name and is associated with
 * an arbitrary number of arguments (all strings). For example, the
 * TR9401 catalog entry "PUBLIC" has two arguments, a public identifier
 * and a system identifier. Each entry has a unique numeric type,
 * assigned automatically when the entry type is created.</p>
 *
 * <p>The number and type of catalog entries is maintained
 * <em>statically</em>. Catalog classes, or their subclasses, can add
 * new entry types, but all Catalog objects share the same global pool
 * of types.</p>
 *
 * <p>Initially there are no valid entries.</p>
 *
 * @see Catalog
 *
 * @author Norman Walsh
 * <a href="mailto:Norman.Walsh@Sun.COM">Norman.Walsh@Sun.COM</a>
 *
 * @version 1.0
 */
public class CatalogEntry {
  /** The nextEntry is the ordinal number of the next entry type. */
  protected static int nextEntry = 0;

  /**
   * The entryTypes vector maps catalog entry names
   * (e.g., 'BASE' or 'SYSTEM') to their type (1, 2, etc.).
   * Names are case sensitive.
   */
  protected static Hashtable entryTypes = new Hashtable();

  /** The entryTypes vector maps catalog entry types to the
      number of arguments they're required to have. */
  protected static Vector entryArgs = new Vector();

  /**
   * Adds a new catalog entry type.
   *
   * @param name The name of the catalog entry type. This must be
   * unique among all types and is case-sensitive. (Adding a duplicate
   * name effectively replaces the old type with the new type.)
   * @param numArgs The number of arguments that this entry type
   * is required to have. There is no provision for variable numbers
   * of arguments.
   * @return The type for the new entry.
   */
  public static int addEntryType(String name, int numArgs) {
    entryTypes.put(name, new Integer(nextEntry));
    entryArgs.add(nextEntry, new Integer(numArgs));
    nextEntry++;

    return nextEntry-1;
  }

  /**
   * Lookup an entry type
   *
   * @param name The name of the catalog entry type.
   * @return The type of the catalog entry with the specified name.
   * @throws InvalidCatalogEntryTypeException if no entry has the
   * specified name.
   */
  public static int getEntryType(String name)
    throws CatalogException {
    if (!entryTypes.containsKey(name)) {
      throw new CatalogException(CatalogException.INVALID_ENTRY_TYPE);
    }

    Integer iType = (Integer) entryTypes.get(name);

    if (iType == null) {
      throw new CatalogException(CatalogException.INVALID_ENTRY_TYPE);
    }

    return iType.intValue();
  }

  /**
   * Find out how many arguments an entry is required to have.
   *
   * @param name The name of the catalog entry type.
   * @return The number of arguments that entry type is required to have.
   * @throws InvalidCatalogEntryTypeException if no entry has the
   * specified name.
   */
  public static int getEntryArgCount(String name)
    throws CatalogException {
    return getEntryArgCount(getEntryType(name));
  }

  /**
   * Find out how many arguments an entry is required to have.
   *
   * @param type A valid catalog entry type.
   * @return The number of arguments that entry type is required to have.
   * @throws InvalidCatalogEntryTypeException if the type is invalid.
   */
  public static int getEntryArgCount(int type)
    throws CatalogException {
    try {
      Integer iArgs = (Integer) entryArgs.get(type);
      return iArgs.intValue();
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new CatalogException(CatalogException.INVALID_ENTRY_TYPE);
    }
  }

  /** The entry type of this entry */
  protected int entryType = 0;

  /** The arguments associated with this entry */
  protected Vector args = null;

  /**
   * Null constructor; something for subclasses to call.
   */
  public CatalogEntry() {}

  /**
   * Construct a catalog entry of the specified type.
   *
   * @param name The name of the entry type
   * @param args A String Vector of arguments
   * @throws InvalidCatalogEntryTypeException if no such entry type
   * exists.
   * @throws InvalidCatalogEntryException if the wrong number of arguments
   * is passed.
   */
  public CatalogEntry(String name, Vector args)
    throws CatalogException {
    Integer iType = (Integer) entryTypes.get(name);

    if (iType == null) {
      throw new CatalogException(CatalogException.INVALID_ENTRY_TYPE);
    }

    int type = iType.intValue();

    try {
      Integer iArgs = (Integer) entryArgs.get(type);
      if (iArgs.intValue() != args.size()) {
	throw new CatalogException(CatalogException.INVALID_ENTRY);
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new CatalogException(CatalogException.INVALID_ENTRY_TYPE);
    }

    entryType = type;
    this.args = args;
  }

  /**
   * Construct a catalog entry of the specified type.
   *
   * @param name The name of the entry type
   * @param args A String Vector of arguments
   * @throws InvalidCatalogEntryTypeException if no such entry type
   * exists.
   * @throws InvalidCatalogEntryException if the wrong number of arguments
   * is passed.
   */
  public CatalogEntry(int type, Vector args)
    throws CatalogException {
    try {
      Integer iArgs = (Integer) entryArgs.get(type);
      if (iArgs.intValue() != args.size()) {
	throw new CatalogException(CatalogException.INVALID_ENTRY);
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new CatalogException(CatalogException.INVALID_ENTRY_TYPE);
    }

    entryType = type;
    this.args = args;
  }

  /**
   * Get the entry type.
   *
   * @return The entry type of the CatalogEntry
   */
  public int getEntryType() {
    return entryType;
  }

  /**
   * Get an entry argument.
   *
   * @param argNum The argument number (arguments are numbered from 0).
   * @return The specified argument or null if an invalid argNum is
   * provided.
   */
  public String getEntryArg(int argNum) {
    try {
      String arg = (String) args.get(argNum);
      return arg;
    } catch (ArrayIndexOutOfBoundsException e) {
      return null;
    }
  }

  /**
   * Set an entry argument.
   *
   * <p>Catalogs sometimes need to adjust the catlog entry parameters,
   * for example to make a relative URI absolute with respect to the
   * current base URI. But in general, this function should only be
   * called shortly after object creation to do some sort of cleanup.
   * Catalog entries should not mutate over time.</p>
   *
   * @param argNum The argument number (arguments are numbered from 0).
   * @throws ArrayIndexOutOfBoundsException if an invalid argument
   * number is provided.
   */
  public void setEntryArg(int argNum, String newspec)
    throws ArrayIndexOutOfBoundsException {
    args.set(argNum, newspec);
  }
}
