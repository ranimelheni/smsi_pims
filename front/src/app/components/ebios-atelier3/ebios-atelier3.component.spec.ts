import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EbiosAtelier3Component } from './ebios-atelier3.component';

describe('EbiosAtelier3Component', () => {
  let component: EbiosAtelier3Component;
  let fixture: ComponentFixture<EbiosAtelier3Component>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EbiosAtelier3Component]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EbiosAtelier3Component);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
