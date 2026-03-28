import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ControlCodesTableComponent } from './control-codes-table.component';

describe('ControlCodesTableComponent', () => {
  let component: ControlCodesTableComponent;
  let fixture: ComponentFixture<ControlCodesTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ControlCodesTableComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ControlCodesTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
