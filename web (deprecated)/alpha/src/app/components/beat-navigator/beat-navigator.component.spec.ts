import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BeatNavigatorComponent } from './beat-navigator.component';

describe('BeatNavigatorComponent', () => {
  let component: BeatNavigatorComponent;
  let fixture: ComponentFixture<BeatNavigatorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ BeatNavigatorComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BeatNavigatorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
