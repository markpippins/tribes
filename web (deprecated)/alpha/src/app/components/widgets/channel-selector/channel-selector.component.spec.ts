import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChannelSelectorComponent } from './channel-selector.component';

describe('ChannelSelectorComponent', () => {
  let component: ChannelSelectorComponent;
  let fixture: ComponentFixture<ChannelSelectorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ChannelSelectorComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ChannelSelectorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
