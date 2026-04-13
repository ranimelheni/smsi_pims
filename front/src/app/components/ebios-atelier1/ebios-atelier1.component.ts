import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder,
         FormGroup, FormArray, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-ebios-atelier1',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './ebios-atelier1.component.html',
  styleUrls:  ['./ebios-atelier1.component.scss']
})
export class EbiosAtelier1Component implements OnInit {

  @Input()  analyse:  any = null;
  @Output() refreshAnalyse = new EventEmitter<void>();

  data: any = null;
  loading  = false;
  success  = '';
  error    = '';

  // Formulaires
  societeForm!: FormGroup;
  entiteForm!:  FormGroup;
  vmForm!:      FormGroup;
  bsForm!:      FormGroup;
  erForm!:      FormGroup;
  socleForm!:   FormGroup;

  // Panels
  showSocieteForm  = false;
  showEntiteForm   = false;
  showVmForm       = false;
  showBsForm       = false;
  showErForm       = false;
  showSocleForm    = false;
  editingEntite:   any = null;
  editingVm:       any = null;
  editingBs:       any = null;
  editingEr:       any = null;
  editingSocle:    any = null;

  natures     = ['processus','information','service','produit'];
  typesBien   = ['materiel','logiciel','reseau','donnees','humain','site','cloud'];
  typesEntite = ['interne','externe','partenaire','client','fournisseur'];
  typesImpact = ['financier','juridique','image','operationnel','humain','autre'];
  typesRef    = ['Norme internationale','Politique interne','Règlementation','Procédure','Standard'];
  etatsApp    = [
    { value: 'applique',     label: 'Appliqué',      class: 'badge-green' },
    { value: 'partiel',      label: 'Partiel',        class: 'badge-amber' },
    { value: 'non_applique', label: 'Non appliqué',   class: 'badge-red'   }
  ];

  private api = 'http://localhost:8080/api/ebios';

  constructor(private fb: FormBuilder, private http: HttpClient) {}

  ngOnInit(): void {
    this.initForms();
    this.load();
  }

  initForms(): void {
    this.societeForm = this.fb.group({
      nom_societe: [''],
      adresse:     [''],
      contact:     [''],
      mission:     ['']
    });

    this.entiteForm = this.fb.group({
      nom_entite:  ['', Validators.required],
      type_entite: ['interne'],
      responsable: [''],
      description: ['']
    });

    this.vmForm = this.fb.group({
      denomination:    ['', Validators.required],
      mission:         [''],
      nature:          ['processus'],
      entite_id:       [''],
      description:     [''],
      besoins_securite: this.fb.group({
        confidentialite: [false],
        integrite:       [false],
        disponibilite:   [false]
      })
    });

    this.bsForm = this.fb.group({
      denomination:     ['', Validators.required],
      type_bien:        ['materiel'],
      valeur_metier_id: ['', Validators.required],
      entite_id:        [''],
      responsable:      [''],
      description:      ['']
    });

    this.erForm = this.fb.group({
      libelle:         ['', Validators.required],
      valeur_metier_id:['', Validators.required],
      gravite:         [2],
      impacts:         this.fb.array([]),
      besoins_securite: this.fb.group({
        confidentialite: [false],
        integrite:       [false],
        disponibilite:   [false]
      })
    });

    this.socleForm = this.fb.group({
      nom_referentiel:  ['', Validators.required],
      type_referentiel: [''],
      etat_application: ['partiel'],
      ecarts:           this.fb.array([])
    });
  }

  // ── FormArrays ───────────────────────────────────────────────────────────
  get impactsArr():  FormArray { return this.erForm.get('impacts')    as FormArray; }
  get ecartsArr():   FormArray { return this.socleForm.get('ecarts')   as FormArray; }

  addImpact():  void { this.impactsArr.push(this.fb.group({ type: ['financier'], description: ['', Validators.required] })); }
  removeImpact(i: number): void { this.impactsArr.removeAt(i); }

  addEcart():   void { this.ecartsArr.push(this.fb.group({ ecart: ['', Validators.required], justification: [''] })); }
  removeEcart(i: number): void { this.ecartsArr.removeAt(i); }

  // ── Load ─────────────────────────────────────────────────────────────────
  load(): void {
    this.loading = true;
    this.http.get<any>(`${this.api}/atelier1`).subscribe({
      next: (d) => {
        this.data    = d;
        this.loading = false;
        // Pré-remplir société si existante
        if (d.societe_mission) {
          this.societeForm.patchValue(d.societe_mission);
          this.showSocieteForm = true;
        }
      },
      error: () => { this.loading = false; }
    });
  }

  // ── Société ──────────────────────────────────────────────────────────────
  saveSociete(): void {
    this.http.post(`${this.api}/societe-mission`, this.societeForm.value).subscribe({
      next: () => {
        this.success = 'Informations société sauvegardées';
        this.load();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => { this.error = err.error?.error || 'Erreur'; }
    });
  }

  // ── Entités ───────────────────────────────────────────────────────────────
  submitEntite(): void {
    if (this.entiteForm.invalid) { this.entiteForm.markAllAsTouched(); return; }
    const url    = this.editingEntite
      ? `${this.api}/entites/${this.editingEntite.id}`
      : `${this.api}/entites`;
    const method = this.editingEntite ? 'put' : 'post';

    (this.http as any)[method](url, this.entiteForm.value).subscribe({
      next: () => {
        this.showEntiteForm = false;
        this.editingEntite  = null;
        this.entiteForm.reset({ type_entite: 'interne' });
        this.success = 'Entité enregistrée';
        this.load();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err: any) => { this.error = err.error?.error || 'Erreur'; }
    });
  }

  editEntite(e: any): void {
    this.editingEntite  = e;
    this.showEntiteForm = true;
    this.entiteForm.patchValue(e);
  }

  deleteEntite(id: number): void {
    if (!confirm('Supprimer cette entité ?')) return;
    this.http.delete(`${this.api}/entites/${id}`).subscribe({
      next: () => this.load()
    });
  }

  // ── Valeurs métier ────────────────────────────────────────────────────────
  submitVM(): void {
    if (this.vmForm.invalid) { this.vmForm.markAllAsTouched(); return; }
    const url    = this.editingVm
      ? `${this.api}/valeurs-metier/${this.editingVm.id}`
      : `${this.api}/valeurs-metier`;
    const method = this.editingVm ? 'put' : 'post';

    const payload = {
      ...this.vmForm.value,
      entite_id: this.vmForm.value.entite_id || null
    };

    (this.http as any)[method](url, payload).subscribe({
      next: () => {
        this.showVmForm = false;
        this.editingVm  = null;
        this.vmForm.reset({ nature: 'processus' });
        this.success = 'Valeur métier enregistrée';
        this.load();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err: any) => { this.error = err.error?.error || 'Erreur'; }
    });
  }

  editVM(v: any): void {
    this.editingVm  = v;
    this.showVmForm = true;
    this.vmForm.patchValue({
      ...v,
      besoins_securite: v.besoins_securite || { confidentialite: false, integrite: false, disponibilite: false }
    });
  }

  deleteVM(id: number): void {
    if (!confirm('Supprimer cette valeur métier ? Les biens supports associés seront aussi supprimés.')) return;
    this.http.delete(`${this.api}/valeurs-metier/${id}`).subscribe({
      next: () => this.load()
    });
  }

  // ── Biens supports ────────────────────────────────────────────────────────
  submitBS(): void {
    if (this.bsForm.invalid) { this.bsForm.markAllAsTouched(); return; }
    const url    = this.editingBs
      ? `${this.api}/biens-support/${this.editingBs.id}`
      : `${this.api}/biens-support`;
    const method = this.editingBs ? 'put' : 'post';

    (this.http as any)[method](url, this.bsForm.value).subscribe({
      next: () => {
        this.showBsForm = false;
        this.editingBs  = null;
        this.bsForm.reset({ type_bien: 'materiel' });
        this.success = 'Bien support enregistré';
        this.load();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err: any) => { this.error = err.error?.error || 'Erreur'; }
    });
  }

  editBS(b: any): void {
    this.editingBs  = b;
    this.showBsForm = true;
    this.bsForm.patchValue(b);
  }

  deleteBS(id: number): void {
    if (!confirm('Supprimer ce bien support ?')) return;
    this.http.delete(`${this.api}/biens-support/${id}`).subscribe({
      next: () => this.load()
    });
  }

  // ── Événements redoutés ───────────────────────────────────────────────────
  submitER(): void {
    if (this.erForm.invalid) { this.erForm.markAllAsTouched(); return; }
    const url    = this.editingEr
      ? `${this.api}/evenements-redoutes/${this.editingEr.id}`
      : `${this.api}/evenements-redoutes`;
    const method = this.editingEr ? 'put' : 'post';

    // Transformer besoins_securite en tableau
    const bs = this.erForm.value.besoins_securite;
    const bsArray = Object.keys(bs).filter(k => bs[k]);

    const payload = {
      ...this.erForm.value,
      besoins_securite: bsArray,
      impacts: this.erForm.value.impacts || []
    };

    (this.http as any)[method](url, payload).subscribe({
      next: () => {
        this.showErForm = false;
        this.editingEr  = null;
        this.erForm.reset({ gravite: 2 });
        this.impactsArr.clear();
        this.success = 'Événement redouté enregistré';
        this.load();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err: any) => { this.error = err.error?.error || 'Erreur'; }
    });
  }

  editER(er: any): void {
    this.editingEr  = er;
    this.showErForm = true;
    this.impactsArr.clear();

    // Reconstruire les impacts
    if (Array.isArray(er.impacts)) {
      er.impacts.forEach((imp: any) => {
        this.impactsArr.push(this.fb.group({
          type:        [imp.type        || 'financier'],
          description: [imp.description || '']
        }));
      });
    }

    // Reconstruire besoins_securite (tableau → objet)
    const bs = { confidentialite: false, integrite: false, disponibilite: false };
    if (Array.isArray(er.besoins_securite)) {
      er.besoins_securite.forEach((k: string) => { (bs as any)[k] = true; });
    }

    this.erForm.patchValue({ ...er, besoins_securite: bs });
  }

  deleteER(id: number): void {
    if (!confirm('Supprimer cet événement redouté ?')) return;
    this.http.delete(`${this.api}/evenements-redoutes/${id}`).subscribe({
      next: () => this.load()
    });
  }

  // ── Socle sécurité ────────────────────────────────────────────────────────
  submitSocle(): void {
    if (this.socleForm.invalid) { this.socleForm.markAllAsTouched(); return; }
    const url    = this.editingSocle
      ? `${this.api}/socle-securite/${this.editingSocle.id}`
      : `${this.api}/socle-securite`;
    const method = this.editingSocle ? 'put' : 'post';

    (this.http as any)[method](url, this.socleForm.value).subscribe({
      next: () => {
        this.showSocleForm = false;
        this.editingSocle  = null;
        this.socleForm.reset({ etat_application: 'partiel' });
        this.ecartsArr.clear();
        this.success = 'Référentiel enregistré';
        this.load();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err: any) => { this.error = err.error?.error || 'Erreur'; }
    });
  }

  editSocle(s: any): void {
    this.editingSocle  = s;
    this.showSocleForm = true;
    this.ecartsArr.clear();
    if (Array.isArray(s.ecarts)) {
      s.ecarts.forEach((ec: any) => {
        this.ecartsArr.push(this.fb.group({
          ecart:         [ec.ecart         || ''],
          justification: [ec.justification || '']
        }));
      });
    }
    this.socleForm.patchValue(s);
  }

  deleteSocle(id: number): void {
    if (!confirm('Supprimer ce référentiel ?')) return;
    this.http.delete(`${this.api}/socle-securite/${id}`).subscribe({
      next: () => this.load()
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  get listeEntites():  any[] { return this.data?.entites         || []; }
  get listeVM():       any[] { return this.data?.valeurs_metier  || []; }
  get listeBS():       any[] { return this.data?.biens_support   || []; }
  get listeER():       any[] { return this.data?.evenements_redoutes || []; }
  get listeSocle():    any[] { return this.data?.socle_securite  || []; }

  getBsForVM(vmId: number): any[] {
    return this.listeBS.filter(b => b.valeur_metier_id === vmId);
  }

  getErForVM(vmId: number): any[] {
    return this.listeER.filter(er => er.valeur_metier_id === vmId);
  }

  getEtatClass(v: string): string {
    return this.etatsApp.find(e => e.value === v)?.class || 'badge-gray';
  }

  getEtatLabel(v: string): string {
    return this.etatsApp.find(e => e.value === v)?.label || v;
  }

  getGraviteClass(g: number): string {
    const m: Record<number, string> = {
      1: 'g-negligeable', 2: 'g-limite', 3: 'g-important', 4: 'g-critique'
    };
    return m[g] || '';
  }

  getGraviteLabel(g: number): string {
    const m: Record<number, string> = {
      1: 'Négligeable', 2: 'Limitée', 3: 'Importante', 4: 'Critique'
    };
    return m[g] || '—';
  }

  getTypeBienIcon(t: string): string {
    const m: Record<string, string> = {
      materiel: '🖥️', logiciel: '💿', reseau: '🌐',
      donnees: '🗄️', humain: '👤', site: '🏢', cloud: '☁️'
    };
    return m[t] || '📦';
  }

  get nombreBesoinsTotaux(): number {
    let total = 0;
    this.listeVM.forEach(vm => {
      const bs = vm.besoins_securite || {};
      if (bs.confidentialite) total++;
      if (bs.integrite)       total++;
      if (bs.disponibilite)   total++;
    });
    return total;
  }
}