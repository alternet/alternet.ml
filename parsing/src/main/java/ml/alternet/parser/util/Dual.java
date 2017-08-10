package ml.alternet.parser.util;

/**
 * Wraps a value subject to transformation
 * (the value may exist in its source form,
 * or its target form).
 *
 * @author Philippe Poulard
 *
 * @param <Source> The source type
 * @param <Target> The target type
 */
public class Dual<Source, Target> {

    private Object value;
    private boolean isSource;

    /**
     * Set this value as a source.
     *
     * @param source The source value.
     *
     * @return This
     *
     * @param <T> The concrete dual type.
     */
    @SuppressWarnings("unchecked")
    public <T extends Dual<Source, Target>> T setSource(Source source) {
        this.value = source;
        this.isSource = true;
        return (T) this;
    }

    /**
     * Set this value as a target.
     *
     * @param target The target value.
     *
     * @return This
     *
     * @param <T> The concrete dual type.
     */
    @SuppressWarnings("unchecked")
    public <T extends Dual<Source, Target>> T setTarget(Target target) {
        this.value = target;
        this.isSource = false;
        return (T) this;
    }

    /**
     * Set this value and this type.
     *
     * @param value The value.
     *
     * @return This
     *
     * @param <T> The concrete dual type.
     */
    @SuppressWarnings("unchecked")
    public <T extends Dual<Source, Target>> T setValue(Dual<Source, Target> value) {
        this.value = value.value;
        this.isSource = value.isSource;
        return (T) this;
    }

    /**
     * Return this source value.
     *
     * @return The value.
     */
    @SuppressWarnings("unchecked")
    public Source getSource() {
        return (Source) this.value;
    }

    /**
     * Return this target value.
     *
     * @return The value.
     */
    @SuppressWarnings("unchecked")
    public Target getTarget() {
        return (Target) this.value;
    }

    /**
     * Indicates which kind of value is actually wrapped.
     *
     * @return <code>true</code> if the value is a source,
     *      <code>false</code> otherwise.
     */
    public boolean isSource() {
        return this.isSource;
    }

    @Override
    public String toString() {
        return (isSource() ? "⦉" : "⦗")
            + (this.value == null ? "" : this.value.toString())
            + (isSource() ? "⦊" : "⦘");
    }

}
