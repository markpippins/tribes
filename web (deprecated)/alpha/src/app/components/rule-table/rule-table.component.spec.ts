import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RuleTableComponent } from './rule-table.component';

describe('RuleTableComponent', () => {
  let component: RuleTableComponent;
  let fixture: ComponentFixture<RuleTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ RuleTableComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RuleTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
