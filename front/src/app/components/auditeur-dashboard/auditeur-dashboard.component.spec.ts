import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AuditeurDashboardComponent } from './auditeur-dashboard.component';

describe('AuditeurDashboardComponent', () => {
  let component: AuditeurDashboardComponent;
  let fixture: ComponentFixture<AuditeurDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuditeurDashboardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AuditeurDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
