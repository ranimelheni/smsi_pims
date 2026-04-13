import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EbiosComponent } from './ebios.component';

describe('EbiosComponent', () => {
  let component: EbiosComponent;
  let fixture: ComponentFixture<EbiosComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EbiosComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EbiosComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
