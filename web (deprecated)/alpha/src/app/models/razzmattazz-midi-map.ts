export interface MidiMapItem {
  mmchan: string;
  mmmsg: string;
  mmctrl: string;
  mmcell: string;
  mmparam: string;
};

export interface RazzmattazzMidiMap {
  midimap: MidiMapItem[]
}
