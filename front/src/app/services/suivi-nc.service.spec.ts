import { TestBed } from '@angular/core/testing';

import { SuiviNcService } from './suivi-nc.service';

describe('SuiviNcService', () => {
  let service: SuiviNcService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SuiviNcService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
