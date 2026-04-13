import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EbiosAtelier5Component } from './ebios-atelier5.component';

describe('EbiosAtelier5Component', () => {
  let component: EbiosAtelier5Component;
  let fixture: ComponentFixture<EbiosAtelier5Component>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EbiosAtelier5Component]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EbiosAtelier5Component);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
