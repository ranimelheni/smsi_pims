import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SuiviNcComponent } from './suivi-nc.component';

describe('SuiviNcComponent', () => {
  let component: SuiviNcComponent;
  let fixture: ComponentFixture<SuiviNcComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SuiviNcComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SuiviNcComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
