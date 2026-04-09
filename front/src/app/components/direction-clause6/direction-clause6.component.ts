import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-direction-clause6',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './direction-clause6.component.html',
  styleUrls: ['./direction-clause6.component.scss']
})
export class DirectionClause6Component implements OnInit {

  currentUser: any = null;
  activeTab: 'soa' | 'methodologie' = 'soa';

  success = '';
  error = '';

  private api = 'http://localhost:8080/api';

  // === SOA ===
  soaData: any = null;
  validantSoa = false;
  commentaireSoa = '';

  // === MÉTHODOLOGIE ===
  methodo: any = null;
  validantMethodo = false;
  commentaireMeth = '';

  constructor(
    private http: HttpClient,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    this.currentUser = this.auth.getCurrentUser();
    this.loadSoa();
  }

  // ================= SOA =================
  loadSoa(): void {
    this.http.get<any>(`${this.api}/soa`).subscribe({
      next: (d) => { this.soaData = d; }
    });
  }

  validerSoa(decision: string): void {
    this.validantSoa = true;
    this.http.put(`${this.api}/soa/valider`, {
      decision,
      commentaire: this.commentaireSoa
    }).subscribe({
      next: () => {
        this.validantSoa = false;
        this.soaData.statut = decision === 'valide' ? 'valide' : 'brouillon';
        this.success = 'SoA ' + (decision === 'valide' ? 'validée' : 'rejetée');
        setTimeout(() => this.success = '', 4000);
      },
      error: (err: any) => {
        this.validantSoa = false;
        this.error = err.error?.error || 'Erreur';
      }
    });
  }

  validerControleDir(id: number): void {
    this.http.put(`${this.api}/soa/controles/${id}/valider-direction`, {}).subscribe({
      next: (updated: any) => {
        if (this.soaData?.controles) {
          const idx = this.soaData.controles.findIndex((c: any) => c.id === id);
          if (idx >= 0) this.soaData.controles[idx] = updated;
        }
      }
    });
  }

  // ================= MÉTHODOLOGIE =================
  loadMethodologie(): void {
    this.http.get<any>(`${this.api}/methodologie-risque`).subscribe({
      next: (d) => { this.methodo = d && Object.keys(d).length > 0 ? d : null; }
    });
  }

  validerMethodologie(decision: string): void {
    this.validantMethodo = true;
    this.http.put(`${this.api}/methodologie-risque/valider`, {
      decision,
      commentaire: this.commentaireMeth
    }).subscribe({
      next: (res: any) => {
        this.validantMethodo = false;
        if (this.methodo) this.methodo.statut = res.statut;
        this.success = 'Méthodologie ' + (decision === 'valide' ? 'validée' : 'rejetée');
        setTimeout(() => this.success = '', 4000);
      },
      error: (err: any) => {
        this.validantMethodo = false;
        this.error = err.error?.error || 'Erreur';
      }
    });
  }
}