import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EbiosAtelier1Component } from './ebios-atelier1.component';

describe('EbiosAtelier1Component', () => {
  let component: EbiosAtelier1Component;
  let fixture: ComponentFixture<EbiosAtelier1Component>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EbiosAtelier1Component]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EbiosAtelier1Component);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
