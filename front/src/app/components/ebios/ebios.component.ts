// ebios.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { EbiosAtelier1Component } from '../ebios-atelier1/ebios-atelier1.component';
import { EbiosAtelier2Component } from '../ebios-atelier2/ebios-atelier2.component';
import { EbiosAtelier3Component } from '../ebios-atelier3/ebios-atelier3.component';
import { EbiosAtelier4Component } from '../ebios-atelier4/ebios-atelier4.component';
import { EbiosAtelier5Component } from '../ebios-atelier5/ebios-atelier5.component';
import { EbiosKpiDashboardComponent } from '../ebios-kpi-dashboard/ebios-kpi-dashboard.component';

@Component({
  selector: 'app-ebios',
  standalone: true,
  imports: [CommonModule, FormsModule, EbiosAtelier1Component, EbiosAtelier2Component, 
    EbiosAtelier3Component, EbiosAtelier4Component, EbiosAtelier5Component,EbiosKpiDashboardComponent],
  templateUrl: './ebios.component.html',
  styleUrls:  ['./ebios.component.scss']
})
export class EbiosComponent implements OnInit {

  currentUser: any  = null;
  analyse:     any  = null;
  methodo:     any  = null;
  loading      = false;
  initialising = false;
  success      = '';
  error        = '';
  auditType: string = 'iso27001';
activeAtelier: 0 | 1 | 2 | 3 | 4 | 5 = 0;
  titreAnalyse = 'Analyse EBIOS RM';

  private api = 'http://localhost:8080/api';

  ateliers = [
    { n: 0, icon: '📊', label: 'Tableau de bord',              desc: 'Vue globale de l\'analyse' },
    { n: 1, icon: '🎯', label: 'Atelier 1',                    desc: 'Cadrage et socle de sécurité' },
    { n: 2, icon: '⚡', label: 'Atelier 2',                    desc: 'Sources de risque' },
    { n: 3, icon: '🗺️', label: 'Atelier 3',                    desc: 'Scénarios stratégiques' },
    { n: 4, icon: '🔬', label: 'Atelier 4',                    desc: 'Scénarios opérationnels' },
    { n: 5, icon: '🛡️', label: 'Atelier 5',                    desc: 'Traitement du risque' }
  ];

  constructor(
    private auth: AuthService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.currentUser = this.auth.getCurrentUser();
    this.loadMethodo();
    this.loadAnalyse();
  }

  loadMethodo(): void {
    this.http.get<any>(`${this.api}/methodologie-risque`).subscribe({
      next: (d) => { this.methodo = d && d.id ? d : null; }
    });
  }

  loadAnalyse(): void {
    this.loading = true;
    this.http.get<any>(`${this.api}/ebios/analyse`).subscribe({
      next: (d) => {
        this.analyse = d && d.id ? d : null;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  initAnalyse(): void {
    this.initialising = true;
    this.http.post<any>(`${this.api}/ebios/analyse/init`,
      { titre: this.titreAnalyse }).subscribe({
      next: (d) => {
        this.initialising = false;
        this.success = d.message;
        this.loadAnalyse();
        setTimeout(() => this.success = '', 4000);
      },
      error: (err) => {
        this.initialising = false;
        this.error = err.error?.error || 'Erreur initialisation';
        setTimeout(() => this.error = '', 5000);
      }
    });
  }

 selectAtelier(n: number): void {
  this.activeAtelier = n as 0 | 1 | 2 | 3 | 4 | 5;
}

  get canInit(): boolean {
    return this.methodo?.statut === 'valide' && this.methodo?.methode === 'ebios_rm';
  }

  get completionPercent(): number {
    if (!this.analyse?.stats) return 0;
    const s = this.analyse.stats;
    let score = 0;
    if (s.valeurs_metier          > 0) score += 20;
    if (s.sources_risque          > 0) score += 20;
    if (s.scenarios_strategiques  > 0) score += 20;
    if (s.scenarios_operationnels > 0) score += 20;
    if (s.mesures_securite        > 0) score += 20;
    return score;
  }

  getMatricePreview(): string[][] {
    if (!this.analyse) return [];
    const p   = this.analyse.echelle_probabilite || 4;
    const i   = this.analyse.echelle_impact      || 4;
    const acc = this.analyse.seuil_acceptable    || 6;
    const elv = this.analyse.seuil_eleve         || 12;
    const rows: string[][] = [];
    for (let pi = p; pi >= 1; pi--) {
      const row: string[] = [];
      for (let ii = 1; ii <= i; ii++) {
        const score = pi * ii;
        row.push(score <= acc ? 'f:'+score : score <= elv ? 'm:'+score : 'e:'+score);
      }
      rows.push(row);
    }
    return rows;
  }

  getLabelsProba(): string[] {
    if (!this.analyse?.labels_probabilite) return [];
    const v = this.analyse.labels_probabilite;
    return Array.isArray(v) ? v : [];
  }

  getLabelsImpact(): string[] {
    if (!this.analyse?.labels_impact) return [];
    const v = this.analyse.labels_impact;
    return Array.isArray(v) ? v : [];
  }

  getRangeP(): number[] {
    const e = this.analyse?.echelle_probabilite || 4;
    return Array.from({ length: e }, (_, i) => e - i);
  }

  getRangeI(): number[] {
    const e = this.analyse?.echelle_impact || 4;
    return Array.from({ length: e }, (_, i) => i + 1);
  }

  getCellClass(cell: string): string {
    const n = cell.split(':')[0];
    return n === 'f' ? 'cell-faible' : n === 'm' ? 'cell-moyen' : 'cell-eleve';
  }

  getCellScore(cell: string): string { return cell.split(':')[1]; }
  get ateliersSansDashboard() {
  return this.ateliers.slice(1);
}
}