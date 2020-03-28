/*
 * Copyright 2020 Alexandru Nedelcu
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

package httpdope.common.utils

import java.time.Duration

import cats.effect.{Resource, Sync}
import httpdope.common.utils.CacheManager.Cache
import org.ehcache.config.builders.{CacheConfigurationBuilder, CacheManagerBuilder, ExpiryPolicyBuilder, ResourcePoolsBuilder}
import org.ehcache.config.units.MemoryUnit
import org.ehcache.expiry.ExpiryPolicy
import org.ehcache.{Cache => EHCache, CacheManager => EHCacheManager}

import scala.concurrent.duration._
import scala.reflect.ClassTag

final case class CacheEvictionPolicy(
  heapItems: Long,
  offHeapMB: Option[Long],
  timeToLiveExpiration: Option[FiniteDuration]
)

final class CacheManager[F[_]](private val manager: EHCacheManager)(implicit F: Sync[F]) {
  /**
    * Creates a named cache with the given eviction policy.
    */
  def createCache[K, V](alias: String, config: CacheEvictionPolicy)
    (implicit K: ClassTag[K], V: ClassTag[V]): Resource[F, Cache[F, K, V]] = {

    Resource(F.delay {
      val classK = K.runtimeClass.asInstanceOf[Class[K]]
      val classV = V.runtimeClass.asInstanceOf[Class[V]]

      val resourceBuilder = {
        val withHeap = ResourcePoolsBuilder.heap(config.heapItems)
        config.offHeapMB.fold(withHeap)(mb => withHeap.offheap(mb, MemoryUnit.MB))
      }
      val policy = config.timeToLiveExpiration.map { dt =>
        ExpiryPolicyBuilder.timeToLiveExpiration(Duration.of(dt.length, dt.unit.toChronoUnit))
          .asInstanceOf[ExpiryPolicy[_ >: K, _ >: V]]
      }
      val configBuilder = {
        val init = CacheConfigurationBuilder.newCacheConfigurationBuilder(classK, classV, resourceBuilder)
        policy.fold(init)((expiry: ExpiryPolicy[_ >: K, _ >: V]) => init.withExpiry(expiry))
      }

      val cache = manager.createCache(alias, configBuilder)
      val ref = new Cache(cache)
      (ref, F.delay { manager.removeCache(alias) })
    })
  }
}

object CacheManager {
  /**
    * Builder for a [[CacheManager]]..
    */
  def apply[F[_]](implicit F: Sync[F]): Resource[F, CacheManager[F]] =
    Resource(F.delay {
      val manager = CacheManagerBuilder.newCacheManagerBuilder.build()
      val ref = new CacheManager(manager)
      manager.init()
      (ref, F.delay { manager.close() })
    })

  final class Cache[F[_], K, V] private[CacheManager] (ehCache: EHCache[K, V])(implicit F: Sync[F]) {
    def get(key: K): F[Option[V]] =
      F.delay {
        Option(ehCache.get(key))
      }

    def put(key: K, value: V): F[Unit] =
      F.delay {
        ehCache.put(key, value)
      }

    def remove(key: K): F[Unit] =
      F.delay {
        ehCache.remove(key)
      }

    def getOrEvalIfAbsent(key: K, task: F[V]): F[V] =
      F.suspend {
        ehCache.get(key) match {
          case null =>
            F.flatMap(task) { value =>
              F.map(put(key, value))(_ => value)
            }
          case value =>
            F.pure(value)
        }
      }
  }
}
