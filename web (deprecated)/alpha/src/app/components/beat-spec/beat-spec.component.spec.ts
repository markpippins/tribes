import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BeatSpecComponent } from './beat-spec.component';

describe('BeatSpecComponent', () => {
  let component: BeatSpecComponent;
  let fixture: ComponentFixture<BeatSpecComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ BeatSpecComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BeatSpecComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
