import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BeatSpecDetailComponent } from './beat-spec-detail.component';

describe('BeatSpecDetailComponent', () => {
  let component: BeatSpecDetailComponent;
  let fixture: ComponentFixture<BeatSpecDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ BeatSpecDetailComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BeatSpecDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
