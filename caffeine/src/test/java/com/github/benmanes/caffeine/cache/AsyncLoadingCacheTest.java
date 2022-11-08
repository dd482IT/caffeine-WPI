/*
 * Copyright 2015 Ben Manes. All Rights Reserved.
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

import static com.github.benmanes.caffeine.cache.RemovalCause.REPLACED;
import static com.github.benmanes.caffeine.cache.testing.AsyncCacheSubject.assertThat;
import static com.github.benmanes.caffeine.cache.testing.CacheContextSubject.assertThat;
import static com.github.benmanes.caffeine.testing.Awaits.await;
import static com.github.benmanes.caffeine.testing.CollectionSubject.assertThat;
import static com.github.benmanes.caffeine.testing.FutureSubject.assertThat;
import static com.github.benmanes.caffeine.testing.MapSubject.assertThat;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.truth.Truth.assertThat;
import static java.util.function.Function.identity;
import static uk.org.lidalia.slf4jext.Level.WARN;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.github.benmanes.caffeine.cache.LocalAsyncCache.AsyncBulkCompleter.NullMapCompletionException;
import com.github.benmanes.caffeine.cache.testing.CacheContext;
import com.github.benmanes.caffeine.cache.testing.CacheProvider;
import com.github.benmanes.caffeine.cache.testing.CacheSpec;
import com.github.benmanes.caffeine.cache.testing.CacheSpec.CacheExecutor;
import com.github.benmanes.caffeine.cache.testing.CacheSpec.Compute;
import com.github.benmanes.caffeine.cache.testing.CacheSpec.ExecutorFailure;
import com.github.benmanes.caffeine.cache.testing.CacheSpec.Listener;
import com.github.benmanes.caffeine.cache.testing.CacheSpec.Loader;
import com.github.benmanes.caffeine.cache.testing.CacheSpec.Population;
import com.github.benmanes.caffeine.cache.testing.CacheValidationListener;
import com.github.benmanes.caffeine.cache.testing.CheckMaxLogLevel;
import com.github.benmanes.caffeine.cache.testing.CheckNoEvictions;
import com.github.benmanes.caffeine.cache.testing.CheckNoStats;
import com.github.benmanes.caffeine.testing.Int;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Ints;

/**
 * The test cases for the {@link AsyncLoadingCache} interface that simulate the most generic usages.
 *
 * @author ben.manes@gmail.com (Ben Manes)
 */
@SuppressWarnings({"FutureReturnValueIgnored", "PreferJavaTimeOverload"})
public final class AsyncLoadingCacheTest {

  /* --------------- get --------------- */

  public void get_null(AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    cache.get(null);
  }

  public void get_absent(AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    assertThat(cache.get(context.absentKey())).succeedsWith(context.absentValue());
  }

  public void get_absent_failure(AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    assertThat(cache.get(context.absentKey())).hasCompletedExceptionally();
    assertThat(cache).doesNotContainKey(context.absentKey());
  }

      executorFailure = ExecutorFailure.IGNORED)
  public void get_absent_failure_async(AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    Int key = context.absentKey();
    var done = new AtomicBoolean();
    var valueFuture = cache.get(key).whenComplete((r, e) -> done.set(true));

    await().untilTrue(done);
    await().untilAsserted(() -> assertThat(cache).doesNotContainKey(key));
    await().untilAsserted(() -> assertThat(cache).hasSize(context.initialSize()));

    assertThat(valueFuture).hasCompletedExceptionally();
    assertThat(context).stats().hits(0).misses(1).success(0).failures(1);
  }

  public void get_absent_throwsException(AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    try {
      cache.get(context.absentKey()).join();
      Assert.fail();
    } catch (IllegalStateException e) {
      assertThat(context).stats().hits(0).misses(1).success(0).failures(1);
    }
  }

  public void get_absent_throwsCheckedException(
      AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    try {
      cache.get(context.absentKey()).join();
      Assert.fail();
    } catch (CompletionException e) {
      assertThat(e).hasCauseThat().isInstanceOf(ExecutionException.class);
      assertThat(context).stats().hits(0).misses(1).success(0).failures(1);
    }
  }

  public void get_absent_interrupted(AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    try {
      cache.get(context.absentKey()).join();
      Assert.fail();
    } catch (CompletionException e) {
      assertThat(Thread.interrupted()).isTrue();
      assertThat(e).hasCauseThat().isInstanceOf(InterruptedException.class);
      assertThat(context).stats().hits(0).misses(1).success(0).failures(1);
    }
  }

  public void get_present(AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    assertThat(cache.get(context.firstKey())).succeedsWith(context.firstKey().negate());
    assertThat(cache.get(context.middleKey())).succeedsWith(context.middleKey().negate());
    assertThat(cache.get(context.lastKey())).succeedsWith(context.lastKey().negate());
    assertThat(context).stats().hits(3).misses(0).success(0).failures(0);
  }

  /* --------------- getAll --------------- */

  public void getAll_iterable_null(AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    cache.getAll(null);
  }

      removalListener = { Listener.DISABLED, Listener.REJECTING })
  public void getAll_iterable_nullKey(AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    cache.getAll(Collections.singletonList(null));
  }

      removalListener = { Listener.DISABLED, Listener.REJECTING })
  public void getAll_iterable_empty(AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    assertThat(cache.getAll(List.of()).join()).isExhaustivelyEmpty();
  }

  public void getAll_immutable_keys_loader(
      AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    var future = cache.getAll(context.absentKeys());
    assertThat(future).failsWith(CompletionException.class)
        .hasCauseThat().isInstanceOf(UnsupportedOperationException.class);
  }

  public void getAll_immutable_keys_asyncLoader(
      AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    cache.getAll(context.absentKeys());
  }

  public void getAll_immutable_result(AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    cache.getAll(context.absentKeys()).join().clear();
  }

  public void getAll_absent_bulkNull(AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    assertThat(cache.getAll(context.absentKeys())).failsWith(NullMapCompletionException.class);
    assertThat(context).stats().hits(0).misses(context.absentKeys().size()).success(0).failures(1);
  }

  public void getAll_absent_failure(AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    assertThat(cache.getAll(context.absentKeys())).failsWith(CompletionException.class);
    int misses = context.absentKeys().size();
    int loadFailures = (context.loader().isBulk() || context.isSync()) ? 1 : misses;
    assertThat(context).stats().hits(0).misses(misses).success(0).failures(loadFailures);
  }

  public void getAll_absent_throwsException(
      AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    try {
      cache.getAll(context.absentKeys()).join();
      Assert.fail();
    } catch (IllegalStateException e) {
      int misses = context.loader().isBulk() ? context.absentKeys().size() : 1;
      assertThat(context).stats().hits(0).misses(misses).success(0).failures(1);
    }
  }

  public void getAll_absent_throwsCheckedException(
      AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    try {
      cache.getAll(context.absentKeys()).join();
      Assert.fail();
    } catch (CompletionException e) {
      assertThat(e).hasCauseThat().isInstanceOf(ExecutionException.class);
      int misses = context.loader().isBulk() ? context.absentKeys().size() : 1;
      assertThat(context).stats().hits(0).misses(misses).success(0).failures(1);
    }
  }

  public void getAll_absent_interrupted(AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    try {
      cache.getAll(context.absentKeys());
      Assert.fail();
    } catch (CompletionException e) {
      assertThat(Thread.interrupted()).isTrue();
      assertThat(e).hasCauseThat().isInstanceOf(InterruptedException.class);
      int misses = context.loader().isBulk() ? context.absentKeys().size() : 1;
      assertThat(context).stats().hits(0).misses(misses).success(0).failures(1);
    }
  }

      removalListener = { Listener.DISABLED, Listener.REJECTING })
  public void getAll_absent(AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    var result = cache.getAll(context.absentKeys()).join();

    int count = context.absentKeys().size();
    int loads = context.loader().isBulk() ? 1 : count;
    assertThat(result).hasSize(count);
    assertThat(context).stats().hits(0).misses(count).success(loads).failures(0);
  }

      removalListener = { Listener.DISABLED, Listener.REJECTING })
  public void getAll_present_partial(AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    var expect = new HashMap<Int, Int>();
    expect.put(context.firstKey(), context.firstKey().negate());
    expect.put(context.middleKey(), context.middleKey().negate());
    expect.put(context.lastKey(), context.lastKey().negate());
    var result = cache.getAll(expect.keySet()).join();

    assertThat(result).containsExactlyEntriesIn(expect);
    assertThat(context).stats().hits(expect.size()).misses(0).success(0).failures(0);
  }

      removalListener = { Listener.DISABLED, Listener.REJECTING })
  public void getAll_exceeds(AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    var result = cache.getAll(context.absentKeys()).join();

    assertThat(result.keySet()).containsExactlyElementsIn(context.absentKeys());
    assertThat(cache).hasSizeGreaterThan(context.initialSize() + context.absentKeys().size());
    assertThat(context).stats().hits(0).misses(result.size()).success(1).failures(0);
  }

      removalListener = { Listener.DISABLED, Listener.REJECTING })
  public void getAll_different(AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    var result = cache.getAll(context.absentKeys()).join();

    assertThat(result).isEmpty();
    assertThat(cache.asMap()).containsAtLeastEntriesIn(result);
    assertThat(context).stats().hits(0).misses(context.absent().size()).success(1).failures(0);
  }

      removalListener = { Listener.DISABLED, Listener.REJECTING })
  public void getAll_duplicates(AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    var absentKeys = ImmutableSet.copyOf(Iterables.limit(context.absentKeys(),
        Ints.saturatedCast(context.maximum().max() - context.initialSize())));
    var keys = Iterables.concat(absentKeys, absentKeys,
        context.original().keySet(), context.original().keySet());
    var result = cache.getAll(keys).join();
    assertThat(result).containsExactlyKeys(keys);

    int loads = context.loader().isBulk() ? 1 : absentKeys.size();
    assertThat(context).stats().hits(context.initialSize())
        .misses(absentKeys.size()).success(loads).failures(0);
  }

      removalListener = { Listener.DISABLED, Listener.REJECTING })
  public void getAllPresent_ordered_absent(
      AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    var keys = new ArrayList<>(context.absentKeys());
    Collections.shuffle(keys);

    var result = cache.getAll(keys).join();
    assertThat(result).containsExactlyKeys(keys).inOrder();
  }

      removalListener = { Listener.DISABLED, Listener.REJECTING })
  public void getAllPresent_ordered_partial(
      AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    var keys = new ArrayList<>(context.original().keySet());
    keys.addAll(context.absentKeys());
    Collections.shuffle(keys);

    var result = cache.getAll(keys).join();
    assertThat(result).containsExactlyKeys(keys).inOrder();
  }

      removalListener = { Listener.DISABLED, Listener.REJECTING })
  public void getAllPresent_ordered_present(
      AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    var keys = new ArrayList<>(context.original().keySet());
    Collections.shuffle(keys);

    var result = cache.getAll(keys).join();
    assertThat(result).containsExactlyKeys(keys).inOrder();
  }

      removalListener = { Listener.DISABLED, Listener.REJECTING })
  public void getAllPresent_ordered_exceeds(
      AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    var keys = new ArrayList<>(context.original().keySet());
    keys.addAll(context.absentKeys());
    Collections.shuffle(keys);

    var result = cache.getAll(keys).join();
    assertThat(result).containsExactlyKeys(keys).inOrder();
  }

  public void getAll_badLoader(CacheContext context) {
    var loader = new AsyncCacheLoader<Int, Int>() {
      @Override public CompletableFuture<Int> asyncLoad(Int key, Executor executor) {
        throw new IllegalStateException();
      }
      @Override public CompletableFuture<Map<Int, Int>> asyncLoadAll(
          Set<? extends Int> keys, Executor executor) {
        throw new LoadAllException();
      }
    };
    var cache = context.buildAsync(loader);

    try {
      cache.getAll(context.absentKeys());
      Assert.fail();
    } catch (LoadAllException e) {
      assertThat(cache).isEmpty();
    }
  }

  @SuppressWarnings("serial")
  private static final class LoadAllException extends RuntimeException {}

  /* --------------- put --------------- */

  public void put_replace(AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    var replaced = new HashMap<Int, Int>();
    var value = context.absentValue().asFuture();
    for (Int key : context.firstMiddleLastKeys()) {
      cache.put(key, value);
      assertThat(cache.get(key)).succeedsWith(context.absentValue());
      replaced.put(key, context.original().get(key));
    }
    assertThat(cache).hasSize(context.initialSize());
    assertThat(context).removalNotifications().withCause(REPLACED)
        .contains(replaced).exclusively();
  }

  /* --------------- refresh --------------- */

      compute = Compute.ASYNC, executor = CacheExecutor.THREADED)
  public void refresh(CacheContext context) {
    var done = new AtomicBoolean();
    var cache = context.buildAsync((Int key) -> {
      await().untilTrue(done);
      return key.negate();
    });

    Int key = Int.valueOf(1);
    cache.synchronous().put(key, key);
    var original = cache.get(key);
    for (int i = 0; i < 10; i++) {
      context.ticker().advance(1, TimeUnit.SECONDS);
      cache.synchronous().refresh(key);

      var next = cache.get(key);
      assertThat(next).isSameInstanceAs(original);
    }
    done.set(true);
    await().untilAsserted(() -> assertThat(cache).containsEntry(key, key.negate()));
  }

  public void refresh_nullFuture_load(CacheContext context) {
    var cache = context.buildAsync((Int key, Executor executor) -> null);
    cache.synchronous().refresh(context.absentKey());
  }

  public void refresh_nullFuture_reload(CacheContext context) {
    var cache = context.buildAsync(new AsyncCacheLoader<Int, Int>() {
      @Override public CompletableFuture<Int> asyncLoad(Int key, Executor executor) {
        throw new IllegalStateException();
      }
      @Override public CompletableFuture<Int> asyncReload(
          Int key, Int oldValue, Executor executor) {
        return null;
      }
    });
    cache.synchronous().put(context.absentKey(), context.absentValue());
    cache.synchronous().refresh(context.absentKey());
  }

  // Issue #69
      compute = Compute.ASYNC, executor = CacheExecutor.THREADED)
  public void refresh_deadlock(CacheContext context) {
    var future = new CompletableFuture<Int>();
    var cache = context.buildAsync((Int k, Executor e) -> future);

    cache.synchronous().refresh(context.absentKey());
    var get = cache.get(context.absentKey());

    future.complete(context.absentValue());
    assertThat(get).succeedsWith(context.absentValue());
  }

  public void refresh_throwsException(AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    try {
      var key = context.original().isEmpty() ? context.absentKey() : context.firstKey();
      cache.synchronous().refresh(key);
      Assert.fail();
    } catch (IllegalStateException e) {
      int failures = context.isGuava() ? 1 : 0;
      assertThat(context).stats().hits(0).misses(0).success(0).failures(failures);
    }
  }

  public void refresh_throwsCheckedException(
      AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    try {
      var key = context.original().isEmpty() ? context.absentKey() : context.firstKey();
      cache.synchronous().refresh(key);
      Assert.fail();
    } catch (CompletionException e) {
      assertThat(e).hasCauseThat().isInstanceOf(ExecutionException.class);

      int failures = context.isGuava() ? 1 : 0;
      assertThat(context).stats().hits(0).misses(0).success(0).failures(failures);
    }
  }

  public void refresh_interrupted(AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    try {
      var key = context.original().isEmpty() ? context.absentKey() : context.firstKey();
      cache.synchronous().refresh(key);
      Assert.fail();
    } catch (CompletionException e) {
      assertThat(Thread.interrupted()).isTrue();
      assertThat(e).hasCauseThat().isInstanceOf(InterruptedException.class);

      int failures = context.isGuava() ? 1 : 0;
      assertThat(context).stats().hits(0).misses(0).success(0).failures(failures);
    }
  }

  public void refresh_current_inFlight(AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    var future = new CompletableFuture<Int>();
    cache.put(context.absentKey(), future);
    cache.synchronous().refresh(context.absentKey());
    assertThat(cache).containsEntry(context.absentKey(), future);
    assertThat(cache.synchronous().policy().refreshes()).isEmpty();
    future.complete(context.absentValue());
  }

  public void refresh_current_sameInstance(CacheContext context) {
    var future = context.absentValue().asFuture();
    var cache = context.buildAsync((key, executor) -> future);

    cache.put(context.absentKey(), future);
    cache.synchronous().refresh(context.absentKey());
    assertThat(context).notifications().isEmpty();
  }

  public void refresh_current_failed(AsyncLoadingCache<Int, Int> cache, CacheContext context) {
    var future = context.absentValue().asFuture();
    cache.put(context.absentKey(), future);

    future.obtrudeException(new Exception());
    assertThat(cache.asMap()).containsKey(context.absentKey());

    cache.synchronous().refresh(context.absentKey());
    assertThat(cache).containsEntry(context.absentKey(), context.absentValue());
  }

      removalListener = Listener.CONSUMING, executor = CacheExecutor.THREADED)
  public void refresh_current_removed(CacheContext context) {
    var started = new AtomicBoolean();
    var done = new AtomicBoolean();
    var cache = context.buildAsync((Int key) -> {
      started.set(true);
      await().untilTrue(done);
      return key;
    });

    cache.put(context.absentKey(), context.absentValue().asFuture());
    cache.synchronous().refresh(context.absentKey());
    await().untilTrue(started);

    cache.synchronous().invalidate(context.absentKey());
    done.set(true);

    await().untilAsserted(() -> {
      assertThat(context).removalNotifications().containsExactlyValues(
          context.absentKey(), context.absentValue());
    });
  }

  /* --------------- AsyncCacheLoader --------------- */

  public void asyncLoadAll() throws Exception {
    AsyncCacheLoader<Int, Int> loader = (key, executor) -> key.negate().asFuture();
    loader.asyncLoadAll(Set.of(), Runnable::run);
  }

  public void asyncReload() throws Exception {
    AsyncCacheLoader<Int, Int> loader = (key, executor) -> key.negate().asFuture();
    var future = loader.asyncReload(Int.valueOf(1), Int.valueOf(2), Runnable::run);
    assertThat(future).succeedsWith(-1);
  }

  @SuppressWarnings("CheckReturnValue")
  public void bulk_function_null() {
    Function<Set<? extends Int>, Map<Int, Int>> f = null;
    AsyncCacheLoader.bulk(f);
  }

  public void bulk_function_absent() throws Exception {
    AsyncCacheLoader<Int, Int> loader = AsyncCacheLoader.bulk(keys -> Map.of());
    assertThat(loader.asyncLoadAll(Set.of(), Runnable::run)).succeedsWith(Map.of());
    assertThat(loader.asyncLoad(Int.valueOf(1), Runnable::run)).succeedsWithNull();
  }

  public void bulk_function_present() throws Exception {
    AsyncCacheLoader<Int, Int> loader = AsyncCacheLoader.bulk(keys -> {
      return keys.stream().collect(toImmutableMap(identity(), identity()));
    });
    assertThat(loader.asyncLoadAll(Int.setOf(1, 2), Runnable::run))
        .succeedsWith(Int.mapOf(1, 1, 2, 2));
    assertThat(loader.asyncLoad(Int.valueOf(1), Runnable::run)).succeedsWith(1);
  }

  @SuppressWarnings("CheckReturnValue")
  public void bulk_bifunction_null() {
    BiFunction<Set<? extends Int>, Executor, CompletableFuture<Map<Int, Int>>> f = null;
    AsyncCacheLoader.bulk(f);
  }

  public void bulk_absent() throws Exception {
    BiFunction<Set<? extends Int>, Executor, CompletableFuture<Map<Int, Int>>> f =
        (keys, executor) -> CompletableFuture.completedFuture(Map.of());
    var loader = AsyncCacheLoader.bulk(f);
    assertThat(loader.asyncLoadAll(Set.of(), Runnable::run)).succeedsWith(Map.of());
    assertThat(loader.asyncLoad(Int.valueOf(1), Runnable::run)).succeedsWithNull();
  }

  public void bulk_present() throws Exception {
    BiFunction<Set<? extends Int>, Executor, CompletableFuture<Map<Int, Int>>> f =
        (keys, executor) -> {
          ImmutableMap<Int, Int> results = keys.stream()
              .collect(toImmutableMap(identity(), identity()));
          return CompletableFuture.completedFuture(results);
        };
    var loader = AsyncCacheLoader.bulk(f);
    assertThat(loader.asyncLoadAll(Int.setOf(1, 2), Runnable::run))
        .succeedsWith(Int.mapOf(1, 1, 2, 2));
    assertThat(loader.asyncLoad(Int.valueOf(1), Runnable::run)).succeedsWith(1);
  }
}
