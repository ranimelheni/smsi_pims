import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EbiosAtelier4Component } from './ebios-atelier4.component';

describe('EbiosAtelier4Component', () => {
  let component: EbiosAtelier4Component;
  let fixture: ComponentFixture<EbiosAtelier4Component>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EbiosAtelier4Component]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EbiosAtelier4Component);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
