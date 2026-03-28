import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ParamsPanel808Component } from './params-panel808.component';

describe('ParamsPanel808Component', () => {
  let component: ParamsPanel808Component;
  let fixture: ComponentFixture<ParamsPanel808Component>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ParamsPanel808Component ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ParamsPanel808Component);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
