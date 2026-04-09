import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DirectionClause6Component } from './direction-clause6.component';

describe('DirectionClause6Component', () => {
  let component: DirectionClause6Component;
  let fixture: ComponentFixture<DirectionClause6Component>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DirectionClause6Component]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DirectionClause6Component);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
