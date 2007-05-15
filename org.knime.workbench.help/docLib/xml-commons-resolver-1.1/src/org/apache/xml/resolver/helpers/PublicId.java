// PublicId.java - Information about public identifiers

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

package org.apache.xml.resolver.helpers;

/**
 * Static methods for dealing with public identifiers.
 *
 * <p>This class defines a set of static methods that can be called
 * to handle public identifiers.</p>
 *
 * @author Norman Walsh
 * <a href="mailto:Norman.Walsh@Sun.COM">Norman.Walsh@Sun.COM</a>
 *
 * @version 1.0
 */
public abstract class PublicId {
  protected PublicId() { }

  /**
   * Normalize a public identifier.
   *
   * <p>Public identifiers must be normalized according to the following
   * rules before comparisons between them can be made:</p>
   *
   * <ul>
   * <li>Whitespace characters are normalized to spaces (e.g., line feeds,
   * tabs, etc. become spaces).</li>
   * <li>Leading and trailing whitespace is removed.</li>
   * <li>Multiple internal whitespaces are normalized to a single
   * space.</li>
   * </ul>
   *
   * <p>This method is declared static so that other classes
   * can use it directly.</p>
   *
   * @param publicId The unnormalized public identifier.
   *
   * @return The normalized identifier.
   */
  public static String normalize(String publicId) {
    String normal = publicId.replace('\t', ' ');
    normal = normal.replace('\r', ' ');
    normal = normal.replace('\n', ' ');
    normal = normal.trim();

    int pos;

    while ((pos = normal.indexOf("  ")) >= 0) {
      normal = normal.substring(0, pos) + normal.substring(pos+1);
    }

    return normal;
  }

  /**
   * Encode a public identifier as a "publicid" URN.
   *
   * <p>This method is declared static so that other classes
   * can use it directly.</p>
   *
   * @param publicId The unnormalized public identifier.
   *
   * @return The normalized identifier.
   */
  public static String encodeURN(String publicId) {
    String urn = PublicId.normalize(publicId);

    urn = PublicId.stringReplace(urn, "%", "%25");
    urn = PublicId.stringReplace(urn, ";", "%3B");
    urn = PublicId.stringReplace(urn, "'", "%27");
    urn = PublicId.stringReplace(urn, "?", "%3F");
    urn = PublicId.stringReplace(urn, "#", "%23");
    urn = PublicId.stringReplace(urn, "+", "%2B");
    urn = PublicId.stringReplace(urn, " ", "+");
    urn = PublicId.stringReplace(urn, "::", ";");
    urn = PublicId.stringReplace(urn, ":", "%3A");
    urn = PublicId.stringReplace(urn, "//", ":");
    urn = PublicId.stringReplace(urn, "/", "%2F");

    return "urn:publicid:" + urn;
  }

  /**
   * Decode a "publicid" URN into a public identifier.
   *
   * <p>This method is declared static so that other classes
   * can use it directly.</p>
   *
   * @param publicId The unnormalized public identifier.
   *
   * @return The normalized identifier.
   */
  public static String decodeURN(String urn) {
    String publicId = "";

    if (urn.startsWith("urn:publicid:")) {
      publicId = urn.substring(13);
    } else {
      return urn;
    }

    publicId = PublicId.stringReplace(publicId, "%2F", "/");
    publicId = PublicId.stringReplace(publicId, ":", "//");
    publicId = PublicId.stringReplace(publicId, "%3A", ":");
    publicId = PublicId.stringReplace(publicId, ";", "::");
    publicId = PublicId.stringReplace(publicId, "+", " ");
    publicId = PublicId.stringReplace(publicId, "%2B", "+");
    publicId = PublicId.stringReplace(publicId, "%23", "#");
    publicId = PublicId.stringReplace(publicId, "%3F", "?");
    publicId = PublicId.stringReplace(publicId, "%27", "'");
    publicId = PublicId.stringReplace(publicId, "%3B", ";");
    publicId = PublicId.stringReplace(publicId, "%25", "%");

    return publicId;
    }

  /**
   * Replace one string with another.
   *
   */
  private static String stringReplace(String str,
				      String oldStr,
				      String newStr) {

    String result = "";
    int pos = str.indexOf(oldStr);

    //    System.out.println(str + ": " + oldStr + " => " + newStr);

    while (pos >= 0) {
      //      System.out.println(str + " (" + pos + ")");
      result += str.substring(0, pos);
      result += newStr;
      str = str.substring(pos+1);

      pos = str.indexOf(oldStr);
    }

    return result + str;
  }
}
