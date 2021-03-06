/*
 * Copyright (C) 2015 The Fig Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.worksap.icefig.lang;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Range is an element generator on the basis of start point,
 * end point and next element function.
 */
public class Range<C extends Comparable<C>> implements Iterable<C> {
    private C from;
    private C to;
    private boolean toIncluded;
    private Function<C, C> next;
    private BiFunction<C, Integer, C> biNext;

    /**
     * @param from The start point.
     * @throws NullPointerException if from is null.
     */
    public Range(C from) {
        this.from(from);
    }

    /**
     * @param from The start point.
     * @param to   The end point. It is included in the range.
     * @param next The next element generator.
     * @throws NullPointerException if from, to or next is null.
     */
    public Range(C from, C to, Function<C, C> next) {
        this.from(from);
        this.to(to);
        this.next(next);
    }

    /**
     * Set start point
     *
     * @param from The start point.
     * @return Current range object.
     * @throws NullPointerException if from is null.
     */
    public Range<C> from(C from) {
        Objects.requireNonNull(from);
        this.from = from;
        return this;
    }

    /**
     * Set end point. The end point is included in this range.
     *
     * @param to The end point.
     * @return Current range object.
     * @throws NullPointerException if to is null.
     */
    public Range<C> to(C to) {
        Objects.requireNonNull(to);
        this.to = to;
        toIncluded = true;
        return this;
    }

    /**
     * Set end point. The end point is excluded in this range.
     *
     * @param to The end point.
     * @return Current range object.
     * @throws NullPointerException if to is null.
     */
    public Range<C> until(C to) {
        Objects.requireNonNull(to);
        this.to = to;
        toIncluded = false;
        return this;
    }

    /**
     * Set next element generator.
     *
     * @param next The next element generator
     * @return Current range object.
     * @throws NullPointerException if next is null.
     */
    public Range<C> next(Function<C, C> next) {
        Objects.requireNonNull(next);
        this.next = next;
        return this;
    }

    /**
     * Set next element generator.
     *
     * @param next The next element generator
     * @return Current range object.
     * @throws NullPointerException if next is null.
     */
    public Range<C> next(BiFunction<C, Integer, C> next) {
        Objects.requireNonNull(next);
        this.biNext = next;
        return this;
    }

    /**
     * @return The start point
     */
    public C getFrom() {
        return from;
    }

    /**
     * @return The end point
     */
    public C getTo() {
        return to;
    }

    /**
     * @return Whether the end point is included in this range.
     */
    public boolean isToIncluded() {
        return toIncluded;
    }

    /**
     * Iterate each element of the range.
     *
     * @throws NullPointerException if action, this.from, this.to or this.next is null.
     */
    public void forEach(Consumer<? super C> action) {
        forEach((e, i) -> action.accept(e));
    }

    /**
     * Similar to {@link #forEach(Consumer)}, with additional parameter "index" as the second parameter of the lambda expression.
     *
     * @throws NullPointerException if action, this.from, this.to or this.next is null.
     */
    public void forEach(BiConsumer<? super C, Integer> action) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(to);

        Itr itr = new Itr();
        while (itr.hasNext()) {
            int idx = itr.cursor;
            C current = itr.next();
            action.accept(current, idx);
        }
    }

    public Seq<C> toSeq() {
        MutableSeq<C> seq = Seqs.newMutableSeq();
        forEach((Consumer<C>) seq::appendInPlace);
        return seq;
    }

    public MutableSeq<C> toMutableSeq() {
        MutableSeq<C> seq = Seqs.newMutableSeq();
        forEach((Consumer<C>) seq::appendInPlace);
        return seq;
    }

    /**
     *
     * @throws NullPointerException if this.from or this.next is null.
     */
    @Override
    public Iterator<C> iterator() {
        return new Itr();
    }

    @Override
    public Spliterator<C> spliterator() {
        throw new UnsupportedOperationException("spliterator");
    }

    /**
     * Get the first n elements of this range.
     *
     * @return The Seq containing the first n elements.
     * @throws IllegalArgumentException if n < 0
     */
    public Seq<C> take(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n");
        }
        Itr itr = new Itr();
        MutableSeq<C> seq = Seqs.newMutableSeq();

        while (itr.hasNext() && itr.cursor < n) {
            seq.appendInPlace(itr.next());
        }

        return seq;
    }

    /**
     * Get elements at the front of this range which satisfy the condition.
     *
     * @param condition the condition used to filter elements by passing the element,
     *                  returns true if the element satisfies the condition, otherwise returns false.
     * @return The seq containing all the elements satisfying the condition
     * @throws NullPointerException if condition is null
     */
    public Seq<C> takeWhile(Predicate<C> condition) {
        Objects.requireNonNull(condition);

        Itr itr = new Itr();
        MutableSeq<C> seq = Seqs.newMutableSeq();

        while (itr.hasNext()) {
            C candidate = itr.next();
            if (!condition.test(candidate)) {
                break;
            }
            seq.appendInPlace(candidate);
        }

        return seq;
    }

    /**
     * Get elements at the front of this range which satisfy the condition.
     * <p>
     * Similar to {@link #takeWhile(Predicate)}, with additional parameter "index" as the second parameter of the lambda expression.
     * </p>
     *
     * @param condition the condition used to filter elements by passing the element and its index,
     *                  returns true if the element satisfies the condition, otherwise returns false.
     * @return The seq containing all the elements satisfying the condition
     * @throws NullPointerException if condition is null
     */
    public Seq<C> takeWhile(BiPredicate<C, Integer> condition) {
        Objects.requireNonNull(condition);

        Itr itr = new Itr();
        MutableSeq<C> seq = Seqs.newMutableSeq();

        while (itr.hasNext()) {
            int idx = itr.cursor;
            C candidate = itr.next();
            if (!condition.test(candidate, idx)) {
                break;
            }
            seq.appendInPlace(candidate);
        }

        return seq;
    }

    private class Itr implements Iterator<C> {
        int cursor;
        C current;
        C last;
        C end;
        boolean endIncluded;

        /*
         * orientation = 0 means to is equal to from;
         * orientation > 0 means to is greater than from;
         * otherwise to is less than from.
         */
        final Optional<Integer> orientation;

        private Itr() {
            Objects.requireNonNull(from);

            if (Objects.isNull(biNext)) {
                Objects.requireNonNull(next);
            }

            current = from;
            end = to;
            endIncluded = toIncluded;

            if (Objects.isNull(end)) {
                orientation = Optional.empty();
            } else {
                orientation = Optional.of(current.compareTo(end));
            }
        }

        @Override
        public boolean hasNext() {
            if (!orientation.isPresent()) {
                return true;
            }
            int cmp = current.compareTo(end);
            return cmp * orientation.get() > 0
                    || endIncluded && cmp == 0;
        }

        @Override
        public C next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            last = current;
            if (Objects.nonNull(next)) {
                current = next.apply(current);
            } else {
                current = biNext.apply(current, cursor);
            }
            Objects.requireNonNull(current);
            ++ cursor;

            return last;
        }
    }
}
