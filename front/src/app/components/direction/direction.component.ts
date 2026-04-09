import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder,
         FormGroup, FormArray, Validators } from '@angular/forms';
import { HttpClient, HttpEventType } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-direction',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './direction.component.html',
  styleUrls: ['./direction.component.scss']
})
export class DirectionComponent implements OnInit {

  today: Date = new Date();
  currentUser: any = null;
activeTab: 'validation' | 'organigramme' | 'politique' | 'roles' | 'sensibilisation' | 'indicateurs' | 'soa' | 'methodologie' = 'validation';

  // Clause 4 RSSI (lecture seule pour validation)
  clause4:    any = null;
  clause5:    any = null;
  loading     = false;

  // Validation
  validationForm!: FormGroup;
  savingValidation = false;

  // Politique
  politiqueForm!: FormGroup;
  savingPolitique = false;

  // Rôles et ressources
  rolesForm!: FormGroup;
  savingRoles = false;

  // Organigramme
  organigramme:    any    = null;
  uploadProgress   = 0;
  uploading        = false;
  selectedFile:    File | null = null;
  previewUrl:      string | null = null;

  // Sensibilisation
  sensibilisation: any[]  = [];
  sensStats:       any    = null;
  acteurs:         any[]  = [];
  loadingSens      = false;
  showAddSens      = false;
  sensForm!:       FormGroup;
  addingSens       = false;
  editingSens:     any    = null;

  // Indicateurs
  indicateursForm!: FormGroup;
  savingIndicateurs = false;

  success = '';
  error   = '';

private api = 'http://localhost:8080/api';
  typesFormation = [
    { value: 'formation',  label: 'Formation présentielle' },
    { value: 'atelier',    label: 'Atelier / Workshop' },
    { value: 'e_learning', label: 'E-learning' },
    { value: 'document',   label: 'Document / Support' },
    { value: 'autre',      label: 'Autre' }
  ];

  statutsSens = [
    { value: 'planifie',    label: 'Planifié',     color: 'blue'  },
    { value: 'en_cours',    label: 'En cours',     color: 'amber' },
    { value: 'realise',     label: 'Réalisé',      color: 'green' },
    { value: 'non_realise', label: 'Non réalisé',  color: 'red'   }
  ];

  constructor(
    private fb:   FormBuilder,
    private auth: AuthService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.currentUser = this.auth.getCurrentUser();
    this.initForms();
    this.loadAll();
  }

  initForms(): void {
    this.validationForm = this.fb.group({
      section:     ['enjeux_externes'],
      decision:    ['approuve'],
      commentaire: ['']
    });

    this.politiqueForm = this.fb.group({
      politique_securite_contenu: ['', Validators.required],
      politique_diffusion:        this.fb.array([]),
      objectifs_securite_metier:  this.fb.array([])
    });

    this.rolesForm = this.fb.group({
      exigences_processus: this.fb.array([]),
      ressources_smsi:     this.fb.array([])
    });

    this.indicateursForm = this.fb.group({
      indicateurs_smsi: this.fb.array([])
    });

    this.sensForm = this.fb.group({
      acteur_id:        ['', Validators.required],
      titre:            ['', Validators.required],
      type:             ['formation', Validators.required],
      date_echeance:    [''],
      date_realisation: [''],
      statut:           ['planifie'],
      score:            [''],
      commentaire:      ['']
    });
  }

  // ── FormArray getters ─────────────────────────────────────────────────────
  get politiqueDiffusion():       FormArray { return this.politiqueForm.get('politique_diffusion')       as FormArray; }
  get objectifsSecuriteMetier():  FormArray { return this.politiqueForm.get('objectifs_securite_metier') as FormArray; }
  get exigencesProcessus():       FormArray { return this.rolesForm.get('exigences_processus')           as FormArray; }
  get ressourcesSmsi():           FormArray { return this.rolesForm.get('ressources_smsi')               as FormArray; }
  get indicateursSmsi():          FormArray { return this.indicateursForm.get('indicateurs_smsi')        as FormArray; }

  newDiffusion()  { return this.fb.group({ canal: ['', Validators.required], date: [''], destinataires: [''] }); }
  newObjectifLien(){ return this.fb.group({ objectif_securite: ['', Validators.required], objectif_metier: ['', Validators.required], lien_description: [''] }); }
  newExigence()   { return this.fb.group({ processus: ['', Validators.required], exigence_securite: ['', Validators.required], responsable: [''], statut: ['a_traiter'] }); }
  newRessource()  { return this.fb.group({ type: ['humaine'], description: ['', Validators.required], quantite: [''], allouee: [false], responsable: [''] }); }
  newIndicateur() { return this.fb.group({ nom: ['', Validators.required], valeur: [''], cible: [''], periode: ['mensuelle'], statut: ['nominal'] }); }

  addTo(arr: FormArray, item: any): void { arr.push(item); }
  removeFrom(arr: FormArray, i: number): void { arr.removeAt(i); }

  // ── Load ──────────────────────────────────────────────────────────────────
  loadAll(): void {
    this.loading = true;
    // Clause 4 (RSSI)
    this.http.get<any>(`${this.api}/clause4`).subscribe({
      next: (d) => { this.clause4 = d; this.loading = false; }
    });
    // Clause 5 (direction)
    this.http.get<any>(`${this.api}/clause5`).subscribe({
      next: (d) => {
        this.clause5 = d;
        if (d && Object.keys(d).length > 0) this.patchForms(d);
      }
    });
    // Organigramme
    this.http.get<any>(`${this.api}/clause5/organigramme`).subscribe({
      next: (d) => { if (d && d.id) this.organigramme = d; }
    });
    // Sensibilisation
    this.loadSensibilisation();
    // Acteurs
    this.http.get<any[]>(`${this.api}/actors`).subscribe({
      next: (d) => { this.acteurs = d || []; }
    });
  }

  patchForms(data: any): void {
    // Politique
    this.politiqueForm.patchValue({
      politique_securite_contenu: data.politique_securite_contenu || ''
    });

    const arrays: [string, FormArray, () => any][] = [
      ['politique_diffusion',      this.politiqueDiffusion,      () => this.newDiffusion()],
      ['objectifs_securite_metier',this.objectifsSecuriteMetier, () => this.newObjectifLien()],
      ['exigences_processus',      this.exigencesProcessus,      () => this.newExigence()],
      ['ressources_smsi',          this.ressourcesSmsi,          () => this.newRessource()],
      ['indicateurs_smsi',         this.indicateursSmsi,         () => this.newIndicateur()]
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

  loadSensibilisation(): void {
    this.loadingSens = true;
    this.http.get<any>(`${this.api}/clause5/sensibilisation`).subscribe({
      next: (d) => {
        this.sensibilisation = d.items || [];
        this.sensStats       = d.stats || null;
        this.loadingSens     = false;
      },
      error: () => { this.loadingSens = false; }
    });
  }

  // ── Validation clause 4 ───────────────────────────────────────────────────
  submitValidation(section: string, decision: string, commentaire: string): void {
    this.savingValidation = true;
    this.http.put(`${this.api}/clause5/valider-section`, { section, decision, commentaire }).subscribe({
      next: (res: any) => {
        this.savingValidation = false;
        if (!this.clause5) this.clause5 = {};
        this.clause5['validation_' + section] = decision;
        this.clause5['commentaire_' + section] = commentaire;
        this.success = `Section "${section}" : ${decision}`;
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => {
        this.savingValidation = false;
        this.error = err.error?.error || 'Erreur validation';
      }
    });
  }

  // ── Politique ─────────────────────────────────────────────────────────────
  savePolitique(): void {
    this.savingPolitique = true;
    this.http.post(`${this.api}/clause5`, this.politiqueForm.value).subscribe({
      next: (d: any) => {
        this.savingPolitique = false;
        this.clause5 = d;
        this.success = 'Politique sauvegardée';
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => {
        this.savingPolitique = false;
        this.error = err.error?.error || 'Erreur';
      }
    });
  }

  // ── Rôles ─────────────────────────────────────────────────────────────────
  saveRoles(): void {
    this.savingRoles = true;
    this.http.post(`${this.api}/clause5`, this.rolesForm.value).subscribe({
      next: () => {
        this.savingRoles = false;
        this.success = 'Rôles et ressources sauvegardés';
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => {
        this.savingRoles = false;
        this.error = err.error?.error || 'Erreur';
      }
    });
  }

  // ── Indicateurs ───────────────────────────────────────────────────────────
  saveIndicateurs(): void {
    this.savingIndicateurs = true;
    this.http.post(`${this.api}/clause5`, this.indicateursForm.value).subscribe({
      next: () => {
        this.savingIndicateurs = false;
        this.success = 'Indicateurs sauvegardés';
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => {
        this.savingIndicateurs = false;
        this.error = err.error?.error || 'Erreur';
      }
    });
  }

  // ── Organigramme ──────────────────────────────────────────────────────────
  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) {
      this.selectedFile = input.files[0];
      if (this.selectedFile.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = (e) => { this.previewUrl = e.target?.result as string; };
        reader.readAsDataURL(this.selectedFile);
      } else {
        this.previewUrl = null;
      }
    }
  }

  uploadOrganigramme(): void {
    if (!this.selectedFile) return;
    this.uploading = true;
    this.uploadProgress = 0;

    const formData = new FormData();
    formData.append('fichier', this.selectedFile);

    this.http.post<any>(`${this.api}/clause5/organigramme`, formData, {
      reportProgress: true, observe: 'events'
    }).subscribe({
      next: (event) => {
        if (event.type === HttpEventType.UploadProgress && event.total) {
          this.uploadProgress = Math.round(100 * event.loaded / event.total);
        } else if (event.type === HttpEventType.Response) {
          this.uploading = false;
          this.organigramme = event.body;
          this.selectedFile = null;
          this.previewUrl   = null;
          this.success = 'Organigramme uploadé';
          setTimeout(() => this.success = '', 3000);
        }
      },
      error: (err) => {
        this.uploading = false;
        this.error = err.error?.error || 'Erreur upload';
      }
    });
  }

  downloadOrganigramme(): void {
    this.http.get(`${this.api}/clause5/organigramme/download`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const url  = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href  = url;
        link.download = this.organigramme?.nom_fichier || 'organigramme';
        link.click();
        URL.revokeObjectURL(url);
      }
    });
  }

  // ── Sensibilisation ───────────────────────────────────────────────────────
  submitSens(): void {
    if (this.sensForm.invalid) { this.sensForm.markAllAsTouched(); return; }
    this.addingSens = true;

    const url = this.editingSens
      ? `${this.api}/clause5/sensibilisation/${this.editingSens.id}`
      : `${this.api}/clause5/sensibilisation`;
    const method = this.editingSens ? 'put' : 'post';

    (this.http as any)[method](url, this.sensForm.value).subscribe({
      next: () => {
        this.addingSens  = false;
        this.showAddSens = false;
        this.editingSens = null;
        this.sensForm.reset({ type: 'formation', statut: 'planifie' });
        this.success = this.editingSens ? 'Mis à jour' : 'Action de sensibilisation ajoutée';
        this.loadSensibilisation();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err: any) => {
        this.addingSens = false;
        this.error = err.error?.error || 'Erreur';
      }
    });
  }

  editSens(s: any): void {
    this.editingSens  = s;
    this.showAddSens  = true;
    this.sensForm.patchValue(s);
  }

  deleteSens(id: number): void {
    if (!confirm('Supprimer cette action de sensibilisation ?')) return;
    this.http.delete(`${this.api}/clause5/sensibilisation/${id}`).subscribe({
      next: () => { this.loadSensibilisation(); }
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  getValidationClass(v: string): string {
    const m: Record<string, string> = {
      approuve: 'badge-green', rejete: 'badge-red', en_attente: 'badge-amber'
    };
    return m[v] || 'badge-gray';
  }

  getValidationLabel(v: string): string {
    const m: Record<string, string> = {
      approuve: '✓ Approuvé', rejete: '✗ Rejeté', en_attente: '⏳ En attente'
    };
    return m[v] || v;
  }

  getSensStatutClass(s: string): string {
    const m: Record<string, string> = {
      planifie: 'badge-blue', en_cours: 'badge-amber',
      realise: 'badge-green', non_realise: 'badge-red'
    };
    return m[s] || 'badge-gray';
  }

  getSensStatutLabel(s: string): string {
    return this.statutsSens.find(x => x.value === s)?.label || s;
  }

  getActeurNom(id: number): string {
    const a = this.acteurs.find(x => x.id === id);
    return a ? `${a.prenom} ${a.nom}` : '—';
  }

  get toutValide(): boolean {
    if (!this.clause5) return false;
    return ['enjeux_externes','enjeux_internes','parties','perimetre','ressources']
      .every(s => this.clause5['validation_' + s] === 'approuve');
  }

  sections41 = [
    { key: 'enjeux_externes', label: 'Enjeux externes',  desc: '4.1 — Facteurs externes impactant le SI' },
    { key: 'enjeux_internes', label: 'Enjeux internes',  desc: '4.1 — Facteurs internes impactant le SI' },
    { key: 'parties',         label: 'Parties intéressées', desc: '4.2 — Exigences des parties prenantes' },
    { key: 'perimetre',       label: 'Périmètre SMSI',   desc: '4.3 — Domaine d\'application' },
    { key: 'ressources',      label: 'Ressources & politiques', desc: '4.4 — Engagements et ressources' }
  ];
  isOverdue(date: string): boolean {
  if (!date) return false;

  const d = new Date(date);
  d.setHours(0,0,0,0);

  const t = new Date(this.today);
  t.setHours(0,0,0,0);

  return d < t;
}
// Ajouter dans DirectionComponent

soaData: any = null;
validantSoa = false;
commentaireSoa = '';

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
// Ajouter dans DirectionComponent

methodo:          any    = null;
validantMethodo   = false;
commentaireMeth   = '';

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