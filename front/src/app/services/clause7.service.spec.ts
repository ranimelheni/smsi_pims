import { TestBed } from '@angular/core/testing';

import { Clause7Service } from './clause7.service';

describe('Clause7Service', () => {
  let service: Clause7Service;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Clause7Service);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
