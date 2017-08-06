package ml.alternet.security.web.server;

/**
 * Embed size limits (content size and number of keys)
 * when handling forms.
 *
 * @author Philippe Poulard
 */
public interface FormLimit {

    /**
     * Get the maximum content size of a form post, to protect
     * against DOS attacks from large forms. <code>-1</code>
     * means no limit.
     *
     * @return maxFormContentSize, e.g. 10000
     */
    int getMaxFormContentSize();

    /**
     * Get the maximum size of a form post, to protect against
     * DOS attacks from large forms. <code>-1</code>
     * means no limit.
     *
     * @return maxFormKeys, e.g. 100
     */
    int getMaxFormKeys();

}
