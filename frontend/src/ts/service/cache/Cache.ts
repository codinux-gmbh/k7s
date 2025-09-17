export class Cache<K, V> {

  private entries = new Map<K, V>()

  constructor() { }


  put(key: K, value: V) {
    this.entries.set(key, value)
  }

  get(key: K): V | undefined {
    return this.entries.get(key)
  }

}