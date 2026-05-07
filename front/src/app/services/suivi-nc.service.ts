import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SuiviNc, SuiviNcKpi, SessionResume } from '../models/suivi-nc.models';

@Injectable({ providedIn: 'root' })
export class SuiviNcService {

  private base = 'http://localhost:8080/api/suivi-nc';

  constructor(private http: HttpClient) {}

  // Sessions disponibles (avec NC)
  getSessions(): Observable<SessionResume[]> {
    return this.http.get<SessionResume[]>(`${this.base}/sessions`);
  }

  // NC d'une session
  getBySession(sessionId: number): Observable<SuiviNc[]> {
    return this.http.get<SuiviNc[]>(`${this.base}/sessions/${sessionId}`);
  }

  // Évaluer une NC
  evaluer(id: number, payload: Partial<{
    statut_impl:      string;
    responsable_rssi: string;
    echeance_rssi:    string;
    commentaire_rssi: string;
  }>): Observable<SuiviNc> {
    return this.http.put<SuiviNc>(`${this.base}/${id}`, payload);
  }

  // KPI d'une session
  getKpiSession(sessionId: number): Observable<SuiviNcKpi> {
    return this.http.get<SuiviNcKpi>(`${this.base}/sessions/${sessionId}/kpi`);
  }
}