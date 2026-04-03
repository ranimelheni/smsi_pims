import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-fiche-technique',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './fiche-technique.component.html',
  styleUrls: ['./fiche-technique.component.scss']
})
export class FicheTechniqueComponent implements OnInit {

  currentUser: any  = null;
  fiche:       any  = null;
  loading      = true;
  saving       = false;
  submitting   = false;
  success      = '';
  error        = '';
  activeSection = 0;

  form!: FormGroup;

  private api = 'http://localhost:8080/api/fiches-techniques';

  sections = [
    { label: 'Identification',    icon: '📋' },
    { label: 'Serveurs',          icon: '🖥️' },
    { label: 'Postes & Réseau',   icon: '💻' },
    { label: 'Applications & BDD',icon: '💿' },
    { label: 'Données & Stockage',icon: '🗄️' },
    { label: 'Services & Accès',  icon: '☁️' },
    { label: 'Sécurité',          icon: '🔒' },
  ];

  niveaux = [
    { value: 1, label: 'Public' },
    { value: 2, label: 'Interne' },
    { value: 3, label: 'Confidentiel' },
    { value: 4, label: 'Secret' }
  ];

  constructor(
    private fb:   FormBuilder,
    private auth: AuthService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.currentUser = this.auth.getCurrentUser();
    this.initForm();
    this.loadFiche();
  }

  // ── Form init ──────────────────────────────────────────────────────────────
  initForm(): void {
    this.form = this.fb.group({
      intitule:  ['', Validators.required],
      perimetre: [''],

      // Actifs matériels
      actifs_serveurs: this.fb.array([]),
      actifs_postes:   this.fb.array([]),
      actifs_reseau:   this.fb.array([]),

      // Actifs logiciels
      actifs_applications: this.fb.array([]),
      actifs_licences:     this.fb.array([]),
      actifs_bdd:          this.fb.array([]),

      // Actifs données
      actifs_sauvegardes: this.fb.array([]),
      actifs_stockages:   this.fb.array([]),

      // Actifs services
      actifs_cloud:       this.fb.array([]),
      actifs_acces:       this.fb.array([]),
      actifs_certificats: this.fb.array([]),

      // Mesures de sécurité
      mesures_securite: this.fb.group({
        antivirus:          [false],
        firewall:           [false],
        chiffrement:        [false],
        mfa:                [false],
        patch_management:   [false],
        backup_chiffre:     [false],
        ids_ips:            [false],
        siem:               [false],
        dlp:                [false],
        autres:             ['']
      })
    });
  }

  // ── FormArray getters ─────────────────────────────────────────────────────
  get serveurs():     FormArray { return this.form.get('actifs_serveurs')     as FormArray; }
  get postes():       FormArray { return this.form.get('actifs_postes')       as FormArray; }
  get reseau():       FormArray { return this.form.get('actifs_reseau')       as FormArray; }
  get applications(): FormArray { return this.form.get('actifs_applications') as FormArray; }
  get licences():     FormArray { return this.form.get('actifs_licences')     as FormArray; }
  get bdd():          FormArray { return this.form.get('actifs_bdd')          as FormArray; }
  get sauvegardes():  FormArray { return this.form.get('actifs_sauvegardes')  as FormArray; }
  get stockages():    FormArray { return this.form.get('actifs_stockages')    as FormArray; }
  get cloud():        FormArray { return this.form.get('actifs_cloud')        as FormArray; }
  get acces():        FormArray { return this.form.get('actifs_acces')        as FormArray; }
  get certificats():  FormArray { return this.form.get('actifs_certificats')  as FormArray; }

  // ── Templates FormGroup ───────────────────────────────────────────────────
  newServeur()     { return this.fb.group({ nom: ['', Validators.required], type: ['physique'], os: [''], version: [''], localisation: [''], confidentialite: [2], integrite: [3], disponibilite: [3] }); }
  newPoste()       { return this.fb.group({ nom: ['', Validators.required], type: ['fixe'], nb: [1], os: [''], localisation: [''], confidentialite: [2], integrite: [2], disponibilite: [2] }); }
  newReseau()      { return this.fb.group({ nom: ['', Validators.required], type: ['switch'], marque: [''], modele: [''], localisation: [''], confidentialite: [2], integrite: [3], disponibilite: [4] }); }
  newApplication() { return this.fb.group({ nom: ['', Validators.required], version: [''], editeur: [''], usage: [''], confidentialite: [2], integrite: [3], disponibilite: [3] }); }
  newLicence()     { return this.fb.group({ nom: ['', Validators.required], nb_licences: [1], expiration: [''], editeur: [''] }); }
  newBdd()         { return this.fb.group({ nom: ['', Validators.required], type: ['postgresql'], version: [''], serveur: [''], confidentialite: [3], integrite: [4], disponibilite: [3] }); }
  newSauvegarde()  { return this.fb.group({ nom: ['', Validators.required], type: ['complete'], frequence: ['quotidienne'], retention: ['30 jours'], lieu: [''] }); }
  newStockage()    { return this.fb.group({ nom: ['', Validators.required], type: ['nas'], capacite: [''], lieu: [''], confidentialite: [3], integrite: [3], disponibilite: [3] }); }
  newCloud()       { return this.fb.group({ nom: ['', Validators.required], fournisseur: [''], type: ['SaaS'], region: [''], confidentialite: [2], integrite: [2], disponibilite: [3] }); }
  newAcces()       { return this.fb.group({ nom: ['', Validators.required], type: ['VPN'], utilisateurs: [''], confidentialite: [3], integrite: [3], disponibilite: [3] }); }
  newCertificat()  { return this.fb.group({ nom: ['', Validators.required], autorite: [''], expiration: [''], usage: [''] }); }

  add(arr: FormArray, item: any): void { arr.push(item); }
  remove(arr: FormArray, i: number): void { arr.removeAt(i); }

  // ── Load ──────────────────────────────────────────────────────────────────
  loadFiche(): void {
    this.loading = true;
    this.http.get<any>(`${this.api}/mine`).subscribe({
      next: (data) => {
        this.fiche   = data;
        this.loading = false;
        this.patchForm(data);
      },
      error: () => { this.loading = false; }
    });
  }

  patchForm(data: any): void {
    this.form.patchValue({
      intitule:  data.intitule  || '',
      perimetre: data.perimetre || '',
      mesures_securite: data.mesures_securite || {}
    });

    // Peupler FormArrays
    const arrays: [string, FormArray, () => FormGroup][] = [
      ['actifs_serveurs',     this.serveurs,     () => this.newServeur()],
      ['actifs_postes',       this.postes,       () => this.newPoste()],
      ['actifs_reseau',       this.reseau,       () => this.newReseau()],
      ['actifs_applications', this.applications, () => this.newApplication()],
      ['actifs_licences',     this.licences,     () => this.newLicence()],
      ['actifs_bdd',          this.bdd,          () => this.newBdd()],
      ['actifs_sauvegardes',  this.sauvegardes,  () => this.newSauvegarde()],
      ['actifs_stockages',    this.stockages,    () => this.newStockage()],
      ['actifs_cloud',        this.cloud,        () => this.newCloud()],
      ['actifs_acces',        this.acces,        () => this.newAcces()],
      ['actifs_certificats',  this.certificats,  () => this.newCertificat()],
    ];

    arrays.forEach(([key, arr, factory]) => {
      arr.clear();
      const items = data[key] || [];
      items.forEach((item: any) => {
        const g = factory();
        g.patchValue(item);
        arr.push(g);
      });
    });
  }

  // ── Save ──────────────────────────────────────────────────────────────────
  save(): void {
    if (!this.fiche) return;
    this.saving = true;
    this.error  = '';
    this.http.put(`${this.api}/${this.fiche.id}`, this.form.value).subscribe({
      next: (data: any) => {
        this.saving  = false;
        this.fiche   = data;
        this.success = 'Fiche sauvegardée';
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => {
        this.saving = false;
        this.error  = err.error?.error || 'Erreur de sauvegarde';
      }
    });
  }

  submit(): void {
    if (!this.fiche) return;
    this.submitting = true;
    // Sauvegarder d'abord puis changer le statut
    this.http.put(`${this.api}/${this.fiche.id}`, this.form.value).subscribe({
      next: () => {
        this.http.put(`${this.api}/${this.fiche.id}/statut`, { statut: 'soumis_rssi' }).subscribe({
          next: (res: any) => {
            this.submitting      = false;
            this.fiche.statut    = res.statut;
            this.success = 'Fiche soumise au RSSI pour validation';
            setTimeout(() => this.success = '', 5000);
          },
          error: (err) => {
            this.submitting = false;
            this.error = err.error?.error || 'Erreur de soumission';
          }
        });
      },
      error: (err) => {
        this.submitting = false;
        this.error = err.error?.error || 'Erreur de sauvegarde';
      }
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  getStatutLabel(s: string): string {
    const m: Record<string, string> = {
      brouillon: 'Brouillon', soumis_rssi: 'Soumis au RSSI',
      valide: 'Validée', rejete: 'Rejetée'
    };
    return m[s] || s;
  }

  getStatutClass(s: string): string {
    const m: Record<string, string> = {
      brouillon: 'badge-gray', soumis_rssi: 'badge-amber',
      valide: 'badge-green',   rejete: 'badge-red'
    };
    return m[s] || 'badge-gray';
  }

  canEdit(): boolean {
    return ['brouillon', 'rejete'].includes(this.fiche?.statut);
  }
}