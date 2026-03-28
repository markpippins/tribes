import { TickerStatus } from "./ticker-status";

export interface TickListener {
  update(tickerStatus: TickerStatus): void;
}
