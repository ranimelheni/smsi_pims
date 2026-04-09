import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder,
         FormGroup, FormArray, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-clause6-rssi',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './clause6-rssi.component.html',
  styleUrls: ['./clause6-rssi.component.scss']
})
export class Clause6RssiComponent implements OnInit {

  currentUser: any = null;
activeTab: 'soa' | 'objectifs' | 'modifications' | 'methodologie' = 'soa';
  // ── SoA ───────────────────────────────────────────────────────────────────
  soa: any = null;
  domaines: any[] = [];
  selectedDom: string = '';
  controles: any[] = [];
  loadingSoa = false;
  initialising = false;
  soumettant = false;

  // Filtre SoA
  filtreApplicable = '';
  filtreStatutImpl = '';
  filtreTexte = '';

  // Édition contrôle inline
  editingControle: any = null;
  editForm!: FormGroup;

  // Variables simplifiées pour SoA batch
  roleOrganisme = 'responsable';
  pendingChanges: Map<number, any> = new Map();
  saving = false;

  // ── Objectifs ─────────────────────────────────────────────────────────────
  objectifs: any[] = [];
  objStats: any = null;
  showObjForm = false;
  editingObj: any = null;
  objForm!: FormGroup;
  savingObj = false;

  // ── Modifications ─────────────────────────────────────────────────────────
  modifications: any[] = [];
  showModifForm = false;
  editingModif: any = null;
  modifForm!: FormGroup;
  savingModif = false;

  success = '';
  error = '';

  // ── Méthodologie ─────────────────────────────────────────────────────────
methodo:      any    = null;
methodoForm!: FormGroup;
savingMeth    = false;
soumettantMeth = false;
// Remplacer le tableau methodes existant par :
get methodes() {
  return this.isIso27701
    ? [] // pas de choix pour 27701, PIA est imposé
    : [
        { value: 'ebios_rm',  icon: '🇫🇷', label: 'EBIOS Risk Manager',
          desc: 'Méthode ANSSI — 5 ateliers. Recommandée pour les organisations françaises.' },
        { value: 'iso_27005', icon: '🌐', label: 'ISO 27005',
          desc: 'Norme internationale de gestion des risques SI.' },
        { value: 'mehari',    icon: '📊', label: 'MEHARI',
          desc: 'Méthode CLUSIF — orientée tableaux de bord.' },
        { value: 'octave',    icon: '🔷', label: 'OCTAVE',
          desc: 'Méthode Carnegie Mellon — auto-évaluation.' },
        { value: 'autre',     icon: '⚙️', label: 'Autre méthode',
          desc: 'Méthode personnalisée ou hybride.' }
      ];
}

// Ajouter ces getters :
get isPiaMode(): boolean {
  return this.isIso27701 || this.methodo?.type_audit === 'iso27701';
}

get methodoTitre(): string {
  return this.isPiaMode
    ? 'Analyse d\'Impact relative à la Protection des données (PIA / DPIA)'
    : 'Méthodologie de gestion des risques SI';
}

get methodoDescription(): string {
  return this.isPiaMode
    ? 'Pour un audit ISO 27701, la méthode de gestion des risques privacy est le PIA (Privacy Impact Assessment). Cette analyse est obligatoire pour les traitements à risque élevé (RGPD Art. 35).'
    : 'Choisir la méthodologie de gestion des risques adaptée à l\'organisme et paramétrer la matrice de risque.';
}

  private api = 'http://localhost:8080/api';

  // ── Labels annexes (ISO 27001 + ISO 27701) ────────────────────────────────
  domainesLabels: Record<string, string> = {
    // ISO 27001
    'A.5': 'A.5 — Contrôles organisationnels',
    'A.6': 'A.6 — Contrôles des personnes',
    'A.7': 'A.7 — Contrôles physiques',
    'A.8': 'A.8 — Contrôles technologiques',
    // ISO 27701
    'A.1': 'A.1 — Responsables de traitement de DCP',
    'A.2': 'A.2 — Sous-traitants de DCP',
    'A.3': 'A.3 — Contrôles communs RT et ST'
  };

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.currentUser = this.auth.getCurrentUser();

    // Debug : vérifier la structure du user
    console.log('>>> currentUser:', JSON.stringify(this.currentUser));
    console.log('>>> organism:', this.currentUser?.organism);
    console.log('>>> auditType:', this.currentUser?.organism?.auditType);

    this.initForms();
    this.loadSoa();
    this.loadObjectifs();
    this.loadModifications();
    this.loadMethodologie();
    
  }
loadMethodologie(): void {
  this.http.get<any>(`${this.api}/methodologie-risque`).subscribe({
    next: (d) => {
      this.methodo = d && Object.keys(d).length > 0 ? d : null;
      if (this.methodo) {
        this.methodoForm.patchValue({
          ...this.methodo,
          labels_probabilite: this.methodo.labels_probabilite || [],
          labels_impact:      this.methodo.labels_impact      || []
        });
      }
    }
  });
}
saveMethodo(): void {
  if (this.methodoForm.invalid) { this.methodoForm.markAllAsTouched(); return; }
  this.savingMeth = true;
  this.http.post(`${this.api}/methodologie-risque`, this.methodoForm.value).subscribe({
    next: (d: any) => {
      this.savingMeth = false;
      this.methodo    = d;
      this.success    = 'Méthodologie sauvegardée';
      setTimeout(() => this.success = '', 3000);
    },
    error: (err: any) => { this.savingMeth = false; this.error = err.error?.error || 'Erreur'; }
  });
}

soumettreMethodo(): void {
  this.soumettantMeth = true;
  this.http.put(`${this.api}/methodologie-risque/soumettre`, {}).subscribe({
    next: () => {
      this.soumettantMeth = false;
      if (this.methodo) this.methodo.statut = 'soumis_direction';
      this.success = 'Méthodologie soumise à la direction';
      setTimeout(() => this.success = '', 4000);
    },
    error: (err: any) => {
      this.soumettantMeth = false;
      this.error = err.error?.error || 'Erreur';
    }
  });
}

getMethLabel(v: string): string {
  const m: Record<string, string> = {
    ebios_rm:  'EBIOS Risk Manager',
    iso_27005: 'ISO 27005',
    mehari:    'MEHARI',
    octave:    'OCTAVE',
    autre:     'Autre méthode'
  };
  return m[v] || v;
}

getMethStatutClass(v: string): string {
  const m: Record<string, string> = {
    propose:          'badge-gray',
    soumis_direction: 'badge-amber',
    valide:           'badge-green',
    rejete:           'badge-red'
  };
  return m[v] || 'badge-gray';
}

getNiveauClass(v: string): string {
  const m: Record<string, string> = {
    faible: 'cell-faible', moyen: 'cell-moyen', eleve: 'cell-eleve'
  };
  return m[v] || '';
}

getCellScore(cell: string): number {
  return parseInt(cell.split(':')[1]);
}

getCellNiveau(cell: string): string {
  return cell.split(':')[0];
}

get labelsProba(): string[] {
  const v = this.methodoForm.get('labels_probabilite')?.value;
  return Array.isArray(v) ? v : [];
}

get labelsImpact(): string[] {
  const v = this.methodoForm.get('labels_impact')?.value;
  return Array.isArray(v) ? v : [];
}

updateLabel(type: 'proba'|'impact', idx: number, val: string): void {
  const key = type === 'proba' ? 'labels_probabilite' : 'labels_impact';
  const arr  = [...(this.methodoForm.get(key)?.value || [])];
  arr[idx]   = val;
  this.methodoForm.get(key)?.setValue(arr);
}

get matricePreview(): any[][] {
  if (!this.methodo?.matrice) return [];
  return this.methodo.matrice;
}

get echellePRow(): number[] {
  const e = this.methodoForm.get('echelle_probabilite')?.value || 4;
  return Array.from({ length: e }, (_, i) => e - i);
}

get echelleIRow(): number[] {
  const e = this.methodoForm.get('echelle_impact')?.value || 4;
  return Array.from({ length: e }, (_, i) => i + 1);
}

get matriceFormPreview(): string[][] {
  const p   = this.methodoForm.get('echelle_probabilite')?.value || 4;
  const i   = this.methodoForm.get('echelle_impact')?.value      || 4;
  const acc = this.methodoForm.get('seuil_acceptable')?.value    || 6;
  const elv = this.methodoForm.get('seuil_eleve')?.value         || 12;
  const rows: string[][] = [];
  for (let pi = p; pi >= 1; pi--) {
    const row: string[] = [];
    for (let ii = 1; ii <= i; ii++) {
      const score = pi * ii;
      row.push(score <= acc ? 'faible:'+score : score <= elv ? 'moyen:'+score : 'eleve:'+score);
    }
    rows.push(row);
  }
  return rows;
}
  // ── Getter : détecter si l'organisme est ISO 27701 ────────────────────────
  // Cherche dans toutes les clés possibles selon ce que retourne le backend
  get isIso27701(): boolean {
    const auditType =
      this.currentUser?.organism?.auditType       ||  // camelCase Java
      this.currentUser?.organism?.audit_type      ||  // snake_case SQL
      this.currentUser?.auditType                 ||  // directement sur user
      this.currentUser?.audit_type                ||  // snake_case direct
      this.soa?.audit_type                        ||  // depuis la SoA chargée
      '';

    console.log('>>> isIso27701 check — auditType =', auditType);

    return auditType === 'ISO_27701'
        || auditType === 'iso27701'
        || auditType === 'iso_27701';
  }

  get soaLabel(): string {
    return this.isIso27701
      ? 'ISO 27701 — Protection des données personnelles'
      : 'ISO 27001:2022 — Sécurité de l\'information';
  }

  get soaDescription(): string {
    return this.isIso27701
      ? 'La SoA liste les contrôles ISO 27701 (Annexes A.1, A.2, A.3) pour la protection des données personnelles (DCP).'
      : 'La SoA liste les 93 contrôles de sécurité de l\'ISO 27001:2022 et permet de définir leur applicabilité et leur statut d\'implémentation.';
  }

  get initConfirmMessage(): string {
    if (this.isIso27701) {
      const roleLabel = this.roleOrganisme === 'responsable'
        ? 'Responsable de traitement'
        : this.roleOrganisme === 'sous_traitant'
          ? 'Sous-traitant'
          : 'Responsable + Sous-traitant';
      return `Initialiser la SoA ISO 27701 (${roleLabel}) avec les contrôles des Annexes A.1/A.2/A.3 ?`;
    }
    return 'Initialiser la SoA avec les 93 contrôles ISO 27001:2022 ?';
  }

  // ── Initialisation Forms ───────────────────────────────────────────────────
  initForms(): void {
    this.editForm = this.fb.group({
      applicable: ['applicable'],
      justification: [''],
      reference_doc: [''],
      statut_impl: ['planifie'],
      responsable: [''],
      echeance: [''],
      note_impl: ['']
    });

    this.objForm = this.fb.group({
      titre: ['', Validators.required],
      description: [''],
      lien_politique: [''],
      responsable: ['', Validators.required],
      ressources: [''],
      echeance: ['', Validators.required],
      moyen_evaluation: ['', Validators.required],
      statut: ['planifie'],
      avancement: [0],
      commentaire: ['']
    });

    this.modifForm = this.fb.group({
      titre: ['', Validators.required],
      description: ['', Validators.required],
      type_modification: ['technologique'],
      impacts: ['', Validators.required],
      actions: this.fb.array([]),
      statut: ['en_analyse']
    });
    this.methodoForm = this.fb.group({
  methode:              ['ebios_rm', Validators.required],
  methode_custom:       [''],
  justification:        ['', Validators.required],
  perimetre_risque:     [''],
  objectifs_risque:     [''],
  criteres_acceptation: [''],
  echelle_probabilite:  [4],
  echelle_impact:       [4],
  seuil_acceptable:     [6],
  seuil_eleve:          [12],
  formule_calcul:       ['probabilite_x_impact'],
  labels_probabilite:   [['Rare', 'Peu probable', 'Probable', 'Quasi-certain']],
  labels_impact:        [['Négligeable', 'Limité', 'Important', 'Critique']]
});
  }

  // ── SoA ───────────────────────────────────────────────────────────────────
  loadSoa(): void {
    this.loadingSoa = true;
    this.http.get<any>(`${this.api}/soa`).subscribe({
      next: (data) => {
        this.loadingSoa = false;
        if (!data || Object.keys(data).length === 0) { this.soa = null; return; }
        this.soa = data;
        this.controles = data.controles || [];
        console.log('>>> SoA chargée — audit_type:', data.audit_type);
        this.loadDomaines();
      },
      error: () => { this.loadingSoa = false; }
    });
  }

  loadDomaines(): void {
    this.http.get<any[]>(`${this.api}/soa/domaines`).subscribe({
      next: (d) => { this.domaines = d || []; }
    });
  }

  initSoa(): void {
    console.log('>>> initSoa — isIso27701:', this.isIso27701);
    console.log('>>> initSoa — roleOrganisme:', this.roleOrganisme);

    if (!confirm(this.initConfirmMessage)) return;

    this.initialising = true;

    // Pour ISO 27701 : envoyer le rôle ; pour ISO 27001 : body vide
    const body = this.isIso27701
      ? { role_organisme: this.roleOrganisme }
      : {};

    console.log('>>> POST /api/soa/init — body:', body);

    this.http.post<any>(`${this.api}/soa/init`, body).subscribe({
      next: (res) => {
        this.initialising = false;
        this.success = res?.message || 'SoA initialisée avec succès';
        console.log('>>> SoA initialisée:', res);
        this.loadSoa();
        setTimeout(() => this.success = '', 4000);
      },
      error: (err) => {
        this.initialising = false;
        this.error = err.error?.error || 'Erreur initialisation';
        console.error('>>> Erreur init SoA:', err);
      }
    });
  }

  soumettreSoa(): void {
    this.soumettant = true;
    this.http.put(`${this.api}/soa/soumettre`, {}).subscribe({
      next: () => {
        this.soumettant = false;
        this.soa.statut = 'soumis_direction';
        this.success = 'SoA soumise à la direction pour validation';
        setTimeout(() => this.success = '', 4000);
      },
      error: (err) => {
        this.soumettant = false;
        this.error = err.error?.error || 'Erreur soumission';
      }
    });
  }

  // Filtrer les contrôles
  get controlesFiltres(): any[] {
    return this.controles.filter(c => {
      const matchDom = !this.selectedDom || c.domaine === this.selectedDom;
      const matchAppl = !this.filtreApplicable || c.applicable === this.filtreApplicable;
      const matchImpl = !this.filtreStatutImpl || c.statut_impl === this.filtreStatutImpl;
      const matchTxt = !this.filtreTexte ||
        c.controle_label.toLowerCase().includes(this.filtreTexte.toLowerCase()) ||
        c.controle_id.toLowerCase().includes(this.filtreTexte.toLowerCase());
      return matchDom && matchAppl && matchImpl && matchTxt;
    });
  }

  // ── Edition Contrôle Inline ───────────────────────────────────────────────
  startEdit(c: any): void {
    this.editingControle = c;
    this.editForm.patchValue({
      applicable: c.applicable || 'applicable',
      justification: c.justification || '',
      reference_doc: c.reference_doc || '',
      statut_impl: c.statut_impl || 'planifie',
      responsable: c.responsable || '',
      echeance: c.echeance || '',
      note_impl: c.note_impl || ''
    });
  }

  cancelEdit(): void { this.editingControle = null; }

  saveControle(): void {
    if (!this.editingControle) return;
    this.http.put(`${this.api}/soa/controles/${this.editingControle.id}`, this.editForm.value)
      .subscribe({
        next: (updated: any) => {
          const idx = this.controles.findIndex(c => c.id === updated.id);
          if (idx >= 0) this.controles[idx] = updated;
          this.editingControle = null;
          this.loadDomaines();
          this.updateSoaStats();
        },
        error: (err) => { this.error = err.error?.error || 'Erreur mise à jour'; }
      });
  }

  // ── Gestion batch SoA ─────────────────────────────────────────────────────
  toggleInclus(c: any): void {
    c.inclus = !c.inclus;
    if (!c.inclus && !c.justification_exclusion)
      c.justification_exclusion = '';
    this.pendingChanges.set(c.id, {
      id: c.id,
      inclus: c.inclus,
      justification_exclusion: c.justification_exclusion,
      reference_doc: c.reference_doc,
      statut_impl: c.statut_impl,
      responsable: c.responsable
    });
  }

  updateField(c: any, field: string, value: any): void {
    (c as any)[field] = value;
    const existing = this.pendingChanges.get(c.id) || { id: c.id };
    existing[field] = value;
    this.pendingChanges.set(c.id, existing);
  }

  saveAll(): void {
    if (this.pendingChanges.size === 0) return;
    this.saving = true;
    const updates = Array.from(this.pendingChanges.values());
    this.http.put(`${this.api}/soa/controles/batch`, updates).subscribe({
      next: () => {
        this.saving = false;
        this.pendingChanges.clear();
        this.success = updates.length + ' contrôle(s) sauvegardé(s)';
        this.updateSoaStats();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => {
        this.saving = false;
        this.error = err.error?.error || 'Erreur sauvegarde';
      }
    });
  }

  get hasPendingChanges(): boolean { return this.pendingChanges.size > 0; }

  updateSoaStats(): void {
    const applicable = this.controles.filter(c => c.applicable === 'applicable').length;
    const implemente = this.controles.filter(c => c.statut_impl === 'implemente').length;
    if (this.soa?.stats) {
      this.soa.stats.applicable = applicable;
      this.soa.stats.implemente = implemente;
      this.soa.stats.taux_impl = this.controles.length > 0
        ? Math.round(implemente * 100 / this.controles.length) : 0;
    }
  }

  // ── Objectifs ─────────────────────────────────────────────────────────────
  loadObjectifs(): void {
    this.http.get<any>(`${this.api}/objectifs-securite`).subscribe({
      next: (d) => { this.objectifs = d.items || []; this.objStats = d.stats; }
    });
  }

  submitObj(): void {
    if (this.objForm.invalid) { this.objForm.markAllAsTouched(); return; }
    this.savingObj = true;
    const url = this.editingObj
      ? `${this.api}/objectifs-securite/${this.editingObj.id}`
      : `${this.api}/objectifs-securite`;
    const method = this.editingObj ? 'put' : 'post';

    (this.http as any)[method](url, this.objForm.value).subscribe({
      next: () => {
        this.savingObj = false;
        this.showObjForm = false;
        this.editingObj = null;
        this.objForm.reset({ statut: 'planifie', avancement: 0 });
        this.success = this.editingObj ? 'Objectif mis à jour' : 'Objectif créé';
        this.loadObjectifs();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err: any) => { this.savingObj = false; this.error = err.error?.error || 'Erreur'; }
    });
  }

  editObj(o: any): void {
    this.editingObj = o;
    this.showObjForm = true;
    this.objForm.patchValue(o);
  }

  deleteObj(id: number): void {
    if (!confirm('Supprimer cet objectif ?')) return;
    this.http.delete(`${this.api}/objectifs-securite/${id}`).subscribe({
      next: () => this.loadObjectifs()
    });
  }

  // ── Modifications ─────────────────────────────────────────────────────────
  get actionsModif(): FormArray { return this.modifForm.get('actions') as FormArray; }

  newAction() {
    return this.fb.group({
      action: ['', Validators.required],
      responsable: [''],
      echeance: [''],
      statut: ['a_faire']
    });
  }

  loadModifications(): void {
    this.http.get<any[]>(`${this.api}/modifications-smsi`).subscribe({
      next: (d) => { this.modifications = d || []; }
    });
  }

  submitModif(): void {
    if (this.modifForm.invalid) { this.modifForm.markAllAsTouched(); return; }
    this.savingModif = true;
    const url = this.editingModif
      ? `${this.api}/modifications-smsi/${this.editingModif.id}`
      : `${this.api}/modifications-smsi`;
    const method = this.editingModif ? 'put' : 'post';

    (this.http as any)[method](url, this.modifForm.value).subscribe({
      next: () => {
        this.savingModif = false;
        this.showModifForm = false;
        this.editingModif = null;
        this.modifForm.reset({ type_modification: 'technologique', statut: 'en_analyse' });
        this.actionsModif.clear();
        this.loadModifications();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err: any) => { this.savingModif = false; this.error = err.error?.error || 'Erreur'; }
    });
  }

  editModif(m: any): void {
    this.editingModif = m;
    this.showModifForm = true;
    this.actionsModif.clear();
    this.modifForm.patchValue(m);
    if (Array.isArray(m.actions)) {
      m.actions.forEach((a: any) => {
        const g = this.newAction();
        g.patchValue(a);
        this.actionsModif.push(g);
      });
    }
  }

  deleteModif(id: number): void {
    if (!confirm('Supprimer cette modification ?')) return;
    this.http.delete(`${this.api}/modifications-smsi/${id}`).subscribe({
      next: () => this.loadModifications()
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  getApplicableClass(v: string): string {
    const m: Record<string, string> = {
      applicable: 'badge-green', non_applicable: 'badge-gray', partiel: 'badge-amber'
    };
    return m[v] || 'badge-gray';
  }

  getImplClass(v: string): string {
    const m: Record<string, string> = {
      implemente: 'badge-green', en_cours: 'badge-amber',
      planifie: 'badge-blue', non_applicable: 'badge-gray'
    };
    return m[v] || 'badge-gray';
  }

  getImplLabel(v: string): string {
    const m: Record<string, string> = {
      implemente: 'Implémenté', en_cours: 'En cours',
      planifie: 'Planifié', non_applicable: 'N/A'
    };
    return m[v] || v;
  }

  getObjStatutClass(v: string): string {
    const m: Record<string, string> = {
      atteint: 'badge-green', en_cours: 'badge-amber',
      planifie: 'badge-blue', non_atteint: 'badge-red', reporte: 'badge-gray'
    };
    return m[v] || 'badge-gray';
  }

  getModifStatutClass(v: string): string {
    const m: Record<string, string> = {
      en_analyse: 'badge-amber', en_cours: 'badge-blue', cloture: 'badge-green'
    };
    return m[v] || 'badge-gray';
  }

  getDomaineSoaStats(dom: string): any {
    return this.domaines.find(d => d.domaine === dom);
  }

  getAnnexeKeys(): string[] {
    if (!this.soa?.par_annexe) return [];
    return Object.keys(this.soa.par_annexe);
  }

  getAnnexeLabel(annexe: string): string {
    const first = this.soa?.par_annexe?.[annexe]?.[0];
    return first?.annexe_label || this.domainesLabels[annexe] || annexe;
  }

  getControlesForAnnexe(annexe: string): any[] {
    const all = this.soa?.par_annexe?.[annexe] || [];
    return all.filter((c: any) => {
      const matchAppl = !this.filtreApplicable ||
        (this.filtreApplicable === 'inclus'  &&  c.inclus) ||
        (this.filtreApplicable === 'exclus'  && !c.inclus);
      const matchImpl = !this.filtreStatutImpl || c.statut_impl === this.filtreStatutImpl;
      const matchTxt  = !this.filtreTexte ||
        c.controle_label.toLowerCase().includes(this.filtreTexte.toLowerCase()) ||
        c.controle_id.toLowerCase().includes(this.filtreTexte.toLowerCase());
      return matchAppl && matchImpl && matchTxt;
    });
  }

  getAnnexeStats(annexe: string): { total: number, inclus: number } {
    const all = this.soa?.par_annexe?.[annexe] || [];
    return {
      total:  all.length,
      inclus: all.filter((c: any) => c.inclus).length
    };
  }

  getImplSelectClass(v: string): string {
    const m: Record<string, string> = {
      planifie:   'select-blue',
      en_cours:   'select-amber',
      implemente: 'select-green'
    };
    return m[v] || '';
  }
  
}