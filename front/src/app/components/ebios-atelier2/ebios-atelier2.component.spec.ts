import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EbiosAtelier2Component } from './ebios-atelier2.component';

describe('EbiosAtelier2Component', () => {
  let component: EbiosAtelier2Component;
  let fixture: ComponentFixture<EbiosAtelier2Component>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EbiosAtelier2Component]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EbiosAtelier2Component);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
