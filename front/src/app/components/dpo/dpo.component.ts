import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-dpo',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './dpo.component.html',
  styleUrls: ['./dpo.component.css']
})
export class DpoComponent implements OnInit {
  currentUser: any = null;
  fiches:      any[] = [];
  selectedFiche: any = null;
  loading    = false;
  saving     = false;
  submitting = false;
  success    = '';
  error      = '';

  form!: FormGroup;

  private api = 'http://localhost:8080/api/fiches';

  basesLegales = [
    { value: 'consentement',       label: 'Consentement (Art. 6.1.a)' },
    { value: 'execution_contrat',  label: 'Exécution d\'un contrat (Art. 6.1.b)' },
    { value: 'obligation_legale',  label: 'Obligation légale (Art. 6.1.c)' },
    { value: 'interet_vital',      label: 'Intérêts vitaux (Art. 6.1.d)' },
    { value: 'mission_publique',   label: 'Mission d\'intérêt public (Art. 6.1.e)' },
    { value: 'interet_legitime',   label: 'Intérêt légitime (Art. 6.1.f)' }
  ];

  categoriesPersonnes = [
    'Clients', 'Employés', 'Prospects', 'Fournisseurs',
    'Partenaires', 'Visiteurs', 'Candidats', 'Mineurs'
  ];

  categoriesDonnees = [
    'État civil / Identité', 'Données de contact', 'Données financières',
    'Données de connexion / Logs', 'Données de localisation',
    'Vie personnelle', 'Données professionnelles'
  ];

  donneesSensibles = [
    'Origine raciale ou ethnique', 'Opinions politiques',
    'Convictions religieuses ou philosophiques', 'Appartenance syndicale',
    'Données génétiques', 'Données biométriques',
    'Données de santé', 'Vie sexuelle / orientation sexuelle',
    'Condamnations pénales', 'NIR (numéro sécurité sociale)'
  ];

  constructor(
    private fb:   FormBuilder,
    private auth: AuthService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.currentUser = this.auth.getCurrentUser();
    this.initForm();
    this.loadFiches();
  }

  initForm(): void {
    this.form = this.fb.group({
      // Base légale
      base_legale:              ['', Validators.required],
      base_legale_detail:       [''],

      // Personnes concernées
      categories_personnes:     [[], Validators.required],
      nombre_personnes:         ['', Validators.required],

      // Données traitées
      categories_donnees:       [[]],
      donnees_sensibles:        [[]],
      donnees_hautement_perso:  [false],
      source_donnees:           ['', Validators.required],

      // Durée de conservation
      duree_conservation:       ['', Validators.required],

      // Droits des personnes
      personnes_informees:      [false],
      modalite_information:     [''],
      modalite_consentement:    [''],
      droit_acces:              [false],
      droit_rectification:      [false],
      droit_opposition:         [false],
      droit_portabilite:        [false],
      droit_limitation:         [false],
      notification_violation:   [false],
      gestion_duree_traitee:    [false],

      // Destinataires
      qui_acces:                [''],
      nombre_acces:             [''],
      partage_inter_services:   [false],

      // Sous-traitants
      sous_traitant_nom:        [''],
      sous_traitant_contrat:    [''],
      sous_traitant_clause:     [false],

      // Transferts hors EU
      transfert_hors_eu:        [false],
      pays_destination:         [''],
      fondement_juridique:      [''],

      // Outil informatique
      outil_nom:                [''],
      outil_description:        [''],
      stockage_format:          ['electronique'],
      stockage_lieu:            [''],
      stockage_pays:            ['France'],

      // Mesures de sécurité
      acces_physique_protege:   [false],
      authentification:         [false],
      journalisation:           [false],
      reseau_interne:           [false],
      chiffrement:              [false],
      autres_mesures:           [''],

      // Responsable de traitement
      responsable_nom:          ['', Validators.required],
      responsable_departement:  [''],
      responsable_region:       [''],
      declaration_cnil:         [''],

      // Analyse de risques RGPD
      pia_realise:              [false],
      actions_conformite:       ['']
    });
  }

  loadFiches(): void {
    this.loading = true;
    this.http.get<any>(`${this.api}/mine`).subscribe({
      next: (data) => {
        this.loading = false;
        this.fiches  = Array.isArray(data) ? data : [data];
      },
      error: () => { this.loading = false; }
    });
  }

  selectFiche(fiche: any): void {
    this.selectedFiche = fiche;
    this.error         = '';
    this.success       = '';

    // Pré-remplir si data_dpo existe déjà
    if (fiche.data_dpo && Object.keys(fiche.data_dpo).length > 0) {
      this.form.patchValue(fiche.data_dpo);
    }
  }

  toggleCheckbox(controlName: string, value: string): void {
    const current: string[] = this.form.get(controlName)?.value || [];
    const idx = current.indexOf(value);
    if (idx === -1) {
      this.form.get(controlName)?.setValue([...current, value]);
    } else {
      this.form.get(controlName)?.setValue(current.filter(v => v !== value));
    }
  }

  isChecked(controlName: string, value: string): boolean {
    return (this.form.get(controlName)?.value || []).includes(value);
  }

  saveDraft(): void {
    if (!this.selectedFiche) return;
    this.saving = true;
    this.http.put(`${this.api}/${this.selectedFiche.id}/dpo`,
      { data_dpo: this.form.value }
    ).subscribe({
      next: () => {
        this.saving  = false;
        this.success = 'Brouillon sauvegardé';
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => {
        this.saving = false;
        this.error  = err.error?.error || 'Erreur lors de la sauvegarde';
      }
    });
  }

  submitDpo(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    if (!this.selectedFiche) return;

    this.submitting = true;

    // 1. Sauvegarder les données DPO
    this.http.put(`${this.api}/${this.selectedFiche.id}/dpo`,
      { data_dpo: this.form.value }
    ).subscribe({
      next: () => {
        // 2. Changer le statut → complete_dpo
        this.http.put(`${this.api}/${this.selectedFiche.id}/statut`,
          { statut: 'complete_dpo' }
        ).subscribe({
          next: (res: any) => {
            this.submitting = false;
            this.success    = 'Section DPO soumise — en attente de validation RSSI';
            this.selectedFiche.statut = res.statut;
            setTimeout(() => this.success = '', 5000);
          },
          error: (err) => {
            this.submitting = false;
            this.error = err.error?.error || 'Erreur lors de la soumission';
          }
        });
      },
      error: (err) => {
        this.submitting = false;
        this.error = err.error?.error || 'Erreur lors de la sauvegarde';
      }
    });
  }

  getStatutClass(statut: string): string {
    const map: Record<string, string> = {
      soumis_dpo:   'badge-amber',
      complete_dpo: 'badge-blue',
      valide:       'badge-green',
      rejete:       'badge-red'
    };
    return map[statut] || 'badge-gray';
  }

  getStatutLabel(statut: string): string {
    const map: Record<string, string> = {
      soumis_dpo:   'En attente DPO',
      complete_dpo: 'Soumis au RSSI',
      valide:       'Validé',
      rejete:       'Rejeté'
    };
    return map[statut] || statut;
  }
}