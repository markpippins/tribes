import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InstrumentSelectorComponent } from './instrument-selector.component';

describe('InstrumentSelectorComponent', () => {
  let component: InstrumentSelectorComponent;
  let fixture: ComponentFixture<InstrumentSelectorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ InstrumentSelectorComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(InstrumentSelectorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
