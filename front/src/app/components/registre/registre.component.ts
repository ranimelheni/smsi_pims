import { Component, OnInit } from '@angular/core';
import { CommonModule }      from '@angular/common';
import { FormsModule }       from '@angular/forms';
import { HttpClient }        from '@angular/common/http';
import { Router }            from '@angular/router';
import { AuthService }       from '../../services/auth.service';
import { DomSanitizer } from '@angular/platform-browser';
import { SafePipe } from '../../safe.pipe';

@Component({
  selector:    'app-registre',
  standalone:  true,
  imports:     [CommonModule, FormsModule, SafePipe],
  templateUrl: './registre.component.html',
  styleUrls:   ['./registre.component.scss']
})
export class RegistreComponent implements OnInit {

  currentUser: any   = null;
  loading            = true;
  registres: any[]   = [];
  stats: any         = {};
  error              = '';
  success            = '';

  // Filtres
  filtreSearch = '';
  filtreRisque = '';
  filtrePia    = '';

  // Ligne sélectionnée pour détail
  selectedRow: any   = null;

  // Edition risques inline
  editingRisque: any = null;
  savingRisque       = false;
    // ── PIA modal ──────────────────────────────────────────────────────────
  showPiaModal      = false;
  piaIframeUrl      = '';
  piaCurrentId: number | null = null;
  piaCurrentService = '';

    PIA_URL = 'http://localhost:4300';


  private api = 'http://localhost:8080/api';

  basesLegalesMap: Record<string, string> = {
    consentement:      'Consentement',
    contrat:           'Contrat',
    obligation_legale: 'Obligation légale',
    interet_vital:     'Intérêts vitaux',
    mission_publique:  'Mission publique',
    interet_legitime:  'Intérêt légitime'
  };

  piaStatutMap: Record<string, string> = {
    non_requis: 'Non requis',
    a_realiser: 'À réaliser',
    en_cours:   'En cours',
    realise:    'Réalisé'
  };

  constructor(
    private http:   HttpClient,
    private router: Router,
    private auth:   AuthService,
    private sanitizer: DomSanitizer

  ) {}

  ngOnInit(): void {
    this.currentUser = this.auth.getCurrentUser();
    this.load();
  }

  // ── Chargement ────────────────────────────────────────────────────────
  load(): void {
    this.loading = true;
    this.http.get<any>(`${this.api}/registre-traitement`).subscribe({
      next: (d) => {
        this.registres = d.registres || [];
        this.stats     = d.stats     || {};
        this.loading   = false;
      },
      error: () => {
        this.error   = 'Erreur lors du chargement du registre';
        this.loading = false;
      }
    });
  }

  // ── Liste filtrée ──────────────────────────────────────────────────────
  get listeFiltree(): any[] {
    return this.registres.filter(r => {
      const q  = this.filtreSearch.toLowerCase();
      const ms = !q ||
        r.service?.toLowerCase().includes(q) ||
        r.macro_finalite?.toLowerCase().includes(q) ||
        r.responsable_traitement?.toLowerCase().includes(q) ||
        r.base_legale?.toLowerCase().includes(q);
      const mr = !this.filtreRisque || r.risque_max === this.filtreRisque;
      const mp = !this.filtrePia    || r.pia_statut  === this.filtrePia;
      return ms && mr && mp;
    });
  }

  // ── Sélection ligne ────────────────────────────────────────────────────
  selectRow(r: any): void {
    this.selectedRow  = this.selectedRow?.id === r.id ? null : r;
    this.editingRisque = null;
  }

  closeDetail(): void {
    this.selectedRow   = null;
    this.editingRisque = null;
  }

  // ── Edition risques inline ─────────────────────────────────────────────
  openEditRisque(r: any, event: Event): void {
    event.stopPropagation();
    this.editingRisque = {
      id:              r.id,
      risque_physique: r.risque_physique  || 0,
      risque_moral:    r.risque_moral     || 0,
      risque_materiel: r.risque_materiel  || 0,
      explication_risque: r.explication_risque || ''
    };
  }

  saveRisque(): void {
    this.savingRisque = true;
    this.http.put<any>(
      `${this.api}/registre-traitement/${this.editingRisque.id}`,
      this.editingRisque
    ).subscribe({
      next: (updated) => {
        this.savingRisque  = false;
        this.editingRisque = null;
        const idx = this.registres.findIndex(r => r.id === updated.id);
        if (idx >= 0) this.registres[idx] = updated;
        if (this.selectedRow?.id === updated.id) this.selectedRow = updated;
        this.showSuccess('Risques mis à jour');
      },
      error: () => { this.savingRisque = false; }
    });
  }

  // ── PIA ───────────────────────────────────────────────────────────────
  ouvrirPia(r: any, event: Event): void {
      event.stopPropagation();
    this.piaCurrentId      = r.id;
    this.piaCurrentService = r.service;
    this.piaIframeUrl      = this.PIA_URL;
    this.showPiaModal      = true;

    // Marquer en cours si pas encore fait
    if (r.pia_statut === 'non_requis' || r.pia_statut === 'a_realiser') {
      this.http.patch<any>(
        `${this.api}/registre-traitement/${r.id}/pia`,
        { pia_statut: 'en_cours' }
      ).subscribe({ next: (u) => this.replaceInList(u) });
    }
  }
  fermerPia(): void {
    this.showPiaModal   = false;
    this.piaIframeUrl   = '';
    this.piaCurrentId   = null;
    this.piaCurrentService = '';
  }
  
  /** Marque le PIA comme réalisé et ferme la modale */
  confirmerPiaRealise(): void {
    if (!this.piaCurrentId) return;
    this.http.patch<any>(
      `${this.api}/registre-traitement/${this.piaCurrentId}/pia`,
      { analyse_pia: true, pia_statut: 'realise' }
    ).subscribe({
      next: (u) => {
        this.replaceInList(u);
        this.fermerPia();
        this.showSuccess('✅ PIA marqué comme réalisé');
      }
    });
  }

  /** Marquer réalisé depuis la ligne (sans ouvrir la modale) */
  marquerPiaRealise(r: any, event: Event): void {
    event.stopPropagation();
    this.http.patch<any>(
      `${this.api}/registre-traitement/${r.id}/pia`,
      { analyse_pia: true, pia_statut: 'realise' }
    ).subscribe({ next: (u) => { this.replaceInList(u); this.showSuccess('✅ PIA réalisé'); } });
  }
  // ── Helpers ────────────────────────────────────────────────────────────
  private replaceInList(updated: any): void {
    const idx = this.registres.findIndex(x => x.id === updated.id);
    if (idx >= 0) this.registres[idx] = updated;
    if (this.selectedRow?.id === updated.id) this.selectedRow = updated;
  }
  // ── Valider un traitement ─────────────────────────────────────────────
  validerTraitement(r: any, event: Event): void {
    event.stopPropagation();
    this.http.put<any>(
      `${this.api}/registre-traitement/${r.id}`,
      { statut: 'valide', date_mise_a_jour: new Date().toISOString().split('T')[0] }
    ).subscribe({
      next: (updated) => {
        const idx = this.registres.findIndex(x => x.id === updated.id);
        if (idx >= 0) this.registres[idx] = updated;
        if (this.selectedRow?.id === updated.id) this.selectedRow = updated;
        this.showSuccess('Traitement validé');
      }
    });
  }

  // ── Helpers affichage ─────────────────────────────────────────────────
  getRisqueClass(v: string): string {
    const m: Record<string, string> = {
      'Élevé': 'risque-eleve', 'Moyen': 'risque-moyen',
      'Faible': 'risque-faible', 'Aucun': 'risque-aucun'
    };
    return m[v] || '';
  }

  getPiaClass(v: string): string {
    const m: Record<string, string> = {
      non_requis: 'pia-nr', a_realiser: 'pia-ar',
      en_cours: 'pia-ec',   realise: 'pia-ok'
    };
    return m[v] || '';
  }

  getStatutClass(v: string): string {
    return v === 'valide' ? 'statut-valide' : 'statut-brouillon';
  }

  getBaseLegale(v: string): string {
    return this.basesLegalesMap[v] || v || '—';
  }

  getPiaLabel(v: string): string {
    return this.piaStatutMap[v] || v;
  }

  formatArr(arr: any[]): string {
    if (!Array.isArray(arr) || !arr.length) return '—';
    return arr.join(', ');
  }

  getBoolIcon(v: boolean): string {
    return v ? '✓' : '✗';
  }

  private showSuccess(msg: string): void {
    this.success = msg;
    setTimeout(() => this.success = '', 3000);
  }

  retourDashboard(): void {
    this.router.navigate(['/dashboard/dpo']);
  }
  
}