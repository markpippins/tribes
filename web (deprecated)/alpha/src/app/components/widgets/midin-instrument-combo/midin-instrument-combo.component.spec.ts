import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MidinInstrumentComboComponent } from './midin-instrument-combo.component';

describe('MidinInstrumentComboComponent', () => {
  let component: MidinInstrumentComboComponent;
  let fixture: ComponentFixture<MidinInstrumentComboComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ MidinInstrumentComboComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MidinInstrumentComboComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
