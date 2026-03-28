import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RandomizerPanelComponent } from './randomizer-panel.component';

describe('RandomizerPanelComponent', () => {
  let component: RandomizerPanelComponent;
  let fixture: ComponentFixture<RandomizerPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ RandomizerPanelComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RandomizerPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
