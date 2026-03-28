import { CommonModule, NgStyle } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { MatSliderModule } from '@angular/material/slider';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatToolbarModule } from '@angular/material/toolbar';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { SocketIoConfig, SocketIoModule } from 'ngx-socket-io';
import { CheckboxModule } from 'primeng/checkbox';
import { KnobModule } from "primeng/knob";
import { AppComponent } from './app.component';
import { BeatNavigatorComponent } from './components/beat-navigator/beat-navigator.component';
import { BeatSpecDetailComponent } from './components/beat-spec-detail/beat-spec-detail.component';
import { BeatSpecPanelComponent } from './components/beat-spec-panel/beat-spec-panel.component';
import { BeatSpecComponent } from './components/beat-spec/beat-spec.component';
import { CaptionsTableComponent } from './components/captions-table/captions-table.component';
import { ControlCodesTableComponent } from './components/control-codes-table/control-codes-table.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { DevicePanelComponent } from './components/device-panel/device-panel.component';
import { DrumGridComponent } from './components/drum-grid/drum-grid.component';
import { InstrumentTableComponent } from './components/instrument-table/instrument-table.component';
import { PlayerTableComponent } from './components/player-table/player-table.component';
import { RuleTableComponent } from './components/rule-table/rule-table.component';
import { SliderPanelComponent } from './components/slider-panel/slider-panel.component';
import { StatusPanelComponent } from './components/status-panel/status-panel.component';
import { ButtonPanelComponent } from './components/widgets/button-panel/button-panel.component';
import { ChannelSelectorComponent } from './components/widgets/channel-selector/channel-selector.component';
import { ControlPanel808Component } from './components/widgets/control-panel808/control-panel808.component';
import { DeviceTableComponent } from './components/widgets/device-table/device-table.component';
import { DrumPadComponent } from './components/widgets/drum-pad/drum-pad.component';
import { InstrumentSelectorComponent } from './components/widgets/instrument-selector/instrument-selector.component';
import { LaunchpadComponent } from './components/widgets/launchpad/launchpad.component';
import { MidiKnobComponent } from './components/widgets/midi-knob/midi-knob.component';
import { MidinInstrumentComboComponent } from './components/widgets/midin-instrument-combo/midin-instrument-combo.component';
import { MiniSliderPanelComponent } from './components/widgets/mini-slider-panel/mini-slider-panel.component';
import { PadsPanelComponent } from './components/widgets/pads-panel/pads-panel.component';
import { ParamsPanel808Component } from './components/widgets/params-panel808/params-panel808.component';
import { RandomizerPanelComponent } from './components/widgets/randomizer-panel/randomizer-panel.component';
import { SetNavComponent } from './components/widgets/set-nav/set-nav.component';
import { SliderComponent } from './components/widgets/slider/slider.component';
import { StatusReadoutComponent } from './components/widgets/status-readout/status-readout.component';
import { StrikeDetailComponent } from './components/widgets/strike-detail/strike-detail.component';
import { TickerNavComponent } from './components/widgets/ticker-nav/ticker-nav.component';
import { TransportControlComponent } from './components/widgets/transport-control/transport-control.component';

const config: SocketIoConfig = { url: 'http://localhost:8988', options: {} };

@NgModule({
  declarations: [
    AppComponent,
    DashboardComponent,
    SliderPanelComponent,
    ChannelSelectorComponent,
    InstrumentSelectorComponent,
    PadsPanelComponent,
    DrumPadComponent,
    StatusPanelComponent,
    TransportControlComponent,
    DrumGridComponent,
    PlayerTableComponent,
    SliderComponent,
    BeatSpecComponent,
    BeatSpecPanelComponent,
    RuleTableComponent,
    DevicePanelComponent,
    DeviceTableComponent,
    RandomizerPanelComponent,
    ControlPanel808Component,
    ParamsPanel808Component,
    StrikeDetailComponent,
    BeatSpecDetailComponent,
    MidinInstrumentComboComponent,
    BeatNavigatorComponent,
    TickerNavComponent,
    SetNavComponent,
    ButtonPanelComponent,
    StatusReadoutComponent,
    MidiKnobComponent,
    LaunchpadComponent,
    MiniSliderPanelComponent,
    InstrumentTableComponent,
    ControlCodesTableComponent,
    CaptionsTableComponent,
  ],
  imports: [
    CommonModule,
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    MatSelectModule,
    FormsModule,
    MatRadioModule,
    MatIconModule,
    MatInputModule,
    MatTableModule,
    KnobModule,
    CheckboxModule,
    MatSliderModule,
    MatButtonModule,
    MatTabsModule,
    MatCheckboxModule,
    MatListModule,
    MatToolbarModule,
    MatSliderModule,
    SocketIoModule.forRoot(config),
    NgStyle,
  ],
  providers: [],
  bootstrap: [AppComponent],
})
export class AppModule {}
