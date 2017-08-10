package ml.alternet.parser.util;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Supplier;

import ml.alternet.facet.Rewindable;

/**
 * A markable stack.
 *
 * @author Philippe Poulard
 *
 * @param <V> The type of value.
 */
public class ValueStack<V> implements Rewindable, Iterable<V>, Supplier<V> {

    /**
     * Chain values.
     *
     * @author Philippe Poulard
     *
     * @param <E> The type of value.
     */
    protected static class Value<E> {

        /**
         * The actual value.
         */
        protected final E value;

        /**
         * The previous values.
         */
        protected final Value<E> tail;

        /**
         * Wraps a value.
         *
         * @param value The actual values.
         * @param tail The previous values.
         */
        protected Value(E value, Value<E> tail) {
            this.value = value;
            this.tail = tail;
        }
    }

    Value<V> head;
    V tempValue;
    Deque<Value<V>> marks = new LinkedList<>();

    /**
     * Initializes an empty value stack.
     */
    public ValueStack() { }

    /**
     * Initializes a value stack containing the given values with the last value being at the top of the stack.
     *
     * @param values the initial stack values
     */
    public ValueStack(Iterable<V> values) {
        pushAll(values);
    }

    /**
     * Indicates whether this stack is empty.
     *
     * @return <code>true</code> if this stack is empty,
     *      <code>false</code> otherwise.
     */
    public boolean isEmpty() {
        return head == null;
    }

    /**
     * Return the number of elements in this stack.
     *
     * @return This stack's size.
     */
    public int size() {
        Value<V> cursor = head;
        int size = 0;
        while (cursor != null) {
            size++;
            cursor = cursor.tail;
        }
        return size;
    }

    /**
     * Clear the content of this stack.
     */
    public void clear() {
        head = null;
    }

    @Override
    public void mark() {
        this.marks.push(this.head);
    }

    @Override
    public void cancel() throws IllegalStateException {
        this.head = this.marks.pop();
    }

    @Override
    public void consume() throws IllegalStateException {
        this.marks.pop();
    }

    /**
     * Push a value
     *
     * @param value The actual value.
     */
    public void push(V value) {
        head = new Value<>(value, head);
    }

    /**
     * Push a value at a specific position.
     *
     * @param down The position where to push the value.
     * @param value The actual value.
     */
    public void push(int down, V value) {
        head = push(down, value, head);
    }

    private Value<V> push(int down, V value, Value<V> head) {
        if (down == 0) {
            return new Value<>(value, head);
        }
        if (down > 0) {
            return new Value<>(head.value, push(down - 1, value, head.tail));
        }
        throw new IllegalArgumentException("Argument 'down' must not be negative");
    }

    /**
     * Push some values.
     *
     * @param values The actual values.
     */
    public void pushAll(V... values) {
        head = null;
        for (V value : values) {
            push(value);
        }
    }

    /**
     * Push some values.
     *
     * @param values The actual values.
     */
    public void pushAll(Iterable<V> values) {
        head = null;
        for (V value : values) {
            push(value);
        }
    }

    /**
     * Pop the top value.
     *
     * @return The top value.
     */
    public V pop() {
        return pop(0);
    }

    /**
     * Pop the value at a given position.
     *
     * @param down The position of the value to pop.
     * @return The value.
     */
    public V pop(int down) {
        head = pop(down, head);
        V result = tempValue;
        tempValue = null; // avoid memory leak
        return result;
    }

    private Value<V> pop(int down, Value<V> head) {
        if (down == 0) {
            tempValue = head.value;
            return head.tail;
        }
        if (down > 0) {
            return new Value<>(head.value, pop(down - 1, head.tail));
        }
        throw new IllegalArgumentException("Argument 'down' must not be negative");
    }

    /**
     * Peek the top value.
     *
     * @return The top value.
     */
    public V peek() {
        return peek(0);
    }

    /**
     * Peek the value at a given position.
     *
     * @param down The position of the value to peek.
     * @return The value.
     */
    public V peek(int down) {
        return peek(down, head);
    }

    private V peek(int down, Value<V> head) {
        if (down == 0) {
            return head.value;
        }
        if (down > 0) {
            return peek(down - 1, head.tail);
        }
        throw new IllegalArgumentException("Argument 'down' must not be negative");
    }

    /**
     * Poke a value.
     *
     * @param value The actual value.
     */
    public void poke(V value) {
        poke(0, value);
    }

    /**
     * Poke a value at a specific position.
     *
     * @param down The position where to poke the value.
     * @param value The actual value.
     */
    public void poke(int down, V value) {
        head = poke(down, value, head);
    }

    private Value<V> poke(int down, V value, Value<V> head) {
        if (down == 0) {
            return new Value<>(value, head.tail);
        }
        if (down > 0) {
            return new Value<>(head.value, poke(down - 1, value, head.tail));
        }
        throw new IllegalArgumentException("Argument 'down' must not be negative");
    }

    /**
     * Duplicate the top value.
     */
    public void dup() {
        push(peek());
    }

    /**
     * Swap the 2 first values.
     */
    public void swap() {
        Value<V> down1 = head.tail;
        head = new Value<>(down1.value, new Value<>(head.value, down1.tail));
    }

    /**
     * Swap the 3 first values.
     */
    public void swap3() {
        Value<V> down1 = head.tail;
        Value<V> down2 = down1.tail;
        head = new Value<>(down2.value, new Value<>(down1.value, new Value<>(head.value, down2.tail)));
    }

    /**
     * Swap the 4 first values.
     */
    public void swap4() {
        Value<V> down1 = head.tail;
        Value<V> down2 = down1.tail;
        Value<V> down3 = down2.tail;
        head = new Value<>(down3.value, new Value<>(down2.value, new Value<>(down1.value, new Value<>(head.value,
                down3.tail))));
    }

    /**
     * Swap the 5 first values.
     */
    public void swap5() {
        Value<V> down1 = head.tail;
        Value<V> down2 = down1.tail;
        Value<V> down3 = down2.tail;
        Value<V> down4 = down3.tail;
        head = new Value<>(down4.value, new Value<>(down3.value, new Value<>(down2.value, new Value<>(down1.value,
                new Value<>(head.value, down4.tail)))));
    }

    /**
     * Swap the 6 first values.
     */
    public void swap6() {
        Value<V> down1 = head.tail;
        Value<V> down2 = down1.tail;
        Value<V> down3 = down2.tail;
        Value<V> down4 = down3.tail;
        Value<V> down5 = down4.tail;
        head = new Value<>(down5.value, new Value<>(down4.value, new Value<>(down3.value, new Value<>(down2.value,
                new Value<>(down1.value, new Value<>(head.value, down5.tail))))));
    }

    @Override
    public Iterator<V> iterator() {
        return new Iterator<V>() {
            private Value<V> next = head;
            @Override
            public boolean hasNext() {
                return next != null;
            }
            @Override
            public V next() {
                V value = next.value;
                next = next.tail;
                return value;
            }
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public V get() {
        return head.value;
    }

}
