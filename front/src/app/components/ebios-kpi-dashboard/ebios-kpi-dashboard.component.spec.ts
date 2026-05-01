import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EbiosKpiDashboardComponent } from './ebios-kpi-dashboard.component';

describe('EbiosKpiDashboardComponent', () => {
  let component: EbiosKpiDashboardComponent;
  let fixture: ComponentFixture<EbiosKpiDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EbiosKpiDashboardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EbiosKpiDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
