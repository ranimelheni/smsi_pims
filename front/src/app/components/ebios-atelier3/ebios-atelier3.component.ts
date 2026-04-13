// ebios-atelier3.component.ts
import { Component, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder,
         FormGroup, FormArray, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-ebios-atelier3',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './ebios-atelier3.component.html',
  styleUrls:  ['./ebios-atelier3.component.scss']
})
export class EbiosAtelier3Component implements OnInit {

  @Input() analyse: any = null;

  data:    any  = null;
  loading  = false;
  success  = '';
  error    = '';

  activeView: 'parties' | 'scenarios' | 'cartographie' = 'parties';

  // Formulaires
  ppForm!: FormGroup;
  ssForm!: FormGroup;

  showPpForm = false;
  showSsForm = false;
  editingPp: any = null;
  editingSs: any = null;

  // Filtre scénarios
  filtreNiveau = '';
  filtreDecision = '';

  private api = 'http://localhost:8080/api/ebios';

  categoriesPP = [
    { value: 'fournisseur',        label: 'Fournisseur',         icon: '📦' },
    { value: 'client',             label: 'Client',              icon: '👤' },
    { value: 'partenaire',         label: 'Partenaire',          icon: '🤝' },
    { value: 'prestataire',        label: 'Prestataire',         icon: '🔧' },
    { value: 'personnel_externe',  label: 'Personnel externe',   icon: '👷' },
    { value: 'autre',              label: 'Autre',               icon: '❓' }
  ];

  decisionsTraitement = [
    { value: 'traiter',    label: 'Traiter',     desc: 'Mettre en œuvre des mesures de sécurité',    class: 'dec-traiter' },
    { value: 'accepter',   label: 'Accepter',    desc: 'Risque acceptable, pas de mesure supplémentaire', class: 'dec-accepter' },
    { value: 'eviter',     label: 'Éviter',      desc: 'Supprimer l\'activité ou le traitement',    class: 'dec-eviter' },
    { value: 'transferer', label: 'Transférer',  desc: 'Assurance, sous-traitance',                 class: 'dec-transferer' }
  ];

  niveauxLabels = ['—', 'Faible', 'Moyen', 'Élevé'];

  constructor(private fb: FormBuilder, private http: HttpClient) {}

  ngOnInit(): void {
    this.initForms();
    this.load();
  }

  initForms(): void {
    this.ppForm = this.fb.group({
      libelle:      ['', Validators.required],
      categorie:    ['fournisseur'],
      description:  [''],
      dependance:   [2],
      penetration:  [2],
      maturite_cyber:[2],
      confiance:    [2],
      retenu:       [true]
    });

    this.ssForm = this.fb.group({
      libelle:             ['', Validators.required],
      source_id:           ['', Validators.required],
      objectif_id:         ['', Validators.required],
      evenement_id:        [''],
      description:         [''],
      gravite:             [2],
      vraisemblance:       [2],
      parties_prenantes:   this.fb.array([]),
      decision_traitement: ['traiter'],
      justification_decision: ['']
    });
  }

  get ppParties(): FormArray {
    return this.ssForm.get('parties_prenantes') as FormArray;
  }

  addPPInSS(ppId: number): void {
    // Vérifier si déjà ajouté
    const existing = this.ppParties.controls.find(c => c.get('pp_id')?.value === ppId);
    if (existing) return;
    this.ppParties.push(this.fb.group({
      pp_id: [ppId],
      role:  ['vecteur']
    }));
  }

  removePPFromSS(i: number): void {
    this.ppParties.removeAt(i);
  }

  load(): void {
    this.loading = true;
    this.http.get<any>(`${this.api}/atelier3`).subscribe({
      next: (d) => { this.data = d; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  // ── Parties prenantes ─────────────────────────────────────────────────────
  submitPP(): void {
    if (this.ppForm.invalid) { this.ppForm.markAllAsTouched(); return; }
    const url    = this.editingPp
      ? `${this.api}/parties-prenantes/${this.editingPp.id}`
      : `${this.api}/parties-prenantes`;
    const method = this.editingPp ? 'put' : 'post';

    (this.http as any)[method](url, this.ppForm.value).subscribe({
      next: () => {
        this.showPpForm = false;
        this.editingPp  = null;
        this.ppForm.reset({ categorie:'fournisseur', dependance:2, penetration:2, maturite_cyber:2, confiance:2, retenu:true });
        this.success = 'Partie prenante enregistrée';
        this.load();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err: any) => { this.error = err.error?.error || 'Erreur'; }
    });
  }

  editPP(pp: any): void {
    this.editingPp  = pp;
    this.showPpForm = true;
    this.ppForm.patchValue(pp);
  }

  deletePP(id: number): void {
    if (!confirm('Supprimer cette partie prenante ?')) return;
    this.http.delete(`${this.api}/parties-prenantes/${id}`).subscribe({
      next: () => this.load()
    });
  }

  toggleRetenuPP(pp: any): void {
    this.http.put(`${this.api}/parties-prenantes/${pp.id}`, {
      ...pp, retenu: !pp.retenu
    }).subscribe({ next: () => this.load() });
  }

  // ── Scénarios stratégiques ────────────────────────────────────────────────
  submitSS(): void {
    if (this.ssForm.invalid) { this.ssForm.markAllAsTouched(); return; }
    const url    = this.editingSs
      ? `${this.api}/scenarios-strategiques/${this.editingSs.id}`
      : `${this.api}/scenarios-strategiques`;
    const method = this.editingSs ? 'put' : 'post';

    (this.http as any)[method](url, this.ssForm.value).subscribe({
      next: () => {
        this.showSsForm = false;
        this.editingSs  = null;
        this.ssForm.reset({ gravite:2, vraisemblance:2, decision_traitement:'traiter' });
        this.ppParties.clear();
        this.success = 'Scénario stratégique enregistré';
        this.load();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err: any) => { this.error = err.error?.error || 'Erreur'; }
    });
  }

  editSS(ss: any): void {
    this.editingSs  = ss;
    this.showSsForm = true;
    this.ppParties.clear();
    if (Array.isArray(ss.parties_prenantes)) {
      ss.parties_prenantes.forEach((pp: any) => {
        this.ppParties.push(this.fb.group({ pp_id: [pp.pp_id], role: [pp.role || 'vecteur'] }));
      });
    }
    this.ssForm.patchValue({ ...ss, evenement_id: ss.evenement_id || '' });
  }

  deleteSS(id: number): void {
    if (!confirm('Supprimer ce scénario stratégique ?')) return;
    this.http.delete(`${this.api}/scenarios-strategiques/${id}`).subscribe({
      next: () => this.load()
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  get listePP():  any[] { return this.data?.parties_prenantes       || []; }
  get listeSS():  any[] { return this.data?.scenarios_strategiques  || []; }
  get listeSR():  any[] { return this.data?.sources_retenues        || []; }
  get listeOV():  any[] { return this.data?.objectifs_retenus       || []; }
  get listeER():  any[] { return this.data?.evenements_redoutes     || []; }

  getOVForSR(srId: number): any[] {
    return this.listeOV.filter(o => o.source_id === srId);
  }

  get listeSsFiltre(): any[] {
    return this.listeSS.filter(s => {
      const matchN = !this.filtreNiveau   || s.niveau_risque?.toString() === this.filtreNiveau;
      const matchD = !this.filtreDecision || s.decision_traitement === this.filtreDecision;
      return matchN && matchD;
    });
  }

  getPPById(id: number): any {
    return this.listePP.find(p => p.id === id);
  }

  getCatPP(v: string) {
    return this.categoriesPP.find(c => c.value === v) || { icon: '❓', label: v };
  }

  getNiveauLabel(n: number): string { return this.niveauxLabels[n] || '—'; }

  getNiveauClass(n: number): string {
    return n === 1 ? 'niveau-faible' : n === 2 ? 'niveau-moyen' : n === 3 ? 'niveau-eleve' : '';
  }

  getDecision(v: string) {
    return this.decisionsTraitement.find(d => d.value === v) ||
      { label: v, desc: '', class: '' };
  }

  getFiabiliteClass(f: number): string {
    if (f <= 1)  return 'fib-faible';
    if (f <= 2)  return 'fib-moyen';
    if (f <= 4)  return 'fib-eleve';
    return 'fib-critique';
  }

  // Cartographie : grouper par niveau de risque
  get ssParNiveau(): { eleve: any[], moyen: any[], faible: any[] } {
    return {
      eleve:  this.listeSS.filter(s => s.niveau_risque === 3),
      moyen:  this.listeSS.filter(s => s.niveau_risque === 2),
      faible: this.listeSS.filter(s => s.niveau_risque === 1)
    };
  }
  getRange(n: number): number[] {
  return Array.from({ length: n }, (_, i) => i + 1);
}

getGVClass(n: number, max: number): string {
  const ratio = n / max;
  if (ratio <= 0.25) return 'gv-1';
  if (ratio <= 0.5)  return 'gv-2';
  if (ratio <= 0.75) return 'gv-3';
  return 'gv-4';
}

calcNiveauPreview(): number {
  const g   = this.ssForm.get('gravite')?.value || 2;
  const v   = this.ssForm.get('vraisemblance')?.value || 2;
  const acc = this.analyse?.seuil_acceptable || 6;
  const elv = this.analyse?.seuil_eleve      || 12;
  const score = g * v;
  if (score <= acc) return 1;
  if (score <= elv) return 2;
  return 3;
}

// Dans le template Math.min est nécessaire
Math = Math;
}