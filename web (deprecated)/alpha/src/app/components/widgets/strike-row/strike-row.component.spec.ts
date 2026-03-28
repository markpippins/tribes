import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StrikeRowComponent } from './strike-row.component';

describe('StrikeRowComponent', () => {
  let component: StrikeRowComponent;
  let fixture: ComponentFixture<StrikeRowComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ StrikeRowComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StrikeRowComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
