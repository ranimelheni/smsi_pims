import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DpoComponent } from './dpo.component';

describe('DpoComponent', () => {
  let component: DpoComponent;
  let fixture: ComponentFixture<DpoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DpoComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DpoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
