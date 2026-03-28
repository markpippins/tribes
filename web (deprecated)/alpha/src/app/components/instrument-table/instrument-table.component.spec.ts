import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InstrumentTableComponent } from './instrument-table.component';

describe('InstrumentTableComponent', () => {
  let component: InstrumentTableComponent;
  let fixture: ComponentFixture<InstrumentTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ InstrumentTableComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(InstrumentTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
