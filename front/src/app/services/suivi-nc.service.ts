import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SuiviNc, SuiviNcKpi } from '../models/suivi-nc.models';

@Injectable({ providedIn: 'root' })
export class SuiviNcService {

  private base = 'http://localhost:8080/api/suivi-nc';

  constructor(private http: HttpClient) {}

  getListe(): Observable<SuiviNc[]> {
    return this.http.get<SuiviNc[]>(this.base);
  }

  evaluer(id: number, payload: Partial<{
    statut_impl: string;
    responsable_rssi: string;
    echeance_rssi: string;
    commentaire_rssi: string;
  }>): Observable<SuiviNc> {
    return this.http.put<SuiviNc>(`${this.base}/${id}`, payload);
  }

  getKpi(): Observable<SuiviNcKpi> {
    return this.http.get<SuiviNcKpi>(`${this.base}/kpi`);
  }
}