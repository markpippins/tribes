import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DrumGridComponent } from './drum-grid.component';

describe('DrumGridComponent', () => {
  let component: DrumGridComponent;
  let fixture: ComponentFixture<DrumGridComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DrumGridComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DrumGridComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
