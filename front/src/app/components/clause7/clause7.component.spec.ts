import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Clause7Component } from './clause7.component';

describe('Clause7Component', () => {
  let component: Clause7Component;
  let fixture: ComponentFixture<Clause7Component>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Clause7Component]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Clause7Component);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
