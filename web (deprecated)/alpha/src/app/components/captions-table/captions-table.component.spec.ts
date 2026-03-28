import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CaptionsTableComponent } from './captions-table.component';

describe('CaptionsTableComponent', () => {
  let component: CaptionsTableComponent;
  let fixture: ComponentFixture<CaptionsTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CaptionsTableComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CaptionsTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
