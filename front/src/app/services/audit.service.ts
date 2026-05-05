import { Injectable }       from '@angular/core';
import { HttpClient }       from '@angular/common/http';
import { Observable, of }   from 'rxjs';
import { catchError }       from 'rxjs/operators';
import {
  AuditSession, AuditContexte, AuditEvaluation, AuditKpi
} from '../models/audit.models';

@Injectable({ providedIn: 'root' })
export class AuditService {

  private readonly api = 'http://localhost:8080/api/audit';

  constructor(private http: HttpClient) {}

  // Sessions
  getSessions(): Observable<AuditSession[]> {
    return this.http.get<AuditSession[]>(`${this.api}/sessions`)
      .pipe(catchError(() => of([])));
  }

  createSession(data: { titre: string; norme: string }): Observable<AuditSession | null> {
    return this.http.post<AuditSession>(`${this.api}/sessions`, data)
      .pipe(catchError(() => of(null)));
  }

  // Contexte RSSI (lecture seule)
  getContexte(): Observable<AuditContexte | null> {
    return this.http.get<AuditContexte>(`${this.api}/contexte`)
      .pipe(catchError(() => of(null)));
  }

  // Évaluations
  getEvaluations(sessionId: number): Observable<AuditEvaluation[]> {
    return this.http.get<AuditEvaluation[]>(
      `${this.api}/sessions/${sessionId}/evaluations`
    ).pipe(catchError(() => of([])));
  }

  updateEvaluation(sessionId: number, clauseCode: string, data: Partial<AuditEvaluation>): Observable<any> {
    return this.http.put(
      `${this.api}/sessions/${sessionId}/evaluations/${clauseCode}`, data
    ).pipe(catchError(err => of({ error: err.error?.error })));
  }

  // KPI audit
  getKpi(sessionId: number): Observable<AuditKpi | null> {
    return this.http.get<AuditKpi>(`${this.api}/sessions/${sessionId}/kpi`)
      .pipe(catchError(() => of(null)));
  }

  finaliser(sessionId: number, commentaire?: string): Observable<any> {
    return this.http.patch(
      `${this.api}/sessions/${sessionId}/finaliser`,
      { commentaire_global: commentaire }
    ).pipe(catchError(() => of(null)));
  }
}