export class Cache<V> {

  private entries = new Map<string, V>()

  constructor() { }


  put(key: string, value: V) {
    this.entries.set(key, value)
  }

  get(key: string): V | undefined {
    return this.entries.get(key)
  }

}