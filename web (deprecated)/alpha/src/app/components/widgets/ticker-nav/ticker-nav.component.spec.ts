import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TickerNavComponent } from './ticker-nav.component';

describe('TickerNavComponent', () => {
  let component: TickerNavComponent;
  let fixture: ComponentFixture<TickerNavComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ TickerNavComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TickerNavComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
