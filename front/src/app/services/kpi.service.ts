import { Injectable }          from '@angular/core';
import { HttpClient }          from '@angular/common/http';
import { Observable, of }      from 'rxjs';
import { catchError, timeout } from 'rxjs/operators';
import { KpiResponse }         from '../models/kpi.models';

@Injectable({ providedIn: 'root' })
export class KpiService {

  private readonly api = 'http://localhost:8080/kpi';
  

  constructor(private http: HttpClient) {}

  getDashboard(periode = 6): Observable<KpiResponse | null> {
    return this.http
      .get<KpiResponse>(`${this.api}/dashboard?periode=${periode}`)
      .pipe(
        timeout(15000),          // 15s max — ne jamais bloquer indéfiniment
        catchError(err => {
          console.error('KPI API error:', err?.status, err?.message);
          return of(null);       // Retourner null plutôt que planter
        })
      );
  }
}