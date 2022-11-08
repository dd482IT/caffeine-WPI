/*
 * Copyright 2014 Ben Manes. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.benmanes.caffeine.cache;

import static com.github.benmanes.caffeine.testing.CollectionSubject.assertThat;
import static com.google.common.collect.Iterators.elementsEqual;
import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.github.benmanes.caffeine.cache.AccessOrderDeque.AccessOrder;
import com.github.benmanes.caffeine.cache.LinkedDeque.PeekingIterator;
import com.github.benmanes.caffeine.cache.WriteOrderDeque.WriteOrder;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

/**
 * A unit-test for the @{@link AbstractLinkedDeque} implementations.
 *
 * @author ben.manes@gmail.com (Ben Manes)
 */
public final class LinkedDequeTest {
  static final int SIZE = 100;

  public void clear_whenEmpty(Deque<?> deque) {
    deque.clear();
    assertThat(deque).isExhaustivelyEmpty();
  }

  public void clear_whenPopulated(Deque<?> deque) {
    deque.clear();
    assertThat(deque).isExhaustivelyEmpty();
  }

  public void isEmpty_whenEmpty(Deque<?> deque) {
    assertThat(deque.isEmpty()).isTrue();
  }

  public void isEmpty_whenPopulated(Deque<?> deque) {
    assertThat(deque.isEmpty()).isFalse();
  }

  public void size_whenEmpty(Deque<?> deque) {
    assertThat(deque.size()).isEqualTo(0);
  }

  public void size_whenPopulated(Deque<?> deque) {
    assertThat(deque.size()).isEqualTo(SIZE);
  }

  public void contains_withNull(Deque<?> deque) {
    assertThat(deque.contains(null)).isFalse();
  }

  public void contains_whenFound(LinkedDeque<LinkedValue> deque) {
    int index = ThreadLocalRandom.current().nextInt(deque.size());
    assertThat(deque.contains(Iterables.get(deque, index))).isTrue();
  }

  public void contains_whenNotFound(LinkedDeque<LinkedValue> deque) {
    var unlinked = new LinkedValue(1);
    assertThat(deque.contains(unlinked)).isFalse();
  }

  /* --------------- Move --------------- */

  public void moveToFront_first(LinkedDeque<LinkedValue> deque) {
    checkMoveToFront(deque, deque.getFirst());
  }

  public void moveToFront_middle(LinkedDeque<LinkedValue> deque) {
    checkMoveToFront(deque, Iterables.get(deque, SIZE / 2));
  }

  public void moveToFront_last(LinkedDeque<LinkedValue> deque) {
    checkMoveToFront(deque, deque.getLast());
  }

  private void checkMoveToFront(LinkedDeque<LinkedValue> deque, LinkedValue element) {
    deque.moveToFront(element);
    assertThat(deque).hasSize(SIZE);
    assertThat(deque.peekFirst()).isEqualTo(element);
  }

  public void moveToBack_first(LinkedDeque<LinkedValue> deque) {
    checkMoveToBack(deque, deque.getFirst());
  }

  public void moveToBack_middle(LinkedDeque<LinkedValue> deque) {
    checkMoveToBack(deque, Iterables.get(deque, SIZE / 2));
  }

  public void moveToBack_last(LinkedDeque<LinkedValue> deque) {
    checkMoveToBack(deque, deque.getLast());
  }

  private void checkMoveToBack(LinkedDeque<LinkedValue> deque, LinkedValue element) {
    deque.moveToBack(element);
    assertThat(deque).hasSize(SIZE);
    assertThat(deque.getLast()).isEqualTo(element);
  }

  /* --------------- First / Last --------------- */

  public void isFirst_whenEmpty(LinkedDeque<LinkedValue> deque) {
    assertThat(deque.isFirst(new LinkedValue(0))).isFalse();
    assertThat(deque.isFirst(null)).isFalse();
  }

  public void isFirst_whenPopulated(AbstractLinkedDeque<LinkedValue> deque) {
    var first = deque.first;
    assertThat(deque).hasSize(SIZE);
    assertThat(deque.isFirst(first)).isTrue();
    assertThat(deque.contains(first)).isTrue();
    assertThat(deque.first).isSameInstanceAs(first);
  }

  public void isLast_whenEmpty(LinkedDeque<LinkedValue> deque) {
    assertThat(deque.isLast(new LinkedValue(0))).isFalse();
    assertThat(deque.isLast(null)).isFalse();
  }

  public void isLast_whenPopulated(AbstractLinkedDeque<LinkedValue> deque) {
    var last = deque.last;
    assertThat(deque).hasSize(SIZE);
    assertThat(deque.isLast(last)).isTrue();
    assertThat(deque.contains(last)).isTrue();
    assertThat(deque.last).isSameInstanceAs(last);
  }

  /* --------------- Peek --------------- */

  public void peek_whenEmpty(LinkedDeque<LinkedValue> deque) {
    assertThat(deque.peek()).isNull();
  }

  public void peek_whenPopulated(AbstractLinkedDeque<LinkedValue> deque) {
    var first = deque.first;
    assertThat(deque).hasSize(SIZE);
    assertThat(deque.contains(first)).isTrue();
    assertThat(deque.first).isSameInstanceAs(first);
    assertThat(deque.peek()).isSameInstanceAs(first);
  }

  public void peekFirst_whenEmpty(LinkedDeque<LinkedValue> deque) {
    assertThat(deque.peekFirst()).isNull();
  }

  public void peekFirst_whenPopulated(AbstractLinkedDeque<LinkedValue> deque) {
    var first = deque.first;
    assertThat(deque).hasSize(SIZE);
    assertThat(deque.contains(first)).isTrue();
    assertThat(deque.first).isSameInstanceAs(first);
    assertThat(deque.peekFirst()).isSameInstanceAs(first);
  }

  public void peekLast_whenEmpty(LinkedDeque<LinkedValue> deque) {
    assertThat(deque.peekLast()).isNull();
  }

  public void peekLast_whenPopulated(AbstractLinkedDeque<LinkedValue> deque) {
    var last = deque.last;
    assertThat(deque).hasSize(SIZE);
    assertThat(deque.contains(last)).isTrue();
    assertThat(deque.last).isSameInstanceAs(last);
    assertThat(deque.peekLast()).isSameInstanceAs(last);
  }

  /* --------------- Get --------------- */

  public void getFirst_whenEmpty(LinkedDeque<LinkedValue> deque) {
    deque.getFirst();
  }

  public void getFirst_whenPopulated(AbstractLinkedDeque<LinkedValue> deque) {
    var first = deque.first;
    assertThat(deque).hasSize(SIZE);
    assertThat(deque.contains(first)).isTrue();
    assertThat(deque.first).isSameInstanceAs(first);
    assertThat(deque.getFirst()).isSameInstanceAs(first);
  }

  public void getLast_whenEmpty(LinkedDeque<LinkedValue> deque) {
    deque.getLast();
  }

  public void getLast_whenPopulated(AbstractLinkedDeque<LinkedValue> deque) {
    var last = deque.last;
    assertThat(deque).hasSize(SIZE);
    assertThat(deque.contains(last)).isTrue();
    assertThat(deque.last).isSameInstanceAs(last);
    assertThat(deque.getLast()).isSameInstanceAs(last);
  }

  /* --------------- Element --------------- */

  public void element_whenEmpty(LinkedDeque<LinkedValue> deque) {
    deque.element();
  }

  public void element_whenPopulated(AbstractLinkedDeque<LinkedValue> deque) {
    var first = deque.first;
    assertThat(deque).hasSize(SIZE);
    assertThat(deque.contains(first)).isTrue();
    assertThat(deque.first).isSameInstanceAs(first);
    assertThat(deque.element()).isSameInstanceAs(first);
  }

  /* --------------- Offer --------------- */

  public void offer_whenEmpty(LinkedDeque<LinkedValue> deque) {
    var value = new LinkedValue(1);
    assertThat(deque.offer(value)).isTrue();
    assertThat(deque.peekFirst()).isSameInstanceAs(value);
    assertThat(deque.peekLast()).isSameInstanceAs(value);
    assertThat(deque).hasSize(1);
  }

  public void offer_whenPopulated(LinkedDeque<LinkedValue> deque) {
    var value = new LinkedValue(SIZE);
    assertThat(deque.offer(value)).isTrue();
    assertThat(deque.peekFirst()).isNotSameInstanceAs(value);
    assertThat(deque.peekLast()).isSameInstanceAs(value);
    assertThat(deque).hasSize(SIZE + 1);
  }

  public void offer_whenLinked(LinkedDeque<LinkedValue> deque) {
    assertThat(deque.offer(deque.peek())).isFalse();
    assertThat(deque).hasSize(SIZE);
  }

  public void offerFirst_whenEmpty(LinkedDeque<LinkedValue> deque) {
    var value = new LinkedValue(1);
    assertThat(deque.offerFirst(value)).isTrue();
    assertThat(deque.peekFirst()).isSameInstanceAs(value);
    assertThat(deque.peekLast()).isSameInstanceAs(value);
    assertThat(deque).hasSize(1);
  }

  public void offerFirst_whenPopulated(LinkedDeque<LinkedValue> deque) {
    var value = new LinkedValue(SIZE);
    assertThat(deque.offerFirst(value)).isTrue();
    assertThat(deque.peekFirst()).isSameInstanceAs(value);
    assertThat(deque.peekLast()).isNotSameInstanceAs(value);
    assertThat(deque).hasSize(SIZE + 1);
  }

  public void offerFirst_whenLinked(LinkedDeque<LinkedValue> deque) {
    assertThat(deque.offerFirst(deque.peek())).isFalse();
    assertThat(deque).hasSize(SIZE);
  }

  public void offerLast_whenEmpty(LinkedDeque<LinkedValue> deque) {
    var value = new LinkedValue(1);
    assertThat(deque.offerLast(value)).isTrue();
    assertThat(deque.peekFirst()).isSameInstanceAs(value);
    assertThat(deque.peekLast()).isSameInstanceAs(value);
    assertThat(deque).hasSize(1);
  }

  public void offerLast_whenPopulated(LinkedDeque<LinkedValue> deque) {
    var value = new LinkedValue(SIZE);
    assertThat(deque.offerLast(value)).isTrue();
    assertThat(deque.peekFirst()).isNotSameInstanceAs(value);
    assertThat(deque.peekLast()).isSameInstanceAs(value);
    assertThat(deque).hasSize(SIZE + 1);
  }

  public void offerLast_whenLinked(LinkedDeque<LinkedValue> deque) {
    assertThat(deque.offerLast(deque.peek())).isFalse();
    assertThat(deque).hasSize(SIZE);
  }

  /* --------------- Add --------------- */

  public void add_whenEmpty(LinkedDeque<LinkedValue> deque) {
    var value = new LinkedValue(1);
    assertThat(deque.add(value)).isTrue();
    assertThat(deque.peekFirst()).isSameInstanceAs(value);
    assertThat(deque.peekLast()).isSameInstanceAs(value);
    assertThat(deque).hasSize(1);
  }

  public void add_whenPopulated(LinkedDeque<LinkedValue> deque) {
    var value = new LinkedValue(SIZE);
    assertThat(deque.add(value)).isTrue();
    assertThat(deque.peekFirst()).isNotSameInstanceAs(value);
    assertThat(deque.peekLast()).isSameInstanceAs(value);
    assertThat(deque).hasSize(SIZE + 1);
  }

  public void add_whenLinked(LinkedDeque<LinkedValue> deque) {
    assertThat(deque.add(deque.peek())).isFalse();
  }

  public void addFirst_whenEmpty(LinkedDeque<LinkedValue> deque) {
    var value = new LinkedValue(1);
    deque.addFirst(value);
    assertThat(deque.peekFirst()).isSameInstanceAs(value);
    assertThat(deque.peekLast()).isSameInstanceAs(value);
    assertThat(deque).hasSize(1);
  }

  public void addFirst_whenPopulated(LinkedDeque<LinkedValue> deque) {
    var value = new LinkedValue(SIZE);
    deque.addFirst(value);
    assertThat(deque.peekFirst()).isSameInstanceAs(value);
    assertThat(deque.peekLast()).isNotSameInstanceAs(value);
    assertThat(deque).hasSize(SIZE + 1);
  }

  public void addFirst_whenLinked(LinkedDeque<LinkedValue> deque) {
    deque.addFirst(deque.peek());
  }

  public void addLast_whenEmpty(LinkedDeque<LinkedValue> deque) {
    var value = new LinkedValue(1);
    deque.addLast(value);
    assertThat(deque.peekFirst()).isSameInstanceAs(value);
    assertThat(deque.peekLast()).isSameInstanceAs(value);
    assertThat(deque).hasSize(1);
  }

  public void addLast_whenPopulated(LinkedDeque<LinkedValue> deque) {
    var value = new LinkedValue(SIZE);
    deque.addLast(value);
    assertThat(deque.peekFirst()).isNotSameInstanceAs(value);
    assertThat(deque.peekLast()).isSameInstanceAs(value);
    assertThat(deque).hasSize(SIZE + 1);
  }

  public void addLast_whenLinked(LinkedDeque<LinkedValue> deque) {
    deque.addLast(deque.peek());
  }

  public void addAll_withEmpty(LinkedDeque<LinkedValue> deque) {
    assertThat(deque.addAll(List.of())).isFalse();
    assertThat(deque).isExhaustivelyEmpty();
  }

  public void addAll_withPopulated(LinkedDeque<LinkedValue> deque) {
    var expected = new ArrayList<LinkedValue>();
    populate(expected);
    assertThat(deque.addAll(expected)).isTrue();
    assertThat(deque).containsExactlyElementsIn(expected).inOrder();
  }

  @SuppressWarnings("ModifyingCollectionWithItself")
  public void addAll_withSelf(LinkedDeque<LinkedValue> deque) {
    assertThat(deque.addAll(deque)).isFalse();
  }

  /* --------------- Poll --------------- */

  public void poll_whenEmpty(LinkedDeque<LinkedValue> deque) {
    assertThat(deque.poll()).isNull();
  }

  public void poll_whenPopulated(LinkedDeque<LinkedValue> deque) {
    var first = deque.peek();
    assertThat(deque.poll()).isSameInstanceAs(first);
    assertThat(deque.contains(first)).isFalse();
    assertThat(deque).hasSize(SIZE - 1);
  }

  public void poll_toEmpty(LinkedDeque<LinkedValue> deque) {
    LinkedValue value;
    while ((value = deque.poll()) != null) {
      assertThat(deque.contains(value)).isFalse();
    }
    assertThat(deque).isExhaustivelyEmpty();
  }

  public void pollFirst_whenEmpty(LinkedDeque<LinkedValue> deque) {
    assertThat(deque.pollFirst()).isNull();
  }

  public void pollFirst_whenPopulated(LinkedDeque<LinkedValue> deque) {
    var first = deque.peekFirst();
    assertThat(deque.pollFirst()).isSameInstanceAs(first);
    assertThat(deque.contains(first)).isFalse();
    assertThat(deque).hasSize(SIZE - 1);
  }

  public void pollFirst_toEmpty(LinkedDeque<LinkedValue> deque) {
    LinkedValue value;
    while ((value = deque.pollFirst()) != null) {
      assertThat(deque.contains(value)).isFalse();
    }
    assertThat(deque).isExhaustivelyEmpty();
  }

  public void pollLast_whenEmpty(LinkedDeque<LinkedValue> deque) {
    assertThat(deque.pollLast()).isNull();
  }

  public void pollLast_whenPopulated(LinkedDeque<LinkedValue> deque) {
    var last = deque.peekLast();
    assertThat(deque.pollLast()).isSameInstanceAs(last);
    assertThat(deque.contains(last)).isFalse();
    assertThat(deque).hasSize(SIZE - 1);
  }

  public void pollLast_toEmpty(LinkedDeque<LinkedValue> deque) {
    LinkedValue value;
    while ((value = deque.pollLast()) != null) {
      assertThat(deque.contains(value)).isFalse();
    }
    assertThat(deque).isExhaustivelyEmpty();
  }

  /* --------------- Remove --------------- */

  public void remove_whenEmpty(LinkedDeque<LinkedValue> deque) {
    deque.remove();
  }

  public void remove_whenPopulated(LinkedDeque<LinkedValue> deque) {
    var first = deque.peekFirst();
    assertThat(deque.remove()).isSameInstanceAs(first);
    assertThat(deque.contains(first)).isFalse();
    assertThat(deque).hasSize(SIZE - 1);
  }

  public void remove_toEmpty(LinkedDeque<LinkedValue> deque) {
    while (!deque.isEmpty()) {
      var value = deque.remove();
      assertThat(deque.contains(value)).isFalse();
    }
    assertThat(deque).isExhaustivelyEmpty();
  }

  public void removeElement_notFound(LinkedDeque<LinkedValue> deque) {
    assertThat(deque.remove(new LinkedValue(0))).isFalse();
  }

  public void removeElement_whenFound(LinkedDeque<LinkedValue> deque) {
    var first = deque.peekFirst();
    assertThat(deque.remove(first)).isTrue();
    assertThat(deque.contains(first)).isFalse();
    assertThat(deque).hasSize(SIZE - 1);
  }

  public void removeElement_toEmpty(LinkedDeque<LinkedValue> deque) {
    while (!deque.isEmpty()) {
      var value = deque.peek();
      assertThat(deque.remove(value)).isTrue();
      assertThat(deque.contains(value)).isFalse();
    }
    assertThat(deque).isExhaustivelyEmpty();
  }

  public void removeFirst_whenEmpty(LinkedDeque<LinkedValue> deque) {
    deque.removeFirst();
  }

  public void removeFirst_whenPopulated(LinkedDeque<LinkedValue> deque) {
    var first = deque.peekFirst();
    assertThat(deque.removeFirst()).isSameInstanceAs(first);
    assertThat(deque.contains(first)).isFalse();
    assertThat(deque).hasSize(SIZE - 1);
  }

  public void removeFirst_toEmpty(LinkedDeque<LinkedValue> deque) {
    while (!deque.isEmpty()) {
      var value = deque.removeFirst();
      assertThat(deque.contains(value)).isFalse();
    }
    assertThat(deque).isExhaustivelyEmpty();
  }

  public void removeLast_whenEmpty(LinkedDeque<LinkedValue> deque) {
    deque.removeLast();
  }

  public void removeLast_whenPopulated(LinkedDeque<LinkedValue> deque) {
    var last = deque.peekLast();
    assertThat(deque.removeLast()).isSameInstanceAs(last);
    assertThat(deque.contains(last)).isFalse();
    assertThat(deque).hasSize(SIZE - 1);
  }

  public void removeLast_toEmpty(LinkedDeque<LinkedValue> deque) {
    while (!deque.isEmpty()) {
      var value = deque.removeLast();
      assertThat(deque.contains(value)).isFalse();
    }
    assertThat(deque).isExhaustivelyEmpty();
  }

  public void removeFirstOccurrence_notFound(LinkedDeque<LinkedValue> deque) {
    assertThat(deque.removeFirstOccurrence(new LinkedValue(0))).isFalse();
  }

  public void removeFirstOccurrence_whenFound(LinkedDeque<LinkedValue> deque) {
    var first = deque.peekFirst();
    assertThat(deque.removeFirstOccurrence(first)).isTrue();
    assertThat(deque.contains(first)).isFalse();
    assertThat(deque).hasSize(SIZE - 1);
  }

  public void removeFirstOccurrence_toEmpty(LinkedDeque<LinkedValue> deque) {
    while (!deque.isEmpty()) {
      var value = deque.peek();
      assertThat(deque.removeFirstOccurrence(value)).isTrue();
      assertThat(deque.contains(value)).isFalse();
    }
    assertThat(deque).isExhaustivelyEmpty();
  }

  public void removeLastOccurrence_notFound(LinkedDeque<LinkedValue> deque) {
    assertThat(deque.removeLastOccurrence(new LinkedValue(0))).isFalse();
  }

  public void removeLastOccurrence_whenFound(LinkedDeque<LinkedValue> deque) {
    var first = deque.peekFirst();
    assertThat(deque.removeLastOccurrence(first)).isTrue();
    assertThat(deque.contains(first)).isFalse();
    assertThat(deque).hasSize(SIZE - 1);
  }

  public void removeLastOccurrence_toEmpty(LinkedDeque<LinkedValue> deque) {
    while (!deque.isEmpty()) {
      var value = deque.peek();
      assertThat(deque.removeLastOccurrence(value)).isTrue();
      assertThat(deque.contains(value)).isFalse();
    }
    assertThat(deque).isExhaustivelyEmpty();
  }

  public void removeAll_withEmpty(LinkedDeque<LinkedValue> deque) {
    assertThat(deque.removeAll(List.of())).isFalse();
    assertThat(deque).isExhaustivelyEmpty();
  }

  public void remove_withPopulated(LinkedDeque<LinkedValue> deque) {
    var first = deque.peekFirst();
    assertThat(deque.removeAll(List.of(first))).isTrue();
    assertThat(deque.contains(first)).isFalse();
    assertThat(deque).hasSize(SIZE - 1);
  }

  public void removeAll_toEmpty(LinkedDeque<LinkedValue> deque) {
    assertThat(deque.removeAll(List.copyOf(deque))).isTrue();
    assertThat(deque).isExhaustivelyEmpty();
  }

  /* --------------- Stack --------------- */

  public void push_whenEmpty(LinkedDeque<LinkedValue> deque) {
    var value = new LinkedValue(1);
    deque.push(value);
    assertThat(deque.peekFirst()).isSameInstanceAs(value);
    assertThat(deque.peekLast()).isSameInstanceAs(value);
    assertThat(deque).hasSize(1);
  }

  public void push_whenPopulated(LinkedDeque<LinkedValue> deque) {
    var value = new LinkedValue(SIZE);
    deque.push(value);
    assertThat(deque.peekFirst()).isSameInstanceAs(value);
    assertThat(deque.peekLast()).isNotSameInstanceAs(value);
    assertThat(deque).hasSize(SIZE + 1);
  }

  public void push_whenLinked(LinkedDeque<LinkedValue> deque) {
    deque.push(deque.peek());
  }

  public void pop_whenEmpty(LinkedDeque<LinkedValue> deque) {
    deque.pop();
  }

  public void pop_whenPopulated(LinkedDeque<LinkedValue> deque) {
    var first = deque.peekFirst();
    assertThat(deque.pop()).isSameInstanceAs(first);
    assertThat(deque.contains(first)).isFalse();
    assertThat(deque).hasSize(SIZE - 1);
  }

  public void pop_toEmpty(LinkedDeque<LinkedValue> deque) {
    while (!deque.isEmpty()) {
      var value = deque.pop();
      assertThat(deque.contains(value)).isFalse();
    }
    assertThat(deque).isExhaustivelyEmpty();
  }

  /* --------------- Iterators --------------- */

  public void iterator_noMoreElements(LinkedDeque<LinkedValue> deque) {
    deque.iterator().next();
  }

  public void iterator_whenEmpty(LinkedDeque<LinkedValue> deque) {
    assertThat(deque.iterator().hasNext()).isFalse();
    assertThat(deque.iterator().peek()).isNull();
  }

  public void iterator_whenWarmed(LinkedDeque<LinkedValue> deque) {
    var expected = new ArrayList<LinkedValue>();
    populate(expected);

    assertThat(deque.peek()).isNotNull();
    assertThat(Iterators.size(deque.iterator())).isEqualTo(deque.size());
    assertThat(elementsEqual(deque.iterator(), expected.iterator())).isTrue();
  }

  public void iterator_removal(LinkedDeque<LinkedValue> deque) {
    var iterator = deque.iterator();
    var value = iterator.next();
    iterator.remove();

    int remaining = 0;
    while (iterator.hasNext()) {
      assertThat(iterator.next()).isNotSameInstanceAs(value);
      remaining++;
    }
    assertThat(remaining).isEqualTo(SIZE - 1);
    assertThat(deque).hasSize(SIZE - 1);
  }

  public void iterator_removal_exception(LinkedDeque<LinkedValue> deque) {
    var iterator = deque.iterator();
    iterator.next();
    iterator.remove();
    iterator.remove();
  }

  public void descendingIterator_noMoreElements(LinkedDeque<LinkedValue> deque) {
    deque.descendingIterator().next();
  }

  public void descendingIterator_whenEmpty(LinkedDeque<LinkedValue> deque) {
    assertThat(deque.descendingIterator().hasNext()).isFalse();
    assertThat(deque.descendingIterator().peek()).isNull();
  }

  public void descendingIterator_whenWarmed(LinkedDeque<LinkedValue> deque) {
    var expected = new ArrayList<LinkedValue>();
    populate(expected);
    Collections.reverse(expected);

    assertThat(deque.descendingIterator().peek()).isNotNull();
    assertThat(elementsEqual(deque.descendingIterator(), expected.iterator())).isTrue();
  }

  public void descendingIterator_removal(LinkedDeque<LinkedValue> deque) {
    var iterator = deque.descendingIterator();
    var value = iterator.next();
    iterator.remove();

    int remaining = 0;
    while (iterator.hasNext()) {
      assertThat(iterator.next()).isNotEqualTo(value);
      remaining++;
    }
    assertThat(remaining).isEqualTo(SIZE - 1);
    assertThat(deque).hasSize(SIZE - 1);
  }

  public void concat(LinkedDeque<LinkedValue> deque) {
    var expect = ImmutableList.copyOf(
        Iterators.concat(deque.iterator(), deque.descendingIterator()));
    Iterable<LinkedValue> actual = () -> PeekingIterator.concat(
        deque.iterator(), deque.descendingIterator());
    assertThat(actual).containsExactlyElementsIn(expect).inOrder();
  }

  public void concat_peek(LinkedDeque<LinkedValue> deque) {
    var iterator = PeekingIterator.concat(deque.iterator(), deque.iterator());
    while (iterator.hasNext()) {
      var expected = iterator.peek();
      assertThat(iterator.next()).isEqualTo(expected);
    }
    assertThat(iterator.peek()).isNull();
  }

  public void concat_noMoreElements(LinkedDeque<LinkedValue> deque) {
    PeekingIterator.concat(deque.iterator(), deque.iterator()).next();
  }

  public void comparing(LinkedDeque<LinkedValue> deque) {
    var expect = ImmutableList.copyOf(
        Iterators.concat(deque.iterator(), deque.descendingIterator()));
    var actual = PeekingIterator.comparing(
        deque.iterator(), deque.descendingIterator(), comparator().reversed());
    assertThat(actual.peek()).isEqualTo(expect.get(0));
    assertThat(ImmutableList.copyOf(actual)).containsExactlyElementsIn(expect).inOrder();
  }

  public void comparing_uneven(LinkedDeque<LinkedValue> deque) {
    var empty = new AccessOrderDeque<LinkedValue>().iterator();
    var left = PeekingIterator.comparing(deque.iterator(), empty, comparator().reversed());
    var right = PeekingIterator.comparing(empty, deque.iterator(), comparator().reversed());

    assertThat(left.peek()).isEqualTo(deque.getFirst());
    assertThat(right.peek()).isEqualTo(deque.getFirst());
  }

  private static Comparator<LinkedValue> comparator() {
    return Comparator.comparingInt((LinkedValue v) -> v.value);
  }

  /* --------------- Deque providers --------------- */

  public Object[][] providesEmptyDeque() {
    return new Object[][] {
        { new AccessOrderDeque<LinkedValue>() },
        { new WriteOrderDeque<LinkedValue>() },
    };
  }

  public Object[][] providesWarmedDeque() {
    var accessOrder = new AccessOrderDeque<LinkedValue>();
    var writeOrder = new WriteOrderDeque<LinkedValue>();
    populate(accessOrder);
    populate(writeOrder);
    return new Object[][] { { accessOrder }, { writeOrder }};
  }

  void populate(Collection<LinkedValue> collection) {
    for (int i = 0; i < SIZE; i++) {
      collection.add(new LinkedValue(i));
    }
  }

  static final class LinkedValue implements AccessOrder<LinkedValue>, WriteOrder<LinkedValue> {
    final int value;

    LinkedValue prev;
    LinkedValue next;

    LinkedValue(int value) {
      this.value = value;
    }
    @Override public LinkedValue getPreviousInAccessOrder() {
      return prev;
    }
    @Override public void setPreviousInAccessOrder(LinkedValue prev) {
      this.prev = prev;
    }
    @Override public LinkedValue getNextInAccessOrder() {
      return next;
    }
    @Override public void setNextInAccessOrder(LinkedValue next) {
      this.next = next;
    }
    @Override public LinkedValue getPreviousInWriteOrder() {
      return prev;
    }
    @Override public void setPreviousInWriteOrder(LinkedValue prev) {
      this.prev = prev;
    }
    @Override public LinkedValue getNextInWriteOrder() {
      return next;
    }
    @Override public void setNextInWriteOrder(LinkedValue next) {
      this.next = next;
    }
    @Override public boolean equals(Object o) {
      return (o instanceof LinkedValue) && (value == ((LinkedValue) o).value);
    }
    @Override public int hashCode() {
      return value;
    }
    @Override public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("value", value)
          .add("prev", (prev == null) ? null : prev.value)
          .add("next", (next == null) ? null : next.value)
          .toString();
    }
  }
}
