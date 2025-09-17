import {CacheEntry} from "./CacheEntry"

export class TimeBasedCache<K, V> {

  private entries = new Map<K, CacheEntry<V>>()


  constructor(readonly evictAfterMillis: number) { }


  get(key: K): V | undefined {
    return this.entries.get(key)?.value
  }

  put(key: K, value: V) {
    this.remove(key) // clear existing timeout

    const timeoutId = setTimeout(() => this.removeFromEntries(key), this.evictAfterMillis)

    this.entries.set(key, new CacheEntry(value, new Date(), timeoutId))
  }

  remove(key: K): boolean {
    const entry = this.entries.get(key)
    if (entry) {
      clearTimeout(entry.timeoutId)
      return this.removeFromEntries(key)
    } else {
      return false
    }
  }

  private removeFromEntries(key: K): boolean {
    return this.entries.delete(key)
  }

}