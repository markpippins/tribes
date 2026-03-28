import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SetNavComponent } from './set-nav.component';

describe('SetNavComponent', () => {
  let component: SetNavComponent;
  let fixture: ComponentFixture<SetNavComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SetNavComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SetNavComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
