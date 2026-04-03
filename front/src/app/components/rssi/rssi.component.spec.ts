import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RssiComponent } from './rssi.component';

describe('RssiComponent', () => {
  let component: RssiComponent;
  let fixture: ComponentFixture<RssiComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RssiComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RssiComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
