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

import static com.github.benmanes.caffeine.cache.RemovalCause.EXPIRED;
import static com.github.benmanes.caffeine.cache.testing.AsyncCacheSubject.assertThat;
import static com.github.benmanes.caffeine.cache.testing.CacheContextSubject.assertThat;
import static com.github.benmanes.caffeine.cache.testing.CacheSpec.Expiration.AFTER_ACCESS;
import static com.github.benmanes.caffeine.cache.testing.CacheSpec.Expiration.VARIABLE;
import static com.github.benmanes.caffeine.cache.testing.CacheSubject.assertThat;
import static com.github.benmanes.caffeine.testing.MapSubject.assertThat;
import static com.google.common.base.Functions.identity;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static uk.org.lidalia.slf4jext.Level.WARN;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.github.benmanes.caffeine.cache.Policy.FixedExpiration;
import com.github.benmanes.caffeine.cache.testing.CacheContext;
import com.github.benmanes.caffeine.cache.testing.CacheProvider;
import com.github.benmanes.caffeine.cache.testing.CacheSpec;
import com.github.benmanes.caffeine.cache.testing.CacheSpec.CacheExpiry;
import com.github.benmanes.caffeine.cache.testing.CacheSpec.Expire;
import com.github.benmanes.caffeine.cache.testing.CacheSpec.Listener;
import com.github.benmanes.caffeine.cache.testing.CacheSpec.Loader;
import com.github.benmanes.caffeine.cache.testing.CacheSpec.Population;
import com.github.benmanes.caffeine.cache.testing.CacheValidationListener;
import com.github.benmanes.caffeine.cache.testing.CheckMaxLogLevel;
import com.github.benmanes.caffeine.cache.testing.CheckNoStats;
import com.github.benmanes.caffeine.cache.testing.ExpireAfterAccess;
import com.github.benmanes.caffeine.testing.Int;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

/**
 * The test cases for caches that support the expire-after-read (time-to-idle) policy.
 *
 * @author ben.manes@gmail.com (Ben Manes)
 */
@SuppressWarnings("PreferJavaTimeOverload")
public final class ExpireAfterAccessTest {

  /* --------------- Cache --------------- */

      expireAfterAccess = Expire.ONE_MINUTE, population = { Population.PARTIAL, Population.FULL })
  public void getIfPresent(Cache<Int, Int> cache, CacheContext context) {
    context.ticker().advance(30, TimeUnit.SECONDS);
    cache.getIfPresent(context.firstKey());
    context.ticker().advance(45, TimeUnit.SECONDS);
    assertThat(cache.getIfPresent(context.firstKey())).isEqualTo(context.firstKey().negate());
    assertThat(cache.getIfPresent(context.lastKey())).isNull();

    cache.cleanUp();
    assertThat(cache).hasSize(1);

    var expected = new HashMap<>(context.original());
    expected.remove(context.firstKey());
    assertThat(context).notifications().withCause(EXPIRED)
        .contains(expected).exclusively();
  }

      expireAfterAccess = Expire.ONE_MINUTE, population = { Population.PARTIAL, Population.FULL })
  public void get(Cache<Int, Int> cache, CacheContext context) {
    Function<Int, Int> mappingFunction = context.original()::get;
    context.ticker().advance(30, TimeUnit.SECONDS);
    cache.get(context.firstKey(), mappingFunction);
    context.ticker().advance(45, TimeUnit.SECONDS);
    cache.get(context.firstKey(), mappingFunction);
    cache.get(context.lastKey(), mappingFunction); // recreated

    cache.cleanUp();
    assertThat(cache).hasSize(2);

    var expected = new HashMap<>(context.original());
    expected.remove(context.firstKey());
    assertThat(context).notifications().withCause(EXPIRED)
        .contains(expected).exclusively();
  }

      expireAfterAccess = Expire.ONE_MINUTE, population = { Population.PARTIAL, Population.FULL })
  public void getAllPresent(Cache<Int, Int> cache, CacheContext context) {
    context.ticker().advance(30, TimeUnit.SECONDS);
    cache.getAllPresent(context.firstMiddleLastKeys());
    context.ticker().advance(45, TimeUnit.SECONDS);
    assertThat(cache.getAllPresent(context.firstMiddleLastKeys())).hasSize(3);

    cache.cleanUp();
    assertThat(cache).hasSize(3);

    var expected = new HashMap<>(context.original());
    expected.keySet().removeAll(context.firstMiddleLastKeys());
    assertThat(context).notifications().withCause(EXPIRED)
        .contains(expected).exclusively();
  }

      expireAfterAccess = Expire.ONE_MINUTE, population = { Population.PARTIAL, Population.FULL })
  public void getAll(Cache<Int, Int> cache, CacheContext context) {
    context.ticker().advance(30, TimeUnit.SECONDS);
    var result1 = cache.getAll(List.of(context.firstKey(), context.middleKey()),
        keys -> Maps.asMap(keys, identity()));
    assertThat(result1).containsExactly(context.firstKey(), context.firstKey().negate(),
        context.middleKey(), context.middleKey().negate());

    context.ticker().advance(45, TimeUnit.SECONDS);
    cache.cleanUp();
    var result2 = cache.getAll(List.of(context.firstKey(), context.absentKey()),
        keys -> Maps.asMap(keys, identity()));
    assertThat(result2).containsExactly(context.firstKey(), context.firstKey().negate(),
        context.absentKey(), context.absentKey());

    context.ticker().advance(45, TimeUnit.SECONDS);
    cache.cleanUp();
    var result3 = cache.getAll(List.of(context.middleKey(), context.absentKey()),
        keys -> Maps.asMap(keys, identity()));
    assertThat(result3).containsExactly(context.middleKey(), context.middleKey(),
        context.absentKey(), context.absentKey());
    assertThat(cache).hasSize(3);

    var expected = new HashMap<>(context.original());
    expected.remove(context.firstKey());
    assertThat(context).notifications().withCause(EXPIRED)
        .contains(expected).exclusively();
  }

  /* --------------- LoadingCache --------------- */

      loader = Loader.IDENTITY, population = { Population.PARTIAL, Population.FULL })
  public void get_loading(LoadingCache<Int, Int> cache, CacheContext context) {
    context.ticker().advance(30, TimeUnit.SECONDS);
    cache.get(context.firstKey());
    context.ticker().advance(45, TimeUnit.SECONDS);
    assertThat(cache.get(context.lastKey())).isEqualTo(context.lastKey());
    cache.cleanUp();
    assertThat(cache).hasSize(2);

    context.ticker().advance(45, TimeUnit.SECONDS);
    cache.cleanUp();

    assertThat(cache).hasSize(1);
    assertThat(context).notifications().withCause(EXPIRED)
        .contains(context.original()).exclusively();
  }

      population = { Population.PARTIAL, Population.FULL })
  public void getAll_loading(LoadingCache<Int, Int> cache, CacheContext context) {
    context.ticker().advance(30, TimeUnit.SECONDS);
    assertThat(cache.getAll(List.of(context.firstKey(), context.middleKey())))
        .containsExactly(context.firstKey(), context.firstKey().negate(),
            context.middleKey(), context.middleKey().negate());

    context.ticker().advance(45, TimeUnit.SECONDS);
    cache.cleanUp();
    assertThat(cache.getAll(List.of(context.firstKey(), context.absentKey())))
        .containsExactly(context.firstKey(), context.firstKey().negate(),
            context.absentKey(), context.absentKey());

    context.ticker().advance(45, TimeUnit.SECONDS);
    cache.cleanUp();
    assertThat(cache.getAll(List.of(context.middleKey(), context.absentKey())))
        .containsExactly(context.middleKey(), context.middleKey(),
            context.absentKey(), context.absentKey());
    assertThat(cache).hasSize(3);

    var expected = new HashMap<>(context.original());
    expected.remove(context.firstKey());
    assertThat(context).notifications().withCause(EXPIRED)
        .contains(expected).exclusively();
  }

  /* --------------- AsyncCache --------------- */

      expireAfterAccess = Expire.ONE_MINUTE, population = { Population.PARTIAL, Population.FULL })
  public void getIfPresent_async(AsyncCache<Int, Int> cache, CacheContext context) {
    context.ticker().advance(30, TimeUnit.SECONDS);
    cache.getIfPresent(context.firstKey()).join();
    context.ticker().advance(45, TimeUnit.SECONDS);
    assertThat(cache).containsKey(context.firstKey());
    assertThat(cache).doesNotContainKey(context.lastKey());

    context.cleanUp();
    assertThat(cache).hasSize(1);

    var expected = new HashMap<>(context.original());
    expected.remove(context.firstKey());
    assertThat(context).notifications().withCause(EXPIRED)
        .contains(expected).exclusively();
  }

  /* --------------- Map --------------- */

      expireAfterAccess = Expire.ONE_MINUTE, population = Population.FULL)
  public void putIfAbsent(Map<Int, Int> map, CacheContext context) {
    context.ticker().advance(30, TimeUnit.SECONDS);
    assertThat(map.putIfAbsent(context.firstKey(), context.absentValue())).isNotNull();

    context.ticker().advance(30, TimeUnit.SECONDS);
    assertThat(map.putIfAbsent(context.lastKey(), context.absentValue())).isNull();

    assertThat(map).hasSize(2);
    var expected = new HashMap<>(context.original());
    expected.remove(context.firstKey());
    assertThat(context).notifications().withCause(EXPIRED)
        .contains(expected).exclusively();
  }

  /* --------------- Policy --------------- */

  public void getIfPresentQuietly(Cache<Int, Int> cache, CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    var original = expireAfterAccess.ageOf(context.firstKey()).orElseThrow();
    var advancement = Duration.ofSeconds(30);
    context.ticker().advance(advancement);
    cache.policy().getIfPresentQuietly(context.firstKey());
    var current = cache.policy().expireAfterAccess()
        .flatMap(policy -> policy.ageOf(context.firstKey())).orElseThrow();
    assertThat(current.minus(advancement)).isEqualTo(original);
  }

  public void getExpiresAfter(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    assertThat(expireAfterAccess.getExpiresAfter(TimeUnit.MINUTES)).isEqualTo(1);
  }

  public void getExpiresAfter_duration(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    assertThat(expireAfterAccess.getExpiresAfter()).isEqualTo(Duration.ofMinutes(1));
  }

  public void setExpiresAfter_negative(Cache<Int, Int> cache, CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    expireAfterAccess.setExpiresAfter(Duration.ofMinutes(-2));
  }

  public void setExpiresAfter_excessive(Cache<Int, Int> cache, CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    expireAfterAccess.setExpiresAfter(ChronoUnit.FOREVER.getDuration());
    assertThat(expireAfterAccess.getExpiresAfter(TimeUnit.NANOSECONDS)).isEqualTo(Long.MAX_VALUE);
  }

  public void setExpiresAfter(Cache<Int, Int> cache, CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    expireAfterAccess.setExpiresAfter(2, TimeUnit.MINUTES);
    assertThat(expireAfterAccess.getExpiresAfter(TimeUnit.MINUTES)).isEqualTo(2);

    context.ticker().advance(90, TimeUnit.SECONDS);
    cache.cleanUp();
    assertThat(cache).hasSize(context.initialSize());
  }

  public void setExpiresAfter_duration(Cache<Int, Int> cache, CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    expireAfterAccess.setExpiresAfter(Duration.ofMinutes(2));
    assertThat(expireAfterAccess.getExpiresAfter()).isEqualTo(Duration.ofMinutes(2));

    context.ticker().advance(90, TimeUnit.SECONDS);
    cache.cleanUp();
    assertThat(cache).hasSize(context.initialSize());
  }

      expireAfterAccess = Expire.ONE_MINUTE)
  public void ageOf(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    assertThat(expireAfterAccess.ageOf(context.firstKey(), TimeUnit.SECONDS)).hasValue(0);
    context.ticker().advance(30, TimeUnit.SECONDS);
    assertThat(expireAfterAccess.ageOf(context.firstKey(), TimeUnit.SECONDS)).hasValue(30);
    context.ticker().advance(45, TimeUnit.SECONDS);
    assertThat(expireAfterAccess.ageOf(context.firstKey(), TimeUnit.SECONDS)).isEmpty();
  }

      expireAfterAccess = Expire.ONE_MINUTE)
  public void ageOf_duration(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    assertThat(expireAfterAccess.ageOf(context.firstKey())).hasValue(Duration.ZERO);
    context.ticker().advance(30, TimeUnit.SECONDS);
    assertThat(expireAfterAccess.ageOf(context.firstKey())).hasValue(Duration.ofSeconds(30));
    context.ticker().advance(45, TimeUnit.SECONDS);
    assertThat(expireAfterAccess.ageOf(context.firstKey())).isEmpty();
  }

  public void ageOf_absent(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    assertThat(expireAfterAccess.ageOf(context.absentKey())).isEmpty();
    assertThat(expireAfterAccess.ageOf(context.absentKey(), TimeUnit.SECONDS)).isEmpty();
  }

      expiry = { CacheExpiry.DISABLED, CacheExpiry.CREATE, CacheExpiry.WRITE, CacheExpiry.ACCESS })
  public void ageOf_expired(Cache<Int, Int> cache, CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    cache.put(context.absentKey(), context.absentValue());
    context.ticker().advance(2, TimeUnit.MINUTES);
    assertThat(expireAfterAccess.ageOf(context.absentKey(), TimeUnit.SECONDS)).isEmpty();
  }

  /* --------------- Policy: oldest --------------- */

  public void oldest_unmodifiable(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    expireAfterAccess.oldest(Integer.MAX_VALUE).clear();
  }

  public void oldest_negative(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    expireAfterAccess.oldest(-1);
  }

  public void oldest_zero(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    assertThat(expireAfterAccess.oldest(0)).isExhaustivelyEmpty();
  }

  public void oldest_partial(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    int count = (int) context.initialSize() / 2;
    assertThat(expireAfterAccess.oldest(count)).hasSize(count);
  }

      expireAfterAccess = Expire.ONE_MINUTE)
  public void oldest_order(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    var oldest = expireAfterAccess.oldest(Integer.MAX_VALUE);
    assertThat(oldest.keySet()).containsExactlyElementsIn(context.original().keySet()).inOrder();
  }

  public void oldest_snapshot(Cache<Int, Int> cache, CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    var oldest = expireAfterAccess.oldest(Integer.MAX_VALUE);
    cache.invalidateAll();
    assertThat(oldest).containsExactlyEntriesIn(context.original());
  }

  public void oldestFunc_null(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    expireAfterAccess.oldest(null);
  }

  public void oldestFunc_nullResult(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    var result = expireAfterAccess.oldest(stream -> null);
    assertThat(result).isNull();
  }

  public void oldestFunc_throwsException(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    var expected = new IllegalStateException();
    try {
      expireAfterAccess.oldest(stream -> { throw expected; });
      Assert.fail();
    } catch (IllegalStateException e) {
      assertThat(e).isSameInstanceAs(expected);
    }
  }

  public void oldestFunc_concurrentModification(Cache<Int, Int> cache,
      CacheContext context, FixedExpiration<Int, Int> expireAfterAccess) {
    expireAfterAccess.oldest(stream -> {
      cache.put(context.absentKey(), context.absentValue());
      return stream.count();
    });
  }

      expectedExceptionsMessageRegExp = "source already consumed or closed")
  public void oldestFunc_closed(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    expireAfterAccess.oldest(stream -> stream).forEach(e -> {});
  }

  public void oldestFunc_partial(Cache<Int, Int> cache,
      CacheContext context, FixedExpiration<Int, Int> expireAfterAccess) {
    var result = expireAfterAccess.oldest(stream -> stream
        .limit(context.initialSize() / 2)
        .collect(toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));
    assertThat(cache.asMap()).containsAtLeastEntriesIn(result);
    assertThat(cache).containsExactlyEntriesIn(context.original());
  }

  public void oldestFunc_full(Cache<Int, Int> cache,
      CacheContext context, FixedExpiration<Int, Int> expireAfterAccess) {
    var result = expireAfterAccess.oldest(stream -> stream
        .collect(toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));
    assertThat(cache).containsExactlyEntriesIn(result);
  }

      expireAfterAccess = Expire.ONE_MINUTE)
  public void oldestFunc_order(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    var oldest = expireAfterAccess.oldest(stream ->
        stream.map(Map.Entry::getKey).collect(toImmutableList()));
    assertThat(oldest).containsExactlyElementsIn(context.original().keySet()).inOrder();
  }

  public void oldestFunc_metadata(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    var entries = expireAfterAccess.oldest(stream -> stream.collect(toImmutableList()));
    for (var entry : entries) {
      assertThat(context).containsEntry(entry);
    }
  }

  public void oldestFunc_metadata_expiresInTraversal(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    context.ticker().setAutoIncrementStep(30, TimeUnit.SECONDS);
    var entries = expireAfterAccess.oldest(stream -> stream.collect(toImmutableList()));
    assertThat(context.cache()).hasSize(context.initialSize());
    assertThat(entries).hasSize(1);
  }

  /* --------------- Policy: youngest --------------- */

  public void youngest_unmodifiable(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    expireAfterAccess.youngest(Integer.MAX_VALUE).clear();
  }

  public void youngest_negative(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    expireAfterAccess.youngest(-1);
  }

  public void youngest_zero(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    assertThat(expireAfterAccess.youngest(0)).isExhaustivelyEmpty();
  }

  public void youngest_partial(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    int count = context.original().size() / 2;
    assertThat(expireAfterAccess.youngest(count)).hasSize(count);
  }

      expireAfterAccess = Expire.ONE_MINUTE)
  public void youngest_order(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    var youngest = expireAfterAccess.youngest(Integer.MAX_VALUE);
    var expected = ImmutableList.copyOf(context.original().keySet()).reverse();
    assertThat(youngest.keySet()).containsExactlyElementsIn(expected).inOrder();
  }

  public void youngest_snapshot(Cache<Int, Int> cache, CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    var youngest = expireAfterAccess.youngest(Integer.MAX_VALUE);
    cache.invalidateAll();
    assertThat(youngest).containsExactlyEntriesIn(context.original());
  }

  public void youngestFunc_null(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    expireAfterAccess.youngest(null);
  }

  public void youngestFunc_nullResult(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    var result = expireAfterAccess.youngest(stream -> null);
    assertThat(result).isNull();
  }

  public void youngestFunc_throwsException(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    var expected = new IllegalStateException();
    try {
      expireAfterAccess.youngest(stream -> { throw expected; });
      Assert.fail();
    } catch (IllegalStateException e) {
      assertThat(e).isSameInstanceAs(expected);
    }
  }

  public void youngestFunc_concurrentModification(Cache<Int, Int> cache,
      CacheContext context, FixedExpiration<Int, Int> expireAfterAccess) {
    expireAfterAccess.youngest(stream -> {
      cache.put(context.absentKey(), context.absentValue());
      return stream.count();
    });
  }

      expectedExceptionsMessageRegExp = "source already consumed or closed")
  public void youngestFunc_closed(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    expireAfterAccess.youngest(stream -> stream).forEach(e -> {});
  }

  public void youngestFunc_partial(Cache<Int, Int> cache,
      CacheContext context, FixedExpiration<Int, Int> expireAfterAccess) {
    var result = expireAfterAccess.youngest(stream -> stream
        .limit(context.initialSize() / 2)
        .collect(toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));
    assertThat(cache.asMap()).containsAtLeastEntriesIn(result);
    assertThat(cache).containsExactlyEntriesIn(context.original());
  }

  public void youngestFunc_full(Cache<Int, Int> cache,
      CacheContext context, FixedExpiration<Int, Int> expireAfterAccess) {
    var result = expireAfterAccess.youngest(stream -> stream
        .collect(toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));
    assertThat(cache).containsExactlyEntriesIn(result);
  }

      expireAfterAccess = Expire.ONE_MINUTE)
  public void youngestFunc_order(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    var youngest = expireAfterAccess.youngest(
        stream -> stream.map(Map.Entry::getKey).collect(toImmutableList()));
    var expected = ImmutableList.copyOf(context.original().keySet()).reverse();
    assertThat(youngest).containsExactlyElementsIn(expected).inOrder();
  }

  public void youngestFunc_metadata(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    var entries = expireAfterAccess.youngest(stream -> stream.collect(toImmutableList()));
    for (var entry : entries) {
      assertThat(context).containsEntry(entry);
    }
  }

  public void youngestFunc_metadata_expiresInTraversal(CacheContext context,
      FixedExpiration<Int, Int> expireAfterAccess) {
    context.ticker().setAutoIncrementStep(30, TimeUnit.SECONDS);
    var entries = expireAfterAccess.youngest(stream -> stream.collect(toImmutableList()));
    assertThat(context.cache()).hasSize(context.initialSize());
    assertThat(entries).hasSize(1);
  }
}
