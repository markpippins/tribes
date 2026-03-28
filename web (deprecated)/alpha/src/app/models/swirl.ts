export class Swirl<T> {
  private items: T[];

  constructor(items: T[]) {
    this.items = items;
  }

  getItem(index: number): T {
    return this.items[index];
  }

  getItems(): T[] {
    return this.items;
  }

  forward(): void {
    const lastItem = this.items.pop();
    if (lastItem != undefined) this.items.unshift(lastItem);
  }

  reverse(): void {
    const firstItem = this.items.shift();
    if (firstItem != undefined) this.items.push(firstItem);
  }
}
