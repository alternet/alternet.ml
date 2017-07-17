package ml.alternet.scan;

import ml.alternet.util.XMLUtil;

/**
 * Read only XML whitespaces.
 *
 * @see XMLUtil#isWhitespace(char)
 *
 * @author Philippe Poulard
 */
public class XMLWhitespace extends JavaWhitespace {

    @Override
    public boolean test(Integer cp) {
        return XMLUtil.isWhitespace((char) cp.intValue());
    }

}
