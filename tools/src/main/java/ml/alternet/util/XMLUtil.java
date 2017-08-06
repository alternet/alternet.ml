package ml.alternet.util;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

/**
 * XML-related utilities.
 *
 * @author Philippe Poulard
 */
@Util
public final class XMLUtil {

    private XMLUtil() {
    }

    /**
     * Indicates if the given MIME type is related to XML or not.
     * <p>
     * XML MIME types :
     * </p>
     * <ul>
     * <li>"<tt>text/xml</tt>",</li>
     * <li>"<tt>application/xml</tt>",</li>
     * <li>"<tt>image/svg+xml</tt>",</li>
     * <li>etc</li>
     * </ul>
     * <p>
     * Non-XML MIME types :
     * </p>
     * <ul>
     * <li>"<tt>application/xml-dtd</tt>",</li>
     * <li>"<tt>application/xml-external-parsed-entity</tt>",</li>
     * <li>etc</li>
     * </ul>
     *
     * @param mimeType
     *            The MIME type to test.
     * @return <code>true</code> if the MIME type contains the string "xml" not
     *         followed by "-", <code>false</code> otherwise.
     */
    public static boolean isXMLMimeType(String mimeType) {
        int index = mimeType.indexOf("xml");
        return index != -1 && (index + 3 < mimeType.length()) ? mimeType.charAt(index + 3) != '-' : true;
        // TRUE for :
        //          "application/xml", "text/xml", "image/svg+xml",
        //          "application/xhtml+xml", etc
        // but not  "application/xml-dtd",
        //          "application/xml-external-parsed-entity" etc
    }

    /**
     * Return the qualified name, that is to say the prefix -if any- with the
     * local name.
     *
     * @param qName
     *            The QName.
     * @return A string that looks like "<tt>prefix:localName</tt>" or "
     *         <tt>NCName</tt>".
     */
    public static String getQualifiedName(QName qName) {
        if (!XMLConstants.NULL_NS_URI.equals(qName.getNamespaceURI())
                && !XMLConstants.DEFAULT_NS_PREFIX.equals(qName.getPrefix()))
        {
            return qName.getPrefix() + ":" + qName.getLocalPart();
        } else {
            return qName.getLocalPart();
        }
    }

    /**
     * Indicates whether a character is an XML whitespace.
     *
     * @param c
     *            The character.
     * @return <tt>true</tt> if the character is an XML whitespace,
     *         <tt>false</tt> otherwise.
     */
    public static boolean isWhitespace(char c) {
        return c == ' ' || c == '\n' || c == '\r' || c == '\t';
    }

    /**
     * Indicates whether a character sequence is an XML whitespace.
     *
     * @param ch
     *            The char sequence.
     * @param start
     *            The start position.
     * @param length
     *            The end position.
     * @return <tt>true</tt> if the character sequence contains only XML
     *         whitespaces, <tt>false</tt> otherwise.
     */
    public static boolean isWhitespace(char[] ch, int start, int length) {
        for (int i = start; i < start + length; i++) {
            char c = ch[i];
            if (c != '\n' && c != '\t' && c != '\r' && c != ' ') {
                return false;
            }
        }
        return true;
    }

}
