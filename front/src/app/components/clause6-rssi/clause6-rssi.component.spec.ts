import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Clause6RssiComponent } from './clause6-rssi.component';

describe('Clause6RssiComponent', () => {
  let component: Clause6RssiComponent;
  let fixture: ComponentFixture<Clause6RssiComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Clause6RssiComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Clause6RssiComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
