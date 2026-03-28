import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StrikeDetailComponent } from './strike-detail.component';

describe('StrikeDetailComponent', () => {
  let component: StrikeDetailComponent;
  let fixture: ComponentFixture<StrikeDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ StrikeDetailComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StrikeDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
