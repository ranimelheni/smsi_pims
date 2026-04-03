import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder,
         FormGroup, FormArray, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-rssi',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './rssi.component.html',
  styleUrls: ['./rssi.component.scss']
})
export class RssiComponent implements OnInit {

  currentUser: any = null;
  activeTab = 'inventaire'; // inventaire | clause4 | fiches
  organism:  any = null;
  isIso27701 = false;

  // ── Inventaire actifs ─────────────────────────────────────────────────────
  actifs:          any[] = [];
  actifsValides:   any[] = [];
  consolidation:   any   = null;
  loadingActifs    = false;
  loadingConsolide = false;
  extracting       = false;
  filterCategorie  = '';
  filterCritique   = '';

  // ── Clause 4 ──────────────────────────────────────────────────────────────
  clause4Form!: FormGroup;
  savingClause4  = false;
  clause4Loaded  = false;
  activeClause4Section = 0;

  // ── Fiches à valider ──────────────────────────────────────────────────────
  fichesProcessus:  any[] = [];
  fichesTechniques: any[] = [];
  selectedFiche:    any   = null;
  ficheType:        string = ''; // processus | technique
  activeSection     = 0;
  showRejectForm    = false;
  commentaireRejet  = '';
  submittingDecision = false;

  success = '';
  error   = '';

   private api = 'http://localhost:8080/api';

  categories = [
    { value: 'materiel', label: 'Matériel',  icon: '🖥️' },
    { value: 'logiciel', label: 'Logiciel',  icon: '💿' },
    { value: 'donnees',  label: 'Données',   icon: '🗄️' },
    { value: 'service',  label: 'Service',   icon: '☁️' },
    { value: 'humain',   label: 'Humain',    icon: '👤' },
    { value: 'site',     label: 'Site',      icon: '🏢' }
  ];

  niveaux = [
    { value: 1, label: 'Public',       color: 'green'  },
    { value: 2, label: 'Interne',      color: 'blue'   },
    { value: 3, label: 'Confidentiel', color: 'amber'  },
    { value: 4, label: 'Secret',       color: 'red'    }
  ];

  clause4Sections = [
    { label: '4.1 Enjeux',           icon: '⚖️'  },
    { label: '4.2 Parties',          icon: '👥'  },
    { label: '4.3 Périmètre SMSI',   icon: '🎯'  },
    { label: '4.4 Ressources',       icon: '🔧'  }
  ];

  fichesSections = [
    'Identification', 'Finalité', 'Déclencheurs', 'Entrées/Sorties',
    'Informations doc.', 'Contraintes', 'Personnel', 'KPI',
    'Surveillance', 'Interactions', 'Risques', 'Opportunités', 'DPO'
  ];

  constructor(
    private fb:   FormBuilder,
    private auth: AuthService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.currentUser = this.auth.getCurrentUser();
    this.initClause4Form();
    this.loadAll();
  }

  loadAll(): void {
    this.loadClause4();
    this.loadActifsValides();
    this.loadConsolidation();
    this.loadFichesProcessus();
    this.loadFichesTechniques();
  }

  // ════════════════════════════════════════════════════════════════════════════
  // INVENTAIRE ACTIFS
  // ════════════════════════════════════════════════════════════════════════════

  loadActifsValides(): void {
    this.http.get<any[]>(`${this.api}/actifs`).subscribe({
      next: (data) => { this.actifsValides = data; }
    });
  }

  loadConsolidation(): void {
    this.loadingConsolide = true;
    this.http.get<any>(`${this.api}/actifs/consolider`).subscribe({
      next: (data) => {
        this.consolidation    = data;
        this.loadingConsolide = false;
      },
      error: () => { this.loadingConsolide = false; }
    });
  }

  toggleActif(idx: number): void {
    if (this.consolidation?.actifs)
      this.consolidation.actifs[idx].selected = !this.consolidation.actifs[idx].selected;
  }

  selectAllActifs():   void { this.consolidation?.actifs.forEach((a: any) => a.selected = true);  }
  deselectAllActifs(): void { this.consolidation?.actifs.forEach((a: any) => a.selected = false); }

  get selectedActifsCount(): number {
    return this.consolidation?.actifs?.filter((a: any) => a.selected).length || 0;
  }

  validerActifs(): void {
    const selected = this.consolidation?.actifs?.filter((a: any) => a.selected) || [];
    if (!selected.length) return;
    this.extracting = true;
    this.http.post(`${this.api}/actifs/valider-lot`, selected).subscribe({
      next: (res: any) => {
        this.extracting = false;
        this.success    = res.message;
        this.loadActifsValides();
        this.loadConsolidation();
        setTimeout(() => this.success = '', 5000);
      },
      error: (err) => {
        this.extracting = false;
        this.error = err.error?.error || 'Erreur validation actifs';
      }
    });
  }

  updateCia(actif: any, field: string, val: number): void { actif[field] = val; }

  getCatIcon(v: string): string { return this.categories.find(c => c.value === v)?.icon || '📦'; }
  getCatLabel(v: string): string { return this.categories.find(c => c.value === v)?.label || v; }
  getNiveauLabel(n: number): string { return this.niveaux.find(x => x.value === n)?.label || '—'; }
  getNiveauClass(n: number): string {
    const m: Record<number, string> = {1:'badge-green',2:'badge-blue',3:'badge-amber',4:'badge-red'};
    return m[n] || 'badge-gray';
  }

  // ════════════════════════════════════════════════════════════════════════════
  // CLAUSE 4
  // ════════════════════════════════════════════════════════════════════════════

  initClause4Form(): void {
    this.clause4Form = this.fb.group({
      // 4.1
      enjeux_externes: this.fb.array([]),
      enjeux_internes: this.fb.array([]),

      // 4.2
      parties_interessees: this.fb.array([]),

      // 4.3
      perimetre_smsi:            ['', Validators.required],
      perimetre_pims:            [''],
      sites_concernes:           [''],
      activites_exclues:         [''],
      justification_exclusions:  [''],
      interfaces_dependances:    [''],
      responsable_traitement:    ['responsable'],

      // 4.4
      engagement_direction:      ['', Validators.required],
      politique_securite:        ['', Validators.required],
      politique_confidentialite: [''],
      ressources_humaines:       this.fb.array([]),
      ressources_logicielles:    this.fb.array([]),
      ressources_materielles:    this.fb.array([]),
      procedures:                this.fb.array([]),
      outils_protection:         this.fb.array([]),
      objectifs_smsi:            this.fb.array([]),
      date_revue:                [''],
      version:                   ['v1.0']
    });
  }

  // FormArray getters
  get enjeuxExternes():      FormArray { return this.clause4Form.get('enjeux_externes')      as FormArray; }
  get enjeuxInternes():      FormArray { return this.clause4Form.get('enjeux_internes')      as FormArray; }
  get partiesInteressees():  FormArray { return this.clause4Form.get('parties_interessees')  as FormArray; }
  get ressourcesHumaines():  FormArray { return this.clause4Form.get('ressources_humaines')  as FormArray; }
  get ressourcesLogicielles():FormArray{ return this.clause4Form.get('ressources_logicielles') as FormArray; }
  get ressourcesMaterielles():FormArray{ return this.clause4Form.get('ressources_materielles') as FormArray; }
  get procedures():          FormArray { return this.clause4Form.get('procedures')           as FormArray; }
  get outilsProtection():    FormArray { return this.clause4Form.get('outils_protection')    as FormArray; }
  get objectifsSmsi():       FormArray { return this.clause4Form.get('objectifs_smsi')       as FormArray; }

  // Templates
  newEnjeuExterne() {
    return this.fb.group({
      categorie:   ['reglementaire'],
      description: ['', Validators.required],
      impact:      ['moyen'],
      opportunite: [false]
    });
  }
  newEnjeuInterne() {
    return this.fb.group({
      categorie:   ['organisationnel'],
      description: ['', Validators.required],
      impact:      ['moyen'],
      opportunite: [false]
    });
  }
  newPartie() {
    return this.fb.group({
      nom:                ['', Validators.required],
      type:               ['interne'],
      exigences:          ['', Validators.required],
      traitee_smsi:       [false],
      responsable_dcp:    [false],
      sous_traitant_dcp:  [false]
    });
  }
  newRessourceHumaine()   { return this.fb.group({ nom: ['', Validators.required], role: [''], competences: [''] }); }
  newRessourceLogicielle(){ return this.fb.group({ nom: ['', Validators.required], version: [''], usage: [''] }); }
  newRessourceMaterielle(){ return this.fb.group({ nom: ['', Validators.required], type: [''], localisation: [''] }); }
  newProcedure()          { return this.fb.group({ titre: ['', Validators.required], reference: [''], description: [''] }); }
  newOutil()              { return this.fb.group({ nom: ['', Validators.required], type: [''], description: [''] }); }
  newObjectif()           { return this.fb.group({ objectif: ['', Validators.required], indicateur: [''], cible: [''], responsable: [''] }); }

  addTo(arr: FormArray, item: any): void { arr.push(item); }
  removeFrom(arr: FormArray, i: number): void { arr.removeAt(i); }

  loadClause4(): void {
    this.http.get<any>(`${this.api}/clause4`).subscribe({
      next: (data) => {
        this.clause4Loaded = true;
        if (!data || Object.keys(data).length === 0) return;
        this.patchClause4(data);
      },
      error: () => { this.clause4Loaded = true; }
    });
  }

  patchClause4(data: any): void {
    // Champs simples
    this.clause4Form.patchValue({
      perimetre_smsi:            data.perimetre_smsi            || '',
      perimetre_pims:            data.perimetre_pims            || '',
      sites_concernes:           data.sites_concernes           || '',
      activites_exclues:         data.activites_exclues         || '',
      justification_exclusions:  data.justification_exclusions  || '',
      interfaces_dependances:    data.interfaces_dependances    || '',
      responsable_traitement:    data.responsable_traitement    || 'responsable',
      engagement_direction:      data.engagement_direction      || '',
      politique_securite:        data.politique_securite        || '',
      politique_confidentialite: data.politique_confidentialite || '',
      date_revue:                data.date_revue                || '',
      version:                   data.version                   || 'v1.0'
    });

    // FormArrays
    const arrays: [string, FormArray, () => any][] = [
      ['enjeux_externes',       this.enjeuxExternes,       () => this.newEnjeuExterne()],
      ['enjeux_internes',       this.enjeuxInternes,       () => this.newEnjeuInterne()],
      ['parties_interessees',   this.partiesInteressees,   () => this.newPartie()],
      ['ressources_humaines',   this.ressourcesHumaines,   () => this.newRessourceHumaine()],
      ['ressources_logicielles',this.ressourcesLogicielles,() => this.newRessourceLogicielle()],
      ['ressources_materielles',this.ressourcesMaterielles,() => this.newRessourceMaterielle()],
      ['procedures',            this.procedures,            () => this.newProcedure()],
      ['outils_protection',     this.outilsProtection,     () => this.newOutil()],
      ['objectifs_smsi',        this.objectifsSmsi,        () => this.newObjectif()]
    ];

    arrays.forEach(([key, arr, factory]) => {
      arr.clear();
      const items = data[key] || [];
      if (Array.isArray(items)) {
        items.forEach((item: any) => {
          const g = factory();
          g.patchValue(item);
          arr.push(g);
        });
      }
    });
  }

  saveClause4(): void {
    this.savingClause4 = true;
    this.http.post(`${this.api}/clause4`, this.clause4Form.value).subscribe({
      next: () => {
        this.savingClause4 = false;
        this.success = 'Clause 4 sauvegardée';
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => {
        this.savingClause4 = false;
        this.error = err.error?.error || 'Erreur sauvegarde';
      }
    });
  }

  // ════════════════════════════════════════════════════════════════════════════
  // FICHES À VALIDER
  // ════════════════════════════════════════════════════════════════════════════

  loadFichesProcessus(): void {
    this.http.get<any>(`${this.api}/fiches/mine`).subscribe({
      next: (data) => {
        this.fichesProcessus = Array.isArray(data) ? data : [data];
      }
    });
  }

  loadFichesTechniques(): void {
    this.http.get<any[]>(`${this.api}/fiches-techniques`).subscribe({
      next: (data) => { this.fichesTechniques = data || []; }
    });
  }

  selectFicheProcessus(fiche: any): void {
    this.selectedFiche   = fiche;
    this.ficheType       = 'processus';
    this.showRejectForm  = false;
    this.commentaireRejet = '';
    this.activeSection   = 0;
  }

  selectFicheTechnique(fiche: any): void {
    this.selectedFiche   = fiche;
    this.ficheType       = 'technique';
    this.showRejectForm  = false;
    this.commentaireRejet = '';
    this.activeSection   = 0;
  }

  validerFiche(): void {
    if (!this.selectedFiche) return;
    this.submittingDecision = true;

    const url = this.ficheType === 'processus'
      ? `${this.api}/fiches/${this.selectedFiche.id}/statut`
      : `${this.api}/fiches-techniques/${this.selectedFiche.id}/statut`;

    this.http.put(url, { statut: 'valide' }).subscribe({
      next: () => {
        this.submittingDecision = false;
        this.selectedFiche.statut = 'valide';
        this.success = '✓ Fiche validée';
        this.ficheType === 'processus' ? this.loadFichesProcessus() : this.loadFichesTechniques();
        setTimeout(() => this.success = '', 4000);
      },
      error: (err) => {
        this.submittingDecision = false;
        this.error = err.error?.error || 'Erreur validation';
      }
    });
  }

  rejeterFiche(): void {
    if (!this.selectedFiche || !this.commentaireRejet.trim()) return;
    this.submittingDecision = true;

    const url = this.ficheType === 'processus'
      ? `${this.api}/fiches/${this.selectedFiche.id}/statut`
      : `${this.api}/fiches-techniques/${this.selectedFiche.id}/statut`;

    this.http.put(url, { statut: 'rejete', commentaire: this.commentaireRejet }).subscribe({
      next: () => {
        this.submittingDecision = false;
        this.selectedFiche.statut = 'rejete';
        this.showRejectForm = false;
        this.success = 'Fiche rejetée';
        this.ficheType === 'processus' ? this.loadFichesProcessus() : this.loadFichesTechniques();
        setTimeout(() => this.success = '', 4000);
      },
      error: (err) => {
        this.submittingDecision = false;
        this.error = err.error?.error || 'Erreur rejet';
      }
    });
  }

  // Helpers
  parseJson(val: any): any[] {
    if (!val) return [];
    if (typeof val === 'string') { try { return JSON.parse(val); } catch { return []; } }
    return Array.isArray(val) ? val : [];
  }

  getStatutLabel(s: string): string {
    const m: Record<string, string> = {
      brouillon: 'Brouillon', soumis_rssi: 'En attente RSSI',
      soumis_dpo: 'En attente DPO', complete_dpo: 'DPO complété',
      valide: 'Validé', rejete: 'Rejeté'
    };
    return m[s] || s;
  }

  getStatutClass(s: string): string {
    const m: Record<string, string> = {
      brouillon: 'badge-gray', soumis_rssi: 'badge-amber',
      complete_dpo: 'badge-blue', valide: 'badge-green', rejete: 'badge-red'
    };
    return m[s] || 'badge-gray';
  }

  canValidate(): boolean {
    return ['soumis_rssi', 'complete_dpo'].includes(this.selectedFiche?.statut);
  }

  get fichesEnAttente(): any[] {
    return this.fichesProcessus.filter(f =>
      ['soumis_rssi', 'complete_dpo'].includes(f.statut));
  }

  get fichesTechEnAttente(): any[] {
    return this.fichesTechniques.filter(f => f.statut === 'soumis_rssi');
  }
}