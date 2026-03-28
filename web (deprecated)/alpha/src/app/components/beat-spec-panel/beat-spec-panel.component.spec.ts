import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BeatSpecPanelComponent } from './beat-spec-panel.component';

describe('BeatSpecPanelComponent', () => {
  let component: BeatSpecPanelComponent;
  let fixture: ComponentFixture<BeatSpecPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ BeatSpecPanelComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BeatSpecPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
