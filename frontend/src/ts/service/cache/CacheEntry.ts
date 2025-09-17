export class CacheEntry<V> {

  constructor(readonly value: V, readonly addedAt: Date, readonly timeoutId: number) { }

}