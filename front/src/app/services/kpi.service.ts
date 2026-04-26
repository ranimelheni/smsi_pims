import { Injectable }   from '@angular/core';
import { HttpClient }   from '@angular/common/http';
import { Observable }   from 'rxjs';

export interface KpiDto {
  code:          string;
  label:         string;
  icon:          string;
  valeur:        number;
  numerateur:    number;
  denominateur:  number;
  unite:         string;
  tendance:      'hausse' | 'baisse' | 'stable';
  tendanceDelta: number;
  couleur:       'green' | 'amber' | 'red';
  visible:       boolean;
}

export interface SoaDetail {
  totalInclus:  number;
  nbImplemente: number;
  a5Inclus:     number;
  a6Inclus:     number;
  a7Inclus:     number;
  a8Inclus:     number;
}

export interface HistoriquePoint {
  date:                   string;
  employe_evalue:         number;
  participation_formation:number;
  lecture_publication:    number;
  documents_valides:      number;
  conformite_soa:         number;
}

export interface KpiDashboard {
  organismId:       number;
  organismNom:      string;
  roleUtilisateur:  string;
  computedAt:       string;
  fromCache:        boolean;
  kpis:             KpiDto[];
  soaDetail:        SoaDetail | null;
  historique:       HistoriquePoint[];
}

@Injectable({ providedIn: 'root' })
export class KpiService {
    private api = 'http://localhost:8080/api/kpi';


  constructor(private http: HttpClient) {}

  getDashboard(cache = true): Observable<KpiDashboard> {
    return this.http.get<KpiDashboard>(`${this.api}/dashboard?cache=${cache}`);
  }

  refreshCache(): Observable<any> {
    return this.http.post(`${this.api}/cache/refresh`, {});
  }
}