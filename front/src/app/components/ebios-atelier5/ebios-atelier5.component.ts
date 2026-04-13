// ebios-atelier5.component.ts
import { Component, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder,
         FormGroup, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-ebios-atelier5',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './ebios-atelier5.component.html',
  styleUrls:  ['./ebios-atelier5.component.scss']
})
export class EbiosAtelier5Component implements OnInit {

  @Input() analyse: any = null;

  data:     any  = null;
  loading   = false;
  success   = '';
  error     = '';

  activeView: 'plan' | 'residuels' | 'synthese' = 'plan';

  msForm!: FormGroup;
  rrForm!: FormGroup;

  showMsForm  = false;
  showRrForm  = false;
  editingMs:  any = null;
  editingRr:  any = null;

  filtreStatutMs = '';
  filtreTypeMesure = '';

  private api = 'http://localhost:8080/api/ebios';

  typesMesure = [
    { value: 'preventive',       label: 'Préventive',       icon: '🛡️', desc: 'Empêcher l\'occurrence du risque' },
    { value: 'detective',        label: 'Détective',        icon: '🔍', desc: 'Détecter un incident en cours' },
    { value: 'corrective',       label: 'Corrective',       icon: '🔧', desc: 'Limiter l\'impact après incident' },
    { value: 'organisationnelle',label: 'Organisationnelle',icon: '📋', desc: 'Politique, procédure, formation' },
    { value: 'technique',        label: 'Technique',        icon: '⚙️', desc: 'Contrôle technique automatisé' }
  ];

  statutsMesure = [
    { value: 'planifiee',   label: 'Planifiée',   class: 'badge-blue'  },
    { value: 'en_cours',    label: 'En cours',    class: 'badge-amber' },
    { value: 'realisee',    label: 'Réalisée',    class: 'badge-green' },
    { value: 'abandonnee',  label: 'Abandonnée',  class: 'badge-red'   }
  ];

  decisionsRR = [
    { value: 'accepter',   label: 'Accepter',    class: 'dec-accepter' },
    { value: 'traiter',    label: 'Traiter',      class: 'dec-traiter'  },
    { value: 'eviter',     label: 'Éviter',       class: 'dec-eviter'   },
    { value: 'transferer', label: 'Transférer',   class: 'dec-transferer'}
  ];

  niveauxLabels = ['—','Faible','Moyen','Élevé'];

  constructor(private fb: FormBuilder, private http: HttpClient) {}

  ngOnInit(): void {
    this.initForms();
    this.load();
  }

  initForms(): void {
    this.msForm = this.fb.group({
      libelle:           ['', Validators.required],
      description:       [''],
      type_mesure:       ['preventive'],
      frein_difficulte:  [''],
      cout_complexite:   [2],
      echeance_mois:     [6],
      statut:            ['planifiee'],
      responsable:       [''],
      scenarios_couverts: [[]]
    });

    this.rrForm = this.fb.group({
      scenario_strategique_id: ['', Validators.required],
      gravite_residuelle:      [1],
      vraisemblance_residuelle:[1],
      decision:                ['accepter'],
      justification:           ['']
    });
  }

  load(): void {
    this.loading = true;
    this.http.get<any>(`${this.api}/atelier5`).subscribe({
      next: (d) => { this.data = d; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  // ── Mesures ───────────────────────────────────────────────────────────────
  submitMS(): void {
    if (this.msForm.invalid) { this.msForm.markAllAsTouched(); return; }
    const url    = this.editingMs
      ? `${this.api}/mesures-securite/${this.editingMs.id}`
      : `${this.api}/mesures-securite`;
    const method = this.editingMs ? 'put' : 'post';

    (this.http as any)[method](url, this.msForm.value).subscribe({
      next: () => {
        this.showMsForm = false; this.editingMs = null;
        this.msForm.reset({ type_mesure:'preventive', cout_complexite:2, echeance_mois:6, statut:'planifiee', scenarios_couverts:[] });
        this.success = 'Mesure enregistrée';
        this.load();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err: any) => { this.error = err.error?.error || 'Erreur'; }
    });
  }

  editMS(ms: any): void {
    this.editingMs  = ms;
    this.showMsForm = true;
    this.msForm.patchValue({
      ...ms,
      scenarios_couverts: Array.isArray(ms.scenarios_couverts) ? ms.scenarios_couverts : []
    });
  }

  deleteMS(id: number): void {
    if (!confirm('Supprimer cette mesure ?')) return;
    this.http.delete(`${this.api}/mesures-securite/${id}`).subscribe({
      next: () => this.load()
    });
  }

  toggleScenario(ssId: number): void {
    const current: number[] = this.msForm.get('scenarios_couverts')?.value || [];
    const idx = current.indexOf(ssId);
    if (idx >= 0) current.splice(idx, 1);
    else current.push(ssId);
    this.msForm.get('scenarios_couverts')?.setValue([...current]);
  }

  isScenarioCouvert(ssId: number): boolean {
    const current: number[] = this.msForm.get('scenarios_couverts')?.value || [];
    return current.includes(ssId);
  }

  // ── Risques résiduels ─────────────────────────────────────────────────────
  submitRR(): void {
    if (this.rrForm.invalid) { this.rrForm.markAllAsTouched(); return; }
    const url    = this.editingRr
      ? `${this.api}/risques-residuels/${this.editingRr.id}`
      : `${this.api}/risques-residuels`;
    const method = this.editingRr ? 'put' : 'post';

    (this.http as any)[method](url, this.rrForm.value).subscribe({
      next: () => {
        this.showRrForm = false; this.editingRr = null;
        this.rrForm.reset({ gravite_residuelle:1, vraisemblance_residuelle:1, decision:'accepter' });
        this.success = 'Risque résiduel enregistré';
        this.load();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err: any) => { this.error = err.error?.error || 'Erreur'; }
    });
  }

  editRR(rr: any): void {
    this.editingRr  = rr;
    this.showRrForm = true;
    this.rrForm.patchValue(rr);
  }

  deleteRR(id: number): void {
    if (!confirm('Supprimer ce risque résiduel ?')) return;
    this.http.delete(`${this.api}/risques-residuels/${id}`).subscribe({
      next: () => this.load()
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  get listeMS():  any[] { return this.data?.mesures_securite      || []; }
  get listeRR():  any[] { return this.data?.risques_residuels     || []; }
  get listeSS():  any[] { return this.data?.scenarios_strategiques || []; }

  get listeMsFiltre(): any[] {
    return this.listeMS.filter(m => {
      const matchS = !this.filtreStatutMs   || m.statut     === this.filtreStatutMs;
      const matchT = !this.filtreTypeMesure || m.type_mesure === this.filtreTypeMesure;
      return matchS && matchT;
    });
  }

  // SS sans risque résiduel défini
  get ssNonEvalues(): any[] {
    const rrSsIds = new Set(this.listeRR.map((r: any) => r.scenario_strategique_id));
    return this.listeSS.filter(ss => !rrSsIds.has(ss.id));
  }

  getSSById(id: number): any { return this.listeSS.find(s => s.id === id); }

  getTypeMesure(v: string) {
    return this.typesMesure.find(t => t.value === v) || { icon:'❓', label:v, desc:'' };
  }

  getStatutMs(v: string) {
    return this.statutsMesure.find(s => s.value === v) || { label:v, class:'badge-gray' };
  }

  getDecision(v: string) {
    return this.decisionsRR.find(d => d.value === v) || { label:v, class:'' };
  }

  getNiveauLabel(n: number): string { return this.niveauxLabels[n] || '—'; }

  getNiveauClass(n: number): string {
    return n === 1 ? 'niveau-faible' : n === 2 ? 'niveau-moyen' : n === 3 ? 'niveau-eleve' : '';
  }

  getCoutLabel(n: number): string {
    const m: Record<number,string> = {1:'Faible',2:'Moyen',3:'Élevé',4:'Très élevé'};
    return m[n] || '—';
  }

  calcPreviewRR(): number {
    const g   = this.rrForm.get('gravite_residuelle')?.value      || 1;
    const v   = this.rrForm.get('vraisemblance_residuelle')?.value || 1;
    const acc = this.analyse?.seuil_acceptable || 6;
    const elv = this.analyse?.seuil_eleve      || 12;
    const s = g * v;
    if (s <= acc) return 1;
    if (s <= elv) return 2;
    return 3;
  }

  getRange(n: number): number[] {
    return Array.from({ length: n }, (_, i) => i + 1);
  }

  // Synthèse : comparer initial vs résiduel
  get synthese(): any[] {
    return this.listeSS.map(ss => {
      const rr = this.listeRR.find((r: any) => r.scenario_strategique_id === ss.id);
      const msCouvertes = this.listeMS.filter((m: any) =>
        (m.scenarios_couverts || []).includes(ss.id));
      return {
        ss,
        rr,
        nb_mesures: msCouvertes.length,
        reduction:  rr ? (ss.niveau_risque - rr.niveau_risque_residuel) : null
      };
    });
  }
  Math = Math;
}